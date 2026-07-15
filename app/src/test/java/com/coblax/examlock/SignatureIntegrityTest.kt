package com.coblax.examlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureIntegrityTest {
    @Test
    fun normalizeFingerprintRemovesColonsAndUppercases() {
        assertEquals("AABBCC", normalizeFingerprint("aa:bb:cc"))
    }

    @Test
    fun resolveExpectedSigningFingerprintsReleaseOnly() {
        val expected = resolveExpectedSigningFingerprints(
            isDebugBuild = false,
            releaseFingerprint = "AA:BB",
            debugFingerprint = "CC:DD"
        )

        assertEquals(listOf("AA:BB"), expected)
    }

    @Test
    fun resolveExpectedSigningFingerprintsDebugIncludesBoth() {
        val expected = resolveExpectedSigningFingerprints(
            isDebugBuild = true,
            releaseFingerprint = "AA:BB",
            debugFingerprint = "CC:DD"
        )

        assertEquals(listOf("AA:BB", "CC:DD"), expected)
    }

    @Test
    fun evaluatorMatchesNormalizedFingerprint() {
        val result = IntegrityEvaluator.evaluate("AA:BB:CC", listOf("aabbcc"))

        assertTrue(result.isMatch)
    }

    @Test
    fun evaluatorFailsWhenExpectedMissing() {
        val result = IntegrityEvaluator.evaluate("AA:BB", emptyList())

        assertFalse(result.isMatch)
    }
}
