package com.example.coblaxexamlock

import com.example.coblaxexamlock.model.RootDetectionDetails
import com.example.coblaxexamlock.model.RootIndicatorType
import com.example.coblaxexamlock.runtime.isBootloaderUnlocked
import com.example.coblaxexamlock.runtime.resolvePrimaryRootIndicator
import org.junit.Assert.assertEquals
import org.junit.Test

class RootDetectionUtilsTest {
    @Test
    fun bootloaderUnlockedWhenVerifiedBootNotGreen() {
        assertEquals(true, isBootloaderUnlocked("orange", "", ""))
    }

    @Test
    fun bootloaderUnlockedWhenVbmetaUnlocked() {
        assertEquals(true, isBootloaderUnlocked("", "unlocked", ""))
    }

    @Test
    fun bootloaderUnlockedWhenFlashLockedZero() {
        assertEquals(true, isBootloaderUnlocked("", "", "0"))
    }

    @Test
    fun bootloaderLockedWhenValuesSafe() {
        assertEquals(false, isBootloaderUnlocked("green", "locked", "1"))
    }

    @Test
    fun primaryIndicatorPrefersZygiskOverOthers() {
        val details = baseDetails().copy(
            zygiskDetected = true,
            magiskPaths = listOf("/sbin/.magisk"),
            rootBinaryPaths = listOf("/system/xbin/su"),
            bootloaderUnlocked = true
        )

        assertEquals(RootIndicatorType.Zygisk, resolvePrimaryRootIndicator(details))
    }

    @Test
    fun primaryIndicatorReturnsMagiskWhenPresent() {
        val details = baseDetails().copy(magiskPaths = listOf("/sbin/.magisk"))

        assertEquals(RootIndicatorType.Magisk, resolvePrimaryRootIndicator(details))
    }

    @Test
    fun primaryIndicatorReturnsRootBinaryWhenPresent() {
        val details = baseDetails().copy(rootBinaryPaths = listOf("/system/xbin/su"))

        assertEquals(RootIndicatorType.RootBinary, resolvePrimaryRootIndicator(details))
    }

    @Test
    fun primaryIndicatorReturnsSelinuxDisabledWhenDisabled() {
        val details = baseDetails().copy(selinuxEnabled = false, selinuxEnforced = false)

        assertEquals(RootIndicatorType.SelinuxDisabled, resolvePrimaryRootIndicator(details))
    }

    @Test
    fun primaryIndicatorReturnsSelinuxPermissiveWhenNotEnforced() {
        val details = baseDetails().copy(selinuxEnabled = true, selinuxEnforced = false)

        assertEquals(RootIndicatorType.SelinuxPermissive, resolvePrimaryRootIndicator(details))
    }

    @Test
    fun primaryIndicatorReturnsBootloaderWhenUnlocked() {
        val details = baseDetails().copy(bootloaderUnlocked = true)

        assertEquals(RootIndicatorType.Bootloader, resolvePrimaryRootIndicator(details))
    }

    @Test
    fun primaryIndicatorReturnsDangerousPropsWhenPresent() {
        val details = baseDetails().copy(dangerousSystemProperties = listOf("ro.debuggable=1"))

        assertEquals(RootIndicatorType.DangerousProps, resolvePrimaryRootIndicator(details))
    }

    @Test
    fun primaryIndicatorReturnsTestKeysWhenPresent() {
        val details = baseDetails().copy(hasTestKeys = true)

        assertEquals(RootIndicatorType.TestKeys, resolvePrimaryRootIndicator(details))
    }

    @Test
    fun primaryIndicatorReturnsNullWhenNoSignals() {
        assertEquals(null, resolvePrimaryRootIndicator(baseDetails()))
    }

    private fun baseDetails(): RootDetectionDetails {
        return RootDetectionDetails(
            hasTestKeys = false,
            hasSuBinary = false,
            foundRootPackages = emptyList<String>(),
            rootBinaryPaths = emptyList<String>(),
            magiskPaths = emptyList<String>(),
            zygiskDetected = false,
            xposedBridgeDetected = false,
            verifiedBootState = "-",
            vbmetaDeviceState = "-",
            flashLocked = "-",
            bootloaderUnlocked = false,
            selinuxEnabled = true,
            selinuxEnforced = true,
            dangerousSystemProperties = emptyList<String>(),
            roDebuggable = "-",
            roSecure = "-",
            roAdbSecure = "-",
            roBuildType = "-"
        )
    }
}
