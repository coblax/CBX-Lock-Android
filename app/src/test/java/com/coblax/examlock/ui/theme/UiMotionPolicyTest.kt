package com.coblax.examlock.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiMotionPolicyTest {
    @Test
    fun `normal profile and positive animator scale enables canonical motion`() {
        val policy = resolveUiMotionPolicy(
            disableNonEssentialAnimations = false,
            animatorDurationScale = 1f
        )

        assertTrue(policy.enabled)
        assertTrue(policy.animationsEnabled)
        assertEquals(100, policy.pressDurationMillis)
        assertEquals(180, policy.contentDurationMillis)
        assertEquals(220, policy.screenDurationMillis)
        assertEquals(180, policy.themeDurationMillis)
    }

    @Test
    fun `low ram reduction disables and snaps all motion`() {
        val policy = resolveUiMotionPolicy(
            disableNonEssentialAnimations = true,
            animatorDurationScale = 1f
        )

        assertFalse(policy.enabled)
        assertEquals(0, policy.pressDurationMillis)
        assertEquals(0, policy.contentDurationMillis)
        assertEquals(0, policy.screenDurationMillis)
        assertEquals(0, policy.themeDurationMillis)
    }

    @Test
    fun `disabled or invalid system animator scale disables motion`() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { scale ->
            assertFalse(
                "scale=$scale",
                resolveUiMotionPolicy(
                    disableNonEssentialAnimations = false,
                    animatorDurationScale = scale
                ).enabled
            )
        }
    }
}
