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
}
