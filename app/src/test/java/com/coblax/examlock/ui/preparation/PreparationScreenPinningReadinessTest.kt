package com.coblax.examlock.ui.preparation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparationScreenPinningReadinessTest {
    @Test
    fun supportedButInactivePinningIsNotReady() {
        val ready = resolvePreparationScreenPinningReady(
            bypassScreenPinning = false,
            screenPinningAvailable = true,
            isScreenPinningActive = false,
            accessibilityGuardAvailable = false,
            accessibilityGuardEnabled = false
        )

        assertFalse(ready)
    }

    @Test
    fun activePinningIsReady() {
        val ready = resolvePreparationScreenPinningReady(
            bypassScreenPinning = false,
            screenPinningAvailable = true,
            isScreenPinningActive = true,
            accessibilityGuardAvailable = false,
            accessibilityGuardEnabled = false
        )

        assertTrue(ready)
    }

    @Test
    fun bypassKeepsPinningReadyWithoutActiveLockTask() {
        val ready = resolvePreparationScreenPinningReady(
            bypassScreenPinning = true,
            screenPinningAvailable = true,
            isScreenPinningActive = false,
            accessibilityGuardAvailable = false,
            accessibilityGuardEnabled = false
        )

        assertTrue(ready)
    }

    @Test
    fun accessibilityFallbackOnlyCountsWhenPinningIsUnavailable() {
        assertTrue(
            resolvePreparationScreenPinningReady(
                bypassScreenPinning = false,
                screenPinningAvailable = false,
                isScreenPinningActive = false,
                accessibilityGuardAvailable = true,
                accessibilityGuardEnabled = true
            )
        )
        assertFalse(
            resolvePreparationScreenPinningReady(
                bypassScreenPinning = false,
                screenPinningAvailable = true,
                isScreenPinningActive = false,
                accessibilityGuardAvailable = true,
                accessibilityGuardEnabled = true
            )
        )
    }
}
