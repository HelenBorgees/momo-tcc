package com.example

import com.example.data.repository.TelemetryPolicy
import org.junit.Assert.*
import org.junit.Test

class TelemetryPolicyTest {
    @Test fun rejectsAbsentStaleAndUnreasonablyFutureTelemetry() {
        val now = 1_800_000_000_000L
        assertFalse(TelemetryPolicy.isRecent(0, now))
        assertFalse(TelemetryPolicy.isRecent(now - 30_000, now))
        assertFalse(TelemetryPolicy.isRecent(now + 5_001, now))
        assertTrue(TelemetryPolicy.isRecent(now + 5_000, now))
        assertTrue(TelemetryPolicy.isRecent(now - 29_999, now))
        assertTrue(TelemetryPolicy.isRecent(now, now))
    }
}
