package com.example.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ModoRepositorio(val rotulo: String) {
    DEMO("Demonstração (Simulado)"),
    HTTP_ESP32("Hardware Real (ESP32-C6)")
}

/**
 * Provedor centralizado do repositório MOMO.
 * Garante que o modo REAL seja o padrão operacional e que o modo DEMO
 * somente seja utilizado sob escolha expressa do usuário.
 */
object MomoRepositoryProvider {

    private val _modoAtual = MutableStateFlow(ModoRepositorio.HTTP_ESP32)
    val modoAtual: StateFlow<ModoRepositorio> = _modoAtual.asStateFlow()

    private var enderecoIpEsp32: String = "192.168.4.1"

    fun getRepository(): MomoRepository {
        return when (_modoAtual.value) {
            ModoRepositorio.DEMO -> DemoMomoRepositoryImpl.getInstance()
            ModoRepositorio.HTTP_ESP32 -> HttpMomoRepositoryImpl.getInstance(enderecoIpEsp32)
        }
    }

    fun getDemoControls(): DemoMomoControls? {
        return if (_modoAtual.value == ModoRepositorio.DEMO) {
            DemoMomoRepositoryImpl.getInstance()
        } else {
            null
        }
    }

    fun alternarModo(novoModo: ModoRepositorio, novoIp: String = enderecoIpEsp32) {
        enderecoIpEsp32 = novoIp
        if (novoModo == ModoRepositorio.HTTP_ESP32) {
            HttpMomoRepositoryImpl.getInstance(novoIp).atualizarIpEsp32(novoIp)
        }
        _modoAtual.value = novoModo
    }

    fun getIpAtual(): String = enderecoIpEsp32
}
