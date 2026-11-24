package com.example.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.repository.MomoRepositoryProvider
import com.example.data.repository.ModoRepositorio
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCollisionError: Boolean = false,
    val isDemoMode: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository.getInstance()
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isCollisionError = MutableStateFlow(false)

    val uiState: StateFlow<AuthUiState> = combine(
        authRepository.authStateFlow(),
        _isLoading,
        _errorMessage,
        _isCollisionError,
        MomoRepositoryProvider.modoAtual
    ) { user, loading, error, collision, demo ->
        AuthUiState(
            currentUser = user,
            isLoading = loading,
            errorMessage = error,
            isCollisionError = collision,
            isDemoMode = demo == ModoRepositorio.DEMO
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthUiState(
            currentUser = authRepository.currentUser,
            isDemoMode = MomoRepositoryProvider.modoAtual.value == ModoRepositorio.DEMO
        )
    )

    fun signInWithGoogle(context: Context, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _isCollisionError.value = false

            when (val result = authRepository.signInWithGoogle(context)) {
                is AuthResult.Success -> {
                    _isLoading.value = false
                    MomoRepositoryProvider.alternarModo(ModoRepositorio.HTTP_ESP32)
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                    _isCollisionError.value = result.isCollision
                }
                is AuthResult.Cancelled -> {
                    _isLoading.value = false
                    // Silent cancellation, no error disruption
                }
            }
        }
    }

    fun signOut(context: Context, onSignedOut: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.signOut(context)
            _isLoading.value = false
            MomoRepositoryProvider.alternarModo(ModoRepositorio.HTTP_ESP32)
            onSignedOut()
        }
    }

    fun enterDemoMode(onEnter: () -> Unit) {
        _errorMessage.value = null
        MomoRepositoryProvider.alternarModo(ModoRepositorio.DEMO)
        onEnter()
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
        _isCollisionError.value = false
    }

    fun dismissError() {
        _errorMessage.value = null
        _isCollisionError.value = false
    }
}

