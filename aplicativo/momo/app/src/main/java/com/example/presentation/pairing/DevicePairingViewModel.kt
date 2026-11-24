package com.example.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DeviceState
import com.example.data.repository.HttpMomoRepositoryImpl
import com.example.data.repository.MomoRepository
import com.example.data.repository.MomoRepositoryProvider
import com.example.data.repository.ModoRepositorio
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PairingStep(val stepNumber: Int, val title: String) {
    PREPARE(1, "Preparando dispositivo"),
    CONNECT_SOFTAP(2, "Conectar ao MOMO-SETUP"),
    CONFIGURE_WIFI(3, "Configurar Wi-Fi"),
    VERIFYING(4, "Verificar no Firebase"),
    SUCCESS(5, "Dispositivo conectado")
}

/**
 * Estados distintos de pareamento conforme Requisito 3 da especificação:
 * - NAO_PAREADO
 * - AGUARDANDO_CONFIGURACAO
 * - ENVIANDO_WIFI
 * - AGUARDANDO_CONFIRMACAO_FIREBASE
 * - ONLINE
 * - OFFLINE
 * - ERRO
 */
enum class PairingStatus(val rotulo: String) {
    NAO_PAREADO("NÃO PAREADO"),
    AGUARDANDO_CONFIGURACAO("AGUARDANDO CONFIGURAÇÃO"),
    ENVIANDO_WIFI("ENVIANDO WI-FI"),
    AGUARDANDO_CONFIRMACAO_FIREBASE("AGUARDANDO CONFIRMAÇÃO DO FIREBASE"),
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    ERRO("ERRO")
}

data class DevicePairingUiState(
    val currentStep: PairingStep = PairingStep.PREPARE,
    val pairingStatus: PairingStatus = PairingStatus.NAO_PAREADO,
    val selectedSsid: String = "",
    val wifiPassword: String = "",
    val deviceId: String = "MOMO-0428",
    val isProcessing: Boolean = false,
    val isCheckingFirebase: Boolean = false,
    val firebaseCheckFailed: Boolean = false,
    val isAutoNavigating: Boolean = false,
    val error: String? = null,
    val isDemoMode: Boolean = false
)

