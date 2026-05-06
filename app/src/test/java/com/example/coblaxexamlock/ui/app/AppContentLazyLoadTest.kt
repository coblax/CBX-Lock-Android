package com.example.coblaxexamlock.ui.app

import com.example.coblaxexamlock.LowRamProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppContentLazyLoadTest {
    @Test
    fun normalRamStartsRuntimeImmediately() {
        val profile = LowRamProfile(enabled = false, severe = false, deferHeavyUi = false)

        assertTrue(shouldStartRuntimeImmediately(profile, initialHomeActionRaw = null))
    }

    @Test
    fun lowRamHomeUsesShellUntilUserAction() {
        val profile = LowRamProfile(enabled = true, severe = false, deferHeavyUi = true)

        assertFalse(shouldStartRuntimeImmediately(profile, initialHomeActionRaw = null))
    }

    @Test
    fun severeLowRamHomeUsesShellUntilUserAction() {
        val profile = LowRamProfile(enabled = true, severe = true, deferHeavyUi = true)

        assertFalse(shouldStartRuntimeImmediately(profile, initialHomeActionRaw = null))
    }

    @Test
    fun pendingHomeActionStartsRuntimeEvenOnLowRam() {
        val profile = LowRamProfile(enabled = true, severe = false, deferHeavyUi = true)

        assertTrue(shouldStartRuntimeImmediately(profile, PendingHomeAction.ScanExam.name))
    }
}
