package com.example.data.repository

/** Usa relógio monotônico entre sincronizações para não depender de ajustes no celular. */
class ServerTimeEstimator(
    private val wallTimeMillis: () -> Long = { System.currentTimeMillis() },
    private val elapsedMillis: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private data class Anchor(val serverMillis: Long, val elapsedMillis: Long)
    @Volatile private var anchor: Anchor? = null

    fun updateOffset(offsetMillis: Long) {
        anchor = Anchor(wallTimeMillis() + offsetMillis, elapsedMillis())
    }

    fun nowMillis(): Long? {
        val current = anchor ?: return null
        return current.serverMillis + (elapsedMillis() - current.elapsedMillis).coerceAtLeast(0L)
    }
}
