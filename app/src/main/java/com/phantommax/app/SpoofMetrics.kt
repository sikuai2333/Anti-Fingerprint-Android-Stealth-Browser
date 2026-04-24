package com.phantommax.app

import java.util.concurrent.atomic.AtomicLong

object SpoofMetrics {
    private val injectAttempts = AtomicLong(0)
    private val injectSuccess = AtomicLong(0)
    private val injectFailure = AtomicLong(0)

    @Volatile
    private var lastError: String = ""

    @Volatile
    private var lastStage: String = ""

    @Volatile
    private var lastInjectAtMs: Long = 0L

    fun recordAttempt(stage: String) {
        injectAttempts.incrementAndGet()
        lastStage = stage
        lastInjectAtMs = System.currentTimeMillis()
    }

    fun recordSuccess() {
        injectSuccess.incrementAndGet()
        lastInjectAtMs = System.currentTimeMillis()
    }

    fun recordFailure(error: String) {
        injectFailure.incrementAndGet()
        lastError = error
        lastInjectAtMs = System.currentTimeMillis()
    }

    fun snapshotText(): String {
        val attempts = injectAttempts.get()
        val success = injectSuccess.get()
        val failed = injectFailure.get()
        val successRate = if (attempts == 0L) 0.0 else success * 100.0 / attempts.toDouble()
        val lastAt = if (lastInjectAtMs == 0L) "—" else java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(lastInjectAtMs))
        return buildString {
            append("Injection metrics\n")
            append("Attempts: ").append(attempts).append('\n')
            append("Success: ").append(success).append('\n')
            append("Failed: ").append(failed).append('\n')
            append("SuccessRate: ").append(String.format(java.util.Locale.US, "%.2f%%", successRate)).append('\n')
            append("LastStage: ").append(lastStage.ifBlank { "—" }).append('\n')
            append("LastError: ").append(lastError.ifBlank { "—" }).append('\n')
            append("LastAt: ").append(lastAt)
        }
    }
}
