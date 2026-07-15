package com.coblax.examlock.ui.exam

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.coblax.examlock.ClipboardChangeDecision
import com.coblax.examlock.diagnosticLabel
import com.coblax.examlock.readClipboardSnapshotLite

internal class ExamRuntimeClipboardUiState(
    val clipboardSignature: MutableState<String>,
    val clipboardDecisionFingerprint: MutableState<String>,
    val clipboardDecisionSemanticSignature: MutableState<String>,
    val clipboardViolationCount: MutableIntState,
    val lastClipboardChangeEvent: MutableState<String>,
    val lastClipboardObservedAt: MutableState<String?>,
    val lastClipboardConfirmedAt: MutableState<String?>,
    val lastClipboardObservedSignature: MutableState<String?>,
    val lastClipboardBaselineSemanticSignature: MutableState<String?>,
    val lastClipboardDetectedSemanticSignature: MutableState<String?>,
    val lastClipboardDecision: MutableState<String>,
    val clipboardPreBackgroundFingerprint: MutableState<String?>,
    val clipboardPreBackgroundSignature: MutableState<String?>,
    val clipboardPreBackgroundSemanticSignature: MutableState<String?>,
    val clipboardConfirmRunnable: MutableState<Runnable?>,
    val clipboardResumeCheckRunnable: MutableState<Runnable?>,
    val clipboardResumeCheckPending: MutableState<Boolean>,
    val showClipboardViolationDialog: MutableState<Boolean>
)

@Composable
internal fun rememberExamRuntimeClipboardUiState(context: Context): ExamRuntimeClipboardUiState {
    val initialClipboardSnapshot = remember(context) { readClipboardSnapshotLite(context) }
    val clipboardSignature = rememberSaveable {
        mutableStateOf(initialClipboardSnapshot.rawSignature)
    }
    val clipboardDecisionFingerprint = rememberSaveable {
        mutableStateOf(initialClipboardSnapshot.decisionFingerprint)
    }
    val clipboardDecisionSemanticSignature = rememberSaveable {
        mutableStateOf(initialClipboardSnapshot.semanticSignature)
    }
    val clipboardViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val lastClipboardChangeEvent = rememberSaveable { mutableStateOf("Belum ada") }
    val lastClipboardObservedAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastClipboardConfirmedAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastClipboardObservedSignature = rememberSaveable { mutableStateOf<String?>(null) }
    val lastClipboardBaselineSemanticSignature = rememberSaveable { mutableStateOf<String?>(null) }
    val lastClipboardDetectedSemanticSignature = rememberSaveable { mutableStateOf<String?>(null) }
    val lastClipboardDecision = rememberSaveable {
        mutableStateOf(ClipboardChangeDecision.Idle.diagnosticLabel())
    }
    val clipboardPreBackgroundFingerprint = rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardPreBackgroundSignature = rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardPreBackgroundSemanticSignature = rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardConfirmRunnable = remember { mutableStateOf<Runnable?>(null) }
    val clipboardResumeCheckRunnable = remember { mutableStateOf<Runnable?>(null) }
    val clipboardResumeCheckPending = rememberSaveable { mutableStateOf(false) }
    val showClipboardViolationDialog = rememberSaveable { mutableStateOf(false) }
    return ExamRuntimeClipboardUiState(
        clipboardSignature = clipboardSignature,
        clipboardDecisionFingerprint = clipboardDecisionFingerprint,
        clipboardDecisionSemanticSignature = clipboardDecisionSemanticSignature,
        clipboardViolationCount = clipboardViolationCount,
        lastClipboardChangeEvent = lastClipboardChangeEvent,
        lastClipboardObservedAt = lastClipboardObservedAt,
        lastClipboardConfirmedAt = lastClipboardConfirmedAt,
        lastClipboardObservedSignature = lastClipboardObservedSignature,
        lastClipboardBaselineSemanticSignature = lastClipboardBaselineSemanticSignature,
        lastClipboardDetectedSemanticSignature = lastClipboardDetectedSemanticSignature,
        lastClipboardDecision = lastClipboardDecision,
        clipboardPreBackgroundFingerprint = clipboardPreBackgroundFingerprint,
        clipboardPreBackgroundSignature = clipboardPreBackgroundSignature,
        clipboardPreBackgroundSemanticSignature = clipboardPreBackgroundSemanticSignature,
        clipboardConfirmRunnable = clipboardConfirmRunnable,
        clipboardResumeCheckRunnable = clipboardResumeCheckRunnable,
        clipboardResumeCheckPending = clipboardResumeCheckPending,
        showClipboardViolationDialog = showClipboardViolationDialog
    )
}
