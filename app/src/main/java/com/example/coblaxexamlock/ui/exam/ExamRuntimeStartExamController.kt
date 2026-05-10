package com.example.coblaxexamlock.ui.exam

import android.content.Context
import android.os.SystemClock
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.clearExamWebViewSessionData
import com.example.coblaxexamlock.ClipboardChangeDecision
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
    lockTaskAlreadyActive: Boolean,
    hideSystemKeyboard: () -> Unit,
    showSystemKeyboard: () -> Unit
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
        showSystemKeyboard()
    }
    flowUiState.sideArrowControlsVisible.value = true
}

internal suspend fun prepareCleanExamWebViewSessionForStart(
    context: Context,
    existingWebView: SecureExamWebView?,
    flowUiState: ExamRuntimeFlowUiState,
    adminUiState: ExamRuntimeAdminUiState,
    uiLanguage: UiLanguage,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    onRecoveryStateIdle: () -> Unit
): Boolean {
    if (flowUiState.webViewSessionResetInFlight.value) {
        return false
    }

    flowUiState.webViewSessionResetInFlight.value = true
    flowUiState.webViewSessionResetError.value = null
    recordAction("WEBVIEW_SESSION_RESET_STARTED", "strict_all", DiagnosticEventLevel.INFO)

    val resetResult = debugMeasureExamStartSuspendWork("prepareCleanExamWebViewSessionForStart") {
        clearExamWebViewSessionData(
            context = context,
            existingWebView = existingWebView
        )
    }

    flowUiState.webViewSessionResetInFlight.value = false
    if (resetResult.isSuccess) {
        recordAction("WEBVIEW_SESSION_RESET_SUCCEEDED", "strict_all", DiagnosticEventLevel.INFO)
        onRecoveryStateIdle()
        return true
    }

    val failureDetails = resetResult.exceptionOrNull()?.message ?: "unknown"
    val userMessage = localized(
        uiLanguage,
        "The app could not clear the previous WebView session data yet. Retry Start Exam Mode. If this keeps happening, close and reopen the app.",
        "Aplikasi belum bisa membersihkan data sesi WebView sebelumnya. Coba lagi Mulai Ujian. Jika tetap gagal, tutup lalu buka ulang aplikasi."
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
