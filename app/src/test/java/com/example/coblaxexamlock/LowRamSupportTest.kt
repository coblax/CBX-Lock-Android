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
    fun normalProfileKeepsFullPolicy() {
        val profile = calculateLowRamProfile(
            isLowRamDevice = false,
            totalMemoryBytes = 2_048L * 1024L * 1024L,
            memoryClassMb = 192
        )

        assertFalse(profile.enabled)
        assertFalse(profile.severe)
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
        assertEquals(960, profile.qrMaxEdgePx)
        assertEquals(2, profile.slowPollingMultiplier)
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
        assertEquals(960, profile.qrMaxEdgePx)
    }

    @Test
    fun severeQrDecodeCapsSampleEdgeAndUsesRgb565() {
        val severeProfile = LowRamProfile(
            enabled = true,
            severe = true,
            qrMaxEdgePx = 960,
            deferHeavyUi = true,
            slowPollingMultiplier = 2
        )

        val sampleSize = calculateBitmapSampleSize(
            width = 3840,
            height = 2160,
            maxWidth = severeProfile.qrMaxEdgePx,
            maxHeight = severeProfile.qrMaxEdgePx
        )

        assertEquals(4, sampleSize)
        assertEquals(Bitmap.Config.RGB_565, qrDecodePreferredBitmapConfig(severeProfile))
    }
}
