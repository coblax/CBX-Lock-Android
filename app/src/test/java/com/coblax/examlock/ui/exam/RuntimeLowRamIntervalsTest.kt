package com.coblax.examlock.ui.exam

import com.coblax.examlock.LowRamProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeLowRamIntervalsTest {
    @Test
    fun lowRamSlowsFastStaticSecurityPolling() {
        val profile = LowRamProfile(
            enabled = true,
            severe = false,
            ultra = false,
            slowPollingMultiplier = 2
        )

        assertEquals(4_000L, runtimeFastStaticSecurityPollIntervalMillis(profile))
    }

    @Test
    fun ultraLowRamSlowsFastStaticSecurityPolling() {
        val profile = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            slowPollingMultiplier = 6
        )

        assertEquals(12_000L, runtimeFastStaticSecurityPollIntervalMillis(profile))
    }

    @Test
    fun lowRamUsesModerateScreenRecorderPolling() {
        val profile = LowRamProfile(
            enabled = true,
            severe = false,
            ultra = false,
            slowPollingMultiplier = 2
        )

        assertEquals(30_000L, runtimeScreenRecorderPollIntervalMillis(profile))
    }

    @Test
    fun ultraLowRamUsesLongerScreenRecorderPolling() {
        val profile = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            slowPollingMultiplier = 6
        )

        assertEquals(45_000L, runtimeScreenRecorderPollIntervalMillis(profile))
    }

    @Test
    fun lowRamUsesModerateGeofenceRecheck() {
        val profile = LowRamProfile(
            enabled = true,
            severe = false,
            ultra = false,
            slowPollingMultiplier = 2
        )

        assertEquals(60_000L, geofenceRuntimeRecheckIntervalMillis(profile))
    }

    @Test
    fun ultraLowRamUsesLongerGeofenceRecheck() {
        val profile = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            slowPollingMultiplier = 6
        )

        assertEquals(90_000L, geofenceRuntimeRecheckIntervalMillis(profile))
    }

    @Test
    fun screenPinningSteadyPollingUsesProfileValue() {
        val lowProfile = LowRamProfile(
            enabled = true,
            screenPinningSteadyPollMillis = 2_000L
        )
        val ultraProfile = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            screenPinningSteadyPollMillis = 4_000L
        )

        assertEquals(2_000L, screenPinningMonitorSteadyIntervalMillis(lowProfile))
        assertEquals(4_000L, screenPinningMonitorSteadyIntervalMillis(ultraProfile))
    }

    @Test
    fun accessibilityLivenessPollingUsesProfileValue() {
        val lowProfile = LowRamProfile(
            enabled = true,
            accessibilityLivenessPollMillis = 2_500L
        )
        val ultraProfile = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            accessibilityLivenessPollMillis = 5_000L
        )

        assertEquals(2_500L, accessibilityGuardLivenessPollMillis(lowProfile))
        assertEquals(5_000L, accessibilityGuardLivenessPollMillis(ultraProfile))
    }

    @Test
    fun examServerProbePollingUsesProfileValue() {
        val lowProfile = LowRamProfile(
            enabled = true,
            examServerProbeIntervalMillis = 60_000L
        )
        val ultraProfile = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            examServerProbeIntervalMillis = 120_000L
        )

        assertEquals(60_000L, examServerProbeIntervalMillis(lowProfile))
        assertEquals(120_000L, examServerProbeIntervalMillis(ultraProfile))
    }
}
