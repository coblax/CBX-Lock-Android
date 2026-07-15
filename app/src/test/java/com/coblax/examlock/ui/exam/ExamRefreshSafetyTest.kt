package com.coblax.examlock.ui.exam

import com.coblax.examlock.DeviceCompatibilityProfile
import com.coblax.examlock.ScreenPinningMode
import com.coblax.examlock.model.DiagnosticEventLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamRefreshSafetyTest {
    @Test
    fun manualRefreshUsesBrowserLikeReloadWhenSafe() {
        var softLoads = 0
        var browserReloads = 0
        var serverProbes = 0
        val actions = buildExamRuntimeChromeActionsForSession(
            examSessionStarted = true,
            screenPinningMode = ScreenPinningMode.Enforced,
            screenPinningAvailable = true,
            lockTaskRequestPending = false,
            deviceCompatibilityProfile = DeviceCompatibilityProfile(),
            isIndonesian = true,
            isCurrentlyLoading = { false },
            lockTaskAlreadyActive = { true },
            markTrustedRuntimeChromeAction = {},
            clearWebViewError = {},
            loadExamUrl = { softLoads += 1 },
            reloadExamUrlLikeBrowser = { browserReloads += 1 },
            stopWebViewLoading = {},
            setLoadingProgress = {},
            setWebViewStopRequested = {},
            setLastExamRefreshDecision = {},
            setScreenPinningMessage = {},
            setShowExitExamDialog = {},
            launchExamServerProbe = { _, _ -> serverProbes += 1 },
            recordAction = { _: String, _: String, _: DiagnosticEventLevel -> },
            sendBuiltInKeyboardText = {},
            sendBuiltInKeyboardBackspace = {},
            sendKeyboardArrowLeft = {},
            sendKeyboardArrowRight = {},
            toggleSideArrowControls = { true },
            sendBuiltInKeyboardEnter = {},
            toggleBuiltInKeyboardShift = {}
        )

        actions.onRefreshPage()

        assertEquals(0, softLoads)
        assertEquals(1, browserReloads)
        assertEquals(1, serverProbes)
    }

    @Test
    fun manualRefreshStopsLoadingPageInsteadOfReloadingAgain() {
        var browserReloads = 0
        var stopLoads = 0
        var stopRequested = false
        var loadingProgress = 0f
        val events = mutableListOf<String>()
        val actions = buildExamRuntimeChromeActionsForSession(
            examSessionStarted = true,
            screenPinningMode = ScreenPinningMode.Enforced,
            screenPinningAvailable = true,
            lockTaskRequestPending = false,
            deviceCompatibilityProfile = DeviceCompatibilityProfile(),
            isIndonesian = true,
            isCurrentlyLoading = { true },
            lockTaskAlreadyActive = { true },
            markTrustedRuntimeChromeAction = {},
            clearWebViewError = {},
            loadExamUrl = {},
            reloadExamUrlLikeBrowser = { browserReloads += 1 },
            stopWebViewLoading = { stopLoads += 1 },
            setLoadingProgress = { loadingProgress = it },
            setWebViewStopRequested = { stopRequested = it },
            setLastExamRefreshDecision = {},
            setScreenPinningMessage = {},
            setShowExitExamDialog = {},
            launchExamServerProbe = { _, _ -> },
            recordAction = { code: String, _: String, _: DiagnosticEventLevel -> events += code },
            sendBuiltInKeyboardText = {},
            sendBuiltInKeyboardBackspace = {},
            sendKeyboardArrowLeft = {},
            sendKeyboardArrowRight = {},
            toggleSideArrowControls = { true },
            sendBuiltInKeyboardEnter = {},
            toggleBuiltInKeyboardShift = {}
        )

        actions.onRefreshPage()

        assertEquals(0, browserReloads)
        assertEquals(1, stopLoads)
        assertTrue(stopRequested)
        assertEquals(1f, loadingProgress, 0.0f)
        assertTrue(events.contains(ExamRuntimeHardeningDiagnostics.ExamRefreshStoppedByUser))
    }

    @Test
    fun activePinnedExamReloadsWithoutRequestingLockTask() {
        val decision = resolveExamRefreshSafetyDecision(
            examSessionStarted = true,
            screenPinningEnforced = true,
            lockTaskAlreadyActive = true,
            lockTaskRequestPending = false
        )

        assertEquals(ExamRefreshSafetyOutcome.SafeReloadOnly, decision.outcome)
        assertTrue(decision.allowWebViewReload)
        assertFalse(decision.shouldRequestLockTask)
        assertEquals(ExamRuntimeHardeningDiagnostics.ExamRefreshSafeLockTaskSkipped, decision.eventCode)
    }

    @Test
    fun refreshWaitsWhenPinningRequestAlreadyPending() {
        val decision = resolveExamRefreshSafetyDecision(
            examSessionStarted = true,
            screenPinningEnforced = true,
            lockTaskAlreadyActive = false,
            lockTaskRequestPending = true
        )

        assertEquals(ExamRefreshSafetyOutcome.BlockedPinningPending, decision.outcome)
        assertFalse(decision.allowWebViewReload)
        assertFalse(decision.shouldRequestLockTask)
        assertEquals(ExamRuntimeHardeningDiagnostics.ExamRefreshPinningPendingBlocked, decision.eventCode)
    }

    @Test
    fun refreshDoesNotTryToRecoverLostPinningByRequestingAgain() {
        val decision = resolveExamRefreshSafetyDecision(
            examSessionStarted = true,
            screenPinningEnforced = true,
            lockTaskAlreadyActive = false,
            lockTaskRequestPending = false
        )

        assertEquals(ExamRefreshSafetyOutcome.BlockedPinningInactive, decision.outcome)
        assertFalse(decision.allowWebViewReload)
        assertFalse(decision.shouldRequestLockTask)
        assertEquals(ExamRuntimeHardeningDiagnostics.ExamRefreshPinningInactiveBlocked, decision.eventCode)
    }

    @Test
    fun bypassedPinningExamCanReloadWithoutLockTask() {
        val decision = resolveExamRefreshSafetyDecision(
            examSessionStarted = true,
            screenPinningEnforced = false,
            lockTaskAlreadyActive = false,
            lockTaskRequestPending = false
        )

        assertEquals(ExamRefreshSafetyOutcome.SafeReloadOnly, decision.outcome)
        assertTrue(decision.allowWebViewReload)
        assertFalse(decision.shouldRequestLockTask)
        assertEquals(ExamRuntimeHardeningDiagnostics.ExamRefreshCompleted, decision.eventCode)
    }
}
