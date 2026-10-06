package com.coblax.examlock.ui.admin

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.coblax.examlock.testsupport.canCaptureScreenshots
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.model.UiLanguage
import java.io.File
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
        composeRule.onNodeWithText("Exam location").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Generate QR").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(CustomQrAdminTab.Generate.name, selectedTab)
        }
    }

    @Test
    fun anEndBeforeTheStartIsShownAndBlocksTheNextStep() {
        var selectedTab by mutableStateOf(CustomQrAdminTab.Exam.name)
        val draft = CustomQrDraftState(
            examUrl = "cbt.sekolah.sch.id",
            examName = "PAS",
            startTime = "30/07/2026 10:00",
            endTime = "30/07/2026 08:00"
        )

        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                CustomQrAdminScreen(
                    showSaveToDirectLinkOption = false,
                    onBack = {},
                    selectedTabName = selectedTab,
                    onSelectedTabNameChange = { selectedTab = it },
                    draft = draft
                )
            }
        }

        composeRule.onNodeWithText("Next").performClick()
        composeRule.onAllNodesWithText("The end time must be after the start time.").onFirst().assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(CustomQrAdminTab.Exam.name, selectedTab) }
    }

    /** The location step on a 320 dp phone with large Indonesian text, saved for a look. */
    @Test
    fun locationStepFitsANarrowPhone() {
        val draft = CustomQrDraftState(
            examUrl = "https://exam.example",
            examName = "Final Exam",
            startTime = "30/07/2026 08:00",
            endTime = "30/07/2026 10:00",
            geofenceEnabled = true,
            geofenceShapeTypeName = GeofenceShapeType.Polygon.name,
            polygonVertices = listOf(
                GeofenceVertex("-7.000000", "110.000000"),
                GeofenceVertex("-7.000000", "110.001000"),
                GeofenceVertex("-7.001000", "110.001000"),
                GeofenceVertex("-7.001000", "110.000000"),
                GeofenceVertex("-7.001500", "110.000500")
            )
        )
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 1.5f),
                    LocalUiLanguage provides UiLanguage.Indonesian
                ) {
                    Box(modifier = Modifier.width(320.dp).height(640.dp)) {
                        CustomQrAdminScreen(
                            showSaveToDirectLinkOption = false,
                            onBack = {},
                            selectedTabName = CustomQrAdminTab.Location.name,
                            draft = draft
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Polygon", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(CustomQrLocationEditTestTag).performScrollTo().assertIsDisplayed()
        if (canCaptureScreenshots) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.getExternalFilesDir(null), "custom_qr_location_320dp_id.png").outputStream().use {
                composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
