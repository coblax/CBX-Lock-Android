package com.coblax.examlock.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.coblax.examlock.AdminAuthSession
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.SecureStrings
import com.coblax.examlock.StartupTrace
import com.coblax.examlock.config.FastExamName
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.AppScreen
import com.coblax.examlock.model.withoutDirectLinkLocationPolicy
import com.coblax.examlock.ui.admin.CustomQrAdminScreen
import com.coblax.examlock.ui.admin.SecretAdminScreen
import com.coblax.examlock.ui.exam.ExamWebViewScreen
import com.coblax.examlock.viewmodel.AdminApplyState
import com.coblax.examlock.viewmodel.AdminFlowUiAction
import com.coblax.examlock.viewmodel.AdminFlowUiState

@Composable
internal fun AppNonHomeRouteHost(
    screen: AppScreen,
    uiState: AdminFlowUiState,
    activeExamPayload: ExamQrPayload?,
    adminSettingsSnapshot: () -> AdminSettings,
    applyAdminSettings: (AdminSettings) -> Unit,
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
            val persistedSettings = uiState.persistedAdminSettings
                ?: adminSettingsSnapshot()
            val draftSettings = uiState.draftAdminSettings
                ?: persistedSettings
            val applyStatusMessage = when (val state = uiState.adminApplyState) {
                AdminApplyState.Idle,
                AdminApplyState.Applying -> null

                AdminApplyState.Success -> tr(
                    "Settings saved and verified.",
                    "Pengaturan tersimpan dan telah diverifikasi."
                )

                is AdminApplyState.Failure -> state.message
                AdminApplyState.ReauthenticationRequired -> tr(
                    "Admin authentication expired. Sign in again before applying security overrides.",
                    "Autentikasi Admin kedaluwarsa. Masuk kembali sebelum menerapkan override keamanan."
                )
            }
            SecretAdminScreen(
                settings = persistedSettings,
                examName = activeExamPayload?.examName?.trim().orEmpty().ifBlank {
                    persistedSettings.fastExamLabel
                },
                onSettingsChange = {
                    dispatch(AdminFlowUiAction.UpdateAdminSettingsDraft(it))
                },
                onResetDirectLink = {
                    dispatch(
                        AdminFlowUiAction.UpdateAdminSettingsDraft(
                            draftSettings.copy(
                            fastExamUrl = SecureStrings.fastExamUrl,
                            fastExamLabel = FastExamName
                            ).withoutDirectLinkLocationPolicy()
                        )
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
                externalDraftSettings = draftSettings,
                onDraftSettingsChange = {
                    dispatch(AdminFlowUiAction.UpdateAdminSettingsDraft(it))
                },
                onApplySettings = applyAdminSettings,
                onRevertSettings = {
                    dispatch(AdminFlowUiAction.RevertAdminSettingsDraft)
                },
                isApplyInProgress = uiState.adminApplyState == AdminApplyState.Applying,
                applyStatusMessage = applyStatusMessage,
                applyStatusIsError = uiState.adminApplyState is AdminApplyState.Failure ||
                    uiState.adminApplyState == AdminApplyState.ReauthenticationRequired,
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
