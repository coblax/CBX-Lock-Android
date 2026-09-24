package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import com.coblax.examlock.ui.theme.UpgradeUiScope
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreparationScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun startStaysLockedWithReadableHintWhileBlockedAtLargeFont() {
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Dark) {
                LargeFont {
                    UpgradeUiScope {
                        Box(Modifier.width(320.dp)) {
                            PreparationStartBar(
                                overview = overview(blocking = PreparationCategory.DeviceSetup),
                                isStartingExam = false,
                                webViewSessionResetInFlight = false,
                                hasBypassIndicators = false,
                                onStartExam = {}
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag(PreparationUiTestTags.StartAction)
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(PreparationUiTestTags.StartHint).assertIsDisplayed()
        composeRule.onNodeWithText("Fix 1 required item first.").assertIsDisplayed()
    }

    @Test
    fun startIsOneTapAwayWhenEverythingPasses() {
        var startCount = 0
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                UpgradeUiScope {
                    PreparationStartBar(
                        overview = overview(blocking = null),
                        isStartingExam = false,
                        webViewSessionResetInFlight = false,
                        hasBypassIndicators = false,
                        onStartExam = { startCount += 1 }
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PreparationUiTestTags.StartAction)
            .assertIsEnabled()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithTag(PreparationUiTestTags.StartHint).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, startCount) }
    }

    @Test
    fun categoryTilesStayTappableOnNarrowPhoneWithLargeFont() {
        var opened: PreparationCategory? = null
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                LargeFont {
                    UpgradeUiScope {
                        Box(Modifier.width(288.dp)) {
                            PreparationCategoryGrid(
                                categories = overview(blocking = PreparationCategory.Location).categories,
                                onOpenCategory = { opened = it }
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag(PreparationUiTestTags.CategoryTilePrefix + PreparationCategory.Location.key)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(PreparationCategory.Location, opened) }
    }

    @Test
    fun attentionCardShowsProblemAndItsFixTogether() {
        var fixCount = 0
        var detailsCount = 0
        val status = PreparationCategoryStatus(
            category = PreparationCategory.DeviceSetup,
            issues = listOf(
                PreparationIssue(
                    key = "bluetooth_on",
                    title = "Bluetooth is on",
                    message = "Turn Bluetooth off before the exam.",
                    blocking = true
                )
            ),
            actions = listOf(
                PreparationQuickFixAction(
                    code = "quick_fix_65",
                    text = "Turn Off Bluetooth",
                    fieldText = "Turn Off Bluetooth",
                    severity = QuickFixSeverity.Blocking,
                    target = QuickFixTarget.All,
                    priority = 65,
                    section = PreparationSection.DeviceSetup,
                    onClick = { fixCount += 1 }
                )
            )
        )
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Dark) {
                UpgradeUiScope {
                    PreparationAttentionCard(
                        status = status,
                        hasGlobalBlockingIssues = true,
                        onOpenDetails = { detailsCount += 1 }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Bluetooth is on").assertIsDisplayed()
        composeRule.onNodeWithText("Turn Off Bluetooth")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("Details").performClick()
        composeRule.runOnIdle {
            assertEquals(1, fixCount)
            assertEquals(1, detailsCount)
        }
    }

    @Test
    fun detailSheetClosesFromCloseButtonAndScrim() {
        var open by mutableStateOf<PreparationCategory?>(PreparationCategory.Location)
        val status = overview(blocking = PreparationCategory.Location).status(PreparationCategory.Location)
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                UpgradeUiScope {
                    Box(
                        Modifier
                            .width(360.dp)
                            .height(640.dp)
                    ) {
                        PreparationCategorySheetHost(
                            openCategory = open,
                            onDismiss = { open = null },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            PreparationCategorySheetContent(
                                status = status,
                                hasGlobalBlockingIssues = true,
                                showGeofenceMapAction = true,
                                onOpenGeofenceMap = {},
                                onDismiss = { open = null }
                            ) {
                                Text("Technical rows")
                            }
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag(PreparationUiTestTags.DetailSheet).assertIsDisplayed()
        composeRule.onNodeWithText("Technical rows").assertIsDisplayed()
        composeRule.onNodeWithTag(PreparationUiTestTags.DetailSheetClose)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag(PreparationUiTestTags.DetailSheet).assertDoesNotExist()

        composeRule.runOnIdle { open = PreparationCategory.Location }
        composeRule.onNodeWithTag(PreparationUiTestTags.DetailSheet).assertIsDisplayed()
        composeRule.onNodeWithTag(PreparationUiTestTags.DetailSheetScrim).performClick()
        composeRule.onNodeWithTag(PreparationUiTestTags.DetailSheet).assertDoesNotExist()
    }

    @androidx.compose.runtime.Composable
    private fun LargeFont(content: @androidx.compose.runtime.Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, fontScale = 1.5f),
            content = content
        )
    }

    private fun overview(blocking: PreparationCategory?): PreparationOverview {
        val categories = PreparationCategory.entries.map { category ->
            PreparationCategoryStatus(
                category = category,
                issues = if (category == blocking) {
                    listOf(
                        PreparationIssue(
                            key = "issue_${category.key}",
                            title = "Needs a fix",
                            message = "Fix it, then come back.",
                            blocking = true
                        )
                    )
                } else {
                    emptyList()
                },
                actions = emptyList()
            )
        }
        return PreparationOverview(
            categories = categories,
            attention = categories.filter { it.tone != PreparationTone.Clear },
            canStartExam = blocking == null,
            scanPending = false
        )
    }
}
