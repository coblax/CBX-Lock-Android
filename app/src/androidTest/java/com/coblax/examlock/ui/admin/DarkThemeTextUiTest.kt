package com.coblax.examlock.ui.admin

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.preparation.CollapsibleChecklistSection
import com.coblax.examlock.ui.preparation.PreparationChecklistHeader
import com.coblax.examlock.ui.preparation.SectionHealth
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import com.coblax.examlock.ui.theme.AppColors
import java.io.File
import org.junit.Rule
import org.junit.Test

class DarkThemeTextUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun themeIconsCycleAndIndonesianChecklistTextRendersInDarkLowRamMode() {
        var theme by mutableStateOf(ThemeMode.Dark)
        composeRule.setContent {
            CompositionLocalProvider(LocalUiLanguage provides UiLanguage.Indonesian) {
                COBLAXEXAMLOCKTheme(themeMode = theme) {
                    Column(Modifier.background(AppColors.current.background)) {
                        ThemeTogglePill(theme) { theme = it }
                        PreparationChecklistHeader(
                            examTitle = "Ujian Sekolah",
                            severeLowRamPreparation = true,
                            blockingCount = 2, warningCount = 0, safeCount = 4,
                            canStartExam = false, firstBlockingReason = "Periksa jaringan",
                            onBackHome = {}
                        )
                        CollapsibleChecklistSection(
                            "network", SectionHealth("Koneksi", false, 2)
                        ) { Text("Periksa Wi-Fi atau data seluler", color = AppColors.current.textPrimary) }
                        CollapsibleChecklistSection(
                            "device", SectionHealth("Perangkat", true, 0)
                        ) { Text("Siap") }
                    }
                }
            }
        }
        composeRule.onNodeWithContentDescription("Tema: gelap").assertIsDisplayed()
        composeRule.onNodeWithText("2 masalah").assertIsDisplayed()
        composeRule.onNodeWithText("Aman semua").assertIsDisplayed()
        composeRule.onNodeWithText("Wizard").assertIsDisplayed()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshot = File(context.getExternalFilesDir(null), "dark-ui-regression.png")
        screenshot.outputStream().use {
            composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        composeRule.onNodeWithContentDescription("Tema: gelap").performClick()
        composeRule.onNodeWithContentDescription("Tema: sistem").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Tema: terang").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Tema: gelap").assertIsDisplayed()
    }
}
