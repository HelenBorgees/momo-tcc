#include <Arduino.h>
#include <Wire.h>
#include <Protocentral_FDC1004.h>
#include <WiFi.h>
#include <WebServer.h>
#include <Preferences.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <ArduinoJson.h>
#include <time.h>
#include <cstring>
#include <cctype>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/semphr.h>
#include "momo_inferencia.h"
#include "momo_config.h"
#include "momo_ca.h"
#include "momo_protocol.h"

static constexpr int MOMO_SDA_GPIO = 22;
static constexpr int MOMO_SCL_GPIO = 23;
static constexpr uint32_t INTERVALO_LEITURA_MS = 250UL;
static constexpr uint8_t CAPDAC_FIXO = 0;
static constexpr bool LOG_CAPACITANCIA = false;  // true somente para aquisicao de ensaios.
static constexpr uint32_t INTERVALO_STATUS_MS = 8000UL;
static constexpr uint32_t INTERVALO_COMANDOS_MS = 2500UL;

FDC1004 conversor(FDC1004_RATE_100HZ);
momo::Inferencia modelo;
WebServer servidor(80);
Preferences preferencias;

struct EstadoMomo {
  char estado[24] = "CALIBRANDO";
  char classe[24] = "";
  char ultimoComando[48] = "";
  double c0Pf = 0.0;
  uint32_t cicloFralda = 1;
  uint32_t eventoId = 0;
  uint8_t progresso = 0;
  bool sensorOk = false;
  uint32_t ultimaLeituraMs = 0;
  bool eventoPendente = false;
  bool calibrado = false;
  bool trocaRecomendada = false;
  uint32_t versao = 0;
};

struct ComandoMomo {
  char acao[20] = "";
  char id[48] = "";
};

EstadoMomo statusAtual;
SemaphoreHandle_t mutexStatus;
SemaphoreHandle_t mutexConfiguracao;

int publicarStatus(const String &idToken, const String &ownerUid, const EstadoMomo &estado);
void iniciarNovaFralda(bool novaTroca = false);
QueueHandle_t filaComandos;

String wifiNome;
String wifiSenha;
String uidResponsavel;
String ultimoComandoTratado;

bool sensorInicializado = false;
bool sensorEmFalha = false;
uint8_t falhasConsecutivas = 0;
uint32_t proximaTentativaSensorMs = 0;
bool apAtivo = false;
bool conexaoPendente = false;
bool aguardandoNovaFralda = false;
uint32_t inicioConexaoPendente = 0;
uint32_t ultimaTentativaWifiMs = 0;
uint32_t proximaAquisicaoMs = 0;
uint32_t inicioCalibracaoMs = 0;
momo::Inferencia::Estado estadoAnterior = momo::Inferencia::CALIBRANDO;

void atualizarEstado(const char *estado, const char *classe = nullptr) {
  if (xSemaphoreTake(mutexStatus, portMAX_DELAY) != pdTRUE) return;
  if (estado != nullptr) {
    strlcpy(statusAtual.estado, estado, sizeof(statusAtual.estado));
  }
  if (classe != nullptr) {
    strlcpy(statusAtual.classe, classe, sizeof(statusAtual.classe));
  }
  ++statusAtual.versao;
  xSemaphoreGive(mutexStatus);
}

void iniciarNovaFralda(bool novaTroca) {
  aguardandoNovaFralda = false;
  modelo.reiniciar(millis());
  estadoAnterior = momo::Inferencia::CALIBRANDO;
  inicioCalibracaoMs = millis();
  proximaAquisicaoMs = millis();

  if (xSemaphoreTake(mutexStatus, portMAX_DELAY) == pdTRUE) {
    if (novaTroca) {
      ++statusAtual.cicloFralda;
      preferencias.putUInt("cycle", statusAtual.cicloFralda);
    }
    strlcpy(statusAtual.estado, "CALIBRANDO", sizeof(statusAtual.estado));
    statusAtual.classe[0] = '\0';
    statusAtual.eventoPendente = false;
    statusAtual.c0Pf = 0.0;
    statusAtual.progresso = 0;
    statusAtual.calibrado = false;
    statusAtual.trocaRecomendada = false;
    ++statusAtual.versao;
    xSemaphoreGive(mutexStatus);
  }

  Serial.println("{\"estado\":\"calibrando\",\"detalhe\":\"fralda deve estar seca\"}");
}

