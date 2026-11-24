package com.example.data.model

object SensorEventPolicy {
    fun id(deviceUid: String, cycle: Int, eventId: Long): String =
        "${deviceUid}_${cycle}_${eventId}"

    fun matchesPrediction(storedClass: Any?, incoming: TipoEvento): Boolean =
        storedClass == incoming.name

    fun comparison(predicted: TipoEvento, observed: ObservacaoCuidador?): String = when {
        observed == null || observed == ObservacaoCuidador.NAO_INFORMADO -> "Pendente"
        observed == ObservacaoCuidador.FRALDA_SECA -> "Falso positivo"
        observed.name == predicted.name -> "Confirmado"
        else -> "Divergente"
    }
}
