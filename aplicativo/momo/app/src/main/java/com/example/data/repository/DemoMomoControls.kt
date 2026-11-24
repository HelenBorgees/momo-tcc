package com.example.data.repository

import com.example.data.model.EstadoFralda

/**
 * Interface dedicada a controles de simulação e testes de laboratório.
 * Utilizada exclusivamente no modo de demonstração acadêmica (DemoMomoRepositoryImpl).
 * A implementação real (HttpMomoRepositoryImpl) NÃO implementa esta interface,
 * garantindo que o modo real dependa exclusivamente de telemetria do ESP32-C6.
 */
interface DemoMomoControls {
    suspend fun simulateDiaperStatus(status: EstadoFralda)
    suspend fun simulateDeviceConnection(online: Boolean)
    suspend fun simulateUrineEvent()
    suspend fun simulateFecesEvent()
    suspend fun simulateDiarrheaEvent()
    suspend fun simulateCalibrationInProgress()
    suspend fun simulateCalibrationComplete()
    suspend fun resetAllDemoData()
}
