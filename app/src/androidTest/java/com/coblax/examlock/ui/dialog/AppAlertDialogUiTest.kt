package com.coblax.examlock.ui.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import com.coblax.examlock.ui.theme.UiStatusTone
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppAlertDialogUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun violationPopupShowsProblemFirstAndFoldsEvidence() {
        var acknowledged = 0
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                KeyboardViolationDialog(
                    violationCount = 3,
                    keyboardLabel = "com.example.cheat.keyboard",
                    onAcknowledge = { acknowledged += 1 }
                )
            }
        }

        composeRule.onNodeWithText("Keyboard not allowed").assertIsDisplayed()
        composeRule.onNodeWithText("Violation #3").assertIsDisplayed()
        composeRule.onNodeWithText("com.example.cheat.keyboard").assertDoesNotExist()

        composeRule.onNodeWithTag(AppAlertDialogTestTags.DetailsToggle)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("com.example.cheat.keyboard").assertIsDisplayed()

        composeRule.onNodeWithTag(AppAlertDialogTestTags.PrimaryAction)
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, acknowledged) }
    }

    @Test
    fun exitPopupConfirmsOrCancels() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Dark) {
                ExitExamDialog(
                    onDismiss = { dismissed += 1 },
                    onConfirm = { confirmed += 1 },
                    isClearingSession = false
                )
            }
        }

        composeRule.onNodeWithText("Exit the exam?").assertIsDisplayed()
        composeRule.onNodeWithText("Exit exam").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle {
            assertEquals(1, confirmed)
            assertEquals(1, dismissed)
        }
    }

    @Test
    fun exitPopupWhileClearingCannotBeCancelled() {
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                ExitExamDialog(
                    onDismiss = {},
                    onConfirm = {},
                    isClearingSession = true
                )
            }
        }

        composeRule.onNodeWithTag(AppAlertDialogTestTags.PrimaryAction).assertIsNotEnabled()
        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
    }

    @Test
    fun longEvidenceScrollsWhileButtonsStayReachable() {
        var acknowledged = 0
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                AppAlertDialog(
                    tone = UiStatusTone.Danger,
                    icon = Icons.Rounded.Info,
                    title = "Location check failed",
                    message = "The app could not check the exam location.",
                    nextStep = "Tell the proctor if this keeps happening.",
                    details = (1..40).map { AppAlertDetail("Field $it", "Value $it") },
                    primaryAction = AppAlertAction("I understand", { acknowledged += 1 })
                )
            }
        }

        composeRule.onNodeWithTag(AppAlertDialogTestTags.DetailsToggle).performClick()
        composeRule.onNodeWithText("Value 40").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(AppAlertDialogTestTags.PrimaryAction)
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, acknowledged) }
    }
}
