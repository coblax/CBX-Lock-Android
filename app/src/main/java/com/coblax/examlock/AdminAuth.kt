package com.coblax.examlock

import android.content.Context
import android.os.SystemClock
import androidx.core.content.edit
import com.coblax.examlock.nativebridge.AdminSecretBridge
import kotlin.math.min

private const val AdminAuthPrefsName = "cbx_admin_auth_rate_limit"
private const val KeyFailedAttempts = "failed_attempts"
private const val KeyBlockedUntilWallClock = "blocked_until_wall_clock"
private const val KeyBlockedUntilElapsed = "blocked_until_elapsed"

internal const val AdminAuthMaxBackoffMillis = 8_000L

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
            val nowWallClock = System.currentTimeMillis()
            val nowElapsed = SystemClock.elapsedRealtime()
            // The wall clock alone let anyone clear the lockout by moving the device
            // date forward, which the app already treats as a live threat elsewhere.
            // The monotonic deadline survives that; the wall-clock one survives a
            // reboot. Staying blocked while either is running closes both holes.
            val wallClockBlockMs = remainingBlockMillis(
                prefs.getLong(KeyBlockedUntilWallClock, 0L),
                nowWallClock
            )
            val elapsedBlockMs = remainingBlockMillis(
                prefs.getLong(KeyBlockedUntilElapsed, 0L),
                nowElapsed
            )
            if (wallClockBlockMs > 0L || elapsedBlockMs > 0L) {
                return false
            }
            val verified = AdminSecretBridge.verify(context, trimmed)
            return if (verified) {
                prefs.edit {
                    putInt(KeyFailedAttempts, 0)
                    putLong(KeyBlockedUntilWallClock, 0L)
                    putLong(KeyBlockedUntilElapsed, 0L)
                }
                AdminAuthSession.issue()
                true
            } else {
                val nextFailedAttempts = failedAttempts + 1
                val backoffMs = progressiveBackoffMillis(nextFailedAttempts)
                prefs.edit {
                    putInt(KeyFailedAttempts, nextFailedAttempts)
                    putLong(KeyBlockedUntilWallClock, nowWallClock + backoffMs)
                    putLong(KeyBlockedUntilElapsed, nowElapsed + backoffMs)
                }
                false
            }
        }
    }

    /**
     * Remaining lockout for one clock, clamped to the longest backoff we ever write.
     * A deadline further out than that means the clock jumped backwards or the device
     * rebooted (which restarts [SystemClock.elapsedRealtime]), not that a real lockout
     * is still running — so neither event can lock a legitimate admin out for hours.
     */
    internal fun remainingBlockMillis(deadline: Long, now: Long): Long {
        if (deadline <= 0L) {
            return 0L
        }
        val remaining = deadline - now
        return when {
            remaining <= 0L -> 0L
            remaining > AdminAuthMaxBackoffMillis -> AdminAuthMaxBackoffMillis
            else -> remaining
        }
    }

    internal fun resetRateLimitForTests(context: Context) {
        synchronized(lock) {
            context.getSharedPreferences(AdminAuthPrefsName, Context.MODE_PRIVATE).edit {
                putInt(KeyFailedAttempts, 0)
                putLong(KeyBlockedUntilWallClock, 0L)
                putLong(KeyBlockedUntilElapsed, 0L)
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

