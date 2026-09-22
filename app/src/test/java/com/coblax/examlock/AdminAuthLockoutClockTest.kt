package com.coblax.examlock

import org.junit.Assert.*
import org.junit.Test

/**
 * The admin lockout used to be tracked on the wall clock alone, so moving the device
 * date forward cleared it and handed back unlimited attempts at the admin secret —
 * on an app that already treats device-time manipulation as a live threat.
 */
class AdminAuthLockoutClockTest {
    @Test
    fun movingTheClockForwardCannotClearAMonotonicLockout() {
        val nowWallClock = 1_700_000_000_000L
        val nowElapsed = 50_000L
        val backoffMs = 8_000L
        val wallClockDeadline = nowWallClock + backoffMs
        val elapsedDeadline = nowElapsed + backoffMs

        // Student jumps the device date a day ahead: the wall-clock deadline is gone...
        val jumpedWallClock = nowWallClock + 24 * 60 * 60 * 1000L
        assertEquals(0L, AdminAuth.remainingBlockMillis(wallClockDeadline, jumpedWallClock))
        // ...but elapsedRealtime is untouched, so the lockout still stands.
        assertTrue(AdminAuth.remainingBlockMillis(elapsedDeadline, nowElapsed) > 0L)
    }

    @Test
    fun rebootAndBackwardsClockCannotLockAnAdminOutForHours() {
        // elapsedRealtime restarts near zero after a reboot, which makes a stored
        // deadline look far in the future. Clamp it instead of trusting it.
        val staleElapsedDeadline = 9_000_000L
        assertEquals(
            AdminAuthMaxBackoffMillis,
            AdminAuth.remainingBlockMillis(staleElapsedDeadline, 1_000L)
        )
        // Same for a wall clock dragged backwards by a year.
        val deadline = 1_700_000_000_000L
        assertEquals(
            AdminAuthMaxBackoffMillis,
            AdminAuth.remainingBlockMillis(deadline, deadline - 365L * 24 * 60 * 60 * 1000L)
        )
    }

    @Test
    fun anExpiredOrUnsetDeadlineDoesNotBlock() {
        assertEquals(0L, AdminAuth.remainingBlockMillis(0L, 1_000L))
        assertEquals(0L, AdminAuth.remainingBlockMillis(-1L, 1_000L))
        assertEquals(0L, AdminAuth.remainingBlockMillis(1_000L, 1_000L))
        assertEquals(0L, AdminAuth.remainingBlockMillis(1_000L, 1_001L))
    }

    @Test
    fun aRunningLockoutReportsItsRealRemainder() {
        assertEquals(3_000L, AdminAuth.remainingBlockMillis(10_000L, 7_000L))
    }
}