void aguardarNovaFralda() {
  aguardandoNovaFralda = true;
  if (xSemaphoreTake(mutexStatus, portMAX_DELAY) == pdTRUE) {
    ++statusAtual.cicloFralda;
    preferencias.putUInt("cycle", statusAtual.cicloFralda);
    strlcpy(statusAtual.estado, "AGUARDANDO_FRALDA", sizeof(statusAtual.estado));
    statusAtual.classe[0] = '\0';
    statusAtual.calibrado = false;
    statusAtual.progresso = 0;
    statusAtual.eventoPendente = false;
    statusAtual.c0Pf = 0.0;
    statusAtual.trocaRecomendada = false;
    ++statusAtual.versao;
    xSemaphoreGive(mutexStatus);
  }
  Serial.println("{\"estado\":\"aguardando_fralda\"}");
}

bool lerCapacitancia(double &valorPf) {
  if (conversor.configureMeasurementSingle(FDC1004_MEASUREMENT_1,
                                           FDC1004_CHANNEL_0,
                                           CAPDAC_FIXO) != FDC1004_SUCCESS) return false;
  if (conversor.triggerSingleMeasurement(FDC1004_MEASUREMENT_1,
                                         FDC1004_RATE_100HZ) != FDC1004_SUCCESS) return false;
  delay(15);

  uint16_t palavras[2] = {0, 0};
  if (conversor.readMeasurement(FDC1004_MEASUREMENT_1, palavras) != FDC1004_SUCCESS)
    return false;

  return momo_protocolo::converterCapacitancia(palavras[0], palavras[1], valorPf);
}

bool uidValido(const String &uid) {
  if (uid.length() < 20 || uid.length() > 100) return false;
  for (unsigned int i = 0; i < uid.length(); ++i) {
    const char c = uid.charAt(i);
    if (!isalnum(static_cast<unsigned char>(c)) && c != '-' && c != '_') return false;
  }
  return true;
}

void responderJson(int codigo, JsonDocument &doc) {
  String corpo;
  serializeJson(doc, corpo);
  servidor.send(codigo, "application/json", corpo);
}

void receberConfiguracaoWifi() {
  const String conteudo = servidor.arg("plain");
  JsonDocument resposta;

  if (conteudo.isEmpty() || conteudo.length() > 512) {
    resposta["sucesso"] = false;
    resposta["mensagem"] = "JSON invalido ou muito extenso";
    responderJson(400, resposta);
    return;
  }

  JsonDocument entrada;
  if (deserializeJson(entrada, conteudo) != DeserializationError::Ok) {
    resposta["sucesso"] = false;
    resposta["mensagem"] = "JSON invalido";
    responderJson(400, resposta);
    return;
  }

  const String novoSsid = entrada["ssid"] | "";
  const String novaSenha = entrada["senha"] | "";
  const String novoUid = entrada["ownerUid"] | "";

  if (novoSsid.isEmpty() || novoSsid.length() > 32 ||
      novaSenha.length() < 8 || novaSenha.length() > 63 ||
      !uidValido(novoUid)) {
    resposta["sucesso"] = false;
    resposta["mensagem"] = "SSID, senha ou usuario invalidos";
    responderJson(400, resposta);
    return;
  }

  // A transferência de responsável exige reset físico/Serial local; o SoftAP
  // não pode trocar silenciosamente o dono de um dispositivo já provisionado.
  if (!uidResponsavel.isEmpty() && uidResponsavel != novoUid) {
    resposta["sucesso"] = false;
    resposta["mensagem"] = "Monitor pertence a outra conta. Requer redefinicao local autorizada.";
    responderJson(409, resposta);
    return;
  }
  JsonDocument configuracao;
  configuracao["ssid"] = novoSsid;
  configuracao["senha"] = novaSenha;
  configuracao["owner"] = novoUid;
  String configuracaoJson;
  serializeJson(configuracao, configuracaoJson);
  if (preferencias.putString("wifiConfig", configuracaoJson) != configuracaoJson.length()) {
    resposta["sucesso"] = false;
    resposta["mensagem"] = "Nao foi possivel persistir configuracao na memoria do monitor";
    responderJson(500, resposta);
    return;
  }
  wifiNome = novoSsid;
  wifiSenha = novaSenha;
  xSemaphoreTake(mutexConfiguracao, portMAX_DELAY);
  uidResponsavel = novoUid;
  xSemaphoreGive(mutexConfiguracao);

  resposta["sucesso"] = true;
  resposta["mensagem"] = "Wi-Fi recebido; reconecte o celular a internet";
  resposta["deviceUid"] = MOMO_FIREBASE_EXPECTED_UID;
  responderJson(200, resposta);

  conexaoPendente = true;
  inicioConexaoPendente = millis();
}

