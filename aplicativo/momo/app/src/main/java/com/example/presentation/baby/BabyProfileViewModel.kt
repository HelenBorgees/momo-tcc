package com.example.presentation.baby

import android.content.Context
import android.net.Uri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BabyProfile
import com.example.data.repository.FirebaseBabyProfileRepository
import com.example.data.repository.MomoRepository
import com.example.data.repository.MomoRepositoryProvider
import com.example.data.storage.BabyPhotoStorageManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BabyProfileUiState(
    val name: String = "",
    val birthDateMillis: Long = 0L,
    val birthDateText: String = "",
    val weightKgText: String = "",
    val photoUri: String? = null,
    val calculatedAge: String = "",
    val isSaving: Boolean = false,
    val isSavedSuccess: Boolean = false,
    val errorMessage: String? = null
)

class BabyProfileViewModel(
    private val repository: MomoRepository = MomoRepositoryProvider.getRepository(),
    private val firebaseProfileRepo: FirebaseBabyProfileRepository = FirebaseBabyProfileRepository.getInstance(),
    private val photoStorageProvider: () -> BabyPhotoStorageManager? = {
        runCatching { BabyPhotoStorageManager.getInstance() }.getOrNull()
    }
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    private val _uiState = MutableStateFlow(BabyProfileUiState())
    val uiState: StateFlow<BabyProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull().takeUnless { MomoRepositoryProvider.modoAtual.value == com.example.data.repository.ModoRepositorio.DEMO }
            if (currentUser != null) {
                // Usuário autenticado via Google: busca de users/{uid}/babyProfile no RTDB
                val remoteProfile = firebaseProfileRepo.getBabyProfile(currentUser.uid)
                val localPhotoUri = photoStorageProvider()?.getPhotoUriString(currentUser.uid)

                if (remoteProfile != null) {
                    val finalPhotoUri = localPhotoUri ?: remoteProfile.photoUri
                    _uiState.value = _uiState.value.copy(
                        name = remoteProfile.name,
                        birthDateMillis = remoteProfile.birthDateMillis,
                        birthDateText = if (remoteProfile.birthDateMillis > 0L) dateFormatter.format(Date(remoteProfile.birthDateMillis)) else "",
                        weightKgText = if (remoteProfile.weightKg > 0f) "%.1f".format(Locale.US, remoteProfile.weightKg) else "",
                        photoUri = finalPhotoUri,
                        calculatedAge = remoteProfile.formattedAge(),
                        errorMessage = null
                    )
                    // Atualiza cache em memória para componentes observadores (Header, Relatórios, etc.)
                    repository.saveBabyProfile(remoteProfile.copy(photoUri = finalPhotoUri))
                } else {
                    // Primeiro acesso: campos iniciam vazios com foto local se já selecionada
                    _uiState.value = BabyProfileUiState(photoUri = localPhotoUri)
                }
            } else {
                // Modo demonstração (sem login Google): perfil isolado simulado para apresentação
                val demoProfile = repository.getBabyProfile().first()
                val demoPhotoUri = photoStorageProvider()?.getPhotoUriString(null)
                if (demoProfile != null) {
                    val finalPhotoUri = demoPhotoUri ?: demoProfile.photoUri
                    _uiState.value = _uiState.value.copy(
                        name = demoProfile.name,
                        birthDateMillis = demoProfile.birthDateMillis,
                        birthDateText = if (demoProfile.birthDateMillis > 0L) dateFormatter.format(Date(demoProfile.birthDateMillis)) else "",
                        weightKgText = if (demoProfile.weightKg > 0f) "%.1f".format(Locale.US, demoProfile.weightKg) else "",
                        photoUri = finalPhotoUri,
                        calculatedAge = demoProfile.formattedAge(),
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name, errorMessage = null)
    }

    fun onBirthDateSelected(millis: Long) {
        val dateStr = dateFormatter.format(Date(millis))
        val profileTemp = BabyProfile(name = _uiState.value.name, birthDateMillis = millis, weightKg = 10f)
        _uiState.value = _uiState.value.copy(
            birthDateMillis = millis,
            birthDateText = dateStr,
            calculatedAge = profileTemp.formattedAge(),
            errorMessage = null
        )
    }

    fun onWeightChange(weight: String) {
        _uiState.value = _uiState.value.copy(weightKgText = weight, errorMessage = null)
    }

    /**
     * Copia a imagem selecionada para o filesDir associada exclusivamente ao UID do usuário autenticado.
     * Preserva a foto anterior caso ocorra erro.
     */
    fun onPhotoPicked(context: Context, sourceUri: Uri) {
        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull().takeUnless { MomoRepositoryProvider.modoAtual.value == com.example.data.repository.ModoRepositorio.DEMO }
        val manager = photoStorageProvider() ?: BabyPhotoStorageManager.getInstance(context)
        val result = manager.savePhoto(sourceUri, currentUser?.uid)

        result.onSuccess { savedUri ->
            _uiState.value = _uiState.value.copy(
                photoUri = savedUri.toString(),
                errorMessage = null
            )
        }.onFailure { _ ->
            _uiState.value = _uiState.value.copy(
                errorMessage = "Não foi possível carregar a imagem selecionada. A foto anterior foi preservada."
            )
        }
    }

    /**
     * Remove a foto do perfil e exclui o arquivo permanente apenas da conta atual.
     */
    fun onPhotoRemoved(context: Context? = null) {
        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull().takeUnless { MomoRepositoryProvider.modoAtual.value == com.example.data.repository.ModoRepositorio.DEMO }
        val manager = photoStorageProvider() ?: context?.let { BabyPhotoStorageManager.getInstance(it) }
        manager?.deletePhoto(currentUser?.uid)
        _uiState.value = _uiState.value.copy(photoUri = null)
    }

    fun onPhotoSelected(uriString: String) {
        _uiState.value = _uiState.value.copy(photoUri = uriString, errorMessage = null)
    }


    fun saveProfile(onSuccess: () -> Unit) {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, informe o nome do bebê.")
            return
        }

        val birthDate = _uiState.value.birthDateMillis
        if (birthDate <= 0L) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, selecione a data de nascimento.")
            return
        }

        val weightStr = _uiState.value.weightKgText.replace(",", ".").trim()
        val weight = weightStr.toFloatOrNull()
        if (weight == null || weight <= 0f) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, informe um peso válido (ex: 11.0).")
            return
        }

        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull().takeUnless { MomoRepositoryProvider.modoAtual.value == com.example.data.repository.ModoRepositorio.DEMO }
        val profile = BabyProfile(
            id = currentUser?.uid ?: "demo",
            name = name,
            birthDateMillis = birthDate,
            weightKg = weight,
            photoUri = _uiState.value.photoUri
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            if (currentUser != null) {
                val result = firebaseProfileRepo.saveBabyProfile(currentUser.uid, profile)
                if (result.isSuccess) {
                    repository.saveBabyProfile(profile)
                    _uiState.value = _uiState.value.copy(isSaving = false, isSavedSuccess = true)
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Falha ao gravar no Firebase: ${result.exceptionOrNull()?.localizedMessage ?: "Erro desconhecido"}"
                    )
                }
            } else {
                repository.saveBabyProfile(profile)
                _uiState.value = _uiState.value.copy(isSaving = false, isSavedSuccess = true)
                onSuccess()
            }
        }
    }
}
