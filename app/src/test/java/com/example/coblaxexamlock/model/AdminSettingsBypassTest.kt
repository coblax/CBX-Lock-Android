package com.example.coblaxexamlock.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminSettingsBypassTest {
    @Test
    fun reverseEngineeringAndApkIntegrityBypassesDefaultOff() {
        val settings = AdminSettings()

        assertFalse(settings.bypassReverseEngineering)
        assertFalse(settings.reverseEngineeringBypassTampered)
        assertFalse(settings.bypassApkIntegrity)
        assertFalse(settings.apkIntegrityBypassTampered)
    }

    @Test
    fun reverseEngineeringAndApkIntegrityBypassesAppearInOverrideSummary() {
        val settings = AdminSettings(
            bypassReverseEngineering = true,
            bypassApkIntegrity = true
        )

        assertTrue(settings.hasAnyBypass())
        assertTrue(settings.overrideSummary().contains("reverse engineering"))
        assertTrue(settings.overrideSummary().contains("apk integrity"))
    }
}
