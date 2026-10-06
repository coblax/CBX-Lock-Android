package com.coblax.examlock.ui.geofence

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.testsupport.canCaptureScreenshots
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The area editor without a map, as a low-RAM phone opens it. */
@RunWith(AndroidJUnit4::class)
class GeofenceAreaEditorUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val lowRam = LowRamProfile(enabled = true, deferHeavyUi = true)

    private fun setEditor(
        shape: GeofenceShapeType,
        points: List<GeofenceVertex> = emptyList(),
        radius: String = "",
        language: UiLanguage = UiLanguage.English,
        themeMode: ThemeMode = ThemeMode.Light,
        fontScale: Float = 1f,
        widthDp: Int = 0,
        onDismiss: () -> Unit = {},
        onSave: (List<GeofenceVertex>, String) -> Unit = { _, _ -> }
    ) {
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = themeMode) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalLowRamProfile provides lowRam,
                    LocalUiLanguage provides language,
                    LocalDensity provides Density(density.density, fontScale = fontScale)
                ) {
                    val editor = @androidx.compose.runtime.Composable {
                        GeofenceAreaEditorScreen(
                            shape = shape,
                            initialPoints = points,
                            initialRadiusMeters = radius,
                            onDismiss = onDismiss,
                            onSave = onSave
                        )
                    }
                    if (widthDp > 0) {
                        Box(Modifier.width(widthDp.dp).height(640.dp)) { editor() }
                    } else {
                        editor()
                    }
                }
            }
        }
    }

    @Test
    fun aPastedCoordinateBecomesACircleCenter() {
        var saved: Pair<List<GeofenceVertex>, String>? = null
        setEditor(GeofenceShapeType.Circle, onSave = { points, radius -> saved = points to radius })

        composeRule.onNodeWithTag(GeofenceEditorSaveTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(GeofenceEditorAddPointTestTag).performScrollTo().performClick()
        composeRule.onNodeWithTag(GeofenceEditorCoordinateTestTag)
            .performScrollTo()
            .performTextInput("https://maps.google.com/?q=-7.0251,110.4171")
        composeRule.onNodeWithText("Ready: 1 center, radius 100 m").assertIsDisplayed()
        composeRule.onNodeWithTag(GeofenceEditorSaveTestTag).assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(GeofenceVertex("-7.025100", "110.417100")), saved?.first)
            assertEquals("100", saved?.second)
        }
    }

    @Test
    fun aTooSmallRadiusBlocksSaving() {
        setEditor(GeofenceShapeType.Circle, points = listOf(GeofenceVertex("-7.0251", "110.4171")), radius = "100")

        composeRule.onNodeWithTag(GeofenceEditorRadiusTestTag).performTextReplacement("5")
        composeRule.onNodeWithText("Use a radius of at least 20 m; phone GPS is not more precise than that.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(GeofenceEditorSaveTestTag).assertIsNotEnabled()
    }

    @Test
    fun aPolygonNeedsThreeCorners() {
        setEditor(
            GeofenceShapeType.Polygon,
            points = listOf(GeofenceVertex("-7.0000", "110.0000"), GeofenceVertex("-7.0000", "110.0010"))
        )

        composeRule.onNodeWithText("Add at least 3 corners.").assertIsDisplayed()
        composeRule.onNodeWithTag(GeofenceEditorSaveTestTag).assertIsNotEnabled()
    }

    @Test
    fun leavingWithChangesAsksFirst() {
        var dismissed = false
        setEditor(GeofenceShapeType.Circle, onDismiss = { dismissed = true })

        composeRule.onNodeWithTag(GeofenceEditorAddPointTestTag).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeRule.onNodeWithText("Keep editing").performClick()
        composeRule.runOnIdle { assertEquals(false, dismissed) }
    }

    /** Small phone, large text, dark theme, Indonesian: saved for a visual check. */
    @Test
    fun narrowPhoneWithLargeTextKeepsEverythingReachable() {
        setEditor(
            GeofenceShapeType.Polygon,
            points = listOf(
                GeofenceVertex("-7.000000", "110.000000"),
                GeofenceVertex("-7.000000", "110.001000"),
                GeofenceVertex("-7.001000", "110.001000"),
                GeofenceVertex("-7.001000", "110.000000")
            ),
            language = UiLanguage.Indonesian,
            themeMode = ThemeMode.Dark,
            fontScale = 1.5f,
            widthDp = 320
        )
        composeRule.onNodeWithTag(GeofenceEditorSaveTestTag).assertIsDisplayed()
        save("geofence_editor_320dp_dark_id.png")
        composeRule.onNodeWithText("Hapus semua").performScrollTo().assertIsDisplayed()
        save("geofence_editor_320dp_dark_id_scrolled.png")
    }

    private fun save(name: String) {
        if (!canCaptureScreenshots) return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use {
            composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
