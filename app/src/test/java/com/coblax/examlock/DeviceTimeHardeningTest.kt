package com.coblax.examlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTimeHardeningTest {
    private val baseline = DeviceTimeBaseline(
        wallClockMillis = 1_000_000L,
        elapsedRealtimeMillis = 500_000L
    )

    @Test
    fun safeWhenAutoTimeFlagsEnabledAndDriftBelowThreshold() {
        val status = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = true,
            autoTimeZoneEnabled = true,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Inactive,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = 1_060_000L,
            nowElapsedRealtimeMillis = 560_000L
        )

        assertEquals(DeviceTimeSecurityVerdict.Safe, status.finalVerdict)
        assertFalse(status.blocking)
    }

    @Test
    fun reportsAutoTimeDisabledWhenAutoTimeOff() {
        val status = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = false,
            autoTimeZoneEnabled = true,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Inactive,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = 1_060_000L,
            nowElapsedRealtimeMillis = 560_000L
        )

        assertEquals(DeviceTimeSecurityVerdict.AutoTimeDisabled, status.finalVerdict)
        assertTrue(status.blocking)
    }

    @Test
    fun reportsAutoTimeZoneDisabledWhenAutoTimeZoneOff() {
        val status = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = true,
            autoTimeZoneEnabled = false,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Inactive,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = 1_060_000L,
            nowElapsedRealtimeMillis = 560_000L
        )

        assertEquals(DeviceTimeSecurityVerdict.AutoTimeZoneDisabled, status.finalVerdict)
        assertTrue(status.blocking)
    }

    @Test
    fun reportsClockDriftDetectedWhenDriftExceedsThreshold() {
        val status = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = true,
            autoTimeZoneEnabled = true,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Inactive,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = 1_300_001L,
            nowElapsedRealtimeMillis = 560_000L
        )

        assertEquals(DeviceTimeSecurityVerdict.ClockDriftDetected, status.finalVerdict)
        assertTrue(status.clockDriftDetected)
        assertTrue(status.blocking)
    }

    @Test
    fun bypassActiveKeepsVerdictButDisablesBlocking() {
        val status = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = false,
            autoTimeZoneEnabled = true,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Active,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = 1_060_000L,
            nowElapsedRealtimeMillis = 560_000L
        )

        assertEquals(DeviceTimeSecurityVerdict.AutoTimeDisabled, status.finalVerdict)
        assertFalse(status.blocking)
    }

    @Test
    fun tamperedBypassDoesNotDisableEnforcement() {
        val status = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = true,
            autoTimeZoneEnabled = false,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Tampered,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = 1_060_000L,
            nowElapsedRealtimeMillis = 560_000L
        )

        assertEquals(DeviceTimeSecurityVerdict.AutoTimeZoneDisabled, status.finalVerdict)
        assertTrue(status.blocking)
    }

    /**
     * The student opened the app with a clock 10 minutes off, then turned automatic time on as
     * told. The jump is the fix: network time agrees, so it is accepted instead of blocking
     * until the app is killed.
     */
    @Test
    fun clockFixedToNetworkTimeIsAcceptedAndRemembered() {
        val fixedWallClock = 1_060_000L + 600_000L
        val status = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = true,
            autoTimeZoneEnabled = true,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Inactive,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = fixedWallClock,
            nowElapsedRealtimeMillis = 560_000L,
            trustedNetworkNowMillis = fixedWallClock + 1_500L
        )

        assertEquals(DeviceTimeSecurityVerdict.Safe, status.finalVerdict)
        assertFalse(status.blocking)
        assertEquals(600_000L, status.acceptedClockCorrectionMillis)

        // Once accepted, the fixed clock stays fine after the network time goes stale.
        val later = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = true,
            autoTimeZoneEnabled = true,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Inactive,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = fixedWallClock + 30_000L,
            nowElapsedRealtimeMillis = 590_000L,
            acceptedCorrectionMillis = status.acceptedClockCorrectionMillis
        )
        assertEquals(DeviceTimeSecurityVerdict.Safe, later.finalVerdict)
        assertEquals(0L, later.acceptedClockCorrectionMillis)
    }

    @Test
    fun clockChangeWithoutNetworkTimeStillBlocksButAsksToConfirm() {
        val status = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = true,
            autoTimeZoneEnabled = true,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Inactive,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = 1_660_000L,
            nowElapsedRealtimeMillis = 560_000L
        )

        assertEquals(DeviceTimeSecurityVerdict.ClockDriftDetected, status.finalVerdict)
        assertTrue(status.blocking)
        assertTrue(status.clockChangeMayBeAFix)
    }

    /** A clock moved away from real time is exactly what the check exists to catch. */
    @Test
    fun clockThatDisagreesWithNetworkTimeStillBlocks() {
        val status = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = true,
            autoTimeZoneEnabled = true,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Inactive,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = 1_660_000L,
            nowElapsedRealtimeMillis = 560_000L,
            trustedNetworkNowMillis = 1_060_000L
        )

        assertEquals(DeviceTimeSecurityVerdict.ClockDriftDetected, status.finalVerdict)
        assertEquals(0L, status.acceptedClockCorrectionMillis)
    }

    @Test
    fun manualTimeNeverAcceptsAJumpEvenIfItMatchesNetworkTime() {
        val status = evaluateDeviceTimeSecurityStatus(
            autoTimeEnabled = false,
            autoTimeZoneEnabled = true,
            baseline = baseline,
            bypassState = DeviceTimeBypassState.Inactive,
            timezoneSummary = "Asia/Jakarta",
            nowWallClockMillis = 1_660_000L,
            nowElapsedRealtimeMillis = 560_000L,
            trustedNetworkNowMillis = 1_660_000L
        )

        assertEquals(DeviceTimeSecurityVerdict.AutoTimeDisabled, status.finalVerdict)
        assertFalse(status.clockChangeMayBeAFix)
        assertEquals(0L, status.acceptedClockCorrectionMillis)
    }
}
