package com.example.coblaxexamlock

import android.graphics.Bitmap
import com.example.coblaxexamlock.runtime.calculateBitmapSampleSize
import com.example.coblaxexamlock.runtime.qrDecodePreferredBitmapConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LowRamSupportTest {
    @Test
    fun profileBadgeLabelsUseRamTier() {
        assertEquals("Profil Normal", lowRamProfileBadgeLabel(LowRamProfile()))
        assertEquals(
            "Profil Low",
            lowRamProfileBadgeLabel(LowRamProfile(enabled = true))
        )
        assertEquals(
            "Profil Ultra",
            lowRamProfileBadgeLabel(
                LowRamProfile(enabled = true, severe = true, ultra = true)
            )
        )
    }

    @Test
    fun parsesLowRamProfileOverrideWithSafeFallback() {
        assertEquals(LowRamProfileOverride.Auto, parseLowRamProfileOverride(null))
        assertEquals(LowRamProfileOverride.Normal, parseLowRamProfileOverride("Normal"))
        assertEquals(LowRamProfileOverride.Low, parseLowRamProfileOverride("low"))
        assertEquals(LowRamProfileOverride.Ultra, parseLowRamProfileOverride(" ULTRA "))
        assertEquals(LowRamProfileOverride.Auto, parseLowRamProfileOverride("broken"))
        assertEquals("Ultra", lowRamProfileOverrideToRaw(LowRamProfileOverride.Ultra))
    }

    @Test
    fun profileOverrideKeepsDetectedRamMetadata() {
        val detected = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            totalMemoryMb = 768,
            availableMemoryMb = 468,
            memoryLow = true,
            qrMaxEdgePx = 720,
            deferHeavyUi = true,
            slowPollingMultiplier = 4
        )

        val auto = applyLowRamProfileOverride(detected, LowRamProfileOverride.Auto)
        assertTrue(auto.enabled)
        assertTrue(auto.severe)
        assertTrue(auto.ultra)
        assertEquals(LowRamProfileOverride.Auto, auto.lowRamOverride)
        assertEquals(LowRamTier.Ultra, auto.detectedTier)

        val normal = applyLowRamProfileOverride(detected, LowRamProfileOverride.Normal)
        assertFalse(normal.enabled)
        assertFalse(normal.severe)
        assertFalse(normal.ultra)
        assertEquals(1, normal.slowPollingMultiplier)
        assertEquals(2560, normal.qrMaxEdgePx)
        assertEquals(768L, normal.totalMemoryMb)
        assertEquals(468L, normal.availableMemoryMb)
        assertEquals(LowRamTier.Ultra, normal.detectedTier)

        val low = applyLowRamProfileOverride(detected, LowRamProfileOverride.Low)
        assertTrue(low.enabled)
        assertFalse(low.severe)
        assertFalse(low.ultra)
        assertEquals(2, low.slowPollingMultiplier)
        assertEquals(1280, low.qrMaxEdgePx)
        assertEquals(LowRamTier.Ultra, low.detectedTier)

        val ultra = applyLowRamProfileOverride(detected, LowRamProfileOverride.Ultra)
        assertTrue(ultra.enabled)
        assertTrue(ultra.severe)
        assertTrue(ultra.ultra)
        assertEquals(4, ultra.slowPollingMultiplier)
        assertEquals(720, ultra.qrMaxEdgePx)
        assertEquals(LowRamTier.Ultra, ultra.detectedTier)
    }

    @Test
    fun lighterOverrideThanDetectedProfileIsRisky() {
        val detectedUltra = LowRamProfile(enabled = true, severe = true, ultra = true)

        assertFalse(isLowRamProfileOverrideRisky(detectedUltra, LowRamProfileOverride.Auto))
        assertTrue(isLowRamProfileOverrideRisky(detectedUltra, LowRamProfileOverride.Normal))
        assertTrue(isLowRamProfileOverrideRisky(detectedUltra, LowRamProfileOverride.Low))
        assertFalse(isLowRamProfileOverrideRisky(detectedUltra, LowRamProfileOverride.Ultra))
    }

    @Test
    fun normalProfileKeepsFullPolicy() {
        val profile = calculateLowRamProfile(
            isLowRamDevice = false,
            totalMemoryBytes = 2_048L * 1024L * 1024L,
            memoryClassMb = 192
        )

        assertFalse(profile.enabled)
        assertFalse(profile.severe)
        assertFalse(profile.ultra)
        assertEquals(2560, profile.qrMaxEdgePx)
        assertFalse(profile.deferHeavyUi)
        assertEquals(1, profile.slowPollingMultiplier)
    }

    @Test
    fun exactlyOneGbEnablesLowRamPolicy() {
        val profile = calculateLowRamProfile(
            isLowRamDevice = false,
            totalMemoryBytes = 1_024L * 1024L * 1024L,
            memoryClassMb = 192
        )

        assertTrue(profile.enabled)
        assertFalse(profile.severe)
        assertFalse(profile.ultra)
        assertEquals(1280, profile.qrMaxEdgePx)
        assertTrue(profile.deferHeavyUi)
        assertEquals(2, profile.slowPollingMultiplier)
    }

    @Test
    fun sevenHundredSixtyEightMbEnablesSeverePolicy() {
        val profile = calculateLowRamProfile(
            isLowRamDevice = false,
            totalMemoryBytes = 768L * 1024L * 1024L,
            memoryClassMb = 192
        )

        assertTrue(profile.enabled)
        assertTrue(profile.severe)
        assertTrue(profile.ultra)
        assertEquals(720, profile.qrMaxEdgePx)
        assertEquals(4, profile.slowPollingMultiplier)
    }

    @Test
    fun smallMemoryClassEnablesSeverePolicyEvenWithUnknownTotalRam() {
        val profile = calculateLowRamProfile(
            isLowRamDevice = false,
            totalMemoryBytes = null,
            memoryClassMb = 96
        )

        assertTrue(profile.enabled)
        assertTrue(profile.severe)
        assertTrue(profile.ultra)
        assertEquals(720, profile.qrMaxEdgePx)
    }

    @Test
    fun lowAvailableMemoryEnablesUltraPolicyEvenWithLargeTotalRam() {
        val profile = calculateLowRamProfile(
            isLowRamDevice = false,
            totalMemoryBytes = 2_048L * 1024L * 1024L,
            memoryClassMb = 192,
            availableMemoryBytes = 468L * 1024L * 1024L
        )

        assertTrue(profile.enabled)
        assertTrue(profile.severe)
        assertTrue(profile.ultra)
        assertEquals(2_048L, profile.totalMemoryMb)
        assertEquals(468L, profile.availableMemoryMb)
        assertEquals(720, profile.qrMaxEdgePx)
        assertEquals(4, profile.slowPollingMultiplier)
    }

    @Test
    fun systemLowMemoryEnablesUltraPolicy() {
        val profile = calculateLowRamProfile(
            isLowRamDevice = false,
            totalMemoryBytes = 2_048L * 1024L * 1024L,
            memoryClassMb = 192,
            memoryLow = true
        )

        assertTrue(profile.enabled)
        assertTrue(profile.severe)
        assertTrue(profile.ultra)
        assertTrue(profile.memoryLow)
    }

    @Test
    fun severeQrDecodeCapsSampleEdgeAndUsesRgb565() {
        val severeProfile = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            qrMaxEdgePx = 720,
            deferHeavyUi = true,
            slowPollingMultiplier = 4
        )

        val sampleSize = calculateBitmapSampleSize(
            width = 3840,
            height = 2160,
            maxWidth = severeProfile.qrMaxEdgePx,
            maxHeight = severeProfile.qrMaxEdgePx
        )

        assertEquals(8, sampleSize)
        assertEquals(Bitmap.Config.RGB_565, qrDecodePreferredBitmapConfig(severeProfile))
    }
}
