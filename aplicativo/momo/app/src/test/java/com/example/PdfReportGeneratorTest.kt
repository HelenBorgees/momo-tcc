package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BabyProfile
import com.example.data.model.DiaperChange
import com.example.data.model.DiaperEvent
import com.example.data.model.ObservacaoCuidador
import com.example.data.model.ReportPeriod
import com.example.data.model.TipoEvento
import com.example.util.PdfReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfReportGeneratorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCalculatePeakDay_emptyList_returnsNoEvents() {
        val result = PdfReportGenerator.calculatePeakDay(emptyList(), ReportPeriod.DAYS_7)
        assertTrue(result is PdfReportGenerator.PeakDayResult.NoEvents)
    }

    @Test
    fun testCalculatePeakDay_onlyChangesWithoutBiologicalEvents_returnsNoEvents() {
        val now = System.currentTimeMillis()
        val changes = listOf(
            DiaperChange(
                id = "c1",
                timestampMillis = now,
                observacaoCuidador = ObservacaoCuidador.NAO_INFORMADO,
                observacoes = "Apenas troca"
            )
        )
        val result = PdfReportGenerator.calculatePeakDay(changes, ReportPeriod.DAYS_7)
        assertTrue(result is PdfReportGenerator.PeakDayResult.NoEvents)
    }

    @Test
    fun testCalculatePeakDay_singlePeakDay_returnsCorrectBreakdownAndNoTie() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
        val t1 = sdf.parse("25/10/2026 09:00")!!.time
        val t2 = sdf.parse("25/10/2026 14:00")!!.time
        val t3 = sdf.parse("26/10/2026 10:00")!!.time

        // Dia 25: 1 troca com Urina + 1 troca com Urina e Fezes -> Urina=2, Fezes=1, Diarreia=0 -> Total=3
        // Dia 26: 1 troca com Urina -> Urina=1 -> Total=1
        val changes = listOf(
            DiaperChange(id = "1", timestampMillis = t1, observacaoCuidador = ObservacaoCuidador.URINA),
            DiaperChange(id = "2", timestampMillis = t2, observacaoCuidador = ObservacaoCuidador.URINA_E_FEZES),
            DiaperChange(id = "3", timestampMillis = t3, observacaoCuidador = ObservacaoCuidador.URINA)
        )

        val result = PdfReportGenerator.calculatePeakDay(changes, ReportPeriod.DAYS_7)
        assertTrue(result is PdfReportGenerator.PeakDayResult.Success)
        val success = result as PdfReportGenerator.PeakDayResult.Success
        assertEquals("25/10/2026", success.dateFormatted)
        assertEquals(3, success.totalEvents)
        assertEquals(2, success.urineCount)
        assertEquals(1, success.fecesCount)
        assertEquals(0, success.diarrheaCount)
        assertFalse(success.hasTie)
    }

    @Test
    fun testCalculatePeakDay_tie_returnsMostRecentAndSignalsTie() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
        val t1 = sdf.parse("20/10/2026 10:00")!!.time
        val t2 = sdf.parse("22/10/2026 11:00")!!.time

        // Ambos os dias têm 2 eventos biológicos
        val changes = listOf(
            DiaperChange(id = "1", timestampMillis = t1, observacaoCuidador = ObservacaoCuidador.URINA_E_FEZES), // 2 eventos
            DiaperChange(id = "2", timestampMillis = t2, observacaoCuidador = ObservacaoCuidador.URINA_E_DIARREIA) // 2 eventos
        )

        val result = PdfReportGenerator.calculatePeakDay(changes, ReportPeriod.DAYS_7)
        assertTrue(result is PdfReportGenerator.PeakDayResult.Success)
        val success = result as PdfReportGenerator.PeakDayResult.Success
        // Deve retornar o mais recente: 22/10/2026
        assertEquals("22/10/2026", success.dateFormatted)
        assertEquals(2, success.totalEvents)
        assertEquals(1, success.urineCount)
        assertEquals(0, success.fecesCount)
        assertEquals(1, success.diarrheaCount)
        assertTrue(success.hasTie)
    }

    @Test
    fun testDistinctDaysWithDiarrhea() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
        val t1 = sdf.parse("25/10/2026 08:00")!!.time
        val t2 = sdf.parse("25/10/2026 14:00")!!.time
        val t3 = sdf.parse("26/10/2026 10:00")!!.time
        val t4 = sdf.parse("27/10/2026 10:00")!!.time

        val changes = listOf(
            DiaperChange(id = "1", timestampMillis = t1, observacaoCuidador = ObservacaoCuidador.DIARREIA),
            DiaperChange(id = "2", timestampMillis = t2, observacaoCuidador = ObservacaoCuidador.URINA_E_DIARREIA),
            DiaperChange(id = "3", timestampMillis = t3, observacaoCuidador = ObservacaoCuidador.DIARREIA),
            DiaperChange(id = "4", timestampMillis = t4, observacaoCuidador = ObservacaoCuidador.URINA) // Sem diarreia
        )

        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        val distinctDays = changes
            .filter { it.observacaoCuidador.hasDiarrhea }
            .map { sdfDate.format(Date(it.timestampMillis)) }
            .distinct()
            .size

        assertEquals(2, distinctDays)
    }

    @Test
    fun testBuildDetailedRecords_unifiesChangesAndSensorEvents_skipsTrocaMirror() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
        val t1 = sdf.parse("25/10/2026 08:00")!!.time
        val t2 = sdf.parse("25/10/2026 10:30")!!.time
        val t3 = sdf.parse("25/10/2026 10:30")!!.time

        val changes = listOf(
            DiaperChange(
                id = "c1",
                timestampMillis = t2,
                observacaoCuidador = ObservacaoCuidador.URINA,
                observacoes = "Fralda estava cheia"
            )
        )

        val sensorEvents = listOf(
            DiaperEvent(
                id = "s1",
                tipo = TipoEvento.URINA,
                timestampMillis = t1,
                titulo = "Urina detectada",
                subtitulo = "Sensor capacitivo",
                horarioFormatado = "08:00",
                tempoRelativo = "há 2 horas",
                observacoes = "Capacitância elevada"
            ),
            // Espelho de troca (não deve ser incluído na tabela para evitar duplicatas)
            DiaperEvent(
                id = "s2",
                tipo = TipoEvento.TROCA,
                timestampMillis = t3,
                titulo = "Troca de fralda",
                subtitulo = "Confirmado",
                horarioFormatado = "10:30",
                tempoRelativo = "agora",
                observacoes = "Troca realizada"
            )
        )

        val records = PdfReportGenerator.buildDetailedRecords(changes, sensorEvents)
        assertEquals(2, records.size)

        // Primeiro registro deve ser o mais recente (t2 = 10:30)
        assertEquals("Cuidador", records[0].origin)
        assertEquals("Urina", records[0].eventType)
        assertEquals("Fralda estava cheia", records[0].observation)

        // Segundo registro é o sensor (t1 = 08:00)
        assertEquals("Sensor", records[1].origin)
        assertEquals("Urina detectada", records[1].eventType)
        assertEquals("Situação: Pendente · Capacitância elevada", records[1].observation)
        assertEquals("", records[1].timeFormatted)
    }

    @Test
    fun testFormatAgeAndWeight() {
        val now = System.currentTimeMillis()
        val birthTenDays = now - (10L * 24 * 3600 * 1000)
        val birthSixMonths = now - (180L * 24 * 3600 * 1000)

        val ageTenDays = PdfReportGenerator.formatAgePtBr(birthTenDays)
        assertTrue(ageTenDays.contains("10 dias"))

        val ageSixMonths = PdfReportGenerator.formatAgePtBr(birthSixMonths)
        assertTrue(ageSixMonths.contains("meses") || ageSixMonths.contains("mês"))

        val weightFormatted = PdfReportGenerator.formatWeightPtBr(9.5f)
        assertEquals("9,5 kg", weightFormatted)

        assertEquals("Não informado", PdfReportGenerator.formatWeightPtBr(null))
        assertEquals("Não informada", PdfReportGenerator.formatAgePtBr(null))
    }
}
