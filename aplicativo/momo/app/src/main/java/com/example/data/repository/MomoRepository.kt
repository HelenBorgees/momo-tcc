package com.example.data.repository

import com.example.data.model.BabyProfile
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
import com.example.data.model.TrocaFralda
import kotlinx.coroutines.flow.Flow

/**
 * Interface do repositório de dados do MOMO.
 * Suporta tanto a implementação de demonstração acadêmica (DemoMomoRepositoryImpl)
 * quanto a implementação de comunicação HTTP real com o ESP32-C6 (HttpMomoRepositoryImpl).
 */
interface MomoRepository {
    fun getBabyProfile(): Flow<BabyProfile?>
    suspend fun saveBabyProfile(profile: BabyProfile)

    fun getDeviceState(): Flow<DeviceState>
    suspend fun updateDeviceState(state: DeviceState)

    fun getDiaperCurrentState(): Flow<DiaperCurrentState>
    fun getEvents(): Flow<List<DiaperEvent>>
    fun getChanges(): Flow<List<DiaperChange>>

    suspend fun recordDiaperChange(observation: ObservacaoCuidador, notes: String? = null, sensorEventId: String? = null)
    suspend fun startDiaperCalibration()
    suspend fun completeCalibrationAndStartMonitoring()
    suspend fun calibrateNewDiaper()

    suspend fun getReportSummary(period: ReportPeriod): ReportSummary

    // Métodos com nomenclatura padronizada em português
    fun obterPerfilBebe(): Flow<BabyProfile?> = getBabyProfile()
    suspend fun salvarPerfilBebe(perfil: BabyProfile) = saveBabyProfile(perfil)

    fun obterEstadoDispositivo(): Flow<EstadoDispositivo> = getDeviceState()
    suspend fun atualizarEstadoDispositivo(estado: EstadoDispositivo) = updateDeviceState(estado)

    fun obterEstadoAtualFralda(): Flow<EstadoAtualFralda> = getDiaperCurrentState()
    fun obterEventos(): Flow<List<EventoFralda>> = getEvents()
    fun obterTrocas(): Flow<List<TrocaFralda>> = getChanges()

    suspend fun registrarTroca(observacao: ObservacaoCuidador, notas: String? = null) = recordDiaperChange(observacao, notas)
    suspend fun iniciarCalibracao() = startDiaperCalibration()
    suspend fun configurarWifi(
        ssid: String,
        senha: String,
        ownerUid: String = "",
        idDispositivo: String = "MOMO-0428"
    ): Boolean

    suspend fun verificarDispositivoFirebase(timeoutSeconds: Int = 15): Boolean = false

    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
}

