package com.example.coblaxexamlock.ui.performance

import com.example.coblaxexamlock.LowRamProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LightweightUiBudgetTest {
    @Test
    fun runtimeAnimationsOnlyRenderForNormalBudget() {
        assertTrue(shouldRenderRuntimeAnimation(LowRamProfile()))
        assertFalse(shouldRenderRuntimeAnimation(LowRamProfile(enabled = true)))
        assertFalse(
            shouldRenderRuntimeAnimation(
                LowRamProfile(enabled = true, severe = true, ultra = true)
            )
        )
        assertFalse(
            shouldRenderRuntimeAnimation(
                LowRamProfile(disableNonEssentialAnimations = true)
            )
        )
    }

    @Test
    fun lowRamPreparationPayloadStaysStepScoped() {
        assertTrue(
            shouldBuildFullPreparationPayload(
                lowRamProfile = LowRamProfile(),
                showDetails = false
            )
        )
        assertFalse(
            shouldBuildFullPreparationPayload(
                lowRamProfile = LowRamProfile(enabled = true),
                showDetails = true
            )
        )
        assertFalse(
            shouldBuildFullPreparationPayload(
                lowRamProfile = LowRamProfile(enabled = true, severe = true, ultra = true),
                showDetails = true
            )
        )
    }

    @Test
    fun preparationActionBudgetMatchesProfileWeight() {
        val normalBudget = resolvePreparationActionRenderBudget(LowRamProfile())
        assertEquals(Int.MAX_VALUE, normalBudget.maxBlockingActions)
        assertEquals(Int.MAX_VALUE, normalBudget.maxWarningActions)
        assertTrue(normalBudget.renderWarningsWhileBlocking)

        val lowBudget = resolvePreparationActionRenderBudget(LowRamProfile(enabled = true))
        assertEquals(3, lowBudget.maxBlockingActions)
        assertEquals(2, lowBudget.maxWarningActions)
        assertTrue(lowBudget.renderWarningsWhileBlocking)

        val ultraBudget = resolvePreparationActionRenderBudget(
            LowRamProfile(enabled = true, severe = true, ultra = true)
        )
        assertEquals(0, ultraBudget.maxBlockingActions)
        assertEquals(0, ultraBudget.maxWarningActions)
        assertFalse(ultraBudget.renderWarningsWhileBlocking)
    }
}