void iniciarPortalPareamento() {
  if (apAtivo) return;

  WiFi.mode(WIFI_AP_STA);
  const IPAddress portalIp(192, 168, 4, 1);
  if (!WiFi.softAPConfig(portalIp, portalIp, IPAddress(255, 255, 255, 0))) {
    Serial.println("ERRO: nao foi possivel configurar IP do SoftAP");
    return;
  }
  if (!WiFi.softAP(MOMO_SETUP_SSID, MOMO_SETUP_PASSWORD)) {
    Serial.println("ERRO: nao foi possivel iniciar MOMO-SETUP");
    return;
  }

  servidor.begin();
  apAtivo = true;
  Serial.print("MOMO-SETUP pronto em ");
  Serial.println(WiFi.softAPIP());
}

void encerrarPortalPareamento() {
  if (!apAtivo) return;
  servidor.stop();
  WiFi.softAPdisconnect(true);
  apAtivo = false;
}

void conectarWifiSalvo() {
  if (wifiNome.isEmpty()) {
    iniciarPortalPareamento();
    return;
  }

  encerrarPortalPareamento();
  WiFi.mode(WIFI_STA);
  WiFi.begin(wifiNome.c_str(), wifiSenha.c_str());
  ultimaTentativaWifiMs = millis();
  Serial.println("ESP32 tentando conectar ao Wi-Fi configurado");
}

bool horarioSincronizado() {
  if (time(nullptr) > 1700000000) return true;
  configTime(0, 0, "pool.ntp.org", "time.google.com");
  for (int i = 0; i < 30 && time(nullptr) < 1700000000; ++i) {
    vTaskDelay(pdMS_TO_TICKS(500));
  }
  return time(nullptr) >= 1700000000;
}

int httpsJson(const String &url, const String &metodo, const String &corpo, String &resposta) {
  WiFiClientSecure cliente;
  cliente.setCACert(MOMO_TRUST_ROOTS);
  cliente.setTimeout(10);

  HTTPClient http;
  if (!http.begin(cliente, url)) return -1;

  http.setTimeout(8000);
  http.addHeader("Content-Type", "application/json");
  http.setFollowRedirects(HTTPC_STRICT_FOLLOW_REDIRECTS);

  int codigo = -1;
  if (metodo == "POST") codigo = http.POST(corpo);
  else if (metodo == "PUT") codigo = http.PUT(corpo);
  else if (metodo == "GET") codigo = http.GET();

  if (codigo > 0) resposta = http.getString();
  http.end();
  return codigo;
}

bool autenticarDispositivo(String &idToken) {
  if (String(MOMO_FIREBASE_API_KEY).startsWith("COLE_") ||
      String(MOMO_FIREBASE_PASSWORD).startsWith("COLE_")) {
    Serial.println("Firebase: configure API KEY e senha tecnica em momo_config.h");
    return false;
  }

  JsonDocument requisicao;
  requisicao["email"] = MOMO_FIREBASE_EMAIL;
  requisicao["password"] = MOMO_FIREBASE_PASSWORD;
  requisicao["returnSecureToken"] = true;
  String corpo;
  serializeJson(requisicao, corpo);

  const String url = String("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=")
                   + MOMO_FIREBASE_API_KEY;
  String resposta;
  const int codigo = httpsJson(url, "POST", corpo, resposta);
  if (codigo != 200) {
    Serial.printf("Firebase Auth: erro HTTP %d\n", codigo);
    return false;
  }

  JsonDocument dados;
  if (deserializeJson(dados, resposta) != DeserializationError::Ok) return false;
  const String uid = dados["localId"] | "";
  const String token = dados["idToken"] | "";
  if (uid != MOMO_FIREBASE_EXPECTED_UID || token.isEmpty()) {
    Serial.println("Firebase Auth: UID tecnico inesperado");
    return false;
  }
  idToken = token;
  Serial.println("Firebase Auth: dispositivo autenticado");
  return true;
}

