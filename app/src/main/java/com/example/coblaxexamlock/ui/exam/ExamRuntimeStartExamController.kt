package com.example.coblaxexamlock.ui.exam

import android.content.Context
import android.os.SystemClock
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.ExamWebViewSessionResetStep
import com.example.coblaxexamlock.clearExamWebViewSessionData
import com.example.coblaxexamlock.ClipboardChangeDecision
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.readClipboardSnapshotLite

internal fun resetStartExamPreparationSecurityEpisodes(flowUiState: ExamRuntimeFlowUiState) {
    flowUiState.geofenceViolationCount.intValue = 0
    flowUiState.geofenceRuntimeEpisodeKey.value = null
    flowUiState.lastGeofenceTrigger.value = null
    flowUiState.lastGeofenceAt.value = null
    flowUiState.lastGeofenceContext.value = null
    flowUiState.lastGeofenceRefreshAt.value = null
    flowUiState.showGeofenceViolationDialog.value = false
    flowUiState.fakeLocationViolationCount.intValue = 0
    flowUiState.fakeLocationRuntimeEpisodeKey.value = null
    flowUiState.lastFakeLocationTrigger.value = null
    flowUiState.lastFakeLocationAt.value = null
    flowUiState.lastFakeLocationContext.value = null
    flowUiState.showFakeLocationViolationDialog.value = false
}

internal fun finalizeStartExamSession(
    context: Context,
    lockTaskBridge: ActivityLockTaskBridge,
    flowUiState: ExamRuntimeFlowUiState,
    adminUiState: ExamRuntimeAdminUiState,
    clipboardUiState: ExamRuntimeClipboardUiState,
    securityUiState: ExamRuntimeSecurityUiState,
    lockTaskAlreadyActive: Boolean,
    hideSystemKeyboard: () -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit
) {
    flowUiState.webViewSessionResetError.value = null
    adminUiState.examSessionStartedAtElapsedMs.value = SystemClock.elapsedRealtime()
    // Set examSessionStarted BEFORE engage to prevent examGuardArmed from
    // briefly becoming false during the transition (race condition fix).
    flowUiState.examSessionStarted.value = true
    if (!lockTaskAlreadyActive) {
        lockTaskBridge.engage(allowLockTask = false)
    }
    val clipboardSnapshot = readClipboardSnapshotLite(context)
    clipboardUiState.clipboardDecisionFingerprint.value = clipboardSnapshot.decisionFingerprint
    clipboardUiState.clipboardDecisionSemanticSignature.value = clipboardSnapshot.semanticSignature
    clipboardUiState.lastClipboardObservedAt.value = null
    clipboardUiState.lastClipboardConfirmedAt.value = null
    clipboardUiState.lastClipboardObservedSignature.value = null
    clipboardUiState.lastClipboardBaselineSemanticSignature.value = null
    clipboardUiState.lastClipboardDetectedSemanticSignature.value = null
    clipboardUiState.lastClipboardDecision.value = ClipboardChangeDecision.Idle.diagnosticLabel()
    clipboardUiState.clipboardPreBackgroundFingerprint.value = null
    clipboardUiState.clipboardPreBackgroundSignature.value = null
    clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
    clipboardUiState.clipboardResumeCheckPending.value = false
    if (flowUiState.useBuiltInExamKeyboard.value) {
        hideSystemKeyboard()
    } else {
        flowUiState.showBuiltInExamKeyboard.value = false
    }
    flowUiState.sideArrowControlsVisible.value = true
    securityUiState.overlayGuardActive.value = false
    recordAction(
        "OVERLAY_GUARD_WINDOW_DISABLED",
        "reason=transparent_overlay_does_not_block_foreign_windows",
        DiagnosticEventLevel.INFO
    )
}

internal suspend fun prepareCleanExamWebViewSessionForStart(
    context: Context,
    existingWebView: SecureExamWebView?,
    lowRamProfile: LowRamProfile,
    flowUiState: ExamRuntimeFlowUiState,
    adminUiState: ExamRuntimeAdminUiState,
    uiLanguage: UiLanguage,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    onRecoveryStateIdle: () -> Unit,
    onResetProgress: ((ExamWebViewSessionResetStep) -> Unit)? = null
): Boolean {
    if (flowUiState.webViewSessionResetInFlight.value) {
        return false
    }

    flowUiState.webViewSessionResetInFlight.value = true
    flowUiState.webViewSessionResetError.value = null
    recordAction("WEBVIEW_SESSION_RESET_STARTED", "strict_all", DiagnosticEventLevel.INFO)

    // Pre-warm the OS DNS cache for the exam host before clearing session
    // data. When WebView starts loading the URL, the resolved address will
    // already be in the system DNS cache, avoiding a cold-start DNS lookup
    // that might time out on congested school Wi-Fi networks.
    val examHost = existingWebView?.requestedExamUrl?.let { url ->
        runCatching { java.net.URI(url.trim()).host }.getOrNull()
    }
    if (!examHost.isNullOrBlank()) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { java.net.InetAddress.getByName(examHost) }
        }
    }

    val resetResult = debugMeasureExamStartSuspendWork("prepareCleanExamWebViewSessionForStart") {
        clearExamWebViewSessionData(
            context = context,
            existingWebView = existingWebView,
            lowRamProfile = lowRamProfile,
            onProgress = onResetProgress
        )
    }

    flowUiState.webViewSessionResetInFlight.value = false
    if (resetResult.isSuccess) {
        recordAction("WEBVIEW_SESSION_RESET_SUCCEEDED", "strict_all", DiagnosticEventLevel.INFO)
        onRecoveryStateIdle()
        return true
    }

    val failureDetails = resetResult.exceptionOrNull()?.message ?: "unknown"
    val failureCause = when {
        failureDetails.contains("timeout", ignoreCase = true) ->
            localized(uiLanguage, "Cause: session cleanup timed out.", "Penyebab: pembersihan sesi timeout.")
        failureDetails.contains("memory", ignoreCase = true) || failureDetails.contains("oom", ignoreCase = true) ->
            localized(uiLanguage, "Cause: device is low on memory.", "Penyebab: memori perangkat rendah.")
        failureDetails.contains("webview", ignoreCase = true) ->
            localized(uiLanguage, "Cause: the browser engine is busy or unresponsive.", "Penyebab: mesin browser sedang sibuk atau tidak merespons.")
        else ->
            localized(uiLanguage, "Cause: $failureDetails", "Penyebab: $failureDetails")
    }
    val userMessage = localized(
        uiLanguage,
        "The app could not clear the previous WebView session data yet. $failureCause Retry Start Exam Mode. If this keeps happening, close and reopen the app.",
        "Aplikasi belum bisa membersihkan data sesi WebView sebelumnya. $failureCause Coba lagi Mulai Ujian. Jika tetap gagal, tutup lalu buka ulang aplikasi."
    )
    recordAction("WEBVIEW_SESSION_RESET_FAILED", failureDetails, DiagnosticEventLevel.ERROR)
    flowUiState.webViewSessionResetError.value = userMessage
    adminUiState.securityIssueDialogTitle.value = localized(
        uiLanguage,
        "Unable to Prepare Clean Exam Session",
        "Gagal Menyiapkan Sesi Ujian Bersih"
    )
    adminUiState.securityIssueDialogMessage.value = userMessage
    return false
}
