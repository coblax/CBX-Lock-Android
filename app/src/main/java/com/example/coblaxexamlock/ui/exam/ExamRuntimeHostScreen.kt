package com.example.coblaxexamlock.ui.exam

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.model.AdminSettings

@Composable
internal fun ExamWebViewScreen(
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
    val mainActivity = LocalContext.current as? MainActivity
    DisposableEffect(mainActivity) {
        mainActivity?.setExamPortraitMode(enabled = true)
        onDispose {
            mainActivity?.setExamPortraitMode(enabled = false)
        }
    }

    key(examSessionRecoveryNonce) {
        ExamRuntimeSessionScreen(
            payload = payload,
            adminSettings = adminSettings,
            pendingDirectLinkSaveLog = pendingDirectLinkSaveLog,
            pendingRecoveryEventDetails = pendingRecoveryEventDetails,
            onDirectLinkSaveLogConsumed = onDirectLinkSaveLogConsumed,
            onRecoveryEventConsumed = onRecoveryEventConsumed,
            examSessionRecoveryNonce = examSessionRecoveryNonce,
            deviceTimeBaselineWallClockMillis = deviceTimeBaselineWallClockMillis,
            deviceTimeBaselineElapsedRealtimeMillis = deviceTimeBaselineElapsedRealtimeMillis,
            onExamSessionStartedStateChange = onExamSessionStartedStateChange,
            onExit = onExit,
            modifier = modifier
        )
    }
}
