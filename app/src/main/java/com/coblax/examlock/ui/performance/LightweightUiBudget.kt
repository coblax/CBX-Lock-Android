package com.coblax.examlock.ui.performance

import com.coblax.examlock.LowRamProfile

internal data class PreparationActionRenderBudget(
    val maxBlockingActions: Int,
    val maxWarningActions: Int,
    val renderWarningsWhileBlocking: Boolean
)

internal fun shouldRenderRuntimeAnimation(lowRamProfile: LowRamProfile): Boolean =
    !lowRamProfile.enabled && !lowRamProfile.disableNonEssentialAnimations

@Suppress("UNUSED_PARAMETER")
internal fun shouldBuildFullPreparationPayload(
    lowRamProfile: LowRamProfile,
    showDetails: Boolean
): Boolean = !lowRamProfile.enabled

internal fun resolvePreparationActionRenderBudget(
    lowRamProfile: LowRamProfile
): PreparationActionRenderBudget =
    when {
        lowRamProfile.ultra -> PreparationActionRenderBudget(
            maxBlockingActions = 0,
            maxWarningActions = 0,
            renderWarningsWhileBlocking = false
        )
        lowRamProfile.enabled -> PreparationActionRenderBudget(
            maxBlockingActions = 3,
            maxWarningActions = 2,
            renderWarningsWhileBlocking = true
        )
        else -> PreparationActionRenderBudget(
            maxBlockingActions = Int.MAX_VALUE,
            maxWarningActions = Int.MAX_VALUE,
            renderWarningsWhileBlocking = true
        )
    }
