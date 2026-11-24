package com.example

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class SensorHistoryPolicyTest {
    @Test fun referenceIsSeparateFromDetectionTime() {
        val reconstructed = EventoFralda("test", 0L, TipoEvento.FEZES, "", "", "", "",
            source = "reconstructed_test", trialReferenceAt = 1793760504842L)
        assertNull(reconstructed.detectedAt)
        assertEquals(1793760504842L, reconstructed.historyTimestamp)
        val live = reconstructed.copy(source = "live", timestampMillis = 1793760504842L, detectedAt = 1793760500000L)
        assertEquals(1793760500000L, live.historyTimestamp)
    }

    @Test fun saoPauloBoundariesAndLegacyUndatedRecords() {
        val now = 1793761300000L
        val beforeMidnight = 1793760504842L
        val afterMidnight = 1793761236907L
        assertFalse(HistoryPeriodPolicy.includes(ReportPeriod.TODAY, beforeMidnight, now))
        assertTrue(HistoryPeriodPolicy.includes(ReportPeriod.TODAY, afterMidnight, now))
        for (period in listOf(ReportPeriod.DAYS_7, ReportPeriod.DAYS_30)) {
            assertTrue(HistoryPeriodPolicy.includes(period, beforeMidnight, now))
            assertTrue(HistoryPeriodPolicy.includes(period, afterMidnight, now))
            assertFalse(HistoryPeriodPolicy.includes(period, 0, now))
        }
        assertTrue(HistoryPeriodPolicy.includes(null, 0, now))
    }

    @Test fun comparisonDoesNotChangeStoredPrediction() {
        val prediction = TipoEvento.URINA
        assertEquals("Divergente", SensorEventPolicy.comparison(prediction, ObservacaoCuidador.DIARREIA))
        assertEquals("Confirmado", SensorEventPolicy.comparison(prediction, ObservacaoCuidador.URINA))
        assertEquals("Falso positivo", SensorEventPolicy.comparison(TipoEvento.FEZES, ObservacaoCuidador.FRALDA_SECA))
        assertEquals("Pendente", SensorEventPolicy.comparison(TipoEvento.DIARREIA, null))
        assertTrue(SensorEventPolicy.matchesPrediction("URINA", prediction))
        assertFalse(SensorEventPolicy.matchesPrediction("DIARREIA", prediction))
    }

    @Test fun identifierIsStableOnReconnectAndVariesWithNewEvent() {
        val first = SensorEventPolicy.id("device", 4, 15)
        assertEquals(first, SensorEventPolicy.id("device", 4, 15))
        assertNotEquals(first, SensorEventPolicy.id("device", 4, 16))
        assertNotEquals(first, SensorEventPolicy.id("device", 5, 15))
    }
}
