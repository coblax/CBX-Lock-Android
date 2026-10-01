package com.coblax.examlock.ui.exam

import org.junit.Assert.assertEquals
import org.junit.Test

class StartExamPreflightPhaseTest {
    @Test
    fun phasesFollowTheStartOrder() {
        assertEquals(0, resolveStartExamPreflightPhaseIndex(StartExamPreflightStep.TamperAndIntegrity, 0))
        assertEquals(1, resolveStartExamPreflightPhaseIndex(StartExamPreflightStep.NetworkDns, 0))
        assertEquals(2, resolveStartExamPreflightPhaseIndex(StartExamPreflightStep.StaticSecurity, 1))
        assertEquals(3, resolveStartExamPreflightPhaseIndex(StartExamPreflightStep.PreparingWebView, 2))
    }

    @Test
    fun theFinalClockCheckDoesNotTickTheListBack() {
        // After the phone checks, Start re-checks the clock; that step maps to an earlier phase.
        assertEquals(2, resolveStartExamPreflightPhaseIndex(StartExamPreflightStep.DeviceTime, 2))
    }
}
