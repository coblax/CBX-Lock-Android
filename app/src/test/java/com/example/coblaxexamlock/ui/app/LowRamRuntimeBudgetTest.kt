package com.example.coblaxexamlock.ui.app

import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.LowRamProfileOverride
import com.example.coblaxexamlock.applyLowRamProfileOverride
import com.example.coblaxexamlock.runtime.LowRamDispatchers
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LowRamRuntimeBudgetTest {
    @After
    fun resetBudget() {
        applyLowRamRuntimeDetectorBudget(LowRamProfile())
    }

    @Test
    fun runtimeDetectorBudgetAppliesFullProfilePolicy() {
        val detectedUltra = LowRamProfile(
            enabled = true,
            severe = true,
            ultra = true,
            detectorMetadataCacheMaxEntries = 8,
            detectorParallelism = 1,
            skipDisplayMetadataInScan = true
        )

        val forcedNormal = applyLowRamProfileOverride(
            detectedProfile = detectedUltra,
            override = LowRamProfileOverride.Normal
        )
        applyLowRamRuntimeDetectorBudget(forcedNormal)
        assertEquals(1, SecurityDetectorCache.cacheTtlMultiplier)
        assertEquals(64, SecurityDetectorCache.metadataCacheMaxEntries)
        assertFalse(SecurityDetectorCache.skipDisplayMetadataDefault)
        assertEquals(4, LowRamDispatchers.detectorParallelism)

        val forcedLow = applyLowRamProfileOverride(
            detectedProfile = detectedUltra,
            override = LowRamProfileOverride.Low
        )
        applyLowRamRuntimeDetectorBudget(forcedLow)
        assertEquals(2, SecurityDetectorCache.cacheTtlMultiplier)
        assertEquals(24, SecurityDetectorCache.metadataCacheMaxEntries)
        assertFalse(SecurityDetectorCache.skipDisplayMetadataDefault)
        assertEquals(2, LowRamDispatchers.detectorParallelism)

        val forcedUltra = applyLowRamProfileOverride(
            detectedProfile = detectedUltra,
            override = LowRamProfileOverride.Ultra
        )
        applyLowRamRuntimeDetectorBudget(forcedUltra)
        assertEquals(3, SecurityDetectorCache.cacheTtlMultiplier)
        assertEquals(8, SecurityDetectorCache.metadataCacheMaxEntries)
        assertTrue(SecurityDetectorCache.skipDisplayMetadataDefault)
        assertEquals(1, LowRamDispatchers.detectorParallelism)
    }
}
