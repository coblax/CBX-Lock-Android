package com.example.coblaxexamlock.runtime

import android.os.SystemClock
import com.example.coblaxexamlock.config.TelegramRateLimitMaxTokens
import com.example.coblaxexamlock.config.TelegramRateLimitRefillPeriodMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class TelegramRateLimiter(
    private val maxTokens: Int = TelegramRateLimitMaxTokens,
    private val refillPeriodMs: Long = TelegramRateLimitRefillPeriodMs,
    private val nowMillis: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val mutex = Mutex()
    private var tokens = maxTokens
    private var lastRefillTime = nowMillis()

    suspend fun acquire() {
        while (true) {
            mutex.withLock {
                refillTokens()
                if (tokens > 0) {
                    tokens--
                    return
                }
            }
            delay(refillPeriodMs / maxTokens)
        }
    }

    fun tryAcquire(): Boolean {
        // Non-suspending check — used for diagnostics only.
        // Synchronized to avoid data races on tokens/lastRefillTime with acquire().
        synchronized(this) {
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
