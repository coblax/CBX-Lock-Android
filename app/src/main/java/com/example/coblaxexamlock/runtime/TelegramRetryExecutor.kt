package com.example.coblaxexamlock.runtime

import com.example.coblaxexamlock.config.TelegramRetryBaseDelayMs
import com.example.coblaxexamlock.config.TelegramRetryJitterFraction
import com.example.coblaxexamlock.config.TelegramRetryMaxAttempts
import com.example.coblaxexamlock.config.TelegramRetryMultiplier
import java.io.IOException
import kotlin.math.pow
import kotlin.random.Random
import kotlinx.coroutines.delay

internal class TelegramRetryExecutor(
    private val maxAttempts: Int = TelegramRetryMaxAttempts,
    private val baseDelayMs: Long = TelegramRetryBaseDelayMs,
    private val multiplier: Double = TelegramRetryMultiplier,
    private val jitterFraction: Double = TelegramRetryJitterFraction
) {
    suspend fun execute(token: String, chatId: String, message: String): Result<Unit> {
        var lastException: Exception? = null

        for (attempt in 1..maxAttempts) {
            try {
                sendTelegramTextMessage(token, chatId, message)
                return Result.success(Unit)
            } catch (e: Exception) {
                lastException = e

                if (isPermanentFailure(e)) {
                    return Result.failure(e)
                }

                if (attempt < maxAttempts) {
                    val baseDelay = (baseDelayMs * multiplier.pow((attempt - 1).toDouble())).toLong()
                    val jitter = (baseDelay * jitterFraction * Random.nextDouble(-1.0, 1.0)).toLong()
                    delay(baseDelay + jitter)
                }
            }
        }

        return Result.failure(lastException ?: IOException("All retry attempts exhausted"))
    }

    private fun isPermanentFailure(e: Exception): Boolean {
        if (e is TelegramHttpException) {
            return e.statusCode in 400..499 && e.statusCode != 429
        }
        return false
    }
}
