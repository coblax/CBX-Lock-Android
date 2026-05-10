package com.example.coblaxexamlock.ui.exam

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.model.AdminSettings

@Composable
internal fun ExamRuntimeSessionScreen(
    payload: ExamQrPayload,
    adminSettings: AdminSettings,
    pendingDirectLinkSaveLog: String?,
    pendingRecoveryEventDetails: String?,
    onDirectLinkSaveLogConsumed: () -> Unit,
    onRecoveryEventConsumed: () -> Unit,
    examSessionRecoveryNonce: Long,
    deviceTimeBaselineWallClockMillis: Long,
    deviceTimeBaselineElapsedRealtimeMillis: Long,
    onExamSessionStartedStateChange: (Boolean) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExamRuntimeSessionScreenImpl(
        inputs = ExamRuntimeSessionInputs(
            payload = payload,
            adminSettings = adminSettings,
            pendingDirectLinkSaveLog = pendingDirectLinkSaveLog,
            pendingRecoveryEventDetails = pendingRecoveryEventDetails,
            examSessionRecoveryNonce = examSessionRecoveryNonce,
            deviceTimeBaselineWallClockMillis = deviceTimeBaselineWallClockMillis,
            deviceTimeBaselineElapsedRealtimeMillis = deviceTimeBaselineElapsedRealtimeMillis
        ),
        callbacks = ExamRuntimeSessionCallbacks(
            onDirectLinkSaveLogConsumed = onDirectLinkSaveLogConsumed,
            onRecoveryEventConsumed = onRecoveryEventConsumed,
            onExamSessionStartedStateChange = onExamSessionStartedStateChange,
            onExit = onExit
        ),
        modifier = modifier
    )
}