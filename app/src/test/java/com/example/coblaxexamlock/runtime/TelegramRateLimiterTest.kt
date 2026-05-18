package com.example.coblaxexamlock.runtime

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramRateLimiterTest {

    @Test
    fun allowsUpToMaxTokensImmediately() = runBlocking {
        val limiter = testLimiter(maxTokens = 3, refillPeriodMs = 60_000L)
        // Should not suspend for first 3 acquires
        limiter.acquire()
        limiter.acquire()
        limiter.acquire()
        // If we got here without timeout, test passes
        assertTrue(true)
    }

    @Test
    fun tryAcquireReturnsFalseWhenExhausted() {
        val limiter = testLimiter(maxTokens = 2, refillPeriodMs = 60_000L)
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())
    }

    @Test
    fun tokensRefillAfterPeriod() = runBlocking {
        val limiter = testLimiter(maxTokens = 2, refillPeriodMs = 50L)
        // Exhaust tokens
        limiter.acquire()
        limiter.acquire()
        assertFalse(limiter.tryAcquire())
        // Wait for refill
        delay(60L)
        assertTrue(limiter.tryAcquire())
    }

    @Test
    fun acquireSuspendsWhenExhausted() = runBlocking {
        val limiter = testLimiter(maxTokens = 1, refillPeriodMs = 50L)
        limiter.acquire() // consume the only token

        var acquired = false
        val job = async {
            limiter.acquire()
            acquired = true
        }

        delay(10L)
        assertFalse(acquired) // should still be suspended

        delay(60L) // wait for refill
        job.await()
        assertTrue(acquired)
    }

    @Test
    fun maxTokensRespected() {
        val limiter = testLimiter(maxTokens = 5, refillPeriodMs = 60_000L)
        var count = 0
        while (limiter.tryAcquire()) {
            count++
            if (count > 10) break // safety
        }
        assertEquals(5, count)
    }

    private fun testLimiter(maxTokens: Int, refillPeriodMs: Long): TelegramRateLimiter {
        return TelegramRateLimiter(
            maxTokens = maxTokens,
            refillPeriodMs = refillPeriodMs,
            nowMillis = { System.currentTimeMillis() }
        )
    }
}
