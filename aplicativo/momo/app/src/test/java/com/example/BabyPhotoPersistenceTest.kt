package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.storage.BabyPhotoStorageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BabyPhotoPersistenceTest {

    private lateinit var context: Context
    private lateinit var storageManager: BabyPhotoStorageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storageManager = BabyPhotoStorageManager.initialize(context)

        // Limpa fotos de testes anteriores no filesDir
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("baby_photo_") || file.name.startsWith("temp_photo_")) {
                file.delete()
            }
        }
    }

    private fun createSampleImage(name: String, content: String): Uri {
        val cacheFile = File(context.cacheDir, name)
        cacheFile.writeText(content)
        return Uri.fromFile(cacheFile)
    }

    @Test
    fun testSavePhoto_copiesPermanentlyToFilesDir_associatedWithUid() {
        val uid = "user_alpha_1"
        val sampleUri = createSampleImage("source_test_1.jpg", "JPEG_DATA_1")

        val result = storageManager.savePhoto(sampleUri, uid)
        assertTrue("Salvamento deve ter sucesso", result.isSuccess)

        val targetFile = storageManager.getPhotoFile(uid)
        assertEquals("baby_photo_user_alpha_1.jpg", targetFile.name)
        assertTrue("Arquivo deve existir no filesDir", targetFile.exists())
        assertEquals("JPEG_DATA_1", targetFile.readText())


        val photoUri = storageManager.getPhotoUri(uid)
        assertNotNull("URI da foto deve ser recuperável", photoUri)
        assertTrue(photoUri.toString().contains("baby_photo_user_alpha_1.jpg"))
    }

    @Test
    fun testReopenApp_retainsPhotoForAccount() {
        val uid = "user_reopen_test"
        val sampleUri = createSampleImage("source_test_2.jpg", "JPEG_PERSISTENT")
        storageManager.savePhoto(sampleUri, uid)

        // Simula recriação do gerenciador (fechar e reabrir o app)
        val newManagerInstance = BabyPhotoStorageManager.initialize(context)
        val loadedUri = newManagerInstance.getPhotoUri(uid)

        assertNotNull("Foto deve estar disponível ao reabrir o app", loadedUri)
        val file = newManagerInstance.getPhotoFile(uid)
        assertTrue(file.exists())
        assertEquals("JPEG_PERSISTENT", file.readText())
    }

    @Test
    fun testUserIsolation_switchingAccountsShowsOnlyRespectivePhotos() {
        val user1 = "user_111"
        val user2 = "user_222"

        val imgUser1 = createSampleImage("img_1.jpg", "FOTO_BEBE_USER_1")
        storageManager.savePhoto(imgUser1, user1)

        // Usuário 2 ainda não possui foto
        assertNull("Usuário 2 não deve ver a foto do Usuário 1", storageManager.getPhotoUri(user2))

        // Usuário 2 adiciona sua própria foto
        val imgUser2 = createSampleImage("img_2.jpg", "FOTO_BEBE_USER_2")
        storageManager.savePhoto(imgUser2, user2)

        // Cada usuário acessa estritamente seu próprio arquivo
        val file1 = storageManager.getPhotoFile(user1)
        val file2 = storageManager.getPhotoFile(user2)

        assertEquals("FOTO_BEBE_USER_1", file1.readText())
        assertEquals("FOTO_BEBE_USER_2", file2.readText())
    }

    @Test
    fun testUpdatePhoto_replacesPreviousFileAtomically() {
        val uid = "user_update_test"
        val imgV1 = createSampleImage("v1.jpg", "CONTEUDO_FOTO_ANTIGA")
        storageManager.savePhoto(imgV1, uid)

        val imgV2 = createSampleImage("v2.jpg", "CONTEUDO_FOTO_NOVA")
        val updateResult = storageManager.savePhoto(imgV2, uid)
        assertTrue(updateResult.isSuccess)

        val file = storageManager.getPhotoFile(uid)
        assertEquals("CONTEUDO_FOTO_NOVA", file.readText())
    }

    @Test
    fun testRemovePhoto_deletesOnlyTargetAccountFile() {
        val userA = "user_keep"
        val userB = "user_delete"

        storageManager.savePhoto(createSampleImage("keep.jpg", "MANTER"), userA)
        storageManager.savePhoto(createSampleImage("delete.jpg", "EXCLUIR"), userB)

        // Remove a foto do usuário B
        val deleted = storageManager.deletePhoto(userB)
        assertTrue("Exclusão deve retornar sucesso", deleted)

        assertNull("Foto do usuário B não deve mais existir", storageManager.getPhotoUri(userB))
        assertNotNull("Foto do usuário A deve permanecer intacta", storageManager.getPhotoUri(userA))
        assertEquals("MANTER", storageManager.getPhotoFile(userA).readText())
    }

    @Test
    fun testSaveFailure_preservesPreviousPhotoIntact() {
        val uid = "user_error_test"
        val goodImage = createSampleImage("good.jpg", "FOTO_ORIGINAL_VALIDA")
        storageManager.savePhoto(goodImage, uid)

        // Tenta salvar apontando para arquivo inexistente
        val invalidUri = Uri.fromFile(File(context.cacheDir, "non_existent_file.jpg"))
        val failResult = storageManager.savePhoto(invalidUri, uid)

        assertFalse("Tentativa deve falhar", failResult.isSuccess)

        // Verifica que a foto anterior foi preservada intacta
        val preservedFile = storageManager.getPhotoFile(uid)
        assertTrue("Arquivo anterior deve continuar existindo", preservedFile.exists())
        assertEquals("FOTO_ORIGINAL_VALIDA", preservedFile.readText())
    }

    @Test
    fun testDemoMode_usesDedicatedDemoFile() {
        val demoUri = createSampleImage("demo_sample.jpg", "FOTO_DEMO")
        storageManager.savePhoto(demoUri, null)

        val demoFile = storageManager.getPhotoFile(null)
        assertEquals("baby_photo_demo.jpg", demoFile.name)
        assertEquals("FOTO_DEMO", demoFile.readText())

        // UID real não é afetado pelo demo
        assertNull(storageManager.getPhotoUri("real_user_uid"))
    }
}
