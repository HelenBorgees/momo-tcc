package com.example.data.repository

import com.example.data.model.BabyProfile
import com.example.data.model.ChangeObservation
import com.example.data.model.DeviceState
import com.example.data.model.DiaperChange
import com.example.data.model.DiaperCurrentState
import com.example.data.model.DiaperEvent
import com.example.data.model.DiaperStatus
import com.example.data.model.EstadoAtualFralda
import com.example.data.model.EstadoDispositivo
import com.example.data.model.EstadoFralda
import com.example.data.model.EventoFralda
import com.example.data.model.ObservacaoCuidador
import com.example.data.model.ReportPeriod
import com.example.data.model.ReportSummary
import com.example.data.model.TipoEvento
import com.example.data.model.TrocaFralda
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Implementação em memória (DEMO) do repositório MOMO.
 * Simula os dados e o comportamento operacional do sistema para testes de laboratório.
 *
 * Características:
 * - Registra detecção única de urina por ciclo da fralda, sem contagem de micções consecutivas;
 * - Preserva separadamente a classificação automática do sistema e a observação do cuidador;
 * - Simula a calibração capacitiva C0 (~30s) pelo dispositivo antes de liberar o monitoramento.
 */
class DemoMomoRepositoryImpl private constructor() : MomoRepository, DemoMomoControls {

