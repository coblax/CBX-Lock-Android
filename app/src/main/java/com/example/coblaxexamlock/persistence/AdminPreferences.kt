package com.example.coblaxexamlock.persistence

import android.content.Context
import androidx.core.content.edit
import com.example.coblaxexamlock.CriticalGateResolution
import com.example.coblaxexamlock.GateKey
import com.example.coblaxexamlock.GateKeys
import com.example.coblaxexamlock.ScreenPinningBypassResolver
import com.example.coblaxexamlock.ScreenPinningBypassState
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.config.AdminKeyCustomQrSaveToDirectLinkEnabled
import com.example.coblaxexamlock.config.AdminKeyDirectLinkGeofenceCenterLat
import com.example.coblaxexamlock.config.AdminKeyDirectLinkGeofenceCenterLng
import com.example.coblaxexamlock.config.AdminKeyDirectLinkGeofenceEnabled
import com.example.coblaxexamlock.config.AdminKeyDirectLinkGeofenceRadiusMeters
import com.example.coblaxexamlock.config.AdminKeyDirectLinkLocationPolicySaved
import com.example.coblaxexamlock.config.AdminKeyDirectLinkLocationPolicySerialized
import com.example.coblaxexamlock.config.AdminKeyExamUserAgent
import com.example.coblaxexamlock.config.AdminKeyFastExamLabel
import com.example.coblaxexamlock.config.AdminKeyFastExamUrl
import com.example.coblaxexamlock.config.AdminKeyOfficialApkUrl
import com.example.coblaxexamlock.config.AdminKeyShowChecklistDetails
import com.example.coblaxexamlock.config.AdminPreferencesName
import com.example.coblaxexamlock.config.DefaultExamUserAgent
import com.example.coblaxexamlock.config.FastExamName
import com.example.coblaxexamlock.config.UiLanguagePreferenceKey
import com.example.coblaxexamlock.config.UiPreferencesName
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.model.effectiveExamUserAgent
import com.example.coblaxexamlock.model.normalizeExamUserAgent

internal fun Context.readSavedUiLanguage(): UiLanguage {
    val savedCode = getSharedPreferences(UiPreferencesName, Context.MODE_PRIVATE)
        .getString(UiLanguagePreferenceKey, UiLanguage.English.code)
    return UiLanguage.entries.firstOrNull { it.code == savedCode } ?: UiLanguage.English
}

internal fun Context.saveUiLanguage(language: UiLanguage) {
    getSharedPreferences(UiPreferencesName, Context.MODE_PRIVATE).edit {
        putString(UiLanguagePreferenceKey, language.code)
    }
}

internal data class HomeAdminSettings(
    val fastExamUrl: String = SecureStrings.fastExamUrl,
    val fastExamLabel: String = FastExamName
)

internal fun Context.readHomeAdminSettings(): HomeAdminSettings {
    val preferences = getSharedPreferences(AdminPreferencesName, Context.MODE_PRIVATE)
    return HomeAdminSettings(
        fastExamUrl = preferences.getString(AdminKeyFastExamUrl, SecureStrings.fastExamUrl)
            ?: SecureStrings.fastExamUrl,
        fastExamLabel = preferences.getString(AdminKeyFastExamLabel, FastExamName)
            ?: FastExamName
    )
}

