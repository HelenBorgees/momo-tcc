package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Modelo DTO do status recebido do ESP32-C6 via HTTP GET /status.
 * Contém o estado operacional, telemetria capacitiva e flags de recomendação.
 */
data class StatusEsp32Dto(
    val estado: String = "MONITORANDO",                // "MONITORANDO", "CALIBRANDO", "AGUARDANDO_FRALDA", "URINA", "FEZES", "DIARREIA", "TROCA_RECOMENDADA", "DESCONECTADO"
    val conectado: Boolean = false,
    val calibrado: Boolean = false,
    val progressoCalibracao: Int = 0,                // 0 a 100% informado pelo ESP32-C6
    val ultimaClassificacaoAutomatica: String? = null, // "URINA", "FEZES", "DIARREIA"
    val trocaRecomendada: Boolean = false,
    val c0: Float? = null,                             // Valor capacitivo de referência da fralda seca em pF (fornecido pelo conversor FDC1004 e processado pelo sistema)
    val r5s: Float? = null,                            // Variação média de capacitância entre a janela de 5 s posterior e a janela de 5 s anterior ao candidato detectado.
    val cicloFralda: Int = 1,                          // Identificador sequencial do ciclo da fralda
    val timestamp: Long = 0L
)

/**
 * Payload JSON enviado ao ESP32-C6 via POST /troca ao registrar observação humana.
 */
data class RegistrarTrocaRequestDto(
    val observacaoCuidador: String,                    // "URINA", "FEZES", "URINA_E_FEZES", "DIARREIA", "NAO_INFORMADO"
    val classificacaoAutomatica: String? = null,      // Preserva a classificação gerada pelo algoritmo do ESP32
    val observacoes: String? = null,
    val cicloFralda: Int = 1,
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Resposta padrão retornada pelo firmware do ESP32-C6.
 */
data class RespostaEsp32Dto(
    val sucesso: Boolean,
    val mensagem: String = "",
    val deviceUid: String? = null,
    val estado: String? = null
)

/**
 * Payload enviado ao ESP32-C6 em modo SoftAP (MOMO-SETUP) via POST /wifi-setup.
 */
data class WifiSetupRequestDto(
    val ssid: String,
    val senha: String,
    val ownerUid: String,
    val idDispositivo: String = "MOMO-0428"
)

/**
 * Interface Retrofit para comunicação REST com o firmware do ESP32-C6.
 */
interface Esp32ApiService {
    @GET("status")
    suspend fun obterStatus(): Response<StatusEsp32Dto>

    @POST("calibrar")
    suspend fun iniciarCalibracao(): Response<RespostaEsp32Dto>

    @POST("troca")
    suspend fun registrarTroca(@Body request: RegistrarTrocaRequestDto): Response<RespostaEsp32Dto>

    @POST("wifi-setup")
    suspend fun configurarWifi(@Body request: WifiSetupRequestDto): Response<RespostaEsp32Dto>
}
