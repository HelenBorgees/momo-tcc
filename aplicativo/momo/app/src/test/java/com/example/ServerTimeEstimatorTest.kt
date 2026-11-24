package com.example

import com.example.data.repository.ServerTimeEstimator
import com.example.data.repository.TelemetryPolicy
import org.junit.Assert.*
import org.junit.Test

class ServerTimeEstimatorTest {
    @Test fun validHeartbeatWorksWhenPhoneIsFiveMinutesBehind() {
        val actualServerNow = 1_800_000_000_000L
        var phoneNow = actualServerNow - 300_000L
        var elapsed = 10_000L
        val clock = ServerTimeEstimator({ phoneNow }, { elapsed })
        assertNull(clock.nowMillis())
        assertFalse(TelemetryPolicy.isRecent(actualServerNow, phoneNow))
        clock.updateOffset(300_000L)
        assertTrue(TelemetryPolicy.isRecent(actualServerNow, clock.nowMillis()!!))
        phoneNow += 86_400_000L
        elapsed += 8_000L
        assertEquals(actualServerNow + 8_000L, clock.nowMillis()!!)
        assertTrue(TelemetryPolicy.isRecent(actualServerNow, clock.nowMillis()!!))
        elapsed += 120_000L
        assertFalse(TelemetryPolicy.isRecent(actualServerNow, clock.nowMillis()!!))
    }
}