String urlFirebase(const char *colecao, const String &idToken, const String &ownerUid) {
  return String(MOMO_FIREBASE_DATABASE) + "/" + colecao + "/" +
         ownerUid + ".json?auth=" + idToken;
}

int publicarStatus(const String &idToken, const String &ownerUid, const EstadoMomo &estado) {
  JsonDocument dados;
  dados["deviceUid"] = MOMO_FIREBASE_EXPECTED_UID;
  dados["estado"] = estado.estado;
  const bool leituraRecente = estado.sensorOk && estado.ultimaLeituraMs != 0 &&
      static_cast<uint32_t>(millis() - estado.ultimaLeituraMs) < 2500UL;
  dados["conectado"] = leituraRecente;
  dados["sensorOk"] = leituraRecente;
  dados["erroSensor"] = !leituraRecente;
  dados["eventoPendente"] = estado.eventoPendente;
  // SSID não é inventado nem lido de String compartilhada sem proteção.
  dados["wifiSsid"] = WiFi.SSID();
  dados["versaoFirmware"] = "momo-wifi-1.1";
  dados["calibrado"] = leituraRecente && estado.calibrado;
  dados["progressoCalibracao"] = estado.progresso;
  dados["ultimaClassificacaoAutomatica"] =
      strlen(estado.classe) ? estado.classe : nullptr;
  dados["trocaRecomendada"] = estado.trocaRecomendada;
  dados["c0"] = estado.c0Pf;
  dados["cicloFralda"] = estado.cicloFralda;
  dados["eventoId"] = estado.eventoId;
  dados["lastCommandId"] = estado.ultimoComando;
  JsonObject hora = dados["timestamp"].to<JsonObject>();
  hora[".sv"] = "timestamp";

  String corpo;
  serializeJson(dados, corpo);
  String resposta;
  const int codigo = httpsJson(urlFirebase("deviceStatus", idToken, ownerUid), "PUT", corpo, resposta);
  if (codigo != 200) Serial.printf("Firebase status: HTTP %d\n", codigo);
  return codigo;
}

void consultarComandos(String &idToken, const String &ownerUid) {
  String resposta;
  const int codigo = httpsJson(urlFirebase("deviceCommands", idToken, ownerUid), "GET", "", resposta);
  if (codigo != 200) {
    Serial.printf("Firebase comandos: HTTP %d (confira autenticacao/vinculo/regras)\n", codigo);
    if (codigo == 401) idToken = "";
    return;
  }
  if (resposta == "null") return;

  JsonDocument comando;
  if (deserializeJson(comando, resposta) != DeserializationError::Ok) return;
  const String id = comando["id"] | "";
  const String acao = comando["acao"] | "";
  const int64_t timestamp = comando["timestamp"] | int64_t(0);
  const int64_t agoraMs = static_cast<int64_t>(time(nullptr)) * 1000;
  if (!momo_protocolo::comandoRecente(timestamp, agoraMs) ||
      id.isEmpty() || id.length() >= sizeof(ComandoMomo::id) ||
      (acao != "CALIBRAR" && acao != "TROCA")) return;
  xSemaphoreTake(mutexStatus, portMAX_DELAY);
  const bool jaTratado = id == statusAtual.ultimoComando;
  xSemaphoreGive(mutexStatus);
  if (jaTratado) return;

  ComandoMomo pendente;
  strlcpy(pendente.acao, acao.c_str(), sizeof(pendente.acao));
  strlcpy(pendente.id, id.c_str(), sizeof(pendente.id));
  xQueueSend(filaComandos, &pendente, 0);
}

