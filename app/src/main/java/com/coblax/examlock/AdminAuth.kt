package com.coblax.examlock

import android.content.Context
import android.os.SystemClock
import androidx.core.content.edit
import com.coblax.examlock.nativebridge.AdminSecretBridge
import kotlin.math.min

private const val AdminAuthPrefsName = "cbx_admin_auth_rate_limit"
private const val KeyFailedAttempts = "failed_attempts"
private const val KeyBlockedUntilWallClock = "blocked_until_wall_clock"

object AdminAuth {
    private val lock = Any()

    fun verify(context: Context, input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return false
        }
        synchronized(lock) {
            val prefs = context.getSharedPreferences(AdminAuthPrefsName, Context.MODE_PRIVATE)
            val failedAttempts = prefs.getInt(KeyFailedAttempts, 0)
            val blockedUntilWallClock = prefs.getLong(KeyBlockedUntilWallClock, 0L)
            val nowWallClock = System.currentTimeMillis()
            if (nowWallClock < blockedUntilWallClock) {
                return false
            }
            val verified = AdminSecretBridge.verify(context, trimmed)
            return if (verified) {
                prefs.edit {
                    putInt(KeyFailedAttempts, 0)
                    putLong(KeyBlockedUntilWallClock, 0L)
                }
                AdminAuthSession.issue()
                true
            } else {
                val nextFailedAttempts = failedAttempts + 1
                val backoffMs = progressiveBackoffMillis(nextFailedAttempts)
                prefs.edit {
                    putInt(KeyFailedAttempts, nextFailedAttempts)
                    putLong(KeyBlockedUntilWallClock, nowWallClock + backoffMs)
                }
                false
            }
        }
    }

    internal fun resetRateLimitForTests(context: Context) {
        synchronized(lock) {
            context.getSharedPreferences(AdminAuthPrefsName, Context.MODE_PRIVATE).edit {
                putInt(KeyFailedAttempts, 0)
                putLong(KeyBlockedUntilWallClock, 0L)
            }
        }
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

