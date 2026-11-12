package com.coblax.examlock.ui.exam

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
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
import androidx.test.platform.app.InstrumentationRegistry
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.ExamNetworkStatus
import com.coblax.examlock.model.NetworkDiagnostics
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkReadinessVerdict
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.testsupport.canCaptureScreenshots
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme
import java.io.File
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
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ProctorInfoAction)
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
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .height(640.dp)
                ) {
                    ExamRuntimeChrome(
                        state = chromeState(),
                        actions = chromeActions(),
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
        composeRule.onNodeWithText("Info", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Reload", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Exit", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Arrows", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun infoOpensProctorSheetOverThePageAndCloses() {
        var infoOpened = 0
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .height(720.dp)
                ) {
                    ExamRuntimeChrome(
                        state = chromeState().copy(
                            participantName = "Siti Aminah (12345)",
                            participantGroup = "XII IPA 1 · R07"
                        ),
                        actions = chromeActions(onShowProctorInfo = { infoOpened += 1 }),
                        webViewLayer = {
                            Box(Modifier.fillMaxSize().testTag(WebViewTag))
                        }
                    )
                }
            }
        }

        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ProctorInfoSheet).assertDoesNotExist()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ProctorInfoAction)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ProctorInfoSheet).assertIsDisplayed()
        composeRule.onNodeWithText("Info for the proctor").assertIsDisplayed()
        composeRule.onNodeWithText("Siti Aminah (12345)").assertIsDisplayed()
        composeRule.onNodeWithText("XII IPA 1 · R07").assertIsDisplayed()
        composeRule.onNodeWithText("Wi-Fi · Connected").assertIsDisplayed()
        // Header and footer stay usable around the sheet.
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.Header).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ExitAction).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, infoOpened) }

        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ProctorInfoSheet).assertDoesNotExist()

        // The footer button toggles it too.
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ProctorInfoAction).performClick()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ProctorInfoSheet).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ProctorInfoAction).performClick()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ProctorInfoSheet).assertDoesNotExist()
    }

    @Test
    fun proctorInfoSheetInIndonesianForReview() {
        composeRule.setContent {
            CompositionLocalProvider(LocalUiLanguage provides UiLanguage.Indonesian) {
                COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .height(760.dp)
                    ) {
                        ExamRuntimeChrome(
                            state = chromeState().copy(
                                participantName = "Siti Aminah (12345)",
                                participantGroup = "XII IPA 1 · R07"
                            ),
                            actions = chromeActions(),
                            webViewLayer = {
                                Box(Modifier.fillMaxSize().testTag(WebViewTag))
                            }
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithText("Info", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ProctorInfoAction).performClick()
        composeRule.onNodeWithText("Info untuk pengawas").assertIsDisplayed()
        composeRule.onNodeWithText("Wi-Fi · Terhubung").assertIsDisplayed()
        composeRule.onNodeWithText("Tutup").assertIsDisplayed()
        if (canCaptureScreenshots) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.getExternalFilesDir(null), "exam_proctor_info_id.png").outputStream().use {
                composeRule.onRoot().captureToImage().asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun stallCardShowsTheLogWithoutTheServerAndLeadsWithRestart() {
        var restarts = 0
        var reloads = 0
        val report = ExamStallReport(
            attempt = 2,
            wallClockMs = 0L,
            examHost = "cbt.sekolah.sch.id",
            page = ExamStallPageEvidence(
                readyState = "loading",
                resources = 2,
                finished = 1,
                pending = listOf("cbt.sekolah.sch.id js"),
                pendingCount = 1
            ),
            network = ExamStallNetworkEvidence(
                transport = "cellular",
                validated = true,
                privateDns = "off",
                ipv6 = false,
                lookups = listOf("cbt.sekolah.sch.id" to "103.10.20.30")
            )
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalUiLanguage provides UiLanguage.Indonesian) {
                COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                    Box(modifier = Modifier.width(360.dp).height(760.dp)) {
                        ExamRuntimeChrome(
                            state = chromeState().copy(
                                webViewErrorMessage = examStallMessage(2, 60),
                                stallAttempt = 2,
                                stallReport = report
                            ),
                            actions = chromeActions(
                                onRetry = { reloads += 1 },
                                onRestartApp = { restarts += 1 }
                            ),
                            webViewLayer = { Box(Modifier.fillMaxSize().testTag(WebViewTag)) }
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.StallLog).assertIsDisplayed()
        composeRule.onNodeWithText("Menggantung: 1 JS dari server ujian").assertIsDisplayed()
        composeRule.onNodeWithText("DNS server ujian: terjawab (IPv4)").assertIsDisplayed()
        composeRule.onNodeWithText("sekolah.sch.id", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("103.10.20.30", substring = true).assertDoesNotExist()
        if (canCaptureScreenshots) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.getExternalFilesDir(null), "exam_stall_card_id.png").outputStream().use {
                composeRule.onRoot().captureToImage().asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }

        composeRule.onNode(inCard and hasText("Mulai ulang aplikasi")).performClick()
        composeRule.onNode(inCard and hasText("Muat ulang")).performClick()
        composeRule.runOnIdle {
            assertEquals(1, restarts)
            assertEquals(1, reloads)
        }
    }

    @Test
    fun aFirstStallStillLeadsWithReload() {
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                ExamRuntimeChrome(
                    state = chromeState().copy(webViewErrorMessage = examStallMessage(1, 60), stallAttempt = 1),
                    actions = chromeActions(),
                    webViewLayer = { Box(Modifier.fillMaxSize().testTag(WebViewTag)) }
                )
            }
        }
        // The log is collected in the background; until then the panel says so.
        composeRule.onNodeWithText("Checking the network…").assertIsDisplayed()
        composeRule.onNodeWithTag(com.coblax.examlock.ui.dialog.AppAlertDialogTestTags.PrimaryAction)
            .assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(com.coblax.examlock.ui.dialog.AppAlertDialogTestTags.PrimaryAction) and hasText("Reload")
        ).assertIsDisplayed()
        composeRule.onNode(inCard and hasText("Restart app")).assertIsDisplayed()
    }

    /** A node inside the stall/error card, not the footer's buttons of the same name. */
    private val inCard = hasAnyAncestor(hasTestTag(com.coblax.examlock.ui.dialog.AppAlertDialogTestTags.Dialog))

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
    fun pageFullscreenKeepsHeaderFooterKeyboardAndBanner() {
        // The app's fullscreen hook puts the exam page itself into fullscreen on the first
        // tap; the controls must survive that, or the student loses exit, reload and the
        // built-in keyboard mid-exam.
        var refreshCount = 0
        composeRule.setContent {
            COBLAXEXAMLOCKTheme(themeMode = ThemeMode.Light) {
                ExamRuntimeChrome(
                    state = chromeState(
                        hasFullscreenCustomView = true,
                        useBuiltInExamKeyboard = true,
                        showBuiltInExamKeyboard = true,
                        networkStatus = networkStatus(NetworkReadinessVerdict.Unstable)
                    ),
                    actions = chromeActions(onRefresh = { refreshCount += 1 }),
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
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.Header).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.WarningBanner).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.KeyboardPanel).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.Footer).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.ExitAction).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamRuntimeUiTestTags.RefreshAction)
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, refreshCount) }
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
        onShowProctorInfo: () -> Unit = {},
        onRetry: () -> Unit = {},
        onRestartApp: () -> Unit = {}
    ): ExamRuntimeChromeActions {
        return ExamRuntimeChromeActions(
            onRestartApp = onRestartApp,
            onRetryLoading = onRetry,
            onRefreshPage = onRefresh,
            onGoHome = {},
            onTextKey = {},
            onBackspace = {},
            onShowProctorInfo = onShowProctorInfo,
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