internal fun Context.readAdminSettings(): AdminSettings {
    val preferences = getSharedPreferences(AdminPreferencesName, Context.MODE_PRIVATE)
    val bypassSnapshot = BypassStorageRepository.read(this)
    val screenPinningBypassResolution = bypassSnapshot.resolveCritical(GateKeys.ScreenPinning)
    val screenPinningBypassState = ScreenPinningBypassResolver.stateOf(
        enabled = screenPinningBypassResolution.enabled,
        tampered = screenPinningBypassResolution.tampered
    )
    val accessibilityBypassResolution = bypassSnapshot.resolveCritical(GateKeys.Accessibility)
    val adbBypassResolution = bypassSnapshot.resolveCritical(GateKeys.Adb)
    val rootBypassResolution = bypassSnapshot.resolveCritical(GateKeys.Root)
    val clipboardBypassResolution = bypassSnapshot.resolveCritical(GateKeys.Clipboard)
    val overlayBypassResolution = bypassSnapshot.resolveCritical(GateKeys.Overlay)
    val geofenceBypassResolution = bypassSnapshot.resolveCritical(GateKeys.Geofence)
    val fakeLocationBypassResolution = bypassSnapshot.resolveCritical(GateKeys.FakeLocation)
    val deviceTimeBypassResolution = bypassSnapshot.resolveCritical(GateKeys.DeviceTime)
    val appSwitchBypassResolution = bypassSnapshot.resolveCritical(GateKeys.AppSwitch)
    val vpnBypassResolution = bypassSnapshot.resolveCritical(GateKeys.Vpn)
    val screenRecorderBypassResolution = bypassSnapshot.resolveCritical(GateKeys.ScreenRecorder)
    val displayMirrorBypassResolution = bypassSnapshot.resolveCritical(GateKeys.DisplayMirror)
    val multiWindowBypassResolution = bypassSnapshot.resolveCritical(GateKeys.MultiWindow)
    return AdminSettings(
        fastExamUrl = preferences.getString(
            AdminKeyFastExamUrl,
            SecureStrings.fastExamUrl
        ) ?: SecureStrings.fastExamUrl,
        fastExamLabel = preferences.getString(AdminKeyFastExamLabel, FastExamName) ?: FastExamName,
        officialApkUrl = preferences.getString(AdminKeyOfficialApkUrl, "") ?: "",
        examUserAgent = normalizeExamUserAgent(
            preferences.getString(AdminKeyExamUserAgent, DefaultExamUserAgent)
        ),
        directLinkLocationPolicySaved = preferences.getBoolean(
            AdminKeyDirectLinkLocationPolicySaved,
            false
        ),
        directLinkLocationPolicySerialized = preferences.getString(
            AdminKeyDirectLinkLocationPolicySerialized,
            ""
        ) ?: "",
        directLinkGeofenceEnabled = preferences.getBoolean(AdminKeyDirectLinkGeofenceEnabled, false),
        directLinkGeofenceCenterLat = preferences.getString(
            AdminKeyDirectLinkGeofenceCenterLat,
            ""
        ) ?: "",
        directLinkGeofenceCenterLng = preferences.getString(
            AdminKeyDirectLinkGeofenceCenterLng,
            ""
        ) ?: "",
        directLinkGeofenceRadiusMeters = preferences.getString(
            AdminKeyDirectLinkGeofenceRadiusMeters,
            ""
        ) ?: "",
        customQrSaveToDirectLinkEnabled = preferences.getBoolean(
            AdminKeyCustomQrSaveToDirectLinkEnabled,
            true
        ),
        bypassScreenPinning = screenPinningBypassState == ScreenPinningBypassState.Active,
        screenPinningBypassTampered = screenPinningBypassState == ScreenPinningBypassState.Tampered,
        bypassBluetooth = bypassSnapshot.isEnabled(GateKeys.Bluetooth.id),
        bypassAccessibility = accessibilityBypassResolution.enabled,
        accessibilityBypassTampered = accessibilityBypassResolution.tampered,
        bypassAdb = adbBypassResolution.enabled,
        adbBypassTampered = adbBypassResolution.tampered,
        bypassRoot = rootBypassResolution.enabled,
        rootBypassTampered = rootBypassResolution.tampered,
        bypassVirtualEnvironment = bypassSnapshot.isEnabled(GateKeys.VirtualEnv.id),
        bypassVpn = vpnBypassResolution.enabled,
        vpnBypassTampered = vpnBypassResolution.tampered,
        bypassKeyboardPolicy = bypassSnapshot.isEnabled(GateKeys.KeyboardPolicy.id),
        bypassClipboard = clipboardBypassResolution.enabled,
        clipboardBypassTampered = clipboardBypassResolution.tampered,
        bypassOverlay = overlayBypassResolution.enabled,
        overlayBypassTampered = overlayBypassResolution.tampered,
        bypassGeofence = geofenceBypassResolution.enabled,
        geofenceBypassTampered = geofenceBypassResolution.tampered,
        bypassFakeLocation = fakeLocationBypassResolution.enabled,
        fakeLocationBypassTampered = fakeLocationBypassResolution.tampered,
        bypassDeviceTime = deviceTimeBypassResolution.enabled,
        deviceTimeBypassTampered = deviceTimeBypassResolution.tampered,
        bypassLocation = geofenceBypassResolution.enabled || fakeLocationBypassResolution.enabled,
        locationBypassTampered = geofenceBypassResolution.tampered || fakeLocationBypassResolution.tampered,
        bypassAppSwitch = appSwitchBypassResolution.enabled,
        appSwitchBypassTampered = appSwitchBypassResolution.tampered,
        bypassScreenRecorder = screenRecorderBypassResolution.enabled,
        screenRecorderBypassTampered = screenRecorderBypassResolution.tampered,
        bypassDisplayMirror = displayMirrorBypassResolution.enabled,
        displayMirrorBypassTampered = displayMirrorBypassResolution.tampered,
        bypassMultiWindow = multiWindowBypassResolution.enabled,
        multiWindowBypassTampered = multiWindowBypassResolution.tampered,
        showChecklistDetails = preferences.getBoolean(AdminKeyShowChecklistDetails, false),
        bypassMigrationResetNotice = bypassSnapshot.migrationResetNotice
    )
}

private fun BypassStorageReadResult.resolveCritical(key: GateKey): CriticalGateResolution {
    return CriticalGateResolution(
        enabled = if (tampered) false else isEnabled(key.id),
        tampered = tampered
    )
}

internal fun Context.saveAdminSettings(settings: AdminSettings) {
    val effectiveExamUserAgent = settings.effectiveExamUserAgent()
    getSharedPreferences(AdminPreferencesName, Context.MODE_PRIVATE).edit {
        putString(AdminKeyFastExamUrl, settings.fastExamUrl)
        putString(AdminKeyFastExamLabel, settings.fastExamLabel)
        putString(AdminKeyOfficialApkUrl, settings.officialApkUrl)
        putString(AdminKeyExamUserAgent, effectiveExamUserAgent)
        putBoolean(AdminKeyDirectLinkLocationPolicySaved, settings.directLinkLocationPolicySaved)
        putString(
            AdminKeyDirectLinkLocationPolicySerialized,
            settings.directLinkLocationPolicySerialized
        )
        putBoolean(AdminKeyDirectLinkGeofenceEnabled, settings.directLinkGeofenceEnabled)
        putString(AdminKeyDirectLinkGeofenceCenterLat, settings.directLinkGeofenceCenterLat)
        putString(AdminKeyDirectLinkGeofenceCenterLng, settings.directLinkGeofenceCenterLng)
        putString(AdminKeyDirectLinkGeofenceRadiusMeters, settings.directLinkGeofenceRadiusMeters)
        putBoolean(
            AdminKeyCustomQrSaveToDirectLinkEnabled,
            settings.customQrSaveToDirectLinkEnabled
        )
        putBoolean(AdminKeyShowChecklistDetails, settings.showChecklistDetails)
    }
    AdminBypassController.persistBypassSettings(this, settings)
}