void tarefaFirebase(void *) {
  String token;
  uint32_t momentoLoginMs = 0;
  uint32_t proximoLoginMs = 0;
  uint32_t proximoStatusMs = 0;
  uint32_t proximoComandoMs = 0;
  uint32_t versaoEnviada = UINT32_MAX;
  uint32_t ultimaPublicacaoMs = 0;
  uint32_t retryStatusMs = 0;

  for (;;) {
    xSemaphoreTake(mutexConfiguracao, portMAX_DELAY);
    const String ownerUid = uidResponsavel;
    xSemaphoreGive(mutexConfiguracao);
    if (WiFi.status() != WL_CONNECTED || !uidValido(ownerUid)) {
      token = "";
      vTaskDelay(pdMS_TO_TICKS(1000));
      continue;
    }

    if (!horarioSincronizado()) {
      vTaskDelay(pdMS_TO_TICKS(5000));
      continue;
    }

    const uint32_t agora = millis();
    if (token.isEmpty() || agora - momentoLoginMs >= 3000000UL) {
      if (static_cast<int32_t>(agora - proximoLoginMs) < 0) {
        vTaskDelay(pdMS_TO_TICKS(1000));
        continue;
      }
      if (!autenticarDispositivo(token)) {
        proximoLoginMs = millis() + 15000UL;
        vTaskDelay(pdMS_TO_TICKS(1000));
        continue;
      }
      momentoLoginMs = millis();
      versaoEnviada = UINT32_MAX;
    }

    if (static_cast<int32_t>(millis() - proximoComandoMs) >= 0) {
      consultarComandos(token, ownerUid);
      proximoComandoMs = millis() + INTERVALO_COMANDOS_MS;
    }

    if (token.isEmpty()) continue;
    EstadoMomo copia;
    if (xSemaphoreTake(mutexStatus, portMAX_DELAY) == pdTRUE) {
      copia = statusAtual;
      xSemaphoreGive(mutexStatus);
    }

    const uint32_t agoraStatus = millis();
    if (static_cast<int32_t>(agoraStatus - retryStatusMs) >= 0 &&
        ((copia.versao != versaoEnviada && agoraStatus - ultimaPublicacaoMs >= 1000UL) ||
         static_cast<int32_t>(agoraStatus - proximoStatusMs) >= 0)) {
      ultimaPublicacaoMs = millis();
      const int codigoStatus = publicarStatus(token, ownerUid, copia);
      if (codigoStatus == 200) {
        versaoEnviada = copia.versao;
        proximoStatusMs = millis() + INTERVALO_STATUS_MS;
      } else {
        if (codigoStatus == 401) { token = ""; proximoLoginMs = 0; }
        retryStatusMs = millis() + 4000UL;
        proximoStatusMs = retryStatusMs;
        versaoEnviada = copia.versao;
      }
    }

    vTaskDelay(pdMS_TO_TICKS(350));
  }
}

void tratarComandosRecebidos() {
  ComandoMomo comando;
  while (xQueueReceive(filaComandos, &comando, 0) == pdTRUE) {
    if (strcmp(comando.id, ultimoComandoTratado.c_str()) == 0) continue;

    const bool troca = strcmp(comando.acao, "TROCA") == 0;
    if (troca) aguardarNovaFralda();
    else {
      if (!sensorInicializado || sensorEmFalha || falhasConsecutivas >= 3) {
        Serial.println("CALIBRAR rejeitado: sensor indisponivel");
        continue;
      }
      iniciarNovaFralda();
    }

    ultimoComandoTratado = comando.id;
    preferencias.putString("lastCmd", ultimoComandoTratado);

    if (xSemaphoreTake(mutexStatus, portMAX_DELAY) == pdTRUE) {
      strlcpy(statusAtual.ultimoComando, comando.id, sizeof(statusAtual.ultimoComando));
      ++statusAtual.versao;
      xSemaphoreGive(mutexStatus);
    }
  }
}

