
////////////////////////////////////////////////////////////////////////////////
//
//  AQUISIÇÃO DE DADOS PARA ENSAIOS TEMPORAIS EM FRALDAS
//
//  Trabalho de Conclusão de Curso
//  Engenharia de Controle e Automação - UFOP
//
//  Autora:
//  Helen Borges de Lima Souza
//
//  Descrição:
//  Código desenvolvido para aquisição contínua de dados de capacitância
//  durante ensaios temporais de monitoramento de umidade em fraldas.
//
//  A comunicação com o FDC1004 utiliza a biblioteca
//  ProtoCentral FDC1004 e foi baseada no exemplo oficial
//  "Basic Single Channel Demo for the FDC1004".
//
//  Biblioteca:
//  https://github.com/protocentral/ProtoCentral_fdc1004_breakout
//
//  I2C:
//      SDA = GPIO 22 (D4)
//      SCL = GPIO 23 (D5)
//
//  Comandos:
//      I = iniciar ensaio
//      A = registrar a aplicacao
//      F = finalizar manualmente
//
//  Protocolo:
//      A aquisicao inicia com I.
//      A aplicacao e marcada uma unica vez com A.
//      Apos A, o ensaio continua por 120 segundos e encerra automaticamente.
//
//  Saida serial:
//      Tempo(ms),Capacitancia(pF),CAPDAC,Status,Marcador
//
//  Status:
//       0 = leitura valida
//       1 = CAPDAC fora da faixa / em ajuste
//      -1 = erro de leitura
//
//  Marcador:
//      0 = leitura normal
//      1 = aplicacao
//      2 = inicio
//      3 = fim
///////////

#include <Wire.h>
#include <Protocentral_FDC1004.h>

#define SDA_PIN 22
#define SCL_PIN 23

FDC1004 capacitanceSensor(FDC1004_RATE_100HZ);

const unsigned long INTERVALO_AMOSTRAGEM = 250;
const unsigned long TEMPO_POS_APLICACAO = 120000;
const unsigned long INTERVALO_RECONECTAR = 2000;
const uint8_t LIMITE_ERROS_CONSECUTIVOS = 3;

bool ensaioAtivo = false;
bool aplicacaoRegistrada = false;
bool sensorPronto = false;

unsigned long inicioEnsaio = 0;
unsigned long inicioAplicacao = 0;
unsigned long ultimaMedicao = 0;
unsigned long ultimaTentativaReconexao = 0;

float ultimaCapacitanciaValida = 0.0;
int ultimoCapdacValido = 0;
bool existeLeituraValida = false;

uint8_t errosConsecutivos = 0;

void enviarLinha(unsigned long tempo, float capacitancia, int capdac, int status, int marcador)
{
    Serial.print(tempo);
    Serial.print(",");
    Serial.print(capacitancia, 4);
    Serial.print(",");
    Serial.print(capdac);
    Serial.print(",");
    Serial.print(status);
    Serial.print(",");
    Serial.println(marcador);
}

bool inicializarSensor()
{
    if (!capacitanceSensor.begin())
    {
        return false;
    }

    if (!capacitanceSensor.isConnected())
    {
        return false;
    }

    return true;
}

void enviarLeitura(int marcador)
{
    unsigned long tempo = millis() - inicioEnsaio;

    if (!sensorPronto)
    {
        float capacitancia = existeLeituraValida ? ultimaCapacitanciaValida : 0.0;
        int capdac = existeLeituraValida ? ultimoCapdacValido : 0;
        enviarLinha(tempo, capacitancia, capdac, -1, marcador);
        return;
    }

    fdc1004_capacitance_t measurement =
        capacitanceSensor.getCapacitanceMeasurement(FDC1004_CHANNEL_0);

    if (!isnan(measurement.capacitance_pf))
    {
        ultimaCapacitanciaValida = measurement.capacitance_pf;
        ultimoCapdacValido = measurement.capdac_used;
        existeLeituraValida = true;

        if (errosConsecutivos > 0)
        {
            Serial.println("# Leitura do FDC1004 restabelecida");
        }

        errosConsecutivos = 0;

        int status = measurement.capdac_out_of_range ? 1 : 0;

        enviarLinha(
            tempo,
            measurement.capacitance_pf,
            measurement.capdac_used,
            status,
            marcador
        );
    }
    else
    {
        errosConsecutivos++;

        float capacitancia = existeLeituraValida ? ultimaCapacitanciaValida : 0.0;
        int capdac = existeLeituraValida ? ultimoCapdacValido : 0;

        enviarLinha(tempo, capacitancia, capdac, -1, marcador);

        Serial.print("# Erro de leitura FDC1004. Erros consecutivos: ");
        Serial.println(errosConsecutivos);

        if (errosConsecutivos >= LIMITE_ERROS_CONSECUTIVOS)
        {
            sensorPronto = false;
            ultimaTentativaReconexao = millis();
            Serial.println("# FDC1004 temporariamente indisponivel");
            Serial.println("# Tentativas de reconexao permanecem ativas");
        }
    }
}

