package com.example

import com.example.data.remote.RespostaEsp32Dto
import com.example.data.remote.WifiSetupRequestDto
import com.example.data.repository.ConfirmationIdentity
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class DeviceProtocolTest {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    @Test fun missingSuccessIsRejected() {
        try {
            moshi.adapter(RespostaEsp32Dto::class.java).fromJson("{}")
            fail("Resposta vazia não pode significar sucesso")
        } catch (_: JsonDataException) { }
    }
    @Test fun firmwareReplyPreservesTechnicalUid() {
        val response = moshi.adapter(RespostaEsp32Dto::class.java)
            .fromJson("{\"sucesso\":true,\"deviceUid\":\"hardware-uid\"}")!!
        assertTrue(response.sucesso)
        assertEquals("hardware-uid", response.deviceUid)
    }
    @Test fun setupUsesFirmwareFieldNames() {
        val json = moshi.adapter(WifiSetupRequestDto::class.java)
            .toJson(WifiSetupRequestDto("rede", "senha-teste", "owner", "MOMO-0428"))
        val parsed = moshi.adapter(Map::class.java).fromJson(json)!!
        assertEquals("owner", parsed["ownerUid"])
        assertEquals("rede", parsed["ssid"])
        assertEquals("senha-teste", parsed["senha"])
    }
    @Test fun confirmationIsStableAndFitsFirmwareBuffer() {
        val key = ConfirmationIdentity.forCycle("technical-uid", Int.MAX_VALUE)
        assertEquals(key, ConfirmationIdentity.forCycle("technical-uid", Int.MAX_VALUE))
        assertNotEquals(key, ConfirmationIdentity.forCycle("technical-uid", 1))
        assertNotEquals(key, ConfirmationIdentity.forCycle("other-device", Int.MAX_VALUE))
        assertTrue(key.length < 48)
    }
}
