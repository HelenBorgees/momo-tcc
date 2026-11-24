package com.example.data.model

/**
 * Modelo de dados para o estado do hardware de monitoramento MOMO ESP32-C6.
 */
data class EstadoDispositivo(
    val idDispositivo: String = "—",
    val nomeModelo: String = "Nenhum dispositivo MOMO pareado",
    val pareado: Boolean = false,
    val online: Boolean = false,
    val wifiSsid: String = "—",
    val calibrado: Boolean = false,
    val horarioUltimaComunicacao: String = "—",
    val versaoFirmware: String = "—",
    val enderecoIp: String = "—",
    val modoOperacao: String = "REAL_ESP32",
    val connectionMessage: String = "Aguardando comunicação com o monitor",
    val isDemo: Boolean = false
) {
    val deviceId: String get() = idDispositivo
    val modelName: String get() = nomeModelo
    val isPaired: Boolean get() = pareado
    val isCalibrated: Boolean get() = calibrado
    val lastCommunicationTime: String get() = horarioUltimaComunicacao
    val firmwareVersion: String get() = versaoFirmware
}

typealias DeviceState = EstadoDispositivo
