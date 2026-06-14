package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.UiLanguage

internal data class PreparationChecklistText(
    val accessibilityStatusLabel: String,
    val overlayStatusLabel: String,
    val geofenceStatusLabel: String,
    val geofenceMeta: String?,
    val fakeLocationStatusLabel: String,
    val deviceTimeStatusLabel: String,
    val networkStatusLabel: String,
    val networkValue: String,
    val networkMeta: String?,
    val networkDetail: String?,
    val webViewProviderStatusLabel: String,
    val webViewProviderValue: String,
    val webViewProviderDetail: String?,
    val deviceTimeDetail: String?,
    val bluetoothStatusLabel: String,
    val developerStatusLabel: String,
    val keyboardStatusLabel: String,
    val rootStatusLabel: String,
    val signatureStatusLabel: String,
    val signatureValue: String,
    val virtualEnvironmentStatusLabel: String,
    val screenPinningStatusLabel: String,
    val accessibilityGuardStatusLabel: String,
    val appSwitchStatusLabel: String,
    val keyboardDetail: String?,
    val bluetoothDetail: String?,
    val accessibilityDetail: String?,
    val overlayDetail: String?,
    val developerDetail: String?,
    val rootDetail: String?,
    val signatureDetail: String?,
    val virtualEnvironmentDetail: String?,
    val clipboardDetail: String?,
    val geofenceDetail: String?,
    val fakeLocationDetail: String?,
    val screenPinningDetail: String?,
    val accessibilityGuardDetail: String?,
    val screenRecorderDetail: String?,
    val displayMirrorDetail: String?,
    val multiWindowDetail: String?,
    val appSwitchDetail: String?
)

internal fun loadingPreparationChecklistText(uiLanguage: UiLanguage): PreparationChecklistText {
    val loading = localized(uiLanguage, "Loading details...", "Memuat detail...")
    val pending = localized(uiLanguage, "Checking", "Mengecek")
    return PreparationChecklistText(
        accessibilityStatusLabel = pending,
        overlayStatusLabel = pending,
        geofenceStatusLabel = pending,
        geofenceMeta = null,
        fakeLocationStatusLabel = pending,
        deviceTimeStatusLabel = pending,
        networkStatusLabel = pending,
        networkValue = loading,
        networkMeta = null,
        networkDetail = loading,
        webViewProviderStatusLabel = pending,
        webViewProviderValue = loading,
        webViewProviderDetail = loading,
        deviceTimeDetail = loading,
        bluetoothStatusLabel = pending,
        developerStatusLabel = pending,
        keyboardStatusLabel = pending,
        rootStatusLabel = pending,
        signatureStatusLabel = pending,
        signatureValue = loading,
        virtualEnvironmentStatusLabel = pending,
        screenPinningStatusLabel = pending,
        accessibilityGuardStatusLabel = pending,
        appSwitchStatusLabel = pending,
        keyboardDetail = loading,
        bluetoothDetail = loading,
        accessibilityDetail = loading,
        overlayDetail = loading,
        developerDetail = loading,
        rootDetail = loading,
        signatureDetail = loading,
        virtualEnvironmentDetail = loading,
        clipboardDetail = loading,
        geofenceDetail = loading,
        fakeLocationDetail = loading,
        screenPinningDetail = loading,
        accessibilityGuardDetail = loading,
        screenRecorderDetail = loading,
        displayMirrorDetail = loading,
        multiWindowDetail = loading,
        appSwitchDetail = loading
    )
}

