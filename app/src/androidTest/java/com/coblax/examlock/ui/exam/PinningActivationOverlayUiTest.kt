package com.coblax.examlock.ui.exam

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PinningOverlayTestTag = "pinning_overlay"

/** The "Activating Exam Lock" overlay, over a screen whose whole surface is a button. */
@RunWith(AndroidJUnit4::class)
class PinningActivationOverlayUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var cancelTaps = 0
    private var tapsBehind = 0

    private fun show(smallScreenLargeText: Boolean = false) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalUiLanguage provides UiLanguage.Indonesian,
                LocalDensity provides if (smallScreenLargeText) {
                    Density(density.density, fontScale = 1.6f)
                } else {
                    density
                }
            ) {
                COBLAXEXAMLOCKTheme {
                    Box(if (smallScreenLargeText) Modifier.size(320.dp, 420.dp) else Modifier.fillMaxSize()) {
                        Button(onClick = { tapsBehind++ }, modifier = Modifier.fillMaxSize()) {
                            Text("Mulai")
                        }
                        PinningActivationOverlay(
                            onCancel = { cancelTaps++ },
                            modifier = Modifier.fillMaxSize().zIndex(10f).testTag(PinningOverlayTestTag)
                        )
                    }
                }
            }
        }
    }

    /**
     * A finger drifts a pixel or two between touching and lifting, so a real tap carries
     * move events that an emulator's `input tap` never sends.
     */
    @Test
    fun cancelAnswersAFingerTapThatDriftsSlightly() {
        show()
        composeRule.onNodeWithText("Batal").performScrollTo().performTouchInput {
            down(center)
            moveBy(Offset(2f, 1f))
            moveBy(Offset(-1f, 1f))
            up()
        }
        composeRule.runOnIdle { assertEquals(1, cancelTaps) }
    }

    @Test
    fun touchesNeverReachTheScreenBehind() {
        show()
        composeRule.onRoot().performTouchInput {
            // The dimmed edge beside the card, then a drifting finger on the card's text.
            click(Offset(4f, height / 2f))
            down(Offset(4f, height / 3f))
            moveBy(Offset(3f, 2f))
            up()
        }
        composeRule.onNodeWithText("Mengaktifkan Kunci Ujian...").performTouchInput {
            down(center)
            moveBy(Offset(2f, 2f))
            up()
        }
        composeRule.runOnIdle {
            assertEquals(0, tapsBehind)
            assertEquals(0, cancelTaps)
        }
    }

    /** On a small phone with large text the Cancel button sits below the fold. */
    @Test
    fun cancelCanBeScrolledIntoViewByTouchOnASmallScreen() {
        show(smallScreenLargeText = true)
        composeRule.onNodeWithText("Batal").assertIsNotDisplayed()
        repeat(4) {
            composeRule.onNodeWithTag(PinningOverlayTestTag).performTouchInput {
                swipeUp(startY = height * 0.8f, endY = height * 0.2f)
            }
        }
        composeRule.onNodeWithText("Batal").assertIsDisplayed().performTouchInput {
            down(center)
            moveBy(Offset(1f, 2f))
            up()
        }
        composeRule.runOnIdle {
            assertEquals(1, cancelTaps)
            assertEquals(0, tapsBehind)
        }
    }
}
