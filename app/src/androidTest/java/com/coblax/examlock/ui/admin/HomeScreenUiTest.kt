package com.coblax.examlock.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mainActionsAreLargeAndTappable() {
        var scans = 0
        var directLinks = 0
        var admins = 0
        composeRule.setContent {
            HomeUnderTest(
                onScanExam = { scans += 1 },
                onOpenFastExam = { directLinks += 1 },
                onOpenAdmin = { admins += 1 }
            )
        }

        composeRule.onNodeWithTag(HomeUiTestTags.ScanAction)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag(HomeUiTestTags.DirectLinkAction)
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag(HomeUiTestTags.AdminAction)
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("EXAM_SKANSATP").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, scans)
            assertEquals(1, directLinks)
            assertEquals(1, admins)
        }
    }

    @Test
    fun narrowPhoneWithLargeFontKeepsDirectLinkReadable() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.6f),
                LocalLowRamProfile provides LowRamProfile(enabled = true, severe = true, ultra = true)
            ) {
                Box(
                    Modifier
                        .width(320.dp)
                        .height(640.dp)
                ) {
                    HomeUnderTest(language = UiLanguage.Indonesian)
                }
            }
        }

        composeRule.onNodeWithText("Pindai QR ujian").assertIsDisplayed()
        composeRule.onNodeWithText("EXAM_SKANSATP").assertIsDisplayed()
        composeRule.onNodeWithTag(HomeUiTestTags.SettingsAction).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun settingsSheetChangesThemeAndLanguageThenCloses() {
        var theme by mutableStateOf(ThemeMode.Light)
        var language by mutableStateOf(UiLanguage.Indonesian)
        composeRule.setContent {
            CompositionLocalProvider(LocalUiLanguage provides language) {
                COBLAXEXAMLOCKTheme(themeMode = theme) {
                    ExamLockHomeScreen(
                        uiLanguage = language,
                        onUiLanguageChange = { language = it },
                        themeMode = theme,
                        onThemeModeChange = { theme = it },
                        onScanExam = {},
                        onOpenAdmin = {},
                        onOpenFastExam = {},
                        directLinkLabel = "EXAM_SKANSATP",
                        onSecretTap = {},
                        onOpenPerformanceProfile = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag(HomeUiTestTags.SettingsAction).performClick()
        composeRule.onNodeWithTag(HomeUiTestTags.SettingsSheet).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tema: gelap").performClick()
        composeRule.runOnIdle { assertEquals(ThemeMode.Dark, theme) }
        composeRule.onNodeWithText("English").performClick()
        composeRule.runOnIdle { assertEquals(UiLanguage.English, language) }

        // Back closes the sheet instead of leaving the app.
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag(HomeUiTestTags.SettingsSheet).assertDoesNotExist()

        composeRule.onNodeWithTag(HomeUiTestTags.SettingsAction).performClick()
        composeRule.onNodeWithTag(HomeUiTestTags.SettingsSheetClose)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag(HomeUiTestTags.SettingsSheet).assertDoesNotExist()
    }

    @Test
    fun profileBadgeForwardsEveryTapForTheSecretAdminGesture() {
        var secretTaps = 0
        composeRule.setContent {
            HomeUnderTest(onSecretTap = { secretTaps += 1 })
        }

        repeat(4) {
            composeRule.onNodeWithTag(HomeUiTestTags.ProfileBadge).performScrollTo().performClick()
        }
        composeRule.runOnIdle { assertEquals(4, secretTaps) }
    }

    @Composable
    private fun HomeUnderTest(
        language: UiLanguage = UiLanguage.English,
        onScanExam: () -> Unit = {},
        onOpenFastExam: () -> Unit = {},
        onOpenAdmin: () -> Unit = {},
        onSecretTap: () -> Unit = {}
    ) {
        CompositionLocalProvider(LocalUiLanguage provides language) {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                ExamLockHomeScreen(
                    uiLanguage = language,
                    onUiLanguageChange = {},
                    onScanExam = onScanExam,
                    onOpenAdmin = onOpenAdmin,
                    onOpenFastExam = onOpenFastExam,
                    directLinkLabel = "EXAM_SKANSATP",
                    onSecretTap = onSecretTap,
                    onOpenPerformanceProfile = {}
                )
            }
        }
    }
}
