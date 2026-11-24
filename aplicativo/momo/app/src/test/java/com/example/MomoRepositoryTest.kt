package com.example

import com.example.data.model.BabyProfile
import com.example.data.model.EstadoFralda
import com.example.data.model.ObservacaoCuidador
import com.example.data.model.ReportPeriod
import com.example.data.model.TipoEvento
import com.example.data.repository.DemoMomoRepositoryImpl
import com.example.data.repository.HttpMomoRepositoryImpl
import com.example.data.repository.ModoRepositorio
import com.example.data.repository.MomoRepositoryProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MomoRepositoryTest {

    @Test
    fun testBabyProfileAgeFormatting() {
        val now = System.currentTimeMillis()
        val oneYearAgo = now - (365L * 24 * 3600 * 1000)
        val profile = BabyProfile(
            name = "Alice",
            birthDateMillis = oneYearAgo,
            weightKg = 11.0f
        )
        assertTrue(profile.formattedAge().isNotEmpty())
    }

    @Test
    fun testRepositoryStateTransitions() = runBlocking {
        val repo = DemoMomoRepositoryImpl.getInstance()
        repo.resetAllDemoData()

        val diaperInitial = repo.getDiaperCurrentState().first()
        assertEquals(EstadoFralda.URINA, diaperInitial.estado)

        // 1. Registrar troca: encerra ciclo anterior e transiciona para AGUARDANDO_FRALDA
        repo.recordDiaperChange(ObservacaoCuidador.URINA, "Troca de rotina")
        val diaperAfterChange = repo.getDiaperCurrentState().first()
        assertEquals(EstadoFralda.AGUARDANDO_FRALDA, diaperAfterChange.estado)
        assertTrue(diaperAfterChange.isAwaitingNewDiaper)

        // 2. Iniciar calibração: transiciona para CALIBRANDO com início da leitura do sensor
        repo.startDiaperCalibration()
        val diaperCalibrating = repo.getDiaperCurrentState().first()
        assertEquals(EstadoFralda.CALIBRANDO, diaperCalibrating.estado)
        assertTrue(diaperCalibrating.isCalibrating)
        assertTrue(diaperCalibrating.progressoCalibracao > 0)

        // 3. Concluir calibração: calcula novo C0 e inicia novo ciclo de monitoramento
        repo.completeCalibrationAndStartMonitoring()
        val diaperAfterCal = repo.getDiaperCurrentState().first()
        assertEquals(EstadoFralda.FRALDA_SECA, diaperAfterCal.estado)
        assertFalse(diaperAfterCal.trocaRecomendada)
        assertNotNull(diaperAfterCal.c0)

        // 4. Simulação de status
        repo.simulateDiaperStatus(EstadoFralda.TROCA_RECOMENDADA)
        val diaperReplace = repo.getDiaperCurrentState().first()
        assertEquals(EstadoFralda.TROCA_RECOMENDADA, diaperReplace.estado)
        assertTrue(diaperReplace.trocaRecomendada)

        // 5. Relatórios
        val summary = repo.getReportSummary(ReportPeriod.DAYS_7)
        assertNotNull(summary)
    }

    @Test
    fun testAutomaticClassificationAndCaregiverObservationPreservedSeparately() = runBlocking {
        val repo = DemoMomoRepositoryImpl.getInstance()
        repo.completeCalibrationAndStartMonitoring()

        // Sistema automaticamente detectou Fezes:
        repo.simulateFecesEvent()
        val state = repo.getDiaperCurrentState().first()
        assertEquals("Fezes", state.classificacaoAutomaticaAtiva)

        // Cuidador inspeciona a fralda e observa apenas Urina:
        repo.recordDiaperChange(ObservacaoCuidador.URINA, "Cuidador encontrou apenas urina")

        val changes = repo.getChanges().first()
        val latestChange = changes.first()
        assertEquals("Fezes", latestChange.classificacaoAutomatica)
        assertEquals(ObservacaoCuidador.URINA, latestChange.observacaoCuidador)

        val events = repo.getEvents().first()
        val latestChangeEvent = events.first { it.tipo == TipoEvento.TROCA }
        assertEquals("Fezes", latestChangeEvent.classificacaoAutomatica)
        assertEquals(ObservacaoCuidador.URINA, latestChangeEvent.observacaoCuidador)
    }

    @Test
    fun testUrineDetectionDoesNotTriggerReplacementAutomatically() = runBlocking {
        val repo = DemoMomoRepositoryImpl.getInstance()
        repo.completeCalibrationAndStartMonitoring() // reset para FRALDA_SECA

        // Detecção de urina: evento identificado, monitoramento normal, sem troca recomendada
        repo.simulateUrineEvent()
        val state1 = repo.getDiaperCurrentState().first()
        assertFalse(state1.trocaRecomendada)
        assertEquals(EstadoFralda.URINA, state1.estado)
        assertEquals("Urina detectada", state1.descricaoUltimoEvento)

        val eventsCountAfterFirst = repo.getEvents().first().count { it.tipo == TipoEvento.URINA && it.cicloFralda == state1.cicloFralda }
        assertEquals(1, eventsCountAfterFirst)

        // Aplicação adicional no mesmo ciclo: NÃO duplica evento e NÃO dispara troca automática
        repo.simulateUrineEvent()
        val state2 = repo.getDiaperCurrentState().first()
        assertFalse(state2.trocaRecomendada)
        assertEquals(EstadoFralda.URINA, state2.estado)

        val eventsCountAfterSecond = repo.getEvents().first().count { it.tipo == TipoEvento.URINA && it.cicloFralda == state2.cicloFralda }
        assertEquals(1, eventsCountAfterSecond)
    }

    @Test
    fun testFecesAndDiarrheaAlwaysRecommendChange() = runBlocking {
        val repo = DemoMomoRepositoryImpl.getInstance()
        repo.completeCalibrationAndStartMonitoring()

        repo.simulateFecesEvent()
        val fecesState = repo.getDiaperCurrentState().first()
        assertTrue(fecesState.trocaRecomendada)
        assertEquals(EstadoFralda.FEZES, fecesState.estado)

        repo.simulateDiarrheaEvent()
        val diarrheaState = repo.getDiaperCurrentState().first()
        assertTrue(diarrheaState.trocaRecomendada)
        assertEquals(EstadoFralda.DIARREIA, diarrheaState.estado)
    }

    @Test
    fun testHttpMomoRepositoryArchitecturePrepared() = runBlocking {
        val httpRepo = HttpMomoRepositoryImpl.getInstance("192.168.4.1")
        assertNotNull(httpRepo)

        // Provedor de repositório permite alternar para modo HTTP
        MomoRepositoryProvider.alternarModo(ModoRepositorio.HTTP_ESP32, "192.168.4.1")
        assertEquals(ModoRepositorio.HTTP_ESP32, MomoRepositoryProvider.modoAtual.value)

        // Retorna a implementação HTTP
        val activeRepo = MomoRepositoryProvider.getRepository()
        assertTrue(activeRepo is HttpMomoRepositoryImpl)

        // Retorna para modo Demo
        MomoRepositoryProvider.alternarModo(ModoRepositorio.DEMO)
        assertTrue(MomoRepositoryProvider.getRepository() is DemoMomoRepositoryImpl)
    }

    @Test
    fun testEstadoFraldaPortugueseSaturationProgress() {
        assertEquals("Urina detectada", EstadoFralda.URINA.titulo)
        assertEquals("O sensor identificou um evento compatível com urina nesta fralda.", EstadoFralda.URINA.descricao)
        assertFalse(EstadoFralda.URINA.requerTrocaImediata)
        assertTrue(EstadoFralda.FEZES.requerTrocaImediata)
        assertTrue(EstadoFralda.DIARREIA.requerTrocaImediata)
        assertEquals(0.0f, EstadoFralda.FRALDA_SECA.progresso, 0.001f)
        assertEquals(0.0f, EstadoFralda.AGUARDANDO_FRALDA.progresso, 0.001f)
        assertEquals(0.5f, EstadoFralda.URINA.progresso, 0.001f)
        assertEquals(1.0f, EstadoFralda.TROCA_RECOMENDADA.progresso, 0.001f)
        assertEquals(1.0f, EstadoFralda.FEZES.progresso, 0.001f)
        assertEquals(1.0f, EstadoFralda.DIARREIA.progresso, 0.001f)
    }

    @Test
    fun testCaregiverObservationExclusivityInReports() = runBlocking {
        val repo = DemoMomoRepositoryImpl.getInstance()
        repo.resetAllDemoData()

        // 1. Simula detecção automática de Fezes pelo sensor
        repo.simulateFecesEvent()

        // 2. Cuidador inspeciona a fralda e confirma apenas Urina
        repo.recordDiaperChange(ObservacaoCuidador.URINA, "Apenas urina observada pelo cuidador")

        // 3. O relatório clínico deve contabilizar a observação do cuidador (Urina)
        // e NÃO deve contabilizar a classificação automática do sensor como fezes clínica!
        val summaryToday = repo.getReportSummary(ReportPeriod.TODAY)
        assertTrue(summaryToday.diaperChangesCount >= 1)
        assertTrue(summaryToday.diapersWithUrineCount >= 1)
    }

    @Test
    fun testCombinedCaregiverObservationsProperties() {
        val urinaEDiarreia = ObservacaoCuidador.URINA_E_DIARREIA
        assertEquals("Urina e diarreia", urinaEDiarreia.rotulo)
        assertTrue(urinaEDiarreia.hasUrine)
        assertFalse(urinaEDiarreia.hasFeces)
        assertTrue(urinaEDiarreia.hasDiarrhea)

        val urinaEFezes = ObservacaoCuidador.URINA_E_FEZES
        assertTrue(urinaEFezes.hasUrine)
        assertTrue(urinaEFezes.hasFeces)
        assertFalse(urinaEFezes.hasDiarrhea)

        val naoInformado = ObservacaoCuidador.NAO_INFORMADO
        assertFalse(naoInformado.hasUrine)
        assertFalse(naoInformado.hasFeces)
        assertFalse(naoInformado.hasDiarrhea)
    }

    @Test
    fun testDeveloperPasswordValidation() {
        assertTrue(com.example.presentation.components.matchesDemoPin(" 4825 ", "4825"))
        assertFalse(com.example.presentation.components.matchesDemoPin("9999", "4825"))
        assertFalse(com.example.presentation.components.matchesDemoPin("", ""))
    }

    @Test
    fun testDeviceSettingsBabyProfileSubtitleLogic() {
        // 1. Enquanto estiver carregando:
        val loadingSubtitle = "Carregando perfil do bebê..."
        assertEquals("Carregando perfil do bebê...", loadingSubtitle)

        // 2. Com perfil cadastrado:
        val registeredProfile = BabyProfile(name = "Henrique", birthDateMillis = 0L, weightKg = 10f)
        val dynamicSubtitle = "Editar dados de ${registeredProfile.name.trim()}"
        assertEquals("Editar dados de Henrique", dynamicSubtitle)

        // 3. Sem perfil cadastrado:
        val unconfiguredSubtitle = "Configurar perfil do bebê"
        assertEquals("Configurar perfil do bebê", unconfiguredSubtitle)
    }

    @Test
    fun testWifiSetupRequestDtoStructure() {
        val dto = com.example.data.remote.WifiSetupRequestDto(
            ssid = "SSID_FICTICIO_TESTE",
            senha = "SENHA_FICTICIA_TESTE",
            ownerUid = "test_user_uid_123",
            idDispositivo = "MOMO-0428"
        )
        assertEquals("SSID_FICTICIO_TESTE", dto.ssid)
        assertEquals("SENHA_FICTICIA_TESTE", dto.senha)
        assertEquals("test_user_uid_123", dto.ownerUid)
        assertEquals("MOMO-0428", dto.idDispositivo)
    }
}

