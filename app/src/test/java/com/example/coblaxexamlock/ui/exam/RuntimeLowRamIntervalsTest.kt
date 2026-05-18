package com.example.coblaxexamlock.ui.exam

import com.example.coblaxexamlock.LowRamProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeLowRamIntervalsTest {
    @Test
    fun ultraLowRamSlowsFastStaticSecurityPolling() {
        val profile = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            slowPollingMultiplier = 4
        )

        assertEquals(8_000L, runtimeFastStaticSecurityPollIntervalMillis(profile))
    }

    @Test
    fun ultraLowRamUsesLongerScreenRecorderPolling() {
        val profile = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            slowPollingMultiplier = 4
        )

        assertEquals(45_000L, runtimeScreenRecorderPollIntervalMillis(profile))
    }

    @Test
    fun ultraLowRamUsesLongerGeofenceRecheck() {
        val profile = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            slowPollingMultiplier = 4
        )

        assertEquals(90_000L, geofenceRuntimeRecheckIntervalMillis(profile))
    }
}
