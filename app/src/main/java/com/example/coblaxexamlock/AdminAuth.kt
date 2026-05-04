package com.example.coblaxexamlock

import android.content.Context
import android.os.SystemClock
import com.example.coblaxexamlock.nativebridge.AdminSecretBridge
import kotlin.math.min

object AdminAuth {
    @Volatile
    private var failedAttempts: Int = 0

    @Volatile
    private var blockedUntilElapsedRealtime: Long = 0L

    @Synchronized
    fun verify(context: Context, input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return false
        }
        val now = SystemClock.elapsedRealtime()
        if (now < blockedUntilElapsedRealtime) {
            return false
        }
        val verified = AdminSecretBridge.verify(context, trimmed)
        return if (verified) {
            failedAttempts = 0
            blockedUntilElapsedRealtime = 0L
            AdminAuthSession.issue()
            true
        } else {
            failedAttempts += 1
            blockedUntilElapsedRealtime = now + progressiveBackoffMillis(failedAttempts)
            false
        }
    }

    @Synchronized
    internal fun resetRateLimitForTests() {
        failedAttempts = 0
        blockedUntilElapsedRealtime = 0L
    }

    private fun progressiveBackoffMillis(failures: Int): Long {
        return when (min(failures, 6)) {
            0, 1 -> 0L
            2 -> 500L
            3 -> 1_250L
            4 -> 2_500L
            5 -> 5_000L
            else -> 8_000L
        }
    }
}
