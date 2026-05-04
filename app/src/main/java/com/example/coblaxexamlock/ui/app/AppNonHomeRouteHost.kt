package com.example.coblaxexamlock.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.coblaxexamlock.AdminAuthSession
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.StartupTrace
import com.example.coblaxexamlock.config.FastExamName
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.AppScreen
import com.example.coblaxexamlock.model.withoutDirectLinkLocationPolicy
import com.example.coblaxexamlock.ui.admin.CustomQrAdminScreen
import com.example.coblaxexamlock.ui.admin.SecretAdminScreen
import com.example.coblaxexamlock.ui.exam.ExamWebViewScreen
import com.example.coblaxexamlock.viewmodel.AdminFlowUiAction
import com.example.coblaxexamlock.viewmodel.AdminFlowUiState

@Composable
internal fun AppNonHomeRouteHost(
    screen: AppScreen,
    uiState: AdminFlowUiState,
    activeExamPayload: ExamQrPayload?,
    adminSettingsSnapshot: () -> AdminSettings,
    updateAdminSettings: (AdminSettings) -> Unit,
    dispatch: (AdminFlowUiAction) -> Unit,
    pendingDirectLinkSaveLog: String?,
    pendingRecoveryEventDetails: String?,
    onDirectLinkSaveLogConsumed: () -> Unit,
    onRecoveryEventConsumed: () -> Unit,
    examSessionRecoveryNonce: Long,
    deviceTimeBaselineWallClockMillis: Long,
    deviceTimeBaselineElapsedRealtimeMillis: Long,
    onExamSessionStartedStateChange: (Boolean) -> Unit,
    onExamExit: () -> Unit,
    onMissingExamPayload: () -> Unit
) {
    LaunchedEffect(screen) {
        StartupTrace.mark("route_non_home_opened", "screen=${screen.name}")
    }

    when (screen) {
        AppScreen.Home -> Unit

        AppScreen.CustomQrAdmin -> {
            val activeSettings = adminSettingsSnapshot()
            CustomQrAdminScreen(
                showSaveToDirectLinkOption = activeSettings.customQrSaveToDirectLinkEnabled,
                onBack = { dispatch(AdminFlowUiAction.CloseCustomQrAdmin) },
                selectedTabName = uiState.selectedCustomQrTab,
                onSelectedTabNameChange = {
                    dispatch(AdminFlowUiAction.SelectCustomQrTab(it))
                },
                draft = uiState.customQrDraft,
                onDraftChange = {
                    dispatch(AdminFlowUiAction.SetCustomQrDraft(it))
                },
                showCircleMapEditor = uiState.showCircleMapEditor,
                onShowCircleMapEditorChange = {
                    dispatch(AdminFlowUiAction.SetShowCircleMapEditor(it))
                },
                showPolygonMapEditor = uiState.showPolygonMapEditor,
                onShowPolygonMapEditorChange = {
                    dispatch(AdminFlowUiAction.SetShowPolygonMapEditor(it))
                },
                generatedQrPayload = uiState.generatedQrPayload,
                onGeneratedQrPayloadChange = {
                    dispatch(AdminFlowUiAction.SetGeneratedQrPayload(it))
                },
                generationStatus = uiState.generationStatus,
                onGenerationStatusChange = {
                    dispatch(AdminFlowUiAction.SetGenerationStatus(it))
                },
                generationIsError = uiState.generationIsError,
                onGenerationIsErrorChange = {
                    dispatch(AdminFlowUiAction.SetGenerationIsError(it))
                }
            )
        }

        AppScreen.SecretAdmin -> {
            val activeSettings = adminSettingsSnapshot()
            SecretAdminScreen(
                settings = activeSettings,
                examName = activeExamPayload?.examName?.trim().orEmpty().ifBlank {
                    activeSettings.fastExamLabel
                },
                onSettingsChange = { updateAdminSettings(it) },
                onResetDirectLink = {
                    updateAdminSettings(
                        activeSettings.copy(
                            fastExamUrl = SecureStrings.fastExamUrl,
                            fastExamLabel = FastExamName
                        ).withoutDirectLinkLocationPolicy()
                    )
                },
                onBack = {
                    AdminAuthSession.clear()
                    dispatch(AdminFlowUiAction.CloseSecretAdmin)
                },
                selectedTabName = uiState.selectedSecretTab,
                onSelectedTabNameChange = {
                    dispatch(AdminFlowUiAction.SelectSecretTab(it))
                },
                deviceTimeBaselineWallClockMillis = deviceTimeBaselineWallClockMillis,
                deviceTimeBaselineElapsedRealtimeMillis = deviceTimeBaselineElapsedRealtimeMillis
            )
        }

        AppScreen.ExamWebView -> {
            val payload = activeExamPayload
            if (payload != null) {
                val activeSettings = adminSettingsSnapshot()
                ExamWebViewScreen(
                    payload = payload,
                    adminSettings = activeSettings,
                    pendingDirectLinkSaveLog = pendingDirectLinkSaveLog,
                    pendingRecoveryEventDetails = pendingRecoveryEventDetails,
                    onDirectLinkSaveLogConsumed = onDirectLinkSaveLogConsumed,
                    onRecoveryEventConsumed = onRecoveryEventConsumed,
                    examSessionRecoveryNonce = examSessionRecoveryNonce,
                    deviceTimeBaselineWallClockMillis = deviceTimeBaselineWallClockMillis,
                    deviceTimeBaselineElapsedRealtimeMillis = deviceTimeBaselineElapsedRealtimeMillis,
                    onExamSessionStartedStateChange = onExamSessionStartedStateChange,
                    onExit = onExamExit
                )
            } else {
                LaunchedEffect(Unit) {
                    onMissingExamPayload()
                }
            }
        }
    }
}
