package com.example.coblaxexamlock.ui.preparation

import androidx.compose.ui.graphics.Color
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import org.junit.Assert.assertEquals
import org.junit.Test

class PreparationChecklistStatusColorTest {
    @Test
    fun goodStrongAndBestStatusesUseGreenAccent() {
        val green = Color(0xFF2F8F63)

        listOf("Good", "Baik", "Strong", "Kuat", "Best", "Terbaik").forEach { status ->
            assertEquals(green, preparationStatusAccentColor(status))
        }
    }

    @Test
    fun legacyAndWarningStatusesUseWarningAccent() {
        listOf("Legacy Risk", "Risiko Legacy", "Legacy DPC", "Warning", "Peringatan").forEach { status ->
            assertEquals(LockGoldDark, preparationStatusAccentColor(status))
        }
    }

    @Test
    fun dangerStatusUsesRedAccent() {
        assertEquals(Color(0xFFB34A4A), preparationStatusAccentColor("Danger"))
        assertEquals(Color(0xFFB34A4A), preparationStatusAccentColor("Bahaya"))
    }
}
