package com.example.coblaxexamlock.ui.exam

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

@Composable
internal fun ExamRuntimeDialogsCoordinator(
    pendingSection: DiagnosticSection?,
    uiLanguage: UiLanguage,
    runtimeDialogsState: ExamRuntimeDialogsState,
    runtimeDialogsActions: ExamRuntimeDialogsActions,
    screenPinningMessage: String?,
    securityIssueDialogTitle: String?,
    securityIssueDialogMessage: String?,
    bugReportFeedbackTitle: String?,
    bugReportFeedbackMessage: String?,
    onDismissPendingSection: () -> Unit,
    onConfirmPendingSection: (DiagnosticSection) -> Unit,
    onDismissScreenPinningMessage: () -> Unit,
    onDismissSecurityIssueDialog: () -> Unit,
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

    screenPinningMessage?.let { message ->
        InfoDialog(
            title = "Screen Pinning Diperlukan",
            message = message,
            onDismiss = onDismissScreenPinningMessage
        )
    }

    securityIssueDialogMessage?.let { message ->
        InfoDialog(
            title = securityIssueDialogTitle ?: "Keamanan Perangkat",
            message = message,
            onDismiss = onDismissSecurityIssueDialog
        )
    }

    bugReportFeedbackMessage?.let { message ->
        InfoDialog(
            title = bugReportFeedbackTitle ?: "Info",
            message = message,
            onDismiss = onDismissBugReportFeedback
        )
    }
}
