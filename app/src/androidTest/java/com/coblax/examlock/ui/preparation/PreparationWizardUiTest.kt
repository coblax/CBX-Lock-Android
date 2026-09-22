package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
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
class PreparationWizardUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactBottomBarKeepsExplicitActionsAccessible() {
        var recheckCount = 0
        var nextCount = 0

        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 1.5f)
                ) {
                    UpgradeUiScope {
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .height(180.dp)
                        ) {
                            WizardBottomBar(
                                currentStepIndex = 2,
                                totalSteps = WizardStep.entries.size,
                                currentStepCompleted = false,
                                canStartExam = false,
                                isStartingExam = false,
                                webViewSessionResetInFlight = false,
                                startButtonColor = WizardGreen,
                                startButtonContentColor = androidx.compose.ui.graphics.Color.White,
                                onPrevious = {},
                                onNext = { nextCount += 1 },
                                onRecheck = { recheckCount += 1 },
                                onStartExam = {},
                                onBackHome = {}
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag(PreparationWizardUiTestTags.BackAction)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(PreparationWizardUiTestTags.RecheckAction)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag(PreparationWizardUiTestTags.PrimaryAction)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
        composeRule.onNodeWithText("Skip").assertDoesNotExist()

        composeRule.runOnIdle {
            assertEquals(1, recheckCount)
            assertEquals(1, nextCount)
        }
    }

    @Test
    fun finalStartActionStaysDisabledUntilReadinessPasses() {
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Dark) {
                UpgradeUiScope {
                    WizardBottomBar(
                        currentStepIndex = WizardStep.entries.lastIndex,
                        totalSteps = WizardStep.entries.size,
                        currentStepCompleted = false,
                        canStartExam = false,
                        isStartingExam = false,
                        webViewSessionResetInFlight = false,
                        startButtonColor = WizardRed,
                        startButtonContentColor = androidx.compose.ui.graphics.Color.White,
                        onPrevious = {},
                        onNext = {},
                        onRecheck = {},
                        onStartExam = {},
                        onBackHome = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PreparationWizardUiTestTags.PrimaryAction)
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeRule.onNodeWithText("Start exam").assertIsDisplayed()
        composeRule.onNodeWithTag(PreparationWizardUiTestTags.StepHint).assertIsDisplayed()
    }

    @Test
    fun completedStepKeepsChecklistDetailsCollapsedUntilRequested() {
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                UpgradeUiScope {
                    WizardStepDetailsCard(
                        step = WizardStep.DeviceSetup,
                        issueCount = 0
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .testTag(InnerDetailsTag)
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag(PreparationWizardUiTestTags.DetailsContent)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(PreparationWizardUiTestTags.DetailsToggle)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag(PreparationWizardUiTestTags.DetailsContent)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(InnerDetailsTag).assertIsDisplayed()
    }

    private companion object {
        const val InnerDetailsTag = "preparation_wizard_inner_details"
    }
}
