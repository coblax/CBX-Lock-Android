package com.coblax.examlock.ui.geofence

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.SecureStrings
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The area editor with its real map, as a Normal phone opens it. */
@RunWith(AndroidJUnit4::class)
class GeofenceMapCameraUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val schoolYard = listOf(
        GeofenceVertex("-7.025100", "110.417100"),
        GeofenceVertex("-7.025100", "110.418900"),
        GeofenceVertex("-7.026800", "110.418900"),
        GeofenceVertex("-7.026800", "110.417100")
    )

    /**
     * "Show all points" from the whole-country view used to fly down a dozen zoom levels,
     * streaming every level's tiles; the emulator's renderer died on it now and then. The
     * camera now jumps straight to the school when the move is that long.
     */
    @Test
    fun framingTheSchoolFromTheWholeCountryKeepsTheEditorAlive() {
        assumeTrue("needs the Maps key", SecureStrings.mapsApiKey.isNotBlank())
        composeRule.setContent {
            COBLAXEXAMLOCKTheme {
                CompositionLocalProvider(
                    LocalLowRamProfile provides LowRamProfile(),
                    LocalUiLanguage provides UiLanguage.English
                ) {
                    GeofenceAreaEditorScreen(
                        shape = GeofenceShapeType.Polygon,
                        initialPoints = schoolYard,
                        initialRadiusMeters = "",
                        onDismiss = {},
                        onSave = { _, _ -> }
                    )
                }
            }
        }
        composeRule.waitUntil(30_000) {
            composeRule.onAllNodesWithContentDescription("Show all points").fetchSemanticsNodes().isNotEmpty()
        }
        repeat(3) {
            // Out to roughly the whole country, one level at a time like a student would.
            repeat(13) {
                composeRule.onNodeWithContentDescription("Zoom out").performClick()
                Thread.sleep(120)
            }
            composeRule.onNodeWithContentDescription("Show all points").performClick()
            Thread.sleep(2_000)
        }
        composeRule.onNodeWithContentDescription("Show all points").assertIsDisplayed()
    }
}
