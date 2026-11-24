package com.example.data.repository

import java.util.UUID

object ConfirmationIdentity {
    // UUID de 36 caracteres cabe nos buffers char[48] do firmware.
    fun forCycle(deviceUid: String, cycle: Int): String =
        UUID.nameUUIDFromBytes("$deviceUid:$cycle".toByteArray(Charsets.UTF_8)).toString()
}
