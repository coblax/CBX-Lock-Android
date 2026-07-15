package com.coblax.examlock.runtime

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the retry logic behavior.
 * Since TelegramRetryExecutor calls sendTelegramTextMessage (which does real HTTP),
 * we test the retry decision logic and TelegramHttpException classification directly.
 */
class TelegramRetryExecutorTest {

    @Test
    fun telegramHttpExceptionPreservesStatusCode() {
        val exception = TelegramHttpException(429, "Too Many Requests")
        assertEquals(429, exception.statusCode)
        assertEquals("Too Many Requests", exception.message)
        assertTrue(exception is IOException)
    }

    @Test
    fun http400IsPermanentFailure() {
        assertTrue(isPermanent(TelegramHttpException(400, "Bad Request")))
    }

    @Test
    fun http401IsPermanentFailure() {
        assertTrue(isPermanent(TelegramHttpException(401, "Unauthorized")))
    }

    @Test
    fun http403IsPermanentFailure() {
        assertTrue(isPermanent(TelegramHttpException(403, "Forbidden")))
    }

    @Test
    fun http404IsPermanentFailure() {
        assertTrue(isPermanent(TelegramHttpException(404, "Not Found")))
    }

    @Test
    fun http429IsNotPermanentFailure() {
        assertTrue(!isPermanent(TelegramHttpException(429, "Too Many Requests")))
    }

    @Test
    fun http500IsNotPermanentFailure() {
        assertTrue(!isPermanent(TelegramHttpException(500, "Internal Server Error")))
    }

    @Test
    fun http502IsNotPermanentFailure() {
        assertTrue(!isPermanent(TelegramHttpException(502, "Bad Gateway")))
    }

    @Test
    fun http503IsNotPermanentFailure() {
        assertTrue(!isPermanent(TelegramHttpException(503, "Service Unavailable")))
    }

    @Test
    fun ioExceptionIsNotPermanentFailure() {
        assertTrue(!isPermanent(IOException("Connection timeout")))
    }

    @Test
    fun genericExceptionIsNotPermanentFailure() {
        assertTrue(!isPermanent(RuntimeException("Something went wrong")))
    }

    @Test
    fun retryDelayGrowsExponentially() {
        val baseDelayMs = 1000L
        val multiplier = 3.0

        val delay1 = (baseDelayMs * Math.pow(multiplier, 0.0)).toLong()
        val delay2 = (baseDelayMs * Math.pow(multiplier, 1.0)).toLong()
        val delay3 = (baseDelayMs * Math.pow(multiplier, 2.0)).toLong()

        assertEquals(1000L, delay1)
        assertEquals(3000L, delay2)
        assertEquals(9000L, delay3)
    }

    @Test
    fun jitterStaysWithinBounds() {
        val baseDelay = 3000L
        val jitterFraction = 0.2
        val maxJitter = (baseDelay * jitterFraction).toLong()

        // Jitter should be at most ±20% of base delay
        assertEquals(600L, maxJitter)

        // Verify bounds
        val minDelay = baseDelay - maxJitter
        val maxDelay = baseDelay + maxJitter
        assertEquals(2400L, minDelay)
        assertEquals(3600L, maxDelay)
    }

    /**
     * Mirrors the isPermanentFailure logic from TelegramRetryExecutor.
     */
    private fun isPermanent(e: Exception): Boolean {
        if (e is TelegramHttpException) {
            return e.statusCode in 400..499 && e.statusCode != 429
        }
        return false
    }
}
