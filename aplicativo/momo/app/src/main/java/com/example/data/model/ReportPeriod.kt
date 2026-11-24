package com.example.data.model

enum class ReportPeriod(val label: String) {
    TODAY("Hoje"),
    DAYS_7("7 dias"),
    DAYS_30("30 dias")
}

data class ReportSummary(
    val period: ReportPeriod,
    val diapersWithUrineCount: Int,
    val fecesEventsCount: Int,
    val possibleDiarrheaCount: Int,
    val diaperChangesCount: Int,
    val averageHoursBetweenChanges: Double
) {
    // Retrocompatibilidade
    val urineEventsCount: Int get() = diapersWithUrineCount
    val fraldasComUrinaDetectada: Int get() = diapersWithUrineCount
}

