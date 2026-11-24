package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.example.data.repository.FirebaseBabyProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String, val isCollision: Boolean = false) : AuthResult()
    object Cancelled : AuthResult()
}

class AuthRepository(
    private val authProvider: () -> FirebaseAuth? = { runCatching { Firebase.auth }.getOrNull() }
) {
    val auth: FirebaseAuth?
        get() = authProvider()

    val currentUser: FirebaseUser?
        get() = runCatching { auth?.currentUser }.getOrNull()

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val currentAuth = auth
        if (currentAuth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        currentAuth.addAuthStateListener(listener)
        awaitClose { currentAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(context: Context): AuthResult {
        return try {
            val credentialManager = CredentialManager.create(context)
            val serverClientId = context.getString(R.string.default_web_client_id)

            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val currentAuth = auth ?: runCatching { Firebase.auth }.getOrNull()
                ?: return AuthResult.Error("Firebase não inicializado.")

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = currentAuth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) {
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error(context.getString(R.string.auth_error_generic))
                }
            } else {
                AuthResult.Error("Tipo de credencial não suportado pelo sistema.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign in cancelled by user", e)
            AuthResult.Cancelled
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.w(TAG, "Account collision: email exists with other credential", e)
            AuthResult.Error(
                message = context.getString(R.string.auth_error_collision),
                isCollision = true
            )
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e(TAG, "Invalid credentials", e)
            AuthResult.Error("Credencial do Google inválida ou expirada. Tente novamente.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "CredentialManager failed", e)
            AuthResult.Error(e.localizedMessage ?: context.getString(R.string.auth_error_generic))
        } catch (e: Exception) {
            Log.e(TAG, "Firebase auth failed", e)
            AuthResult.Error(e.localizedMessage ?: context.getString(R.string.auth_error_generic))
        }
    }

    suspend fun signOut(context: Context) {
        try {
            auth?.signOut()
            FirebaseBabyProfileRepository.getInstance().clearCache()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing credential state on signOut", e)
        }
    }

    companion object {
        private const val TAG = "AuthRepository"

        @Volatile
        private var instance: AuthRepository? = null

        fun getInstance(): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository().also { instance = it }
            }
        }
    }
}