    private val repositoryScope = CoroutineScope(Dispatchers.Default + Job())
    private var calibrationSimJob: Job? = null

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    private val initialBirthDateMillis: Long = run {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -15) // ~15 meses
        cal.timeInMillis
    }

    private val _babyProfile = MutableStateFlow<BabyProfile?>(
        BabyProfile(
            id = "momo_baby_demo",
            name = "Bebê (Simulação)",
            birthDateMillis = initialBirthDateMillis,
            weightKg = 11.0f,
            photoUri = null
        )
    )

    private val _deviceState = MutableStateFlow(
        EstadoDispositivo(
            idDispositivo = "SIMULADOR-DEMO",
            nomeModelo = "Simulador MOMO (Demonstração)",
            pareado = true,
            online = true,
            wifiSsid = "Ambiente Simulado",
            calibrado = true,
            horarioUltimaComunicacao = "Agora (Simulado)",
            versaoFirmware = "v1.2-demo",
            enderecoIp = "127.0.0.1",
            modoOperacao = "DEMO",
            isDemo = true
        )
    )

    private val initialLastChanged = System.currentTimeMillis() - (4 * 3600 * 1000 + 33 * 60 * 1000)

    private val _diaperCurrentState = MutableStateFlow(
        EstadoAtualFralda(
            estado = EstadoFralda.URINA,
            contadorFezes = 0,
            timestampUltimaTrocaMillis = initialLastChanged,
            timestampUltimoEventoMillis = System.currentTimeMillis() - (15 * 60 * 1000),
            descricaoUltimoEvento = "Urina detectada",
            classificacaoAutomaticaAtiva = "Urina",
            trocaRecomendada = false,
            calibrando = false,
            progressoCalibracao = 100,
            aguardandoNovaFralda = false,
            textoAtualizadoEm = "Agora",
            cicloFralda = 1,
            c0 = 142.4f,
            r5s = 18.5f
        )
    )

    private val _events = MutableStateFlow<List<EventoFralda>>(createInitialEvents())
    private val _changes = MutableStateFlow<List<TrocaFralda>>(createInitialChanges())
    private val _onboardingCompleted = MutableStateFlow(true)

    private fun createInitialEvents(): List<EventoFralda> {
        val now = System.currentTimeMillis()
        val todayStr = dateFormat.format(Date(now))

        return listOf(
            EventoFralda(
                id = "evt_07",
                timestampMillis = now - (15 * 60 * 1000),
                tipo = TipoEvento.URINA,
                titulo = "Urina detectada",
                subtitulo = "Sensor capacitivo",
                horarioFormatado = timeFormat.format(Date(now - 15 * 60 * 1000)),
                tempoRelativo = "Há 15min ($todayStr)",
                classificacaoAutomatica = "Urina",
                cicloFralda = 1
            ),
            EventoFralda(
                id = "evt_05",
                timestampMillis = now - (4 * 3600 * 1000 + 32 * 60 * 1000),
                tipo = TipoEvento.CALIBRACAO,
                titulo = "Nova fralda preparada",
                subtitulo = "Monitoramento iniciado",
                horarioFormatado = "07:07",
                tempoRelativo = "Há 4h ($todayStr)",
                cicloFralda = 1
            ),
            EventoFralda(
                id = "evt_04",
                timestampMillis = now - (4 * 3600 * 1000 + 33 * 60 * 1000),
                tipo = TipoEvento.TROCA,
                titulo = "Troca de fralda realizada",
                subtitulo = "Observado pelo cuidador: Urina",
                horarioFormatado = "07:06",
                tempoRelativo = "Há 4h ($todayStr)",
                observacoes = "Verificado: apenas urina",
                classificacaoAutomatica = "Fezes",
                observacaoCuidador = ObservacaoCuidador.URINA,
                cicloFralda = 0
            ),
            EventoFralda(
                id = "evt_03",
                timestampMillis = now - (5 * 3600 * 1000),
                tipo = TipoEvento.FEZES,
                titulo = "Possível presença de fezes",
                subtitulo = "Recomendamos verificar a fralda.",
                horarioFormatado = "06:24",
                tempoRelativo = "Há 5h ($todayStr)",
                trocaRecomendada = true,
                classificacaoAutomatica = "Fezes",
                cicloFralda = 0
            ),
            EventoFralda(
                id = "evt_02",
                timestampMillis = now - (6 * 3600 * 1000),
                tipo = TipoEvento.URINA,
                titulo = "Urina detectada",
                subtitulo = "Sensor capacitivo",
                horarioFormatado = "04:48",
                tempoRelativo = "Há 6h ($todayStr)",
                classificacaoAutomatica = "Urina",
                cicloFralda = 0
            ),
            EventoFralda(
                id = "evt_01",
                timestampMillis = now - (8 * 3600 * 1000),
                tipo = TipoEvento.CALIBRACAO,
                titulo = "Nova fralda preparada",
                subtitulo = "Monitoramento iniciado",
                horarioFormatado = "03:36",
                tempoRelativo = "Há 8h ($todayStr)",
                cicloFralda = 0
            )
        )
    }

    private fun createInitialChanges(): List<TrocaFralda> {
        val now = System.currentTimeMillis()
        return listOf(
            TrocaFralda(
                id = "chg_01",
                timestampMillis = now - (4 * 3600 * 1000 + 33 * 60 * 1000),
                observacaoCuidador = ObservacaoCuidador.URINA,
                classificacaoAutomatica = "Fezes",
                observacoes = "Verificado: apenas urina",
                cicloFralda = 0
            ),
            TrocaFralda(
                id = "chg_02",
                timestampMillis = now - (8 * 3600 * 1000),
                observacaoCuidador = ObservacaoCuidador.URINA,
                classificacaoAutomatica = "Urina",
                observacoes = "Troca da manhã",
                cicloFralda = -1
            ),
            TrocaFralda(
                id = "chg_03",
                timestampMillis = now - (28 * 3600 * 1000),
                observacaoCuidador = ObservacaoCuidador.URINA_E_FEZES,
                classificacaoAutomatica = "Fezes",
                observacoes = "Troca noturna",
                cicloFralda = -2
            ),
            TrocaFralda(
                id = "chg_04",
                timestampMillis = now - (52 * 3600 * 1000),
                observacaoCuidador = ObservacaoCuidador.FEZES,
                classificacaoAutomatica = "Fezes",
                observacoes = "Após o almoço",
                cicloFralda = -3
            ),
            TrocaFralda(
                id = "chg_05",
                timestampMillis = now - (78 * 3600 * 1000),
                observacaoCuidador = ObservacaoCuidador.URINA_E_DIARREIA,
                classificacaoAutomatica = "Diarreia",
                observacoes = "Fezes líquidas observadas",
                cicloFralda = -4
            )
        )
    }

    override fun getBabyProfile(): Flow<BabyProfile?> = _babyProfile.asStateFlow()

    override suspend fun saveBabyProfile(profile: BabyProfile) {
        _babyProfile.value = profile.copy(id = "momo_baby_demo")
    }

    override fun getDeviceState(): Flow<DeviceState> = _deviceState.asStateFlow()

    override suspend fun updateDeviceState(state: DeviceState) {
        _deviceState.value = state
    }

    override fun getDiaperCurrentState(): Flow<DiaperCurrentState> = _diaperCurrentState.asStateFlow()

    override fun getEvents(): Flow<List<DiaperEvent>> = _events.asStateFlow()

    override fun getChanges(): Flow<List<DiaperChange>> = _changes.asStateFlow()

    override suspend fun recordDiaperChange(observation: ObservacaoCuidador, notes: String?, sensorEventId: String?) {
        val now = System.currentTimeMillis()
        val timeStr = timeFormat.format(Date(now))
        val dateStr = dateFormat.format(Date(now))

        val currentAutoClass = _diaperCurrentState.value.classificacaoAutomaticaAtiva
        val currentCiclo = _diaperCurrentState.value.cicloFralda

        // 1. Armazena separadamente classificacaoAutomatica e observacaoCuidador
        val newChange = TrocaFralda(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            observacaoCuidador = observation,
            classificacaoAutomatica = currentAutoClass,
            observacoes = notes,
            cicloFralda = currentCiclo
        )
        _changes.value = listOf(newChange) + _changes.value

        // 2. Registra o evento de troca no histórico com ambas as informações preservadas
        val changeEvent = EventoFralda(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            tipo = TipoEvento.TROCA,
            titulo = "Troca de fralda realizada",
            subtitulo = "Observado pelo cuidador: ${observation.rotulo}",
            horarioFormatado = timeStr,
            tempoRelativo = "Agora ($dateStr)",
            observacoes = notes,
            classificacaoAutomatica = currentAutoClass,
            observacaoCuidador = observation,
            cicloFralda = currentCiclo
        )
        _events.value = listOf(changeEvent) + _events.value

        // 3. Encerra o ciclo da fralda anterior, zera contadores e aguarda colocação da nova fralda
        _diaperCurrentState.value = _diaperCurrentState.value.copy(
            estado = EstadoFralda.AGUARDANDO_FRALDA,
            contadorFezes = 0,
            classificacaoAutomaticaAtiva = null,
            trocaRecomendada = false,
            calibrando = false,
            progressoCalibracao = 0,
            aguardandoNovaFralda = true,
            descricaoUltimoEvento = "Troca de fralda realizada",
            textoAtualizadoEm = "Agora",
            cicloFralda = currentCiclo + 1
        )
    }

    override suspend fun startDiaperCalibration() {
        calibrationSimJob?.cancel()

        _diaperCurrentState.value = _diaperCurrentState.value.copy(
            estado = EstadoFralda.CALIBRANDO,
            aguardandoNovaFralda = false,
            calibrando = true,
            progressoCalibracao = 5,
            descricaoUltimoEvento = "Preparando nova fralda",
            textoAtualizadoEm = "Agora"
        )

        // Simula o firmware do ESP32-C6 emitindo telemetria durante a aquisição de ~30s
        calibrationSimJob = repositoryScope.launch {
            var progresso = 5
            while (isActive && progresso < 100) {
                delay(1000L) // Incremento progressivo simulando o hardware
                progresso = (progresso + 4).coerceAtMost(100)
                _diaperCurrentState.value = _diaperCurrentState.value.copy(
                    progressoCalibracao = progresso
                )
            }
            if (isActive) {
                _diaperCurrentState.value = _diaperCurrentState.value.copy(
                    calibrando = false,
                    progressoCalibracao = 100
                )
            }
        }
    }

    override suspend fun completeCalibrationAndStartMonitoring() {
        calibrationSimJob?.cancel()
        val now = System.currentTimeMillis()
        val timeStr = timeFormat.format(Date(now))
        val dateStr = dateFormat.format(Date(now))
        val currentCiclo = _diaperCurrentState.value.cicloFralda

        val calEvent = EventoFralda(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            tipo = TipoEvento.CALIBRACAO,
            titulo = "Nova fralda preparada",
            subtitulo = "Monitoramento iniciado",
            horarioFormatado = timeStr,
            tempoRelativo = "Agora ($dateStr)",
            cicloFralda = currentCiclo
        )
        _events.value = listOf(calEvent) + _events.value

        _deviceState.value = _deviceState.value.copy(
            calibrado = true,
            horarioUltimaComunicacao = timeStr
        )

        // Inicia novo ciclo de monitoramento com nova referência seca C0 calculada
        _diaperCurrentState.value = EstadoAtualFralda(
            estado = EstadoFralda.FRALDA_SECA,
            contadorFezes = 0,
            timestampUltimaTrocaMillis = now,
            timestampUltimoEventoMillis = now,
            descricaoUltimoEvento = "Nova fralda preparada",
            classificacaoAutomaticaAtiva = null,
            trocaRecomendada = false,
            calibrando = false,
            progressoCalibracao = 100,
            aguardandoNovaFralda = false,
            textoAtualizadoEm = "Agora",
            cicloFralda = currentCiclo,
            c0 = 145.2f // Nova referência C0 calculada pelo firmware
        )
    }

    override suspend fun calibrateNewDiaper() {
        startDiaperCalibration()
    }

    override suspend fun getReportSummary(period: ReportPeriod): ReportSummary {
        val now = System.currentTimeMillis()
        val cutoffMillis = when (period) {
            ReportPeriod.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            ReportPeriod.DAYS_7 -> now - (7L * 24 * 3600 * 1000)
            ReportPeriod.DAYS_30 -> now - (30L * 24 * 3600 * 1000)
        }

        // Utiliza ESTRITAMENTE as trocas confirmadas pelo cuidador (_changes) no período.
        // Nenhuma classificação automática do sensor é contabilizada como observação clínica.
        val periodChanges = _changes.value.filter { it.timestampMillis >= cutoffMillis }

        // Cada troca é contabilizada exatamente uma vez
        val changesCount = periodChanges.size

        // Opções combinadas somam para ambas as condições observadas:
        // Urina: URINA, URINA_E_FEZES, URINA_E_DIARREIA
        val diapersWithUrine = periodChanges.count { it.observacaoCuidador.hasUrine }
        // Fezes: FEZES, URINA_E_FEZES
        val feces = periodChanges.count { it.observacaoCuidador.hasFeces }
        // Diarreia: DIARREIA, URINA_E_DIARREIA
        val diarrhea = periodChanges.count { it.observacaoCuidador.hasDiarrhea }
        // "Não informado" (NAO_INFORMADO) contabiliza em changesCount sem somar a urina, fezes ou diarreia.

        val avgHours = if (periodChanges.size >= 2) {
            val sortedChanges = periodChanges.sortedBy { it.timestampMillis }
            val intervals = (1 until sortedChanges.size).map { i ->
                (sortedChanges[i].timestampMillis - sortedChanges[i - 1].timestampMillis) / (1000.0 * 3600.0)
            }
            val avg = intervals.average()
            String.format(Locale.US, "%.1f", avg).toDoubleOrNull() ?: 3.5
        } else {
            3.5
        }

        return ReportSummary(
            period = period,
            diapersWithUrineCount = diapersWithUrine,
            fecesEventsCount = feces,
            possibleDiarrheaCount = diarrhea,
            diaperChangesCount = changesCount,
            averageHoursBetweenChanges = avgHours
        )
    }

    override suspend fun simulateDiaperStatus(status: EstadoFralda) {
        _diaperCurrentState.value = _diaperCurrentState.value.copy(
            estado = status,
            trocaRecomendada = status.requerTrocaImediata,
            descricaoUltimoEvento = status.titulo,
            textoAtualizadoEm = "Agora"
        )
    }

    override suspend fun simulateDeviceConnection(online: Boolean) {
        _deviceState.value = _deviceState.value.copy(
            online = online,
            horarioUltimaComunicacao = if (online) timeFormat.format(Date()) else _deviceState.value.horarioUltimaComunicacao
        )
        if (!online) {
            _diaperCurrentState.value = _diaperCurrentState.value.copy(
                estado = EstadoFralda.DESCONECTADO
            )
        } else if (_diaperCurrentState.value.estado == EstadoFralda.DESCONECTADO) {
            _diaperCurrentState.value = _diaperCurrentState.value.copy(
                estado = EstadoFralda.MONITORANDO
            )
        }
    }

    override suspend fun simulateUrineEvent() {
        val currentCiclo = _diaperCurrentState.value.cicloFralda
        val alreadyDetected = _events.value.any { it.cicloFralda == currentCiclo && it.tipo == TipoEvento.URINA }

        if (!alreadyDetected) {
            val now = System.currentTimeMillis()
            val timeStr = timeFormat.format(Date(now))
            val dateStr = dateFormat.format(Date(now))

            val event = EventoFralda(
                id = UUID.randomUUID().toString(),
                timestampMillis = now,
                tipo = TipoEvento.URINA,
                titulo = "Urina detectada",
                subtitulo = "Sensor capacitivo",
                horarioFormatado = timeStr,
                tempoRelativo = "Agora ($dateStr)",
                trocaRecomendada = false,
                classificacaoAutomatica = "Urina",
                cicloFralda = currentCiclo
            )
            _events.value = listOf(event) + _events.value

            _diaperCurrentState.value = _diaperCurrentState.value.copy(
                estado = EstadoFralda.URINA,
                timestampUltimoEventoMillis = now,
                descricaoUltimoEvento = "Urina detectada",
                classificacaoAutomaticaAtiva = "Urina",
                trocaRecomendada = false,
                textoAtualizadoEm = "Agora"
            )
        } else {
            // Urina já detectada neste ciclo: não adiciona eventos repetidos nem altera recomendação
            _diaperCurrentState.value = _diaperCurrentState.value.copy(
                estado = EstadoFralda.URINA,
                descricaoUltimoEvento = "Urina detectada",
                textoAtualizadoEm = "Agora"
            )
        }
    }

    override suspend fun simulateFecesEvent() {
        val now = System.currentTimeMillis()
        val timeStr = timeFormat.format(Date(now))
        val dateStr = dateFormat.format(Date(now))

        val event = EventoFralda(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            tipo = TipoEvento.FEZES,
            titulo = "Possível presença de fezes",
            subtitulo = "Recomendamos verificar a fralda.",
            horarioFormatado = timeStr,
            tempoRelativo = "Agora ($dateStr)",
            trocaRecomendada = true,
            classificacaoAutomatica = "Fezes",
            cicloFralda = _diaperCurrentState.value.cicloFralda
        )
        _events.value = listOf(event) + _events.value

        _diaperCurrentState.value = _diaperCurrentState.value.copy(
            estado = EstadoFralda.FEZES,
            contadorFezes = _diaperCurrentState.value.contadorFezes + 1,
            timestampUltimoEventoMillis = now,
            descricaoUltimoEvento = "Possível presença de fezes",
            classificacaoAutomaticaAtiva = "Fezes",
            trocaRecomendada = true,
            textoAtualizadoEm = "Agora"
        )
    }

    override suspend fun simulateDiarrheaEvent() {
        val now = System.currentTimeMillis()
        val timeStr = timeFormat.format(Date(now))
        val dateStr = dateFormat.format(Date(now))

        val event = EventoFralda(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            tipo = TipoEvento.DIARREIA,
            titulo = "Possível episódio de diarreia",
            subtitulo = "Recomendamos verificar a fralda.",
            horarioFormatado = timeStr,
            tempoRelativo = "Agora ($dateStr)",
            trocaRecomendada = true,
            classificacaoAutomatica = "Diarreia",
            cicloFralda = _diaperCurrentState.value.cicloFralda
        )
        _events.value = listOf(event) + _events.value

        _diaperCurrentState.value = _diaperCurrentState.value.copy(
            estado = EstadoFralda.DIARREIA,
            timestampUltimoEventoMillis = now,
            descricaoUltimoEvento = "Possível episódio de diarreia",
            classificacaoAutomaticaAtiva = "Diarreia",
            trocaRecomendada = true,
            textoAtualizadoEm = "Agora"
        )
    }

    override suspend fun simulateCalibrationInProgress() {
        calibrationSimJob?.cancel()
        _diaperCurrentState.value = _diaperCurrentState.value.copy(
            estado = EstadoFralda.CALIBRANDO,
            calibrando = true,
            progressoCalibracao = 45,
            aguardandoNovaFralda = false,
            descricaoUltimoEvento = "Preparando nova fralda",
            textoAtualizadoEm = "Agora"
        )
    }

    override suspend fun simulateCalibrationComplete() {
        calibrationSimJob?.cancel()
        _diaperCurrentState.value = _diaperCurrentState.value.copy(
            progressoCalibracao = 100,
            calibrando = false
        )
    }

    override suspend fun resetAllDemoData() {
        calibrationSimJob?.cancel()
        _babyProfile.value = BabyProfile(
            id = "momo_baby_demo",
            name = "Bebê (Simulação)",
            birthDateMillis = initialBirthDateMillis,
            weightKg = 11.0f,
            photoUri = null
        )
        _deviceState.value = EstadoDispositivo(
            idDispositivo = "SIMULADOR-DEMO",
            nomeModelo = "Simulador MOMO (Demonstração)",
            pareado = true,
            online = true,
            wifiSsid = "Ambiente Simulado",
            calibrado = true,
            horarioUltimaComunicacao = "Agora (Simulado)",
            versaoFirmware = "v1.2-demo",
            enderecoIp = "127.0.0.1",
            modoOperacao = "DEMO",
            isDemo = true
        )
        _diaperCurrentState.value = EstadoAtualFralda(
            estado = EstadoFralda.URINA,
            contadorFezes = 0,
            timestampUltimaTrocaMillis = initialLastChanged,
            timestampUltimoEventoMillis = System.currentTimeMillis() - (15 * 60 * 1000),
            descricaoUltimoEvento = "Urina detectada",
            classificacaoAutomaticaAtiva = "Urina",
            trocaRecomendada = false,
            calibrando = false,
            progressoCalibracao = 100,
            aguardandoNovaFralda = false,
            textoAtualizadoEm = "Agora",
            cicloFralda = 1
        )
        _events.value = createInitialEvents()
        _changes.value = createInitialChanges()
    }

    override suspend fun configurarWifi(
        ssid: String,
        senha: String,
        ownerUid: String,
        idDispositivo: String
    ): Boolean {
        _deviceState.value = _deviceState.value.copy(
            wifiSsid = ssid,
            online = true
        )
        return true
    }

    override suspend fun verificarDispositivoFirebase(timeoutSeconds: Int): Boolean = true

    override fun isOnboardingCompleted(): Flow<Boolean> = _onboardingCompleted.asStateFlow()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
    }

    companion object {
        @Volatile
        private var instance: DemoMomoRepositoryImpl? = null

        fun getInstance(): DemoMomoRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: DemoMomoRepositoryImpl().also { instance = it }
            }
        }
    }
}
