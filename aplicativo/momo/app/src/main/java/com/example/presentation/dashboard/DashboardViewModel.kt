package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BabyProfile
import com.example.data.model.DeviceState
import com.example.data.model.DiaperCurrentState
import com.example.data.model.DiaperEvent
import com.example.data.model.ObservacaoCuidador
import com.example.data.repository.MomoRepository
import com.example.data.repository.MomoRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

data class DashboardUiState(
    val babyProfile: BabyProfile? = null,
    val deviceState: DeviceState = DeviceState(),
    val diaperState: DiaperCurrentState = DiaperCurrentState(),
    val recentEvents: List<DiaperEvent> = emptyList(),
    val isRegisterChangeSheetOpen: Boolean = false,
    val isDemoMode: Boolean = false,
    val operationError: String? = null
)

class DashboardViewModel : ViewModel() {

    val operationError = MutableStateFlow<String?>(null)

    private val _isRegisterChangeSheetOpen = MutableStateFlow(false)
    val isRegisterChangeSheetOpen: StateFlow<Boolean> = _isRegisterChangeSheetOpen.asStateFlow()

    private val repository: MomoRepository
        get() = MomoRepositoryProvider.getRepository()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = MomoRepositoryProvider.modoAtual.flatMapLatest { modo ->
        _isRegisterChangeSheetOpen.value = false
        val activeRepo = MomoRepositoryProvider.getRepository()
        combine(
            activeRepo.getBabyProfile(),
            activeRepo.getDeviceState(),
            activeRepo.getDiaperCurrentState(),
            activeRepo.getEvents(),
            _isRegisterChangeSheetOpen
        ) { baby, device, diaper, events, isSheetOpen ->
            DashboardUiState(
                babyProfile = baby,
                deviceState = device,
                diaperState = diaper,
                recentEvents = events.take(3),
                isRegisterChangeSheetOpen = isSheetOpen,
                isDemoMode = modo == com.example.data.repository.ModoRepositorio.DEMO
            )
        }.onStart { emit(DashboardUiState(isDemoMode = modo == com.example.data.repository.ModoRepositorio.DEMO)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private var selectedSensorEventId: String? = null

    fun openRegisterChangeSheet() {
        selectedSensorEventId = uiState.value.diaperState.sensorEventId
        _isRegisterChangeSheetOpen.value = true
    }

    fun closeRegisterChangeSheet() {
        _isRegisterChangeSheetOpen.value = false
    }

    fun confirmDiaperObservation(
        observation: ObservacaoCuidador,
        notes: String?,
        onResult: (Boolean) -> Unit = {}
    ) {
        val sensorEventId = selectedSensorEventId
        viewModelScope.launch {
            try {
                repository.recordDiaperChange(observation, notes, sensorEventId)
                onResult(true)
            } catch (e: Exception) {
                operationError.value = e.message
                onResult(false)
            }
        }
    }

    fun startDiaperCalibration() {
        viewModelScope.launch {
            try { repository.startDiaperCalibration() }
            catch (e: Exception) { operationError.value = e.message }
        }
    }

    fun completeCalibrationAndStartMonitoring() {
        viewModelScope.launch {
            try {
                repository.completeCalibrationAndStartMonitoring()
                _isRegisterChangeSheetOpen.value = false
            } catch (e: Exception) { operationError.value = e.message }
        }
    }
}
