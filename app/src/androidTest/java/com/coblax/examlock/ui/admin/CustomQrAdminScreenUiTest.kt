package com.coblax.examlock.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coblax.examlock.model.CustomQrAdminTab
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import com.coblax.examlock.viewmodel.CustomQrDraftState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomQrAdminScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun incompleteExamDataBlocksForwardStep() {
        var selectedTab by mutableStateOf(CustomQrAdminTab.Exam.name)

        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                val density = LocalDensity.current
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 1.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .height(640.dp)
                    ) {
                        CustomQrAdminScreen(
                            showSaveToDirectLinkOption = false,
                            onBack = {},
                            selectedTabName = selectedTab,
                            onSelectedTabNameChange = { selectedTab = it }
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Location").performClick()
        composeRule.onNodeWithText(
            "Complete the URL, exam name, start time, and end time."
        ).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(CustomQrAdminTab.Exam.name, selectedTab)
        }
    }

    @Test
    fun validExamDataMovesThroughNumberedSteps() {
        var selectedTab by mutableStateOf(CustomQrAdminTab.Exam.name)
        val draft = CustomQrDraftState(
            examUrl = "https://exam.example",
            examName = "Final Exam",
            startTime = "2026-07-30 08:00",
            endTime = "2026-07-30 10:00"
        )

        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Dark) {
                CustomQrAdminScreen(
                    showSaveToDirectLinkOption = false,
                    onBack = {},
                    selectedTabName = selectedTab,
                    onSelectedTabNameChange = { selectedTab = it },
                    draft = draft
                )
            }
        }

        composeRule.onNodeWithText("Location").performClick()
        composeRule.onNodeWithText("Location / Geofence").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Generate QR").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(CustomQrAdminTab.Generate.name, selectedTab)
        }
    }
}