void setup() {
  Serial.begin(115200);
  delay(150);

  mutexStatus = xSemaphoreCreateMutex();
  mutexConfiguracao = xSemaphoreCreateMutex();
  filaComandos = xQueueCreate(4, sizeof(ComandoMomo));
  if (mutexStatus == nullptr || mutexConfiguracao == nullptr || filaComandos == nullptr) {
    Serial.println("ERRO: memoria insuficiente para inicializar tarefas");
    while (true) delay(1000);
  }

  preferencias.begin("momo", false);
  wifiNome = preferencias.getString("ssid", "");
  wifiSenha = preferencias.getString("senha", "");
  uidResponsavel = preferencias.getString("owner", "");
  const String configuracaoJson = preferencias.getString("wifiConfig", "");
  if (!configuracaoJson.isEmpty()) {
    JsonDocument configuracao;
    if (deserializeJson(configuracao, configuracaoJson) == DeserializationError::Ok) {
      wifiNome = configuracao["ssid"] | "";
      wifiSenha = configuracao["senha"] | "";
      uidResponsavel = configuracao["owner"] | "";
    } else {
      wifiNome = ""; wifiSenha = ""; uidResponsavel = "";
      Serial.println("Configuracao salva invalida: reabra o pareamento");
    }
  }
  ultimoComandoTratado = preferencias.getString("lastCmd", "");
  statusAtual.cicloFralda = preferencias.getUInt("cycle", 1);
  strlcpy(statusAtual.ultimoComando, ultimoComandoTratado.c_str(),
          sizeof(statusAtual.ultimoComando));

  Wire.begin(MOMO_SDA_GPIO, MOMO_SCL_GPIO);
  Wire.setTimeOut(50);
  sensorInicializado = conversor.begin();
  statusAtual.sensorOk = sensorInicializado;
  statusAtual.eventoId = preferencias.getUInt("event", 0);
  if (!sensorInicializado) {
    sensorEmFalha = true;
    Serial.println("ERRO: FDC1004 ausente; Wi-Fi continua disponivel, monitoramento bloqueado.");
  }

  servidor.on("/device-info", HTTP_GET, []() {
    JsonDocument dados;
    dados["deviceUid"] = MOMO_FIREBASE_EXPECTED_UID;
    dados["modelo"] = "MOMO XIAO ESP32-C6";
    responderJson(200, dados);
  });
  servidor.on("/wifi-setup", HTTP_POST, receberConfiguracaoWifi);

  if (sensorInicializado) iniciarNovaFralda();
  else atualizarEstado("DESCONECTADO");
  if (wifiNome.isEmpty() || !uidValido(uidResponsavel)) iniciarPortalPareamento();
  else conectarWifiSalvo();

  if (xTaskCreate(tarefaFirebase, "momo_cloud", 16384, nullptr, 1, nullptr) != pdPASS) {
    Serial.println("ERRO: falha ao criar tarefa Firebase");
  }
}

