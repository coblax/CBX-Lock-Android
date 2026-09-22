package com.coblax.examlock.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStatusTest {
    @Test
    fun `each tone has a non-color default icon`() {
        assertEquals(UiStatusIcon.Info, defaultUiStatusIcon(UiStatusTone.Neutral))
        assertEquals(UiStatusIcon.Info, defaultUiStatusIcon(UiStatusTone.Info))
        assertEquals(UiStatusIcon.Check, defaultUiStatusIcon(UiStatusTone.Success))
        assertEquals(UiStatusIcon.Warning, defaultUiStatusIcon(UiStatusTone.Warning))
        assertEquals(UiStatusIcon.Blocked, defaultUiStatusIcon(UiStatusTone.Danger))
    }

    @Test
    fun `blocking status ranks above warning and informational states`() {
        assertTrue(
            uiStatusSeverityRank(UiStatusTone.Danger) >
                uiStatusSeverityRank(UiStatusTone.Warning)
        )
        assertTrue(
            uiStatusSeverityRank(UiStatusTone.Warning) >
                uiStatusSeverityRank(UiStatusTone.Info)
        )
        assertEquals(
            uiStatusSeverityRank(UiStatusTone.Info),
            uiStatusSeverityRank(UiStatusTone.Success)
        )
    }

    @Test
    fun `status foregrounds meet small text contrast in light and dark palettes`() {
        listOf(lightExamLockColors(), darkExamLockColors()).forEach { palette ->
            UiStatusTone.entries.forEach { tone ->
                val colors = resolveUiStatusColors(palette, tone)
                assertTrue(
                    "${palette.isDark} $tone contrast=" +
                        contrastRatio(colors.contentColor, colors.containerColor),
                    contrastRatio(colors.contentColor, colors.containerColor) >= 4.5f
                )
            }
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val foregroundLuminance = foreground.luminance()
        val backgroundLuminance = background.luminance()
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
