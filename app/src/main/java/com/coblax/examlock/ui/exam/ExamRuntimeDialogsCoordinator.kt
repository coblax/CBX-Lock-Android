package com.coblax.examlock.ui.exam

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.coblax.examlock.i18n.diagnosticSectionLabel
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.dialog.AppAlertAction
import com.coblax.examlock.ui.dialog.AppAlertDialog
import com.coblax.examlock.ui.dialog.ExamRuntimeDialogsActions
import com.coblax.examlock.ui.dialog.ExamRuntimeDialogsHost
import com.coblax.examlock.ui.dialog.ExamRuntimeDialogsState
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.UiStatusTone
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

private const val StartExamBlockedNetworkReachabilityCode = "START_EXAM_BLOCKED_NETWORK_REACHABILITY"
private const val StartExamPreflightSlowHintDelayMillis = 2_000L

@Composable
internal fun ExamRuntimeDialogsCoordinator(
    pendingSection: DiagnosticSection?,
    uiLanguage: UiLanguage,
    runtimeDialogsState: ExamRuntimeDialogsState,
    runtimeDialogsActions: ExamRuntimeDialogsActions,
    screenPinningMessage: String?,
    securityIssueDialogTitle: String?,
    securityIssueDialogMessage: String?,
    securityIssueDialogCode: String?,
    startExamPreflightState: StartExamPreflightUiState,
    isRefreshingNetwork: Boolean,
    bugReportFeedbackTitle: String?,
    bugReportFeedbackMessage: String?,
    onDismissPendingSection: () -> Unit,
    onConfirmPendingSection: (DiagnosticSection) -> Unit,
    onDismissScreenPinningMessage: () -> Unit,
    onDismissSecurityIssueDialog: () -> Unit,
    onRefreshNetworkStatus: () -> Unit,
    onDismissBugReportFeedback: () -> Unit,
    onCancelPreflight: () -> Unit,
    lockTaskRequestPending: Boolean
) {
    pendingSection?.let { section ->
        val sectionLabel = diagnosticSectionLabel(section, uiLanguage)
        AppAlertDialog(
            tone = UiStatusTone.Info,
            icon = Icons.AutoMirrored.Rounded.Send,
            title = tr("Send diagnostics?", "Kirim diagnostik?"),
            message = localized(
                uiLanguage,
                "Send diagnostics for $sectionLabel to Telegram?",
                "Kirim diagnostik $sectionLabel ke Telegram?"
            ),
            dismissible = true,
            onDismissRequest = onDismissPendingSection,
            primaryAction = AppAlertAction(tr("Send", "Kirim"), { onConfirmPendingSection(section) }),
            secondaryActions = listOf(AppAlertAction(tr("Cancel", "Batal"), onDismissPendingSection))
        )
    }

    ExamRuntimeDialogsHost(
        state = runtimeDialogsState,
        actions = runtimeDialogsActions
    )

    StartExamPreflightDialog(
        uiLanguage = uiLanguage,
        state = startExamPreflightState,
        onCancel = onCancelPreflight
    )

    screenPinningMessage?.let { message ->
        val title = localized(uiLanguage, "Screen pinning required", "Screen pinning diperlukan")
        if (lockTaskRequestPending) {
            // Waiting on Android's pinning prompt: nothing to press here.
            AppAlertDialog(
                tone = UiStatusTone.Info,
                icon = Icons.Rounded.PhonelinkLock,
                title = title,
                message = message,
                progress = true,
                primaryAction = null
            )
        } else {
            AppAlertDialog(
                tone = UiStatusTone.Warning,
                icon = Icons.Rounded.PhonelinkLock,
                title = title,
                message = message,
                dismissible = true,
                onDismissRequest = onDismissScreenPinningMessage,
                primaryAction = AppAlertAction(tr("Close", "Tutup"), onDismissScreenPinningMessage)
            )
        }
    }

    securityIssueDialogMessage?.let { message ->
        if (securityIssueDialogCode == StartExamBlockedNetworkReachabilityCode) {
            AppAlertDialog(
                tone = UiStatusTone.Warning,
                icon = Icons.Rounded.CloudOff,
                title = securityIssueDialogTitle ?: tr("Exam network not ready", "Jaringan ujian belum siap"),
                message = message,
                dismissible = true,
                onDismissRequest = onDismissSecurityIssueDialog,
                primaryAction = AppAlertAction(
                    label = if (isRefreshingNetwork) {
                        tr("Checking…", "Mengecek…")
                    } else {
                        tr("Check network again", "Cek ulang jaringan")
                    },
                    onClick = {
                        onRefreshNetworkStatus()
                        onDismissSecurityIssueDialog()
                    },
                    loading = isRefreshingNetwork
                ),
                secondaryActions = listOf(AppAlertAction(tr("Close", "Tutup"), onDismissSecurityIssueDialog))
            )
        } else {
            AppAlertDialog(
                tone = UiStatusTone.Warning,
                icon = Icons.Rounded.GppMaybe,
                title = securityIssueDialogTitle ?: tr("Device security", "Keamanan perangkat"),
                message = message,
                dismissible = true,
                onDismissRequest = onDismissSecurityIssueDialog,
                primaryAction = AppAlertAction(tr("Close", "Tutup"), onDismissSecurityIssueDialog)
            )
        }
    }

    bugReportFeedbackMessage?.let { message ->
        AppAlertDialog(
            tone = UiStatusTone.Info,
            icon = Icons.Rounded.Info,
            title = bugReportFeedbackTitle ?: "Info",
            message = message,
            dismissible = true,
            onDismissRequest = onDismissBugReportFeedback,
            primaryAction = AppAlertAction(tr("Close", "Tutup"), onDismissBugReportFeedback)
        )
    }
}

@Composable
private fun StartExamPreflightDialog(
    uiLanguage: UiLanguage,
    state: StartExamPreflightUiState,
    onCancel: () -> Unit
) {
    val visible = state.visible.value
    val step = state.step.value
    val startedAtElapsedMs = state.startedAtElapsedMs.value

    LaunchedEffect(visible, step, startedAtElapsedMs) {
        state.slowHintVisible.value = false
        if (visible) {
            delay(StartExamPreflightSlowHintDelayMillis)
            if (
                state.visible.value &&
                state.step.value == step &&
                state.startedAtElapsedMs.value == startedAtElapsedMs
            ) {
                state.slowHintVisible.value = true
            }
        }
    }

    if (!visible) {
        return
    }

    val label = startExamPreflightStepLabel(step, uiLanguage)
    val detail = state.detail.value ?: startExamPreflightStepDetail(step, uiLanguage)
    val slow = state.slowHintVisible.value
    AppAlertDialog(
        tone = UiStatusTone.Info,
        icon = Icons.Rounded.Sync,
        title = localized(uiLanguage, "Preparing exam…", "Menyiapkan ujian…"),
        message = detail,
        progress = true,
        badge = label,
        extraContent = if (slow) {
            {
                Text(
                    text = localized(
                        uiLanguage,
                        "This is taking longer than usual; the app is waiting for the network or device checks.",
                        "Ini lebih lama dari biasanya; aplikasi sedang menunggu jaringan atau pemeriksaan perangkat."
                    ),
                    color = AppColors.current.textSecondary,
                    style = AppTextStyles.diagnostic,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            null
        },
        primaryAction = null,
        secondaryActions = if (slow) {
            listOf(AppAlertAction(localized(uiLanguage, "Cancel", "Batal"), onCancel))
        } else {
            emptyList()
        }
    )
}
