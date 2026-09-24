package com.coblax.examlock.ui.exam

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.ExamNetworkStatus
import com.coblax.examlock.model.NetworkDiagnostics
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkReadinessVerdict
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExamRuntimeChromeUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactChromeKeepsAllActionsVisibleAndClickable() {
        var refreshCount = 0
        var navBarInset = 0.dp
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                val density = LocalDensity.current
                navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 1.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .height(640.dp)
                    ) {
                        ExamRuntimeChrome(
                            state = chromeState(),
                            actions = chromeActions(onRefresh = { refreshCount += 1 }),
                            webViewLayer = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .testTag(WebViewTag)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Info header on top, actions in the footer; together they stay slim.
        val headerHeight = composeRule.onNodeWithTag(ExamRuntimeUiTestTags.Header)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
            .let { it.bottom - it.top }
        val footerHeight = composeRule.onNodeWithTag(ExamRuntimeUiTestTags.Footer)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
            .let { it.bottom - it.top }
        assertTrue("header is $headerHeight tall", headerHeight <= 36.dp)
        // 1.5x font on a 320dp phone: the words sit under the icons, so the footer grows a little.
        assertTrue("footer is $footerHeight tall", footerHeight <= 64.dp + navBarInset)
        composeRule.onNodeWithTag(WebViewTag).assertHeightIsAtLeast(640.dp - 100.dp - navBarInset)
        composeRule.onNodeWithText("Reload", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ArrowToggle)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.RefreshAction)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ExitAction)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)

        composeRule.runOnIdle {
            assertEquals(1, refreshCount)
        }
    }

    @Test
    fun regularPhoneShowsStatusWordsAndActionLabels() {
        var arrowToggles = 0
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .height(640.dp)
                ) {
                    ExamRuntimeChrome(
                        state = chromeState(),
                        actions = chromeActions(onToggleArrows = { arrowToggles += 1 }),
                        webViewLayer = {
                            Box(Modifier.fillMaxSize().testTag(WebViewTag))
                        }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Ujian Akhir Semester").assertIsDisplayed()
        composeRule.onNodeWithText("Online").assertIsDisplayed()
        composeRule.onNodeWithText("82%").assertIsDisplayed()
        composeRule.onNodeWithText("Protected").assertIsDisplayed()
        composeRule.onNodeWithText("Arrows", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Reload", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Exit", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ArrowToggle)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, arrowToggles) }
    }

    @Test
    fun nonBlockingConnectionIssueUsesWarningBanner() {
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Dark) {
                ExamRuntimeChrome(
                    state = chromeState(
                        networkStatus = networkStatus(NetworkReadinessVerdict.Unstable)
                    ),
                    actions = chromeActions(),
                    webViewLayer = {
                        Box(Modifier.fillMaxSize().testTag(WebViewTag))
                    }
                )
            }
        }

        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.WarningBanner).assertIsDisplayed()
        composeRule.onNodeWithTag(WebViewTag).assertIsDisplayed()
    }

    @Test
    fun customFullscreenHidesHeaderFooterKeyboardAndBanner() {
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                ExamRuntimeChrome(
                    state = chromeState(
                        hasFullscreenCustomView = true,
                        useBuiltInExamKeyboard = true,
                        showBuiltInExamKeyboard = true
                    ),
                    actions = chromeActions(),
                    webViewLayer = {
                        Box(Modifier.fillMaxSize().testTag(WebViewTag))
                    },
                    fullscreenLayer = {
                        Box(Modifier.fillMaxSize().testTag(FullscreenTag))
                    }
                )
            }
        }

        composeRule.onNodeWithTag(FullscreenTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.Header).assertDoesNotExist()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.WarningBanner).assertDoesNotExist()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.KeyboardPanel).assertDoesNotExist()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.Footer).assertDoesNotExist()
    }

    private fun chromeState(
        hasFullscreenCustomView: Boolean = false,
        useBuiltInExamKeyboard: Boolean = false,
        showBuiltInExamKeyboard: Boolean = false,
        networkStatus: NetworkReadinessStatus = networkStatus()
    ): ExamRuntimeChromeState {
        return ExamRuntimeChromeState(
            examSessionStarted = true,
            examDisplayName = "Ujian Akhir Semester",
            loadingProgress = 1f,
            webViewErrorMessage = null,
            hasFullscreenCustomView = hasFullscreenCustomView,
            useBuiltInExamKeyboard = useBuiltInExamKeyboard,
            showBuiltInExamKeyboard = showBuiltInExamKeyboard,
            showSideArrowControls = false,
            hasEditableFocus = showBuiltInExamKeyboard,
            builtInKeyboardShiftEnabled = false,
            networkStatus = networkStatus,
            serverStatus = ExamServerFooterStatus.Online,
            batteryStatus = ExamBatteryStatus(levelPercent = 82, isCharging = false),
            shieldStatus = ExamFooterShieldStatus.Safe
        )
    }

    private fun chromeActions(
        onRefresh: () -> Unit = {},
        onToggleArrows: () -> Unit = {}
    ): ExamRuntimeChromeActions {
        return ExamRuntimeChromeActions(
            onRetryLoading = {},
            onRefreshPage = onRefresh,
            onGoHome = {},
            onTextKey = {},
            onBackspace = {},
            onArrowLeft = {},
            onArrowRight = {},
            onToggleSideArrowControls = onToggleArrows,
            onEnter = {},
            onSpace = {},
            onShiftToggle = {}
        )
    }

    private fun networkStatus(
        verdict: NetworkReadinessVerdict = NetworkReadinessVerdict.ConnectedStable
    ): NetworkReadinessStatus {
        return NetworkReadinessStatus(
            examStatus = ExamNetworkStatus(
                label = if (verdict == NetworkReadinessVerdict.Offline) "Offline" else "Online",
                detail = "Wi-Fi",
                isConnected = verdict != NetworkReadinessVerdict.Offline
            ),
            diagnostics = NetworkDiagnostics(
                activeNetworkAvailable = verdict != NetworkReadinessVerdict.Offline,
                transports = listOf("wifi"),
                hasInternetCapability = verdict != NetworkReadinessVerdict.Offline,
                isValidated = verdict == NetworkReadinessVerdict.ConnectedStable,
                isCaptivePortal = verdict == NetworkReadinessVerdict.CaptivePortal,
                isMetered = false,
                isVpnActive = verdict == NetworkReadinessVerdict.VpnActive,
                isAirplaneModeEnabled = verdict == NetworkReadinessVerdict.AirplaneMode,
                notRoaming = true,
                interfaceName = "wlan0",
                wifi = null,
                cellular = null
            ),
            verdict = verdict,
            transportLabel = "Wi-Fi",
            quickFixReason = null
        )
    }

    private companion object {
        const val WebViewTag = "exam_runtime_test_webview"
        const val FullscreenTag = "exam_runtime_test_fullscreen"
    }
}