internal fun buildPreparationChecklistText(
    state: PreparationScreenState,
    uiLanguage: UiLanguage,
    accessibilityInspection: AccessibilityInspectionResult,
    accessibilityGuardEnabled: Boolean,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardRequired: Boolean,
    needsBluetoothPermission: Boolean
): PreparationChecklistText {
    val statusText = buildPreparationChecklistStatusText(
        state = state,
        uiLanguage = uiLanguage,
        accessibilityInspection = accessibilityInspection,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        accessibilityGuardRequired = accessibilityGuardRequired,
        needsBluetoothPermission = needsBluetoothPermission
    )
    val networkText = buildPreparationChecklistNetworkText(
        state = state,
        uiLanguage = uiLanguage
    )
    val detailText = buildPreparationChecklistDetailText(
        state = state,
        uiLanguage = uiLanguage,
        accessibilityInspection = accessibilityInspection,
        accessibilityGuardEnabled = accessibilityGuardEnabled
    )
    val locationDetailText = buildPreparationChecklistLocationDetailText(
        state = state,
        uiLanguage = uiLanguage
    )
    val deviceTimeDetail = appendPreparationAuditDetail(
        actionDetail = statusText.deviceTimeDetail,
        auditDetail = if (state.showChecklistDetails) {
            buildPreparationDeviceTimeAuditDetail(
                status = state.deviceTimeSecurityStatus,
                uiLanguage = uiLanguage
            )
        } else {
            null
        }
    )
    val screenRecorderActionDetail = if (state.screenRecorderPackages.isNotEmpty()) {
        state.screenRecorderPackages.joinToString("\n")
    } else {
        null
    }
    val screenRecorderDetail = appendPreparationAuditDetail(
        actionDetail = screenRecorderActionDetail,
        auditDetail = if (state.showChecklistDetails) {
            buildPreparationScreenRecorderAuditDetail(
                screenRecorderPackages = state.screenRecorderPackages,
                bypassScreenRecorder = state.bypassScreenRecorder,
                uiLanguage = uiLanguage
            )
        } else {
            null
        }
    )
    val displayMirrorDetail = appendPreparationAuditDetail(
        actionDetail = null,
        auditDetail = if (state.showChecklistDetails) {
            buildPreparationDisplayMirrorAuditDetail(
                externalDisplayDetected = state.externalDisplayDetected,
                externalDisplayCount = state.externalDisplayCount,
                externalDisplayInfoList = state.externalDisplayInfoList,
                bypassDisplayMirror = state.bypassDisplayMirror,
                uiLanguage = uiLanguage
            )
        } else {
            null
        }
    )
    val multiWindowDetail = appendPreparationAuditDetail(
        actionDetail = null,
        auditDetail = if (state.showChecklistDetails) {
            buildPreparationMultiWindowAuditDetail(
                modeInfo = state.multiWindowModeInfo,
                runtimeDetected = state.multiWindowDetected,
                bypassMultiWindow = state.bypassMultiWindow,
                uiLanguage = uiLanguage
            )
        } else {
            null
        }
    )

    return PreparationChecklistText(
        accessibilityStatusLabel = statusText.accessibilityStatusLabel,
        overlayStatusLabel = statusText.overlayStatusLabel,
        geofenceStatusLabel = statusText.geofenceStatusLabel,
        geofenceMeta = statusText.geofenceMeta,
        fakeLocationStatusLabel = statusText.fakeLocationStatusLabel,
        deviceTimeStatusLabel = statusText.deviceTimeStatusLabel,
        networkStatusLabel = networkText.networkStatusLabel,
        networkValue = networkText.networkValue,
        networkMeta = networkText.networkMeta,
        networkDetail = networkText.networkDetail,
        webViewProviderStatusLabel = networkText.webViewProviderStatusLabel,
        webViewProviderValue = networkText.webViewProviderValue,
        webViewProviderDetail = networkText.webViewProviderDetail,
        deviceTimeDetail = deviceTimeDetail,
        bluetoothStatusLabel = statusText.bluetoothStatusLabel,
        developerStatusLabel = statusText.developerStatusLabel,
        keyboardStatusLabel = statusText.keyboardStatusLabel,
        rootStatusLabel = statusText.rootStatusLabel,
        signatureStatusLabel = statusText.signatureStatusLabel,
        signatureValue = statusText.signatureValue,
        virtualEnvironmentStatusLabel = statusText.virtualEnvironmentStatusLabel,
        screenPinningStatusLabel = statusText.screenPinningStatusLabel,
        accessibilityGuardStatusLabel = statusText.accessibilityGuardStatusLabel,
        appSwitchStatusLabel = statusText.appSwitchStatusLabel,
        keyboardDetail = detailText.keyboardDetail,
        bluetoothDetail = detailText.bluetoothDetail,
        accessibilityDetail = detailText.accessibilityDetail,
        overlayDetail = detailText.overlayDetail,
        developerDetail = detailText.developerDetail,
        rootDetail = detailText.rootDetail,
        signatureDetail = detailText.signatureDetail,
        virtualEnvironmentDetail = detailText.virtualEnvironmentDetail,
        clipboardDetail = detailText.clipboardDetail,
        geofenceDetail = locationDetailText.geofenceDetail,
        fakeLocationDetail = locationDetailText.fakeLocationDetail,
        screenPinningDetail = detailText.screenPinningDetail,
        accessibilityGuardDetail = detailText.accessibilityGuardDetail,
        screenRecorderDetail = screenRecorderDetail,
        displayMirrorDetail = displayMirrorDetail,
        multiWindowDetail = multiWindowDetail,
        appSwitchDetail = detailText.appSwitchDetail
    )
}