void loop() {
  if (apAtivo) servidor.handleClient();

  if (conexaoPendente &&
      static_cast<uint32_t>(millis() - inicioConexaoPendente) >= 1600UL) {
    conexaoPendente = false;
    conectarWifiSalvo();
    iniciarNovaFralda();
  }

  if (!apAtivo && !conexaoPendente &&
      WiFi.status() != WL_CONNECTED &&
      static_cast<uint32_t>(millis() - ultimaTentativaWifiMs) >= 30000UL) {
    iniciarPortalPareamento();
  }

  if (apAtivo && !conexaoPendente && WiFi.status() == WL_CONNECTED && !wifiNome.isEmpty()) {
    encerrarPortalPareamento();
    WiFi.mode(WIFI_STA);
  }
  tratarComandosRecebidos();

  while (Serial.available() > 0) {
    const char cmd = static_cast<char>(Serial.read());
    if (cmd == 'C' || cmd == 'c') iniciarNovaFralda();
    if (cmd == 'R') {
      preferencias.remove("wifiConfig");
      preferencias.remove("ssid");
      preferencias.remove("senha");
      preferencias.remove("owner");
      // Preservar lastCmd impede repetir comando ao reconfigurar somente o Wi-Fi.
      // Preservar ciclo/eventos evita reutilizar chaves do histórico da conta.
      Serial.println("Configuracao local do MOMO apagada; reiniciando");
      delay(200);
      ESP.restart();
    }
  }

  const uint32_t agora = millis();
  if (static_cast<int32_t>(agora - proximaAquisicaoMs) < 0) {
    delay(1);
    return;
  }
  proximaAquisicaoMs = agora + INTERVALO_LEITURA_MS;

  if (!sensorInicializado) {
    if (static_cast<int32_t>(agora - proximaTentativaSensorMs) >= 0) {
      sensorInicializado = conversor.begin();
      proximaTentativaSensorMs = agora + 3000UL;
    }
    if (!sensorInicializado) return;
  }
  double capacitanciaPf = 0.0;
  if (!lerCapacitancia(capacitanciaPf)) {
    if (falhasConsecutivas < 3) ++falhasConsecutivas;
    if (falhasConsecutivas >= 3 && !sensorEmFalha) {
      sensorEmFalha = true;
      aguardandoNovaFralda = true;
      xSemaphoreTake(mutexStatus, portMAX_DELAY);
      statusAtual.sensorOk = false;
      statusAtual.calibrado = false;
      statusAtual.progresso = 0;
      statusAtual.eventoPendente = false;
      statusAtual.classe[0] = '\0';
      strlcpy(statusAtual.estado, "DESCONECTADO", sizeof(statusAtual.estado));
      ++statusAtual.versao;
      xSemaphoreGive(mutexStatus);
    }
    Serial.println("ERRO: leitura FDC1004 ausente/invalida; nao alimentar o classificador");
    return;
  }
  falhasConsecutivas = 0;
  xSemaphoreTake(mutexStatus, portMAX_DELAY);
  statusAtual.sensorOk = true;
  statusAtual.ultimaLeituraMs = millis();
  if (sensorEmFalha) {
    // Referência interrompida não é reutilizada como calibração válida.
    aguardandoNovaFralda = true;
    statusAtual.calibrado = false;
    statusAtual.progresso = 0;
    strlcpy(statusAtual.estado, "AGUARDANDO_FRALDA", sizeof(statusAtual.estado));
    ++statusAtual.versao;
  }
  sensorEmFalha = false;
  xSemaphoreGive(mutexStatus);

  if (LOG_CAPACITANCIA) {
    Serial.print("P:");
    Serial.println(capacitanciaPf, 6);
  }

  if (aguardandoNovaFralda) return;

  momo::Classe classe = momo::URINA;
  const uint32_t instante = millis();
  const bool evento = modelo.processar(instante, capacitanciaPf, classe);

  if (modelo.estado() == momo::Inferencia::CALIBRANDO) {
    const uint32_t decorrido = instante - inicioCalibracaoMs;
    const uint8_t progresso = static_cast<uint8_t>(
        min(99UL, 100UL * decorrido / momo_modelo::CALIBRACAO_MS));
    if (progresso != statusAtual.progresso &&
        xSemaphoreTake(mutexStatus, portMAX_DELAY) == pdTRUE) {
      statusAtual.progresso = progresso;
      ++statusAtual.versao;
      xSemaphoreGive(mutexStatus);
    }
  }

  if (modelo.estado() != estadoAnterior) {
    if (estadoAnterior == momo::Inferencia::CALIBRANDO &&
        modelo.estado() != momo::Inferencia::CALIBRANDO) {
      if (xSemaphoreTake(mutexStatus, portMAX_DELAY) == pdTRUE) {
        statusAtual.calibrado = true;
        statusAtual.progresso = 100;
        statusAtual.c0Pf = modelo.referenciaSecaPf();
        ++statusAtual.versao;
        xSemaphoreGive(mutexStatus);
      }
      atualizarEstado("MONITORANDO");
      Serial.print("{\"estado\":\"monitorando\",\"c0_pf\":");
      Serial.print(modelo.referenciaSecaPf(), 6);
      Serial.println("}");
    }

    if (modelo.estado() == momo::Inferencia::COLETANDO_PREFIXO) {
      atualizarEstado("MONITORANDO");
      xSemaphoreTake(mutexStatus, portMAX_DELAY);
      statusAtual.eventoPendente = true;
      xSemaphoreGive(mutexStatus);
      Serial.println("{\"estado\":\"evento_detectado\",\"classe\":\"pendente\"}");
    }

    estadoAnterior = modelo.estado();
  }

  if (evento) {
    const char *nomeClasse = momo::nomeClasse(classe);
    const char *estadoClassificado =
        classe == momo::URINA ? "URINA" :
        classe == momo::FEZES ? "FEZES" : "DIARREIA";

    if (xSemaphoreTake(mutexStatus, portMAX_DELAY) == pdTRUE) {
      strlcpy(statusAtual.classe, estadoClassificado, sizeof(statusAtual.classe));
      statusAtual.eventoPendente = false;
      statusAtual.trocaRecomendada = false; // Política original preservada.
      ++statusAtual.eventoId;
      preferencias.putUInt("event", statusAtual.eventoId);
      ++statusAtual.versao;
      xSemaphoreGive(mutexStatus);
    }
    atualizarEstado(estadoClassificado);

    Serial.print("{\"estado\":\"classificado\",\"classe\":\"");
    Serial.print(nomeClasse);
    Serial.print("\",\"instante_ms\":");
    Serial.print(instante);
    Serial.println("}");
  }
}
