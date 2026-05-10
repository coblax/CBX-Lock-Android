package com.example.coblaxexamlock.ui.preparation

import androidx.compose.runtime.Composable
import com.example.coblaxexamlock.AccessibilityInspectionResult
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
    val appSwitchDetail: String?
)

@Composable
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
        accessibilityInspection = accessibilityInspection,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        accessibilityGuardRequired = accessibilityGuardRequired,
        needsBluetoothPermission = needsBluetoothPermission
    )
    val networkText = buildPreparationChecklistNetworkText(state)
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
        deviceTimeDetail = statusText.deviceTimeDetail,
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
        appSwitchDetail = detailText.appSwitchDetail
    )
}
