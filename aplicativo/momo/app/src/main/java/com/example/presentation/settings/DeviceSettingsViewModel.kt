package com.example.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.model.BabyProfile
import com.example.data.model.DeviceState
import com.example.data.model.DiaperCurrentState
import com.example.data.model.EstadoFralda
import com.example.data.repository.DemoMomoControls
import com.example.data.repository.FirebaseBabyProfileRepository
import com.example.data.repository.MomoRepository
import com.example.data.repository.MomoRepositoryProvider
import com.example.data.repository.ModoRepositorio
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class DeviceSettingsUiState(
    val babyProfile: BabyProfile? = null,
    val isLoadingBabyProfile: Boolean = true,
    val deviceState: DeviceState = DeviceState(),
    val diaperState: DiaperCurrentState = DiaperCurrentState(),
    val isCalibrating: Boolean = false,
    val calibrationSuccess: Boolean = false,
    val showAboutDialog: Boolean = false,
    val modoRepositorio: ModoRepositorio = ModoRepositorio.HTTP_ESP32,
    val ipEsp32: String = "192.168.4.1",
    val currentUser: FirebaseUser? = null,
    val operationError: String? = null,
    val isDemoToolsUnlocked: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSettingsViewModel(
    private val authRepository: AuthRepository = AuthRepository.getInstance(),
    private val firebaseProfileRepo: FirebaseBabyProfileRepository = FirebaseBabyProfileRepository.getInstance()
) : ViewModel() {

    private var calibrationJob: Job? = null

    val operationError = MutableStateFlow<String?>(null)
    private val _isCalibrating = MutableStateFlow(false)
    private val _calibrationSuccess = MutableStateFlow(false)
    private val _showAboutDialog = MutableStateFlow(false)
    private val _ipEsp32 = MutableStateFlow(MomoRepositoryProvider.getIpAtual())
    private val _isDemoToolsUnlocked = MutableStateFlow(false)

    private val repository: MomoRepository
        get() = MomoRepositoryProvider.getRepository()

    private val demoControls: DemoMomoControls?
        get() = MomoRepositoryProvider.getDemoControls()

    private data class LocalUiControls(
        val ip: String,
        val isCalibrating: Boolean,
        val calibrationSuccess: Boolean,
        val showAboutDialog: Boolean,
        val isDemoToolsUnlocked: Boolean
    )

    private val _localControlsFlow = combine(
        _ipEsp32,
        _isCalibrating,
        _calibrationSuccess,
        _showAboutDialog,
        _isDemoToolsUnlocked
    ) { ip, calibrating, calSuccess, aboutDialog, demoUnlocked ->
        LocalUiControls(ip, calibrating, calSuccess, aboutDialog, demoUnlocked)
    }

    val uiState: StateFlow<DeviceSettingsUiState> = MomoRepositoryProvider.modoAtual.flatMapLatest { modo ->
        val activeRepo = MomoRepositoryProvider.getRepository()
        authRepository.authStateFlow().flatMapLatest { user ->
            val babyProfileFlow: Flow<Pair<BabyProfile?, Boolean>> = if (user != null && modo != ModoRepositorio.DEMO) {
                firebaseProfileRepo.observeBabyProfile(user.uid).map { profile ->
                    Pair(profile, false)
                }
            } else {
                activeRepo.getBabyProfile().map { profile ->
                    Pair(profile, false)
                }
            }

            combine(
                _localControlsFlow,
                activeRepo.getDeviceState(),
                activeRepo.getDiaperCurrentState(),
                babyProfileFlow.onStart { emit(Pair(null, true)) }
            ) { local, device, diaper, babyProfilePair ->
                DeviceSettingsUiState(
                    babyProfile = babyProfilePair.first,
                    isLoadingBabyProfile = babyProfilePair.second,
                    deviceState = device,
                    diaperState = diaper,
                    isCalibrating = local.isCalibrating,
                    calibrationSuccess = local.calibrationSuccess,
                    showAboutDialog = local.showAboutDialog,
                    modoRepositorio = modo,
                    ipEsp32 = local.ip,
                    currentUser = user,
                    isDemoToolsUnlocked = local.isDemoToolsUnlocked
                )
            }.onStart { emit(DeviceSettingsUiState(modoRepositorio = modo, currentUser = user)) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeviceSettingsUiState(
            currentUser = authRepository.currentUser,
            isLoadingBabyProfile = true,
            modoRepositorio = MomoRepositoryProvider.modoAtual.value,
            isDemoToolsUnlocked = false
        )
    )

    init {
        viewModelScope.launch {
            MomoRepositoryProvider.modoAtual.collectLatest {
                calibrationJob?.cancel()
                _calibrationSuccess.value = false
                _isCalibrating.value = false
            }
        }
    }

    fun unlockDemoTools() {
        _isDemoToolsUnlocked.value = true
    }

    fun lockDemoTools() {
        _isDemoToolsUnlocked.value = false
    }

    fun alternarModoRepositorio(novoModo: ModoRepositorio, novoIp: String = _ipEsp32.value) {
        _ipEsp32.value = novoIp
        MomoRepositoryProvider.alternarModo(novoModo, novoIp)
    }

    fun atualizarIpEsp32(novoIp: String) {
        _ipEsp32.value = novoIp
        MomoRepositoryProvider.alternarModo(MomoRepositoryProvider.modoAtual.value, novoIp)
    }

    fun calibrateNewDiaper(onComplete: () -> Unit = {}) {
        if (calibrationJob?.isActive == true) return
        calibrationJob = viewModelScope.launch {
            _isCalibrating.value = true
            try {
                repository.calibrateNewDiaper()
                _calibrationSuccess.value = true
                if (_calibrationSuccess.value) onComplete()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { operationError.value = e.message }
            finally { _isCalibrating.value = false }
        }
    }

    fun dismissCalibrationSuccess() {
        _calibrationSuccess.value = false
    }

    fun openAboutDialog() {
        _showAboutDialog.value = true
    }

    fun dismissAboutDialog() {
        _showAboutDialog.value = false
    }

    fun signOut(context: Context, onSignedOut: () -> Unit) {
        viewModelScope.launch {
            _isDemoToolsUnlocked.value = false
            authRepository.signOut(context)
            onSignedOut()
        }
    }

    fun simulateStatus(newStatus: EstadoFralda) {
        viewModelScope.launch {
            demoControls?.simulateDiaperStatus(newStatus)
        }
    }

    fun toggleDeviceOnline() {
        viewModelScope.launch {
            val currentOnline = uiState.value.deviceState.online
            demoControls?.simulateDeviceConnection(!currentOnline)
        }
    }

    fun triggerUrineEvent() {
        viewModelScope.launch {
            demoControls?.simulateUrineEvent()
        }
    }

    fun triggerCalibrationInProgress() {
        viewModelScope.launch {
            demoControls?.simulateCalibrationInProgress()
        }
    }

    fun triggerCalibrationComplete() {
        viewModelScope.launch {
            demoControls?.simulateCalibrationComplete()
        }
    }

    fun triggerFecesEvent() {
        viewModelScope.launch {
            demoControls?.simulateFecesEvent()
        }
    }

    fun triggerDiarrheaEvent() {
        viewModelScope.launch {
            demoControls?.simulateDiarrheaEvent()
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            demoControls?.resetAllDemoData()
        }
    }

    fun resetOnboardingAndSetup(onDone: () -> Unit) {
        viewModelScope.launch {
            demoControls?.resetAllDemoData()
            repository.setOnboardingCompleted(false)
            onDone()
        }
    }
}
