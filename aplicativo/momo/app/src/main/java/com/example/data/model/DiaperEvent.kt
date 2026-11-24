package com.example.data.model

/**
 * Tipos de eventos registrados na linha do tempo do MOMO.
 */
enum class TipoEvento(val rotulo: String) {
    CALIBRACAO("Calibração"),
    URINA("Urina"),
    FEZES("Fezes"),
    DIARREIA("Diarreia"),
    TROCA("Troca de fralda");

    val label: String get() = rotulo
}
typealias EventType = TipoEvento

/**
 * Observação real selecionada pelo cuidador no momento da troca de fralda.
 * Representa confirmação humana independente da classificação automática do sistema.
 */
enum class ObservacaoCuidador(val rotulo: String) {
    URINA("Urina"),
    FEZES("Fezes"),
    URINA_E_FEZES("Urina e fezes"),
    DIARREIA("Diarreia"),
    URINA_E_DIARREIA("Urina e diarreia"),
    FRALDA_SECA("Fralda seca"),
    NAO_INFORMADO("Não informado");

    val label: String get() = rotulo

    val hasUrine: Boolean get() = this == URINA || this == URINA_E_FEZES || this == URINA_E_DIARREIA
    val hasFeces: Boolean get() = this == FEZES || this == URINA_E_FEZES
    val hasDiarrhea: Boolean get() = this == DIARREIA || this == URINA_E_DIARREIA

    companion object {
        fun fromString(str: String?): ObservacaoCuidador {
            return values().firstOrNull {
                it.name.equals(str?.trim(), ignoreCase = true) || it.rotulo.equals(str?.trim(), ignoreCase = true)
            } ?: NAO_INFORMADO
        }
    }
}
typealias ChangeObservation = ObservacaoCuidador

/**
 * Classificação automática gerada pelos algoritmos no firmware do ESP32-C6.
 */
enum class ClassificacaoAutomatica(val rotulo: String) {
    URINA("Urina"),
    FEZES("Fezes"),
    DIARREIA("Diarreia"),
    NENHUMA("Nenhuma");

    val label: String get() = rotulo

    companion object {
        fun fromString(str: String?): ClassificacaoAutomatica {
            return values().firstOrNull {
                it.name.equals(str, ignoreCase = true) || it.rotulo.equals(str, ignoreCase = true)
            } ?: NENHUMA
        }
    }
}
typealias AutomaticClassification = ClassificacaoAutomatica

/**
 * Evento registrado na linha do tempo da fralda.
 */
data class EventoFralda(
    val id: String,
    val timestampMillis: Long,
    val tipo: TipoEvento,
    val titulo: String,
    val subtitulo: String,
    val horarioFormatado: String,
    val tempoRelativo: String,
    val observacoes: String? = null,
    val trocaRecomendada: Boolean = false,
    val classificacaoAutomatica: String? = null,
    val observacaoCuidador: ObservacaoCuidador? = null,
    val cicloFralda: Int = 1,
    val source: String = "live",
    val situacao: String = "Pendente",
    val detectedAt: Long? = null,
    val trialReferenceAt: Long? = null
) {
    // Propriedades compatíveis para código existente
    val historyTimestamp: Long
        get() = if (source == "reconstructed_test") trialReferenceAt ?: 0L
        else detectedAt ?: timestampMillis

    val type: TipoEvento get() = tipo
    val title: String get() = titulo
    val subtitle: String get() = subtitulo
    val timeFormatted: String get() = horarioFormatado
    val relativeTime: String get() = tempoRelativo
    val notes: String? get() = observacoes
    val replacementRecommended: Boolean get() = trocaRecomendada
    val automaticClassification: String? get() = classificacaoAutomatica
    val caregiverObservation: ObservacaoCuidador? get() = observacaoCuidador
}
typealias DiaperEvent = EventoFralda

/**
 * Registro de troca de fralda realizada pelo cuidador.
 * Preserva separadamente a observação humana e a classificação automática para auditoria.
 */
data class TrocaFralda(
    val id: String,
    val timestampMillis: Long,
    val observacaoCuidador: ObservacaoCuidador,
    val classificacaoAutomatica: String? = null,
    val observacoes: String? = null,
    val cicloFralda: Int = 1,
    val sensorEventId: String? = null
) {
    val observation: ObservacaoCuidador get() = observacaoCuidador
    val caregiverObservation: ObservacaoCuidador get() = observacaoCuidador
    val automaticClassification: String? get() = classificacaoAutomatica
    val notes: String? get() = observacoes
}
typealias DiaperChange = TrocaFralda
