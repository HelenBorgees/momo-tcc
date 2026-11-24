package com.example.data.model

import java.util.Calendar
import java.util.Locale

/**
 * Data model for the single baby monitored in MOMO.
 */
data class BabyProfile(
    val id: String = "baby_default",
    val name: String,
    val birthDateMillis: Long,
    val weightKg: Float,
    val photoUri: String? = null
) {
    /**
     * Calculates age automatically from date of birth.
     * e.g. "29m" (29 meses) or "1 ano" or "4 meses"
     */
    fun formattedAge(currentMillis: Long = System.currentTimeMillis()): String {
        if (birthDateMillis <= 0L) return ""
        val diff = currentMillis - birthDateMillis
        if (diff < 0) return "Recém-nascido"

        val days = diff / (1000 * 60 * 60 * 24)
        val months = (days / 30.4375).toInt()
        val years = months / 12

        return when {
            months < 1 -> "${days}d"
            months < 12 -> "${months}m"
            else -> {
                val remMonths = months % 12
                if (remMonths > 0) "${months}m" else "${years} ano(s)"
            }
        }
    }
}
