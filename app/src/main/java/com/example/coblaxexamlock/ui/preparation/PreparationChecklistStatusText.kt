package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.UiLanguage

internal data class PreparationChecklistStatusText(
    val accessibilityStatusLabel: String,
    val overlayStatusLabel: String,
    val geofenceStatusLabel: String,
    val geofenceMeta: String?,
    val fakeLocationStatusLabel: String,
    val deviceTimeStatusLabel: String,
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
    val appSwitchStatusLabel: String
)

internal fun buildPreparationChecklistStatusText(
    state: PreparationScreenState,
    uiLanguage: UiLanguage,
    accessibilityInspection: AccessibilityInspectionResult,
    accessibilityGuardEnabled: Boolean,
    accessibilityGuardRequired: Boolean,
    needsBluetoothPermission: Boolean
): PreparationChecklistStatusText = with(state) {
    fun t(english: String, indonesian: String): String = localized(uiLanguage, english, indonesian)
    val accessibilityStatusLabel = when {
        bypassAccessibility -> t("Bypassed", "Bypass")
        accessibilityInspection.allowedOnlyActive -> t("Allowed", "Diizinkan")
        accessibilityServiceEnabled -> t("Action needed", "Perlu aksi")
        else -> t("Safe", "Aman")
    }
    val overlayStatusLabel = when {
        bypassOverlay -> t("Bypassed", "Bypass")
        overlayRiskResult.confirmedInteractionDetected -> t("Danger", "Bahaya")
        overlayRiskResult.heuristicRisk -> t("Warning", "Peringatan")
        else -> t("Safe", "Aman")
    }
    val geofenceStatusLabel = when {
        geofenceBypassState == GeofenceBypassState.Tampered -> t("Tampered", "Tampered")
        bypassGeofence -> t("Bypassed", "Bypass")
        !geofenceRuntimeStatus.evaluation.enabled -> t("Policy Off", "Policy Nonaktif")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Inside -> t("Inside Area", "Di Dalam Area")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Outside -> t("Outside Area", "Di Luar Area")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.StaleFix -> t("Stale Fix", "Fix Kedaluwarsa")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LowAccuracy -> t("Low Accuracy", "Akurasi Rendah")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.MissingAccuracy -> t("Missing Accuracy", "Akurasi Tidak Ada")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.NoFix -> t("No Fix", "Belum Ada Fix")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.ConfigInvalid -> t("Config Error", "Konfigurasi Salah")
        else -> t("Needs Fix", "Perlu Perbaikan")
    }
    val geofenceProviderSummary = geofenceRuntimeStatus.evaluation.locationSnapshot
        ?.provider
        ?.ifBlank { "-" }
        ?: "-"
    val geofenceFixAgeSummary = formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs)
    val geofenceFixResultSummary = geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()
    val geofenceRefreshAtSummary = lastGeofenceRefreshAt?.ifBlank { "-" } ?: "-"
    val geofenceFixMetaLine = t(
        "Fix: $geofenceProviderSummary | $geofenceFixAgeSummary | $geofenceFixResultSummary",
        "Fix: $geofenceProviderSummary | $geofenceFixAgeSummary | $geofenceFixResultSummary"
    )
    val geofenceRefreshMetaLine = if (isRefreshingGeofence) {
        t(
            "Refresh: running...",
            "Refresh: berjalan..."
        )
    } else if (isWarmingLocation) {
        t(
            "Refresh: warming fresh location...",
            "Refresh: menyiapkan lokasi segar..."
        )
    } else {
        t(
            "Refresh: $geofenceRefreshAtSummary",
            "Refresh: $geofenceRefreshAtSummary"
        )
    }
    val geofenceMeta = when {
        !geofenceRuntimeStatus.evaluation.enabled || bypassGeofence -> null
        else -> "$geofenceFixMetaLine\n$geofenceRefreshMetaLine"
    }
    val fakeLocationStatusLabel = when {
        fakeLocationBypassState == FakeLocationBypassState.Tampered -> t("Tampered", "Tampered")
        bypassFakeLocation -> t("Bypassed", "Bypass")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired ->
            t("Needs Location Permission", "Butuh Izin Lokasi")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled ->
            t("Location Services Off", "Layanan Lokasi Off")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable ->
            t("Waiting for Location", "Menunggu Lokasi")
        !fakeLocationRuntimeStatus.securityStatus.monitoringEnabled -> t("Policy Off", "Policy Nonaktif")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Critical -> t("Spoof Critical", "Spoof Kritis")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Strong -> t("Spoof Strong", "Spoof Kuat")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Warning -> t("Package Warning", "Peringatan Paket")
        else -> t("Clean", "Bersih")
    }
    val deviceTimeStatusLabel = when {
        deviceTimeBypassState == DeviceTimeBypassState.Tampered -> t("Tampered", "Tampered")
        bypassDeviceTime -> t("Bypassed", "Bypass")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.Safe -> t("Safe", "Aman")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> t("Auto Date/Time Off", "Tanggal/Waktu Otomatis Nonaktif")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> t("Auto Time Zone Off", "Zona Waktu Otomatis Nonaktif")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected -> t("Clock Change", "Perubahan Jam")
        else -> t("Action needed", "Perlu aksi")
    }
    val deviceTimeDetail = when {
        deviceTimeBypassState == DeviceTimeBypassState.Tampered -> t(
            "Open Admin Secret to review the bypass integrity.",
            "Buka Admin Secret untuk memeriksa integritas bypass."
        )
        bypassDeviceTime -> null
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.Safe -> null
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> t(
            "Enable automatic date & time, then tap Refresh.",
            "Aktifkan tanggal & waktu otomatis, lalu tekan Refresh."
        )
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> t(
            "Enable automatic time zone, then tap Refresh.",
            "Aktifkan zona waktu otomatis, lalu tekan Refresh."
        )
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected -> t(
            "Enable automatic time, then refresh the check before starting the exam.",
            "Aktifkan waktu otomatis, lalu refresh pemeriksaan sebelum mulai ujian."
        )
        else -> null
    }
    val bluetoothStatusLabel = when {
        bypassBluetooth -> t("Bypassed", "Bypass")
        needsBluetoothPermission && !bluetoothPermissionGranted -> t("Permission needed", "Butuh izin")
        bluetoothEnabled -> t("Action needed", "Perlu aksi")
        else -> t("Safe", "Aman")
    }
    val developerStatusLabel = when {
        adbBypassState == AdbBypassState.Tampered -> t("Warning", "Peringatan")
        bypassAdb -> t("Bypassed", "Bypass")
        adbInspection.blocking -> t("Action needed", "Perlu aksi")
        adbInspection.insecureSystemProperty -> t("Warning", "Peringatan")
        else -> t("Safe", "Aman")
    }
    val keyboardStatusLabel = when {
        bypassKeyboardPolicy -> t("Bypassed", "Bypass")
        keyboardAllowed -> t("Ready", "Siap")
        else -> t("Built-in", "Bawaan")
    }
    val rootStatusLabel = when {
        rootBypassState == RootBypassState.Tampered -> t("Warning", "Peringatan")
        bypassRoot -> t("Bypassed", "Bypass")
        rootSecurityStatus.detected -> t("Danger", "Bahaya")
        rootSecurityStatus.selinuxPermissive -> t("Warning", "Peringatan")
        else -> t("Safe", "Aman")
    }
    val signatureStatusLabel = when {
        signatureMismatchDetected -> t("Danger", "Bahaya")
        else -> t("Safe", "Aman")
    }
    val signatureValue = when {
        signatureMismatchDetected && reinstallApkFixNeeded -> t(
            "Signature mismatch. Reinstall official APK.",
            "Signature tidak cocok. Instal ulang APK resmi."
        )
        signatureMismatchDetected -> t(
            "Signature mismatch detected.",
            "Signature tidak cocok terdeteksi."
        )
        else -> t(
            "Signature matches the official release.",
            "Signature cocok dengan rilis resmi."
        )
    }
    val virtualEnvironmentStatusLabel = when {
        bypassVirtualEnvironment -> t("Bypassed", "Bypass")
        virtualEnvironmentDetected -> t("Danger", "Bahaya")
        else -> t("Safe", "Aman")
    }
    val screenPinningStatusLabel = when {
        bypassScreenPinning -> t("Bypassed", "Bypass")
        isScreenPinningActive -> t("Active", "Aktif")
        screenPinningFixNeeded -> t("Start Required", "Perlu Start")
        screenPinningAvailable -> t("Start Required", "Perlu Start")
        else -> t("Unavailable", "Tidak tersedia")
    }
    val accessibilityGuardStatusLabel = when {
        bypassScreenPinning -> t("Not required", "Tidak wajib")
        accessibilityGuardRequired && accessibilityGuardEnabled -> t("Required Active", "Wajib Aktif")
        accessibilityGuardRequired -> t("Action needed", "Perlu aksi")
        accessibilityGuardEnabled -> t("Optional Active", "Opsional Aktif")
        else -> t("Optional", "Opsional")
    }
    val appSwitchStatusLabel = when {
        bypassAppSwitch -> t("Bypassed", "Bypass")
        appSwitchStatus.hasViolations -> t("Warning", "Peringatan")
        appSwitchStatus.fallbackGuardActive -> t("Fallback", "Fallback")
        else -> t("Ready", "Siap")
    }

    PreparationChecklistStatusText(
        accessibilityStatusLabel = accessibilityStatusLabel,
        overlayStatusLabel = overlayStatusLabel,
        geofenceStatusLabel = geofenceStatusLabel,
        geofenceMeta = geofenceMeta,
        fakeLocationStatusLabel = fakeLocationStatusLabel,
        deviceTimeStatusLabel = deviceTimeStatusLabel,
        deviceTimeDetail = deviceTimeDetail,
        bluetoothStatusLabel = bluetoothStatusLabel,
        developerStatusLabel = developerStatusLabel,
        keyboardStatusLabel = keyboardStatusLabel,
        rootStatusLabel = rootStatusLabel,
        signatureStatusLabel = signatureStatusLabel,
        signatureValue = signatureValue,
        virtualEnvironmentStatusLabel = virtualEnvironmentStatusLabel,
        screenPinningStatusLabel = screenPinningStatusLabel,
        accessibilityGuardStatusLabel = accessibilityGuardStatusLabel,
        appSwitchStatusLabel = appSwitchStatusLabel
    )
}
