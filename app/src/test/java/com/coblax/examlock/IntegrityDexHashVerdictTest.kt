package com.coblax.examlock

import org.junit.Assert.*
import org.junit.Test

/**
 * Regression cover for "APK integrity check failed — hash APK tidak terbaca".
 *
 * Making the hash comparison fail closed also made a blank *actual* hash an enforced
 * issue. That hash is produced by our own reader, and the native one reports any
 * failure by returning an empty string — so a device where it could not complete was
 * refused the exam even though nothing about the APK had changed. A reader that fails
 * is not evidence of tampering, and only the reference asset going missing is.
 */
class IntegrityDexHashVerdictTest {
    private val hash = "A0B0A19713B08D6068374DEAF60C92DB3FDE2FE1DC3DB37F262945931742D3BD"

    @Test
    fun anUnreadableActualHashIsReportedButNeverBlocks() {
        val verdict = IntegrityGuard.resolveDexHashVerdict(
            expectedHash = hash,
            actualHash = "",
            enforceMissingReference = true
        )
        assertNull("an unreadable hash must not be an enforced issue", verdict.issue)
        assertTrue(verdict.unreadable)
    }

    @Test
    fun aStrippedReferenceAssetStillCountsAsTampering() {
        val verdict = IntegrityGuard.resolveDexHashVerdict(
            expectedHash = "",
            actualHash = hash,
            enforceMissingReference = true
        )
        assertEquals("dex_hash_reference_missing", verdict.issue)
        assertFalse(verdict.unreadable)
    }

    @Test
    fun debugBuildsDoNotEnforceTheMissingReference() {
        // The Gradle task only generates the asset for release variants.
        val verdict = IntegrityGuard.resolveDexHashVerdict(
            expectedHash = "",
            actualHash = hash,
            enforceMissingReference = false
        )
        assertNull(verdict.issue)
        assertFalse(verdict.unreadable)
    }

    @Test
    fun aRealMismatchIsStillCaught() {
        val verdict = IntegrityGuard.resolveDexHashVerdict(
            expectedHash = hash,
            actualHash = hash.replaceRange(0, 1, "B"),
            enforceMissingReference = true
        )
        assertEquals("dex_hash_mismatch", verdict.issue)
        assertFalse(verdict.unreadable)
    }

    @Test
    fun matchingHashesRaiseNothing() {
        val verdict = IntegrityGuard.resolveDexHashVerdict(
            expectedHash = hash,
            actualHash = hash,
            enforceMissingReference = true
        )
        assertNull(verdict.issue)
        assertFalse(verdict.unreadable)
    }

    @Test
    fun bothBlankFavoursTheReferenceSignal() {
        // Nothing to compare either way; the stripped asset is the actionable half.
        val verdict = IntegrityGuard.resolveDexHashVerdict(
            expectedHash = "",
            actualHash = "",
            enforceMissingReference = true
        )
        assertEquals("dex_hash_reference_missing", verdict.issue)
        assertFalse(verdict.unreadable)
    }
}