class DevicePairingViewModel(
    private val repository: MomoRepository = MomoRepositoryProvider.getRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DevicePairingUiState(
            isDemoMode = MomoRepositoryProvider.modoAtual.value == ModoRepositorio.DEMO
        )
    )
    val uiState: StateFlow<DevicePairingUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            isDemoMode = MomoRepositoryProvider.modoAtual.value == ModoRepositorio.DEMO,
            pairingStatus = PairingStatus.NAO_PAREADO
        )
    }

    fun nextStep() {
        when (_uiState.value.currentStep) {
            PairingStep.PREPARE -> {
                _uiState.value = _uiState.value.copy(
                    currentStep = PairingStep.CONNECT_SOFTAP,
                    pairingStatus = PairingStatus.AGUARDANDO_CONFIGURACAO,
                    error = null
                )
            }
            PairingStep.CONNECT_SOFTAP -> {
                _uiState.value = _uiState.value.copy(
                    currentStep = PairingStep.CONFIGURE_WIFI,
                    pairingStatus = PairingStatus.AGUARDANDO_CONFIGURACAO,
                    error = null
                )
            }
            PairingStep.CONFIGURE_WIFI -> {
                startVerifying()
            }
            PairingStep.VERIFYING -> {
                startFirebaseVerification()
            }
            PairingStep.SUCCESS -> {
                // Tratado pela navegação da tela
            }
        }
    }

    fun startSimulatedAutoPairing(onAutoFinish: () -> Unit) {
        // Exclusivo do modo DEMO
        if (MomoRepositoryProvider.modoAtual.value != ModoRepositorio.DEMO) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAutoNavigating = true,
                isProcessing = true,
                pairingStatus = PairingStatus.AGUARDANDO_CONFIGURACAO
            )
            // 1. Preparing
            _uiState.value = _uiState.value.copy(currentStep = PairingStep.PREPARE)
            delay(600)
            // 2. SoftAP
            _uiState.value = _uiState.value.copy(currentStep = PairingStep.CONNECT_SOFTAP)
            delay(600)
            // 3. Wi-Fi configured
            _uiState.value = _uiState.value.copy(
                currentStep = PairingStep.CONFIGURE_WIFI,
                pairingStatus = PairingStatus.ENVIANDO_WIFI
            )
            delay(600)
            // 4. Verifying
            _uiState.value = _uiState.value.copy(
                currentStep = PairingStep.VERIFYING,
                pairingStatus = PairingStatus.AGUARDANDO_CONFIRMACAO_FIREBASE
            )
            delay(600)
            // 5. Complete
            repository.updateDeviceState(
                DeviceState(
                    idDispositivo = _uiState.value.deviceId,
                    nomeModelo = "Simulador MOMO (Demonstração)",
                    online = true,
                    pareado = true,
                    isDemo = true,
                    wifiSsid = if (_uiState.value.selectedSsid.isBlank()) "Ambiente Simulado" else _uiState.value.selectedSsid,
                    calibrado = true,
                    horarioUltimaComunicacao = "Agora (Simulado)",
                    versaoFirmware = "v1.0-esp32c6"
                )
            )
            repository.setOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(
                currentStep = PairingStep.SUCCESS,
                pairingStatus = PairingStatus.ONLINE,
                isProcessing = false
            )
            delay(600)
            onAutoFinish()
        }
    }

    fun previousStep() {
        if (_uiState.value.isProcessing || _uiState.value.isCheckingFirebase) return
        val prev = when (_uiState.value.currentStep) {
            PairingStep.PREPARE -> PairingStep.PREPARE
            PairingStep.CONNECT_SOFTAP -> PairingStep.PREPARE
            PairingStep.CONFIGURE_WIFI -> PairingStep.CONNECT_SOFTAP
            PairingStep.VERIFYING -> PairingStep.CONFIGURE_WIFI
            PairingStep.SUCCESS -> PairingStep.CONFIGURE_WIFI
        }
        val status = when (prev) {
            PairingStep.PREPARE -> PairingStatus.NAO_PAREADO
            else -> PairingStatus.AGUARDANDO_CONFIGURACAO
        }
        _uiState.value = _uiState.value.copy(
            currentStep = prev,
            pairingStatus = status,
            error = null,
            firebaseCheckFailed = false
        )
    }

    fun onSsidChange(ssid: String) {
        _uiState.value = _uiState.value.copy(selectedSsid = ssid, error = null)
    }

    fun onPasswordChange(pass: String) {
        _uiState.value = _uiState.value.copy(wifiPassword = pass, error = null)
    }

    fun onDeviceIdChange(id: String) {
        _uiState.value = _uiState.value.copy(deviceId = id)
    }

    fun skipToSuccess() {
        if (MomoRepositoryProvider.modoAtual.value != ModoRepositorio.DEMO) return
        viewModelScope.launch {
            repository.updateDeviceState(
                DeviceState(
                    idDispositivo = _uiState.value.deviceId,
                    nomeModelo = "Simulador MOMO (Demonstração)",
                    online = true,
                    pareado = true,
                    isDemo = true,
                    wifiSsid = if (_uiState.value.selectedSsid.isBlank()) "Ambiente Simulado" else _uiState.value.selectedSsid,
                    calibrado = true,
                    horarioUltimaComunicacao = "Agora (Simulado)",
                    versaoFirmware = "v1.0-esp32c6"
                )
            )
            repository.setOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(
                currentStep = PairingStep.SUCCESS,
                pairingStatus = PairingStatus.ONLINE,
                isProcessing = false,
                isCheckingFirebase = false
            )
        }
    }

    /**
     * Envia a configuração de Wi-Fi para o ESP32-C6 via SoftAP HTTP POST http://192.168.4.1/wifi-setup.
     * Envia ssid, senha, ownerUid e idDispositivo. Não finge sucesso se a resposta for inválida.
     */
    private fun startVerifying() {
        if (_uiState.value.isProcessing || _uiState.value.isCheckingFirebase) return
        val ssid = _uiState.value.selectedSsid.trim()
        val senha = _uiState.value.wifiPassword

        if (ssid.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Por favor, digite o nome da rede Wi-Fi (SSID).",
                pairingStatus = PairingStatus.ERRO
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                pairingStatus = PairingStatus.ENVIANDO_WIFI,
                error = null
            )

            val isDemo = MomoRepositoryProvider.modoAtual.value == ModoRepositorio.DEMO
            if (isDemo) {
                delay(700)
                repository.configurarWifi(
                    ssid = ssid,
                    senha = senha,
                    ownerUid = "demo_uid",
                    idDispositivo = _uiState.value.deviceId
                )
                repository.updateDeviceState(
                    DeviceState(
                        idDispositivo = _uiState.value.deviceId,
                        nomeModelo = "Simulador MOMO (Demonstração)",
                        online = true,
                        pareado = true,
                        isDemo = true,
                        wifiSsid = ssid,
                        calibrado = true,
                        horarioUltimaComunicacao = "Agora (Simulado)",
                        versaoFirmware = "v1.0-esp32c6"
                    )
                )
                repository.setOnboardingCompleted(true)
                _uiState.value = _uiState.value.copy(
                    currentStep = PairingStep.SUCCESS,
                    pairingStatus = PairingStatus.ONLINE,
                    isProcessing = false
                )
            } else {
                // No modo REAL:
                val user = runCatching { Firebase.auth.currentUser }.getOrNull()
                val ownerUid = user?.uid ?: ""

                val sucessoSoftAp = repository.configurarWifi(
                    ssid = ssid,
                    senha = senha,
                    ownerUid = ownerUid,
                    idDispositivo = _uiState.value.deviceId
                )

                if (sucessoSoftAp) {
                    _uiState.value = _uiState.value.copy(
                        currentStep = PairingStep.VERIFYING,
                        pairingStatus = PairingStatus.AGUARDANDO_CONFIRMACAO_FIREBASE,
                        isProcessing = false,
                        isCheckingFirebase = false,
                        firebaseCheckFailed = false,
                        wifiPassword = "",
                        error = null
                    )
                    // Retorne à internet e pressione verificar; não simular confirmação.
                } else {
                    val erroRepo = (repository as? HttpMomoRepositoryImpl)?.ultimoErroWifiSetup
                    val msgErro = erroRepo ?: "Não foi possível conectar ao ESP32-C6 em http://192.168.4.1. Verifique se o smartphone está conectado à rede Wi-Fi temporária MOMO-SETUP-0428."
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        pairingStatus = PairingStatus.ERRO,
                        error = msgErro
                    )
                }
            }
        }
    }

    /**
     * Consulta o Firebase Realtime Database para validar se o ESP32-C6 conectou
     * à internet e está publicando dados recentes em deviceStatus/{ownerUid}.
     * Não utiliza temporizador simulado.
     */
    fun startFirebaseVerification() {
        if (_uiState.value.isCheckingFirebase) return
        val isDemo = MomoRepositoryProvider.modoAtual.value == ModoRepositorio.DEMO
        if (isDemo) {
            skipToSuccess()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingFirebase = true,
                firebaseCheckFailed = false,
                pairingStatus = PairingStatus.AGUARDANDO_CONFIRMACAO_FIREBASE,
                error = null
            )

            // Aguarda até 30 segundos verificando periodicamente no Firebase RTDB
            val verificado = repository.verificarDispositivoFirebase(timeoutSeconds = 30)

            if (verificado) {
                repository.setOnboardingCompleted(true)
                _uiState.value = _uiState.value.copy(
                    currentStep = PairingStep.SUCCESS,
                    pairingStatus = PairingStatus.ONLINE,
                    isCheckingFirebase = false,
                    firebaseCheckFailed = false,
                    error = null
                )
            } else {
                val erroFirebase = (repository as? HttpMomoRepositoryImpl)?.ultimoErroFirebase
                val msg = erroFirebase ?: "Ainda não detectamos o sinal do ESP32 no Firebase. Certifique-se de que reconectou seu celular à internet (Wi-Fi residencial ou 4G/5G) e que o monitor está ligado."
                _uiState.value = _uiState.value.copy(
                    isCheckingFirebase = false,
                    firebaseCheckFailed = true,
                    pairingStatus = PairingStatus.ERRO,
                    error = msg
                )
            }
        }
    }
}
