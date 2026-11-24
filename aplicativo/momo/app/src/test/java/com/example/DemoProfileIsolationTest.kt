package com.example

import com.example.data.model.BabyProfile
import com.example.data.repository.DemoMomoRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DemoProfileIsolationTest {
    @Test fun demoProfileCannotKeepARealAccountIdentifier() = runBlocking {
        val demo = DemoMomoRepositoryImpl.getInstance()
        demo.saveBabyProfile(BabyProfile(id = "google-owner-uid", name = "Perfil de teste", weightKg = 4f))
        val stored = demo.getBabyProfile().first()!!
        assertEquals("momo_baby_demo", stored.id)
        assertEquals("Perfil de teste", stored.name)
        assertTrue(demo.getDeviceState().first().isDemo)
        demo.resetAllDemoData()
    }
}
