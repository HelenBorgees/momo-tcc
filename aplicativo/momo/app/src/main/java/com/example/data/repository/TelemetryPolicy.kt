package com.example.data.repository

object TelemetryPolicy {
    const val MAX_AGE_MILLIS = 30_000L
    // serverTimeOffset é estimado e sofre variação de latência; não aceitar futuro arbitrário.
    const val MAX_FUTURE_SKEW_MILLIS = 5_000L
    fun isRecent(timestamp: Long, estimatedServerNow: Long): Boolean =
        timestamp > 0L && timestamp <= estimatedServerNow + MAX_FUTURE_SKEW_MILLIS &&
            estimatedServerNow - timestamp < MAX_AGE_MILLIS
}
