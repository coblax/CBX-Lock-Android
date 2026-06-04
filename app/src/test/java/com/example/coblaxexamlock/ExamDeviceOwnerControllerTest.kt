package com.example.coblaxexamlock

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamDeviceOwnerControllerTest {
    @Test
    fun android7DeviceOwnerUsesLegacyDpcTierWithoutCreateWindowsRestriction() {
        assertEquals(
            DpcProtectionTier.LegacyDpcAndroid7,
            resolveDpcProtectionTier(
                sdkInt = Build.VERSION_CODES.N,
                deviceOwner = true,
                overlayShieldSupported = false
            )
        )
        assertFalse(
            shouldApplyCreateWindowsRestriction(
                sdkInt = Build.VERSION_CODES.N,
                deviceOwner = true
            )
        )
    }

    @Test
    fun android8DeviceOwnerCanApplyCreateWindowsRestriction() {
        assertEquals(
            DpcProtectionTier.DpcOverlayRestricted,
            resolveDpcProtectionTier(
                sdkInt = Build.VERSION_CODES.O,
                deviceOwner = true,
                overlayShieldSupported = false
            )
        )
        assertTrue(
            shouldApplyCreateWindowsRestriction(
                sdkInt = Build.VERSION_CODES.O,
                deviceOwner = true
            )
        )
    }

    @Test
    fun android12NormalApkUsesOverlayShieldTier() {
        assertEquals(
            DpcProtectionTier.NormalApk,
            resolveDpcProtectionTier(
                sdkInt = Build.VERSION_CODES.S,
                deviceOwner = false,
                overlayShieldSupported = true
            )
        )
    }

    @Test
    fun android7NormalApkHasNoDpcOverlayTier() {
        assertEquals(
            DpcProtectionTier.None,
            resolveDpcProtectionTier(
                sdkInt = Build.VERSION_CODES.N,
                deviceOwner = false,
                overlayShieldSupported = false
            )
        )
    }

    @Test
    fun endExamClearsCreateWindowsRestrictionOnlyWhenSessionAppliedIt() {
        assertFalse(shouldClearCreateWindowsRestriction(sessionAppliedRestriction = false))
        assertTrue(shouldClearCreateWindowsRestriction(sessionAppliedRestriction = true))
    }
}
