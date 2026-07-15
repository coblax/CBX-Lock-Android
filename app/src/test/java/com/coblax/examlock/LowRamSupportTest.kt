package com.coblax.examlock

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import com.coblax.examlock.runtime.calculateBitmapSampleSize
import com.coblax.examlock.runtime.qrDecodePreferredBitmapConfig
import com.coblax.examlock.runtime.shouldSkipQrFullBitmapScanAfterFallback
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
    fun publicLowRamProfileOverrideOptionsExposeAllModes() {
        assertEquals(
            listOf(
                LowRamProfileOverride.Auto,
                LowRamProfileOverride.Normal,
                LowRamProfileOverride.Low,
                LowRamProfileOverride.Ultra
            ),
            lowRamProfileOverrideOptions()
        )
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
        assertEquals(1_000L, normal.screenPinningSteadyPollMillis)
        assertEquals(1_000L, normal.accessibilityLivenessPollMillis)
        assertEquals(30_000L, normal.examServerProbeIntervalMillis)
        assertEquals(64, normal.detectorMetadataCacheMaxEntries)
        assertFalse(normal.disableNonEssentialAnimations)
        assertEquals(768L, normal.totalMemoryMb)
        assertEquals(468L, normal.availableMemoryMb)
        assertEquals(LowRamTier.Ultra, normal.detectedTier)

        val low = applyLowRamProfileOverride(detected, LowRamProfileOverride.Low)
        assertTrue(low.enabled)
        assertFalse(low.severe)
        assertFalse(low.ultra)
        assertEquals(2, low.slowPollingMultiplier)
        assertEquals(1024, low.qrMaxEdgePx)
        assertEquals(16, low.diagnosticLogMaxEntries)
        assertEquals(800L, low.manualRefreshCooldownMillis)
        assertEquals(2_000L, low.screenPinningSteadyPollMillis)
        assertEquals(2_500L, low.accessibilityLivenessPollMillis)
        assertEquals(60_000L, low.examServerProbeIntervalMillis)
        assertEquals(24, low.detectorMetadataCacheMaxEntries)
        assertTrue(low.disableNonEssentialAnimations)
        assertEquals(LowRamTier.Ultra, low.detectedTier)

        val ultra = applyLowRamProfileOverride(detected, LowRamProfileOverride.Ultra)
        assertTrue(ultra.enabled)
        assertTrue(ultra.severe)
        assertTrue(ultra.ultra)
        assertEquals(6, ultra.slowPollingMultiplier)
        assertEquals(720, ultra.qrMaxEdgePx)
        assertEquals(8, ultra.diagnosticLogMaxEntries)
        assertEquals(1_200L, ultra.manualRefreshCooldownMillis)
        assertEquals(4_000L, ultra.screenPinningSteadyPollMillis)
        assertEquals(5_000L, ultra.accessibilityLivenessPollMillis)
        assertEquals(120_000L, ultra.examServerProbeIntervalMillis)
        assertEquals(8, ultra.detectorMetadataCacheMaxEntries)
        assertTrue(ultra.disableNonEssentialAnimations)
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
            totalMemoryBytes = 3_072L * 1024L * 1024L,
            memoryClassMb = 192
        )

        assertFalse(profile.enabled)
        assertFalse(profile.severe)
        assertFalse(profile.ultra)
        assertEquals(2560, profile.qrMaxEdgePx)
        assertFalse(profile.deferHeavyUi)
        assertEquals(1, profile.slowPollingMultiplier)
        assertEquals(20, profile.diagnosticLogMaxEntries)
        assertEquals(0L, profile.manualRefreshCooldownMillis)
        assertEquals(1_000L, profile.screenPinningSteadyPollMillis)
        assertEquals(1_000L, profile.accessibilityLivenessPollMillis)
        assertEquals(30_000L, profile.examServerProbeIntervalMillis)
        assertEquals(64, profile.detectorMetadataCacheMaxEntries)
        assertFalse(profile.disableNonEssentialAnimations)
    }

    @Test
    fun exactlyTwoGbEnablesLowRamPolicy() {
        val profile = calculateLowRamProfile(
            isLowRamDevice = false,
            totalMemoryBytes = 2_048L * 1024L * 1024L,
            memoryClassMb = 192
        )

        assertTrue(profile.enabled)
        assertFalse(profile.severe)
        assertFalse(profile.ultra)
        assertEquals(1024, profile.qrMaxEdgePx)
        assertTrue(profile.deferHeavyUi)
        assertEquals(2, profile.slowPollingMultiplier)
        assertEquals(16, profile.diagnosticLogMaxEntries)
        assertEquals(800L, profile.manualRefreshCooldownMillis)
        assertEquals(2_000L, profile.screenPinningSteadyPollMillis)
        assertEquals(2_500L, profile.accessibilityLivenessPollMillis)
        assertEquals(60_000L, profile.examServerProbeIntervalMillis)
        assertEquals(24, profile.detectorMetadataCacheMaxEntries)
        assertTrue(profile.disableNonEssentialAnimations)
    }

    @Test
    fun exactlyOneGbEnablesUltraPolicy() {
        val profile = calculateLowRamProfile(
            isLowRamDevice = false,
            totalMemoryBytes = 1_024L * 1024L * 1024L,
            memoryClassMb = 192
        )

        assertTrue(profile.enabled)
        assertTrue(profile.severe)
        assertTrue(profile.ultra)
        assertEquals(720, profile.qrMaxEdgePx)
        assertTrue(profile.deferHeavyUi)
        assertEquals(6, profile.slowPollingMultiplier)
        assertEquals(8, profile.diagnosticLogMaxEntries)
        assertEquals(1_200L, profile.manualRefreshCooldownMillis)
        assertEquals(4_000L, profile.screenPinningSteadyPollMillis)
        assertEquals(5_000L, profile.accessibilityLivenessPollMillis)
        assertEquals(120_000L, profile.examServerProbeIntervalMillis)
        assertEquals(8, profile.detectorMetadataCacheMaxEntries)
        assertTrue(profile.disableNonEssentialAnimations)
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
        assertEquals(6, profile.slowPollingMultiplier)
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
        assertEquals(6, profile.slowPollingMultiplier)
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
        assertEquals(Bitmap.Config.RGB_565, QrCodeGenerator.DefaultBitmapConfig)
    }

    @Test
    fun ultraQrSkipsFullBitmapScanOnlyForTallExportImages() {
        val ultraProfile = LowRamProfile(enabled = true, severe = true, ultra = true)
        val lowProfile = LowRamProfile(enabled = true)

        assertTrue(
            shouldSkipQrFullBitmapScanAfterFallback(
                lowRamProfile = ultraProfile,
                width = 720,
                height = 1320
            )
        )
        assertFalse(
            shouldSkipQrFullBitmapScanAfterFallback(
                lowRamProfile = ultraProfile,
                width = 720,
                height = 720
            )
        )
        assertFalse(
            shouldSkipQrFullBitmapScanAfterFallback(
                lowRamProfile = lowProfile,
                width = 720,
                height = 1320
            )
        )
    }

    @Test
    fun webViewSessionResetSkipsFullHttpCacheForUltraOrMissingWebView() {
        val ultraProfile = LowRamProfile(enabled = true, severe = true, ultra = true)
        val normalProfile = LowRamProfile()

        assertFalse(
            shouldClearWebViewHttpCacheForSessionReset(
                lowRamProfile = ultraProfile,
                hasExistingWebView = false
            )
        )
        assertFalse(
            shouldClearWebViewHttpCacheForSessionReset(
                lowRamProfile = ultraProfile,
                hasExistingWebView = true
            )
        )
        assertFalse(
            shouldClearWebViewHttpCacheForSessionReset(
                lowRamProfile = normalProfile,
                hasExistingWebView = false
            )
        )
        assertTrue(
            shouldClearWebViewHttpCacheForSessionReset(
                lowRamProfile = normalProfile,
                hasExistingWebView = true
            )
        )
    }

    @Test
    fun runtimePressureEscalatesProfileToUltra() {
        val base = LowRamProfile(enabled = true, severe = false, ultra = false)

        val trimEscalated = resolveRuntimePressureProfile(
            baseProfile = base,
            trimLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        )
        assertTrue(trimEscalated.ultra)
        assertEquals(6, trimEscalated.slowPollingMultiplier)
        assertEquals(4_000L, trimEscalated.screenPinningSteadyPollMillis)
        assertEquals(8, trimEscalated.detectorMetadataCacheMaxEntries)

        val memoryEscalated = resolveRuntimePressureProfile(
            baseProfile = LowRamProfile(),
            availableMemoryBytes = 512L * 1024L * 1024L
        )
        assertTrue(memoryEscalated.enabled)
        assertTrue(memoryEscalated.severe)
        assertTrue(memoryEscalated.ultra)
    }
}
