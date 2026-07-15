package com.coblax.examlock.runtime

import android.os.SystemClock
import com.coblax.examlock.config.TelegramRateLimitMaxTokens
import com.coblax.examlock.config.TelegramRateLimitRefillPeriodMs
import kotlinx.coroutines.delay

internal class TelegramRateLimiter(
    private val maxTokens: Int = TelegramRateLimitMaxTokens,
    private val refillPeriodMs: Long = TelegramRateLimitRefillPeriodMs,
    private val nowMillis: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val lock = Any()
    private var tokens = maxTokens
    private var lastRefillTime = nowMillis()

    suspend fun acquire() {
        while (true) {
            val acquired = synchronized(lock) {
                refillTokens()
                if (tokens > 0) {
                    tokens--
                    true
                } else {
                    false
                }
            }
            if (acquired) return
            delay(refillPeriodMs / maxTokens)
        }
    }

    fun tryAcquire(): Boolean {
        // Non-suspending check — used for diagnostics only.
        synchronized(lock) {
            refillTokens()
            return if (tokens > 0) {
                tokens--
                true
            } else {
                false
            }
        }
    }

    private fun refillTokens() {
        val now = nowMillis()
        val elapsed = now - lastRefillTime
        if (elapsed >= refillPeriodMs) {
            tokens = maxTokens
            lastRefillTime = now
        }
    }
}
