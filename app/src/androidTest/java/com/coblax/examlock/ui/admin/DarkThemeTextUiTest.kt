package com.coblax.examlock.ui.admin

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.preparation.PreparationCategory
import com.coblax.examlock.ui.preparation.PreparationCategoryGrid
import com.coblax.examlock.ui.preparation.PreparationCategoryStatus
import com.coblax.examlock.ui.preparation.PreparationIssue
import com.coblax.examlock.ui.preparation.PreparationOverview
import com.coblax.examlock.ui.preparation.PreparationStatusCard
import com.coblax.examlock.ui.preparation.PreparationTone
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import com.coblax.examlock.ui.theme.AppColors
import java.io.File
import org.junit.Rule
import org.junit.Test

class DarkThemeTextUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun indonesianPreparationTextRendersInDarkMode() {
        val theme = ThemeMode.Dark
        composeRule.setContent {
            CompositionLocalProvider(LocalUiLanguage provides UiLanguage.Indonesian) {
                COBLAXEXAMLOCKTheme(themeMode = theme) {
                    Column(Modifier.background(AppColors.current.background)) {
                        val categories = PreparationCategory.entries.map { category ->
                            PreparationCategoryStatus(
                                category = category,
                                issues = if (category == PreparationCategory.Connectivity) {
                                    listOf(
                                        PreparationIssue("vpn_active", "VPN masih aktif", "Matikan VPN.", blocking = true),
                                        PreparationIssue("network_unstable", "Koneksi tidak stabil", null, blocking = true)
                                    )
                                } else {
                                    emptyList()
                                },
                                actions = emptyList()
                            )
                        }
                        val overview = PreparationOverview(
                            categories = categories,
                            attention = categories.filter { it.tone != PreparationTone.Clear },
                            canStartExam = false,
                            scanPending = false
                        )
                        PreparationStatusCard(overview = overview, hasBypassIndicators = false)
                        PreparationCategoryGrid(categories = categories, onOpenCategory = {})
                    }
                }
            }
        }
        composeRule.onNodeWithText("2 masalah").assertIsDisplayed()
        composeRule.onNodeWithText("2 hal wajib diperbaiki").assertIsDisplayed()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshot = File(context.getExternalFilesDir(null), "dark-ui-regression.png")
        screenshot.outputStream().use {
            composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
