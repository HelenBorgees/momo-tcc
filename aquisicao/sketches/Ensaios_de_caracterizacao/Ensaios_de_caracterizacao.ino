////////////////////////////////////////////////////////////////////////////////
//
//  AQUISICAO ROBUSTA DE DADOS - FDC1004
//
//  Trabalho de Conclusao de Curso
//  Engenharia de Controle e Automacao - UFOP
//  Autora:
//  Helen Borges de Lima Souza
//
//    A comunicação com o FDC1004 utiliza a biblioteca
//    ProtoCentral FDC1004 e foi baseada no exemplo oficial
//    "Basic Single Channel Demo for the FDC1004".
//
//    Biblioteca:
//    https://github.com/protocentral/ProtoCentral_fdc1004_breakout
//
//
//  COMANDOS:
//      I = iniciar ensaio
//      A = marcar aplicacao/evento
//      F = finalizar ensaio
//
//  FORMATO DOS DADOS:
//      Tempo(ms),Capacitancia(pF),CAPDAC,Status,Marcador
//
//  STATUS:
//       0 = leitura valida / OK
//       1 = CAPDAC fora da faixa / em ajuste
//      -1 = erro de leitura ou sensor temporariamente indisponivel
//
//  MARCADOR:
//      0 = sem evento
//      1 = aplicacao/evento
//      2 = inicio do ensaio
//      3 = fim do ensaio
//
//
////////////////////////////////////////////////////////////////////////////////

#include <Wire.h>
#include <Protocentral_FDC1004.h>

#define SDA_PIN 22
#define SCL_PIN 23

FDC1004 capacitanceSensor(FDC1004_RATE_100HZ);

const unsigned long INTERVALO_AMOSTRAGEM = 250;

const unsigned long INTERVALO_RECONECTAR = 2000;

const uint8_t LIMITE_ERROS_CONSECUTIVOS = 3;

bool ensaioAtivo = false;
bool sensorPronto = false;

unsigned long inicioEnsaio = 0;
unsigned long ultimaMedicao = 0;
unsigned long ultimaTentativaReconexao = 0;

float ultimaCapacitanciaValida = 0.0;
int ultimoCapdacValido = 0;
bool existeLeituraValida = false;

uint8_t errosConsecutivos = 0;


void enviarLinha(unsigned long tempo,
                 float capacitancia,
                 int capdac,
                 int status,
                 int marcador)
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
        float valorPlot = existeLeituraValida ? ultimaCapacitanciaValida : 0.0;
        int capdacPlot = existeLeituraValida ? ultimoCapdacValido : 0;

        enviarLinha(tempo, valorPlot, capdacPlot, -1, marcador);
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

        float valorPlot = existeLeituraValida ? ultimaCapacitanciaValida : 0.0;
        int capdacPlot = existeLeituraValida ? ultimoCapdacValido : 0;

        enviarLinha(tempo, valorPlot, capdacPlot, -1, marcador);

        Serial.print("# ERRO DE LEITURA FDC1004 - consecutivos: ");
        Serial.println(errosConsecutivos);

        if (errosConsecutivos >= LIMITE_ERROS_CONSECUTIVOS)
        {
            sensorPronto = false;
            ultimaTentativaReconexao = millis();

            Serial.println("# ATENCAO: FDC1004 temporariamente indisponivel");
            Serial.println("# O ensaio continua e o sistema tentara reconectar automaticamente");
        }
    }
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
        Serial.println("# O programa continuara tentando reconectar automaticamente");
        ultimaTentativaReconexao = millis();
    }

    Serial.println("# Comandos: I=Iniciar, A=Aplicacao/Evento, F=Finalizar");
    Serial.println("# Dados: Tempo(ms),Capacitancia(pF),CAPDAC,Status,Marcador");
}

void loop()
{

    if (!sensorPronto &&
        (millis() - ultimaTentativaReconexao >= INTERVALO_RECONECTAR))
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
                Serial.println("# NAO FOI POSSIVEL INICIAR: FDC1004 nao esta conectado");
            }
            else
            {
                ensaioAtivo = true;
                inicioEnsaio = millis();
                ultimaMedicao = millis();

                // Registra imediatamente a condicao inicial.
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
            else
            {

                enviarLeitura(1);
                ultimaMedicao = millis();

                Serial.println("# Aplicacao/evento marcado");
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
                // Ultima leitura do ensaio.
                enviarLeitura(3);

                ensaioAtivo = false;

                Serial.println("# Ensaio finalizado");
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

        if (agora - ultimaMedicao >= INTERVALO_AMOSTRAGEM)
        {
            enviarLeitura(0);
            ultimaMedicao = agora;
        }
    }
}
