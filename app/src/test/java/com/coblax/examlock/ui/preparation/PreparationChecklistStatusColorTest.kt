package com.coblax.examlock.ui.preparation

import androidx.compose.ui.graphics.Color
import com.coblax.examlock.ui.theme.LockGoldDark
import com.coblax.examlock.ui.theme.lightExamLockColors
import org.junit.Assert.assertEquals
import org.junit.Test

class PreparationChecklistStatusColorTest {
    private val colors = lightExamLockColors()

    @Test
    fun goodStrongAndBestStatusesUseGreenAccent() {
        val green = Color(0xFF2F8F63)

        listOf("Good", "Baik", "Strong", "Kuat", "Best", "Terbaik").forEach { status ->
            assertEquals(green, preparationStatusAccentColor(status, colors))
        }
    }

    @Test
    fun legacyAndWarningStatusesUseWarningAccent() {
        listOf("Legacy Risk", "Risiko Legacy", "Legacy DPC", "Warning", "Peringatan").forEach { status ->
            assertEquals(LockGoldDark, preparationStatusAccentColor(status, colors))
        }
    }

    @Test
    fun dangerStatusUsesRedAccent() {
        assertEquals(Color(0xFFB34A4A), preparationStatusAccentColor("Danger", colors))
        assertEquals(Color(0xFFB34A4A), preparationStatusAccentColor("Bahaya", colors))
    }
}
