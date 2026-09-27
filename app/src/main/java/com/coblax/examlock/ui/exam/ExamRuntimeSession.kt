package com.coblax.examlock.ui.exam

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.withExamQrBypasses

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
    // Everything below reads the bypasses from here, so the ones the exam QR carries apply
    // to every check of this session without touching the saved Secret Admin switches.
    val sessionAdminSettings = remember(adminSettings, payload.securityBypasses) {
        adminSettings.withExamQrBypasses(payload.securityBypasses)
    }
    ExamRuntimeSessionScreenImpl(
        inputs = ExamRuntimeSessionInputs(
            payload = payload,
            adminSettings = sessionAdminSettings,
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