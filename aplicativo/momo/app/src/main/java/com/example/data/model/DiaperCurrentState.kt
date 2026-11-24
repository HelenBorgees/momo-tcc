package com.example.data.model

/**
 * Estado atual da fralda ativa sendo monitorada pelo sistema MOMO.
 */
data class EstadoAtualFralda(
    val estado: EstadoFralda = EstadoFralda.DESCONECTADO,
    val contadorFezes: Int = 0,
    val timestampUltimaTrocaMillis: Long = 0L,
    val timestampUltimoEventoMillis: Long = 0L,
    val descricaoUltimoEvento: String = "Aguardando registros",
    val classificacaoAutomaticaAtiva: String? = null,
    val trocaRecomendada: Boolean = false,
    val calibrando: Boolean = false,
    val progressoCalibracao: Int = 0,
    val aguardandoNovaFralda: Boolean = false,
    val textoAtualizadoEm: String = "—",
    val cicloFralda: Int = 1,
    val sensorEventId: String? = null,
    val firmwareEventId: Long = 0L,
    // Dados técnicos opcionais recebidos do ESP32-C6 (preservados sem expor na tela principal)
    val c0: Float? = null,
    val r5s: Float? = null
) {
    // Propriedades compatíveis com interfaces existentes
    val status: EstadoFralda get() = estado
    val fecesEventCount: Int get() = contadorFezes
    val lastChangedAtMillis: Long get() = timestampUltimaTrocaMillis
    val lastEventAtMillis: Long get() = timestampUltimoEventoMillis
    val lastEventDescription: String get() = descricaoUltimoEvento
    val activeAutomaticClassification: String? get() = classificacaoAutomaticaAtiva
    val replacementRecommended: Boolean get() = trocaRecomendada
    val isCalibrating: Boolean get() = calibrando
    val isAwaitingNewDiaper: Boolean get() = aguardandoNovaFralda
    val lastUpdatedAtText: String get() = textoAtualizadoEm

    /**
     * Formata o tempo decorrido desde a última troca. Ex: "4h 33m" ou "Sem registro"
     */
    fun formattedElapsedTime(currentMillis: Long = System.currentTimeMillis()): String {
        if (timestampUltimaTrocaMillis <= 0L) return "Sem registro"
        val elapsed = (currentMillis - timestampUltimaTrocaMillis).coerceAtLeast(0L)
        val hours = elapsed / (1000 * 60 * 60)
        val minutes = (elapsed % (1000 * 60 * 60)) / (1000 * 60)
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    /**
     * Formata o horário relativo do último evento identificado. Ex: "Há 15 min" ou "Sem eventos"
     */
    fun formattedLastEventTime(currentMillis: Long = System.currentTimeMillis()): String {
        if (timestampUltimoEventoMillis <= 0L) return "Sem eventos"
        val elapsed = (currentMillis - timestampUltimoEventoMillis).coerceAtLeast(0L)
        val minutes = elapsed / (1000 * 60)
        val hours = minutes / 60
        return when {
            minutes < 1 -> "Agora"
            minutes < 60 -> "Há ${minutes} min"
            hours < 24 -> "Há ${hours}h"
            else -> "Hoje"
        }
    }
}


typealias DiaperCurrentState = EstadoAtualFralda
