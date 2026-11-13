package com.coblax.examlock.ui.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.UpgradeUiScope

@Composable
internal fun ExamRuntimeChrome(
    state: ExamRuntimeChromeState,
    actions: ExamRuntimeChromeActions,
    modifier: Modifier = Modifier,
    webViewLayer: @Composable BoxScope.() -> Unit,
    fullscreenLayer: (@Composable BoxScope.() -> Unit)? = null
) {
    UpgradeUiScope {
        ExamRuntimeChromeContent(
            state = state,
            actions = actions,
            modifier = modifier,
            webViewLayer = webViewLayer,
            fullscreenLayer = fullscreenLayer
        )
    }
}

@Composable
private fun ExamRuntimeChromeContent(
    state: ExamRuntimeChromeState,
    actions: ExamRuntimeChromeActions,
    modifier: Modifier,
    webViewLayer: @Composable BoxScope.() -> Unit,
    fullscreenLayer: (@Composable BoxScope.() -> Unit)?
) {
    val tokens = ScopedUiTokens.current
    val pageLoading = state.loadingProgress >= 0f && state.loadingProgress < 1f
    var proctorInfoOpen by rememberSaveable { mutableStateOf(false) }
    val showProctorInfo = proctorInfoOpen && state.examSessionStarted
    // Back closes the sheet before the exam's own back handling (page back / exit
    // prompt) sees it. Registered only while open: the newest back callback wins.
    if (showProctorInfo) {
        BackHandler { proctorInfoOpen = false }
    }
    // Stays up during a fullscreen custom view too, which is drawn between the bars.
    // The app's own fullscreen hook puts the whole exam page into fullscreen on the
    // first tap of every page, so hiding the bars here took away exit, reload and the
    // built-in keyboard mid-exam, seemingly at random.
    val showRuntimeChrome = state.examSessionStarted
    val connectionNotice = resolveExamRuntimeConnectionNotice(
        networkStatus = state.networkStatus,
        serverStatus = state.serverStatus
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.current.background)
    ) {
        if (showRuntimeChrome) {
            ExamRuntimeHeader(
                examDisplayName = state.examDisplayName,
                networkStatus = state.networkStatus,
                serverStatus = state.serverStatus,
                batteryStatus = state.batteryStatus,
                shieldStatus = state.shieldStatus
            )

            if (connectionNotice != null) {
                RuntimeConnectionWarningBanner(noticeKind = connectionNotice)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.examSessionStarted) {
                webViewLayer()
            }

            if (state.examSessionStarted && pageLoading && !state.hasFullscreenCustomView) {
                LinearProgressIndicator(
                    progress = { state.loadingProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter),
                    color = AppColors.current.blue,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round
                )
            }

            if (
                state.examSessionStarted &&
                !state.hasFullscreenCustomView &&
                !state.webViewErrorMessage.isNullOrBlank()
            ) {
                // Hides the page under the card: a server's own error page (Apache's
                // "Server at … Port 80", for one) can name the exam's address, which is
                // kept from students. Touches stop here too, not on an unseen page.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(AppColors.current.background)
                        .pointerInput(Unit) { detectTapGestures { } }
                )
                ExamWebErrorOverlay(
                    examDisplayName = state.examDisplayName,
                    errorMessage = state.webViewErrorMessage,
                    onRetry = actions.onRetryLoading,
                    modifier = Modifier.align(Alignment.Center),
                    stallAttempt = state.stallAttempt,
                    stallReport = state.stallReport,
                    onRestartApp = actions.onRestartApp
                )
            }

            if (state.examSessionStarted && state.hasFullscreenCustomView) {
                fullscreenLayer?.invoke(this)
            }

            if (showProctorInfo) {
                ExamProctorInfoOverlay(
                    state = state,
                    onClose = { proctorInfoOpen = false },
                    onRestartApp = {
                        proctorInfoOpen = false
                        actions.onRestartApp()
                    },
                    probeSite = actions.probeExamSite
                )
            }
        }

        if (
            showRuntimeChrome &&
            state.useBuiltInExamKeyboard &&
            state.showBuiltInExamKeyboard
        ) {
            ExamBuiltInKeyboardPanel(
                isShiftEnabled = state.builtInKeyboardShiftEnabled,
                onTextKey = actions.onTextKey,
                onBackspace = actions.onBackspace,
                onEnter = actions.onEnter,
                onSpace = actions.onSpace,
                onShiftToggle = actions.onShiftToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ExamRuntimeUiTestTags.KeyboardPanel)
                    .padding(
                        horizontal = tokens.spaceMedium,
                        vertical = tokens.spaceXSmall
                    )
            )
        }

        if (showRuntimeChrome) {
            // Owns the navigation-bar inset (zero in lock mode, where system bars are hidden).
            ExamRuntimeFooter(
                isRefreshing = pageLoading,
                proctorInfoOpen = showProctorInfo,
                onShowProctorInfo = {
                    if (proctorInfoOpen) {
                        proctorInfoOpen = false
                    } else {
                        actions.onShowProctorInfo()
                        proctorInfoOpen = true
                    }
                },
                onRefresh = actions.onRefreshPage,
                onGoHome = actions.onGoHome
            )
        }
    }
}
