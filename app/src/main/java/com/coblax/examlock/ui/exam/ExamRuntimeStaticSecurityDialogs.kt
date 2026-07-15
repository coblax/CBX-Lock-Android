package com.coblax.examlock.ui.exam

import androidx.compose.runtime.Composable
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.ui.dialog.DisplayMirrorRuntimeViolationDialog
import com.coblax.examlock.ui.dialog.MultiWindowRuntimeViolationDialog
import com.coblax.examlock.ui.dialog.ScreenRecorderRuntimeViolationDialog

@Composable
internal fun RuntimeStaticSecurityDialogsHost(
    securityUiState: ExamRuntimeSecurityUiState,
    onOpenAppSettings: () -> Unit,
    onOpenCastSettings: () -> Unit,
    onRefreshStatus: () -> Unit,
    onSendReport: (DiagnosticSection) -> Unit
) {
    if (securityUiState.showScreenRecorderViolationDialog.value) {
        ScreenRecorderRuntimeViolationDialog(
            packages = securityUiState.screenRecorderPackages.value,
            violationCount = securityUiState.screenRecorderViolationCount.intValue,
            onOpenAppSettings = onOpenAppSettings,
            onRefreshStatus = onRefreshStatus,
            onSendReport = { onSendReport(DiagnosticSection.ScreenRecorder) }
        )
    }

    if (securityUiState.showDisplayMirrorViolationDialog.value) {
        DisplayMirrorRuntimeViolationDialog(
            externalDisplayCount = securityUiState.externalDisplayCount.intValue,
            externalDisplayInfoList = securityUiState.externalDisplayInfoList.value,
            violationCount = securityUiState.displayMirrorViolationCount.intValue,
            onOpenCastSettings = onOpenCastSettings,
            onRefreshStatus = onRefreshStatus,
            onSendReport = { onSendReport(DiagnosticSection.DisplayMirror) }
        )
    }

    if (securityUiState.showMultiWindowViolationDialog.value) {
        MultiWindowRuntimeViolationDialog(
            modeInfo = securityUiState.multiWindowModeInfo.value,
            runtimeDetected = securityUiState.multiWindowDetected.value ||
                securityUiState.multiWindowModeInfo.value.inAnySplitMode,
            violationCount = securityUiState.multiWindowViolationCount.intValue,
            onRefreshStatus = onRefreshStatus,
            onSendReport = { onSendReport(DiagnosticSection.MultiWindow) }
        )
    }
}
