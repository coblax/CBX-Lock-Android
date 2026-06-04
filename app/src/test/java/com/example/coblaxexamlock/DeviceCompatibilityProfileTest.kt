package com.example.coblaxexamlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCompatibilityProfileTest {
    @Test
    fun resolvesSamsungSmT295AsLegacyTablet() {
        val profile = resolveDeviceCompatibilityProfile(
            manufacturer = "Samsung",
            brand = "samsung",
            model = "SM-T295",
            sdkInt = 29,
            lowRamProfile = LowRamProfile(enabled = true, severe = true)
        )

        assertEquals(DeviceCompatibilityFamily.SamsungLegacyTablet, profile.family)
        assertTrue(profile.samsungLegacyTablet)
        assertTrue(profile.allowPartialObscuredWebViewTouch)
        assertTrue(profile.manualFirstGeofenceEditor)
        assertEquals(7_000L, profile.screenPinningLostConfirmWindowMillis)
        assertEquals(3_000L, profile.overlayChromeActionSuppressionMillis)
    }

    @Test
    fun resolvesSamsungModernSeparatelyFromLegacyTablet() {
        val profile = resolveDeviceCompatibilityProfile(
            manufacturer = "Samsung",
            brand = "samsung",
            model = "SM-X200",
            sdkInt = 33
        )

        assertEquals(DeviceCompatibilityFamily.SamsungModern, profile.family)
        assertFalse(profile.samsungLegacyTablet)
        assertTrue(profile.allowPartialObscuredWebViewTouch)
        assertEquals(1_500L, profile.overlayFocusLossConfirmWindowMillis)
    }

    @Test
    fun resolvesCommonVendorFamilies() {
        assertEquals(
            DeviceCompatibilityFamily.XiaomiFamily,
            resolveDeviceCompatibilityProfile("Xiaomi", "Redmi", "2303ERA42L", 33).family
        )
        assertEquals(
            DeviceCompatibilityFamily.OppoRealme,
            resolveDeviceCompatibilityProfile("OPPO", "realme", "RMX", 31).family
        )
        assertEquals(
            DeviceCompatibilityFamily.VivoIqoo,
            resolveDeviceCompatibilityProfile("vivo", "iQOO", "I2019", 31).family
        )
        assertEquals(
            DeviceCompatibilityFamily.Generic,
            resolveDeviceCompatibilityProfile("Google", "google", "Pixel 8", 35).family
        )
    }

    @Test
    fun screenPinningPolicySkipsRequestWhenAlreadyActive() {
        val profile = resolveDeviceCompatibilityProfile("Samsung", "samsung", "SM-T295", 29)

        assertTrue(profile.skipScreenPinningRequestWhenAlreadyActive)
    }
}
