package com.example.data.repository

import android.util.Log
import com.example.data.model.BabyProfile
import com.example.data.storage.BabyPhotoStorageManager
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositório para persistência do cadastro do bebê no Firebase Realtime Database.
 *
 * Estrutura: users/{uid}/babyProfile
 *
 * Garante que:
 * 1. Os dados da criança sejam associados exclusivamente ao UID da sessão Google.
 * 2. Nenhuma conta acesse ou veja dados de outra conta.
 * 3. Dados demonstrativos não sejam gravados no banco real nem usados como preenchimento.
 * 4. A fotografia é mantida exclusivamente no armazenamento interno do celular (filesDir),
 *    associada ao UID da conta, sem envio de Base64 para a nuvem.
 */
class FirebaseBabyProfileRepository(
    private val databaseProvider: () -> FirebaseDatabase? = {
        runCatching {
            Firebase.database
        }.getOrNull()
    },
    private val photoStorageProvider: () -> BabyPhotoStorageManager? = {
        runCatching { BabyPhotoStorageManager.getInstance() }.getOrNull()
    }
) {

    private val database: FirebaseDatabase?
        get() = databaseProvider()

    private val _cachedProfile = MutableStateFlow<BabyProfile?>(null)
    val cachedProfile: Flow<BabyProfile?> = _cachedProfile.asStateFlow()

    suspend fun fetchBabyProfile(uid: String): Result<BabyProfile?> {
        val db = database ?: return Result.failure(IllegalStateException("Firebase Realtime Database não inicializado."))
        return try {
            val snapshot = db.reference
                .child("users")
                .child(uid)
                .child("babyProfile")
                .get()
                .await()

            if (snapshot.exists()) {
                val profile = parseSnapshot(uid, snapshot)
                _cachedProfile.value = profile
                Result.success(profile)
            } else {
                _cachedProfile.value = null
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao recuperar cadastro do bebê no Realtime Database para uid=$uid", e)
            Result.failure(e)
        }
    }

    suspend fun getBabyProfile(uid: String): BabyProfile? {
        return fetchBabyProfile(uid).getOrNull()
    }


    suspend fun saveBabyProfile(uid: String, profile: BabyProfile): Result<Unit> {
        val db = database ?: return Result.failure(IllegalStateException("Firebase Realtime Database não inicializado."))
        return try {
            val data = mapOf(
                "name" to profile.name,
                "birthDateMillis" to profile.birthDateMillis,
                "weightKg" to profile.weightKg.toDouble()
            )
            db.reference
                .child("users")
                .child(uid)
                .child("babyProfile")
                .setValue(data)
                .await()

            val localPhoto = photoStorageProvider()?.getPhotoUriString(uid) ?: profile.photoUri
            _cachedProfile.value = profile.copy(photoUri = localPhoto)
            Log.d(TAG, "Cadastro do bebê salvo com sucesso no Realtime Database para uid=$uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao gravar cadastro do bebê no Realtime Database para uid=$uid", e)
            Result.failure(e)
        }
    }

    fun observeBabyProfile(uid: String): Flow<BabyProfile?> = callbackFlow {
        val db = database
        if (db == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val ref = db.reference.child("users").child(uid).child("babyProfile")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val profile = parseSnapshot(uid, snapshot)
                    _cachedProfile.value = profile
                    trySend(profile)
                } else {
                    _cachedProfile.value = null
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _cachedProfile.value = null
                trySend(null)
                Log.w(TAG, "Listener do Realtime Database cancelado: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun clearCache() {
        _cachedProfile.value = null
    }

    private fun parseSnapshot(uid: String, snapshot: DataSnapshot): BabyProfile? {
        val name = snapshot.child("name").getValue(String::class.java)?.trim() ?: return null
        val birthDateMillis = snapshot.child("birthDateMillis").getValue(Long::class.java) ?: 0L
        val weight = when (val rawWeight = snapshot.child("weightKg").value) {
            is Double -> rawWeight.toFloat()
            is Long -> rawWeight.toFloat()
            is Number -> rawWeight.toFloat()
            else -> 0f
        }

        val localPhoto = photoStorageProvider()?.getPhotoUriString(uid)

        return BabyProfile(
            id = uid,
            name = name,
            birthDateMillis = birthDateMillis,
            weightKg = weight,
            photoUri = localPhoto
        )
    }


    companion object {
        private const val TAG = "FirebaseBabyProfileRepo"

        @Volatile
        private var instance: FirebaseBabyProfileRepository? = null

        fun getInstance(): FirebaseBabyProfileRepository {
            return instance ?: synchronized(this) {
                instance ?: FirebaseBabyProfileRepository().also { instance = it }
            }
        }
    }
}
