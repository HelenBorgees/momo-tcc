package com.example.data.model

import java.util.Calendar
import java.util.TimeZone

object HistoryPeriodPolicy {
    fun start(period: ReportPeriod, now: Long): Long =
        Calendar.getInstance(TimeZone.getTimeZone("America/Sao_Paulo")).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, when (period) {
                ReportPeriod.TODAY -> 0
                ReportPeriod.DAYS_7 -> -6
                ReportPeriod.DAYS_30 -> -29
            })
        }.timeInMillis

    fun includes(period: ReportPeriod?, reference: Long, now: Long): Boolean =
        period == null || reference in start(period, now)..now
}
