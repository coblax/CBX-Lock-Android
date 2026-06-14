package com.example.coblaxexamlock.ui.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.i18n.diagnosticSectionLabel
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.ui.admin.InfoDialog
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsActions
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsHost
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsState
import com.example.coblaxexamlock.ui.theme.LockTextSecondary
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
    onDismissBugReportFeedback: () -> Unit
) {
    pendingSection?.let { section ->
        val sectionLabel = diagnosticSectionLabel(section, uiLanguage)
        AlertDialog(
            onDismissRequest = onDismissPendingSection,
            title = { Text(tr("Send diagnostics?", "Kirim diagnostik?")) },
            text = {
                Text(
                    text = localized(
                        uiLanguage,
                        "Send diagnostics for $sectionLabel to Telegram?",
                        "Kirim diagnostik $sectionLabel ke Telegram?"
                    ),
                    color = LockTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirmPendingSection(section) }) {
                    Text(tr("Send", "Kirim"))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPendingSection) {
                    Text(tr("Cancel", "Batal"))
                }
            },
            containerColor = Color.White
        )
    }

    ExamRuntimeDialogsHost(
        state = runtimeDialogsState,
        actions = runtimeDialogsActions
    )

    StartExamPreflightDialog(
        uiLanguage = uiLanguage,
        state = startExamPreflightState
    )

    screenPinningMessage?.let { message ->
        InfoDialog(
            title = "Screen Pinning Diperlukan",
            message = message,
            onDismiss = onDismissScreenPinningMessage
        )
    }

    securityIssueDialogMessage?.let { message ->
        if (securityIssueDialogCode == StartExamBlockedNetworkReachabilityCode) {
            AlertDialog(
                onDismissRequest = onDismissSecurityIssueDialog,
                title = {
                    Text(securityIssueDialogTitle ?: tr("Exam Network Not Ready", "Network Ujian Belum Siap"))
                },
                text = {
                    Text(
                        text = message,
                        color = LockTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !isRefreshingNetwork,
                        onClick = {
                            onRefreshNetworkStatus()
                            onDismissSecurityIssueDialog()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isRefreshingNetwork) {
                                tr("Refreshing...", "Refresh...")
                            } else {
                                tr("Refresh Network", "Refresh Network")
                            }
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissSecurityIssueDialog) {
                        Text(tr("Close", "Tutup"))
                    }
                },
                containerColor = Color.White
            )
        } else {
            InfoDialog(
                title = securityIssueDialogTitle ?: "Keamanan Perangkat",
                message = message,
                onDismiss = onDismissSecurityIssueDialog
            )
        }
    }

    bugReportFeedbackMessage?.let { message ->
        InfoDialog(
            title = bugReportFeedbackTitle ?: "Info",
            message = message,
            onDismiss = onDismissBugReportFeedback
        )
    }
}

@Composable
private fun StartExamPreflightDialog(
    uiLanguage: UiLanguage,
    state: StartExamPreflightUiState
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
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(localized(uiLanguage, "Preparing exam...", "Menyiapkan ujian..."))
        },
        text = {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        lineHeight = 19.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = detail,
                    color = LockTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
                if (state.slowHintVisible.value) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = localized(
                            uiLanguage,
                            "If this takes longer than usual, the app is waiting for the network or device checks to respond.",
                            "Jika lebih lama dari biasanya, aplikasi sedang menunggu jaringan atau pemeriksaan perangkat merespons."
                        ),
                        color = LockTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        },
        confirmButton = {},
        containerColor = Color.White
    )
}
