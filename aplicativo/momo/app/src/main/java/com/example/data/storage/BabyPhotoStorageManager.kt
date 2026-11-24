package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Gerenciador de persistência local da fotografia do bebê no armazenamento interno (filesDir).
 *
 * Características:
 * 1. Arquivo permanente associado exclusivamente ao UID autenticado (ex: baby_photo_<uid>.jpg).
 * 2. Isolamento rigoroso entre contas Google e modo demonstração.
 * 3. Gravação atômica segura via arquivo temporário: em caso de falha na cópia, a foto anterior é preservada.
 * 4. Cache-busting automático para Coil via query parameter ?v=<lastModified>, garantindo atualização instantânea do avatar.
 * 5. Nenhuma sincronização de arquivo com nuvem (sem Base64 e sem Firebase Storage).
 */
class BabyPhotoStorageManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext

    /**
     * Retorna o arquivo de imagem no filesDir para o UID especificado.
     */
    fun getPhotoFile(uid: String?): File {
        val safeUid = if (!uid.isNullOrBlank()) {
            uid.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        } else {
            "demo"
        }
        return File(appContext.filesDir, "baby_photo_${safeUid}.jpg")
    }

    /**
     * Retorna a URI com versão da foto local se o arquivo existir e for válido.
     */
    fun getPhotoUri(uid: String?): Uri? {
        val file = getPhotoFile(uid)
        return if (file.exists() && file.length() > 0) {
            Uri.fromFile(file).buildUpon()
                .appendQueryParameter("v", file.lastModified().toString())
                .build()
        } else {
            null
        }
    }

    /**
     * Retorna a representação textual da URI da foto local.
     */
    fun getPhotoUriString(uid: String?): String? = getPhotoUri(uid)?.toString()

    /**
     * Copia com segurança a foto da sourceUri para o arquivo definitivo do UID.
     * Grava primeiro em arquivo temporário (.tmp). Apenas se a cópia for integralmente concluída,
     * substitui o arquivo permanente daquele UID.
     */
    fun savePhoto(sourceUri: Uri, uid: String?): Result<Uri> {
        val targetFile = getPhotoFile(uid)
        val tempFile = File(appContext.filesDir, "temp_photo_${System.currentTimeMillis()}.tmp")

        return try {
            val inputStream = when (sourceUri.scheme) {
                "file" -> {
                    val path = sourceUri.path
                    if (path != null && File(path).exists()) File(path).inputStream() else null
                }
                else -> appContext.contentResolver.openInputStream(sourceUri)
            } ?: return Result.failure(IllegalStateException("Não foi possível acessar a imagem selecionada."))


            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!tempFile.exists() || tempFile.length() <= 0) {
                if (tempFile.exists()) tempFile.delete()
                return Result.failure(IllegalStateException("Arquivo de imagem vazio ou inacessível."))
            }

            // Exclui versão anterior e substitui pelo novo arquivo
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val moved = tempFile.renameTo(targetFile)
            if (!moved) {
                // Fallback de cópia direta caso renameTo falhe
                tempFile.inputStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.delete()
            }

            targetFile.setLastModified(System.currentTimeMillis())

            val finalUri = Uri.fromFile(targetFile).buildUpon()
                .appendQueryParameter("v", targetFile.lastModified().toString())
                .build()

            Log.d(TAG, "Foto salva com sucesso em ${targetFile.name} para uid=$uid")
            Result.success(finalUri)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao gravar foto para uid=$uid. Foto anterior preservada.", e)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Result.failure(e)
        }
    }

    /**
     * Exclui o arquivo de foto local apenas do UID informado.
     */
    fun deletePhoto(uid: String?): Boolean {
        return try {
            val file = getPhotoFile(uid)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Foto local excluída para uid=$uid: $deleted")
                deleted
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir foto local para uid=$uid", e)
            false
        }
    }

    companion object {
        private const val TAG = "BabyPhotoStorage"

        @Volatile
        private var instance: BabyPhotoStorageManager? = null

        fun initialize(context: Context): BabyPhotoStorageManager {
            return instance ?: synchronized(this) {
                instance ?: BabyPhotoStorageManager(context.applicationContext).also { instance = it }
            }
        }

        fun getInstance(context: Context? = null): BabyPhotoStorageManager {
            val current = instance
            if (current != null) return current
            if (context != null) return initialize(context)
            throw IllegalStateException("BabyPhotoStorageManager não foi inicializado.")
        }
    }
}
