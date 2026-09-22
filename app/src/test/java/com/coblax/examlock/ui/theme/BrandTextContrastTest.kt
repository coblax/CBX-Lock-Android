package com.coblax.examlock.ui.theme

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandTextContrastTest {
    @Test
    fun brandLabelsMeetTextContrastOnLightAndDarkSurfaces() {
        for (colors in listOf(lightExamLockColors(), darkExamLockColors())) {
            for (surface in listOf(colors.background, colors.cardBg, colors.surfaceSoft)) {
                val textLuminance = colors.brandText.luminance()
                val backgroundLuminance = surface.luminance()
                val ratio = (maxOf(textLuminance, backgroundLuminance) + 0.05f) /
                    (minOf(textLuminance, backgroundLuminance) + 0.05f)
                assertTrue("Brand text contrast is $ratio (dark=${colors.isDark})", ratio >= 4.5f)
            }
        }
    }
}
