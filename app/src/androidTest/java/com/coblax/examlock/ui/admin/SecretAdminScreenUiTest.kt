package com.coblax.examlock.ui.admin

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.SecretAdminTab
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecretAdminScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var tab by mutableStateOf(SecretAdminTab.Setup.name)
    private var draft by mutableStateOf(AdminSettings())
    private var applied: AdminSettings? = null

    private fun show(themeMode: ThemeMode = ThemeMode.Light) {
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = themeMode) {
                SecretAdminScreen(
                    settings = AdminSettings(),
                    examName = "Exam",
                    onSettingsChange = { draft = it },
                    onResetDirectLink = {},
                    onBack = {},
                    deviceTimeBaselineWallClockMillis = System.currentTimeMillis(),
                    deviceTimeBaselineElapsedRealtimeMillis = SystemClock.elapsedRealtime(),
                    selectedTabName = tab,
                    onSelectedTabNameChange = { tab = it },
                    externalDraftSettings = draft,
                    onDraftSettingsChange = { draft = it },
                    onApplySettings = { applied = it }
                )
            }
        }
    }

    @Test
    fun everyTabIsReachableWithoutScrollingSideways() {
        show()
        SecretAdminVisibleTabs.forEach { target ->
            composeRule.onNodeWithTag(SecretAdminUiTestTags.TabPrefix + target.name)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .performClick()
            composeRule.runOnIdle { assertEquals(target.name, tab) }
        }
    }

    /** The retired Location tab (a saved name from an older version) opens Setup. */
    @Test
    fun retiredLocationTabOpensSetupWithTheLocationPolicy() {
        tab = SecretAdminTab.Location.name
        show()
        composeRule.onNodeWithText("Location policy").assertIsDisplayed()
        composeRule.onNodeWithText("Exam URL").assertIsDisplayed()
    }

    @Test
    fun turningOnABypassAsksFirstThenWaitsForApply() {
        tab = SecretAdminTab.Overrides.name
        show()
        val vpnRow = composeRule.onNodeWithTag(SecretAdminUiTestTags.OverrideRowPrefix + "vpn")
        vpnRow.performScrollTo().assertIsOff().performClick()

        composeRule.onNodeWithText("Turn on this bypass?").assertIsDisplayed()
        composeRule.onNodeWithText("Turn on").performClick()

        composeRule.runOnIdle { assertTrue(draft.bypassVpn) }
        vpnRow.assertIsOn()
        composeRule.onNodeWithTag(SecretAdminUiTestTags.ApplyAction).performClick()
        composeRule.runOnIdle { assertEquals(true, applied?.bypassVpn) }
    }

    @Test
    fun turnOffAllClearsBypassesWithoutAQuestion() {
        tab = SecretAdminTab.Overrides.name
        draft = AdminSettings().copy(bypassVpn = true, bypassRoot = true)
        show(ThemeMode.Dark)

        composeRule.onNodeWithTag(SecretAdminUiTestTags.DisableAllOverrides)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertFalse(draft.hasAnyBypass())
        }
        composeRule.onNodeWithText("No bypass is active. Every exam check is enforced.").assertIsDisplayed()
    }
}
