package com.coblax.examlock.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopedUiSystemTest {
    @Test
    fun `upgraded controls meet minimum target and canonical action size`() {
        val tokens = DefaultUiDesignTokens

        assertEquals(48f, tokens.touchTarget.value)
        assertEquals(48f, tokens.compactControlHeight.value)
        assertEquals(54f, tokens.primaryActionHeight.value)
        assertTrue(tokens.primaryActionHeight >= tokens.touchTarget)
    }

    @Test
    fun `upgraded radius scale uses only canonical rounded sizes`() {
        val tokens = DefaultUiDesignTokens

        assertEquals(listOf(12f, 16f, 20f), listOf(
            tokens.radiusSmall.value,
            tokens.radiusMedium.value,
            tokens.radiusLarge.value
        ))
        assertTrue(tokens.radiusPill > tokens.radiusLarge)
    }
}