void finalizarEnsaio()
{
    if (!ensaioAtivo)
    {
        return;
    }

    enviarLeitura(3);

    ensaioAtivo = false;
    aplicacaoRegistrada = false;

    Serial.println("# Ensaio finalizado");
}

void setup()
{
    Serial.begin(115200);
    delay(200);

    Wire.begin(SDA_PIN, SCL_PIN);

    Serial.println("# Inicializando FDC1004...");

    sensorPronto = inicializarSensor();

    if (sensorPronto)
    {
        Serial.println("# FDC1004 inicializado com sucesso");
        Serial.println("# FDC1004 respondendo corretamente");
    }
    else
    {
        Serial.println("# Falha ao inicializar o FDC1004");
        Serial.println("# O sistema tentara reconectar automaticamente");
        ultimaTentativaReconexao = millis();
    }

    Serial.println("# Comandos: I=Iniciar, A=Aplicacao, F=Finalizar");
    Serial.println("# Dados: Tempo(ms),Capacitancia(pF),CAPDAC,Status,Marcador");
}

void loop()
{
    if (!sensorPronto &&
        millis() - ultimaTentativaReconexao >= INTERVALO_RECONECTAR)
    {
        ultimaTentativaReconexao = millis();

        Serial.println("# Tentando reconectar ao FDC1004...");

        if (inicializarSensor())
        {
            sensorPronto = true;
            errosConsecutivos = 0;
            Serial.println("# FDC1004 reconectado com sucesso");
        }
        else
        {
            Serial.println("# FDC1004 ainda nao respondeu");
        }
    }

    while (Serial.available() > 0)
    {
        char comando = Serial.read();

        if (comando == '\n' || comando == '\r' || comando == ' ')
        {
            continue;
        }

        if (comando == 'I' || comando == 'i')
        {
            if (ensaioAtivo)
            {
                Serial.println("# Ensaio ja esta em andamento");
            }
            else if (!sensorPronto)
            {
                Serial.println("# Nao foi possivel iniciar: FDC1004 nao esta conectado");
            }
            else
            {
                ensaioAtivo = true;
                aplicacaoRegistrada = false;
                inicioEnsaio = millis();
                ultimaMedicao = millis();
                enviarLeitura(2);
                Serial.println("# Ensaio iniciado");
            }
        }
        else if (comando == 'A' || comando == 'a')
        {
            if (!ensaioAtivo)
            {
                Serial.println("# Aplicacao ignorada: nenhum ensaio em andamento");
            }
            else if (aplicacaoRegistrada)
            {
                Serial.println("# Aplicacao ja foi registrada neste ensaio");
            }
            else
            {
                aplicacaoRegistrada = true;
                inicioAplicacao = millis();
                enviarLeitura(1);
                ultimaMedicao = millis();
                Serial.println("# Aplicacao registrada");
            }
        }
        else if (comando == 'F' || comando == 'f')
        {
            if (!ensaioAtivo)
            {
                Serial.println("# Finalizacao ignorada: nenhum ensaio em andamento");
            }
            else
            {
                finalizarEnsaio();
            }
        }
        else
        {
            Serial.print("# Comando desconhecido: ");
            Serial.println(comando);
        }
    }

    if (ensaioAtivo)
    {
        unsigned long agora = millis();

        if (aplicacaoRegistrada &&
            agora - inicioAplicacao >= TEMPO_POS_APLICACAO)
        {
            finalizarEnsaio();
            return;
        }

        if (agora - ultimaMedicao >= INTERVALO_AMOSTRAGEM)
        {
            enviarLeitura(0);
            ultimaMedicao = agora;
        }
    }
}