internal fun buildPreparationWizardStepText(
    step: WizardStep,
    state: PreparationScreenState,
    uiLanguage: UiLanguage,
    accessibilityInspection: AccessibilityInspectionResult,
    accessibilityGuardEnabled: Boolean,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardRequired: Boolean,
    needsBluetoothPermission: Boolean
): PreparationChecklistText {
    val statusText = buildPreparationChecklistStatusText(
        state = state,
        uiLanguage = uiLanguage,
        accessibilityInspection = accessibilityInspection,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        accessibilityGuardRequired = accessibilityGuardRequired,
        needsBluetoothPermission = needsBluetoothPermission
    )
    val networkText = if (step == WizardStep.Connectivity || step == WizardStep.DeviceHealth) {
        buildPreparationChecklistNetworkText(
            state = state,
            uiLanguage = uiLanguage
        )
    } else {
        inactivePreparationNetworkText()
    }
    val detailText = buildPreparationChecklistDetailText(
        state = state,
        uiLanguage = uiLanguage,
        accessibilityInspection = accessibilityInspection,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        activeWizardStep = step
    )
    val locationDetailText = if (step == WizardStep.Location) {
        buildPreparationChecklistLocationDetailText(
            state = state,
            uiLanguage = uiLanguage
        )
    } else {
        PreparationChecklistLocationDetailText(
            geofenceDetail = null,
            fakeLocationDetail = null
        )
    }
    val deviceTimeDetail = if (step == WizardStep.DeviceHealth) {
        appendPreparationAuditDetail(
            actionDetail = statusText.deviceTimeDetail,
            auditDetail = if (state.showChecklistDetails) {
                buildPreparationDeviceTimeAuditDetail(
                    status = state.deviceTimeSecurityStatus,
                    uiLanguage = uiLanguage
                )
            } else {
                null
            }
        )
    } else {
        null
    }
    val screenRecorderActionDetail = if (
        step == WizardStep.RuntimeSecurity &&
        state.screenRecorderPackages.isNotEmpty()
    ) {
        state.screenRecorderPackages.joinToString("\n")
    } else {
        null
    }
    val screenRecorderDetail = if (step == WizardStep.RuntimeSecurity) {
        appendPreparationAuditDetail(
            actionDetail = screenRecorderActionDetail,
            auditDetail = if (state.showChecklistDetails) {
                buildPreparationScreenRecorderAuditDetail(
                    screenRecorderPackages = state.screenRecorderPackages,
                    bypassScreenRecorder = state.bypassScreenRecorder,
                    uiLanguage = uiLanguage
                )
            } else {
                null
            }
        )
    } else {
        null
    }
    val displayMirrorDetail = if (step == WizardStep.RuntimeSecurity && state.showChecklistDetails) {
        buildPreparationDisplayMirrorAuditDetail(
            externalDisplayDetected = state.externalDisplayDetected,
            externalDisplayCount = state.externalDisplayCount,
            externalDisplayInfoList = state.externalDisplayInfoList,
            bypassDisplayMirror = state.bypassDisplayMirror,
            uiLanguage = uiLanguage
        )
    } else {
        null
    }
    val multiWindowDetail = if (step == WizardStep.RuntimeSecurity && state.showChecklistDetails) {
        buildPreparationMultiWindowAuditDetail(
            modeInfo = state.multiWindowModeInfo,
            runtimeDetected = state.multiWindowDetected,
            bypassMultiWindow = state.bypassMultiWindow,
            uiLanguage = uiLanguage
        )
    } else {
        null
    }

    return PreparationChecklistText(
        accessibilityStatusLabel = statusText.accessibilityStatusLabel,
        overlayStatusLabel = statusText.overlayStatusLabel,
        geofenceStatusLabel = statusText.geofenceStatusLabel,
        geofenceMeta = statusText.geofenceMeta,
        fakeLocationStatusLabel = statusText.fakeLocationStatusLabel,
        deviceTimeStatusLabel = statusText.deviceTimeStatusLabel,
        networkStatusLabel = networkText.networkStatusLabel,
        networkValue = networkText.networkValue,
        networkMeta = networkText.networkMeta,
        networkDetail = networkText.networkDetail,
        webViewProviderStatusLabel = networkText.webViewProviderStatusLabel,
        webViewProviderValue = networkText.webViewProviderValue,
        webViewProviderDetail = networkText.webViewProviderDetail,
        deviceTimeDetail = deviceTimeDetail,
        bluetoothStatusLabel = statusText.bluetoothStatusLabel,
        developerStatusLabel = statusText.developerStatusLabel,
        keyboardStatusLabel = statusText.keyboardStatusLabel,
        rootStatusLabel = statusText.rootStatusLabel,
        signatureStatusLabel = statusText.signatureStatusLabel,
        signatureValue = statusText.signatureValue,
        virtualEnvironmentStatusLabel = statusText.virtualEnvironmentStatusLabel,
        screenPinningStatusLabel = statusText.screenPinningStatusLabel,
        accessibilityGuardStatusLabel = statusText.accessibilityGuardStatusLabel,
        appSwitchStatusLabel = statusText.appSwitchStatusLabel,
        keyboardDetail = detailText.keyboardDetail,
        bluetoothDetail = detailText.bluetoothDetail,
        accessibilityDetail = detailText.accessibilityDetail,
        overlayDetail = detailText.overlayDetail,
        developerDetail = detailText.developerDetail,
        rootDetail = detailText.rootDetail,
        signatureDetail = detailText.signatureDetail,
        virtualEnvironmentDetail = detailText.virtualEnvironmentDetail,
        clipboardDetail = detailText.clipboardDetail,
        geofenceDetail = locationDetailText.geofenceDetail,
        fakeLocationDetail = locationDetailText.fakeLocationDetail,
        screenPinningDetail = detailText.screenPinningDetail,
        accessibilityGuardDetail = detailText.accessibilityGuardDetail,
        screenRecorderDetail = screenRecorderDetail,
        displayMirrorDetail = displayMirrorDetail,
        multiWindowDetail = multiWindowDetail,
        appSwitchDetail = detailText.appSwitchDetail
    )
}

private fun inactivePreparationNetworkText(): PreparationChecklistNetworkText =
    PreparationChecklistNetworkText(
        networkStatusLabel = "",
        networkValue = "",
        networkMeta = null,
        networkDetail = null,
        webViewProviderStatusLabel = "",
        webViewProviderValue = "",
        webViewProviderDetail = null
    )
