package com.example.coblaxexamlock.ui.preparation

import androidx.compose.runtime.Composable
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
import com.example.coblaxexamlock.i18n.tr

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

@Composable
internal fun buildPreparationChecklistStatusText(
    state: PreparationScreenState,
    accessibilityInspection: AccessibilityInspectionResult,
    accessibilityGuardEnabled: Boolean,
    accessibilityGuardRequired: Boolean,
    needsBluetoothPermission: Boolean
): PreparationChecklistStatusText = with(state) {
    val accessibilityStatusLabel = when {
        bypassAccessibility -> tr("Bypassed", "Bypass")
        accessibilityInspection.allowedOnlyActive -> tr("Allowed", "Diizinkan")
        accessibilityServiceEnabled -> tr("Action needed", "Perlu aksi")
        else -> tr("Safe", "Aman")
    }
    val overlayStatusLabel = when {
        bypassOverlay -> tr("Bypassed", "Bypass")
        overlayRiskResult.confirmedInteractionDetected -> tr("Danger", "Bahaya")
        overlayRiskResult.heuristicRisk -> tr("Warning", "Peringatan")
        else -> tr("Safe", "Aman")
    }
    val geofenceStatusLabel = when {
        geofenceBypassState == GeofenceBypassState.Tampered -> tr("Tampered", "Tampered")
        bypassGeofence -> tr("Bypassed", "Bypass")
        !geofenceRuntimeStatus.evaluation.enabled -> tr("Policy Off", "Policy Nonaktif")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Inside -> tr("Inside Area", "Di Dalam Area")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Outside -> tr("Outside Area", "Di Luar Area")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.StaleFix -> tr("Stale Fix", "Fix Kedaluwarsa")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LowAccuracy -> tr("Low Accuracy", "Akurasi Rendah")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.MissingAccuracy -> tr("Missing Accuracy", "Akurasi Tidak Ada")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.NoFix -> tr("No Fix", "Belum Ada Fix")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.ConfigInvalid -> tr("Config Error", "Konfigurasi Salah")
        else -> tr("Needs Fix", "Perlu Perbaikan")
    }
    val geofenceProviderSummary = geofenceRuntimeStatus.evaluation.locationSnapshot
        ?.provider
        ?.ifBlank { "-" }
        ?: "-"
    val geofenceFixAgeSummary = formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs)
    val geofenceFixResultSummary = geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()
    val geofenceRefreshAtSummary = lastGeofenceRefreshAt?.ifBlank { "-" } ?: "-"
    val geofenceFixMetaLine = tr(
        "Fix: $geofenceProviderSummary | $geofenceFixAgeSummary | $geofenceFixResultSummary",
        "Fix: $geofenceProviderSummary | $geofenceFixAgeSummary | $geofenceFixResultSummary"
    )
    val geofenceRefreshMetaLine = if (isRefreshingGeofence) {
        tr(
            "Refresh: running...",
            "Refresh: berjalan..."
        )
    } else if (isWarmingLocation) {
        tr(
            "Refresh: warming fresh location...",
            "Refresh: menyiapkan lokasi segar..."
        )
    } else {
        tr(
            "Refresh: $geofenceRefreshAtSummary",
            "Refresh: $geofenceRefreshAtSummary"
        )
    }
    val geofenceMeta = when {
        !geofenceRuntimeStatus.evaluation.enabled || bypassGeofence -> null
        else -> "$geofenceFixMetaLine\n$geofenceRefreshMetaLine"
    }
    val fakeLocationStatusLabel = when {
        fakeLocationBypassState == FakeLocationBypassState.Tampered -> tr("Tampered", "Tampered")
        bypassFakeLocation -> tr("Bypassed", "Bypass")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired ->
            tr("Needs Location Permission", "Butuh Izin Lokasi")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled ->
            tr("Location Services Off", "Layanan Lokasi Off")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable ->
            tr("Waiting for Location", "Menunggu Lokasi")
        !fakeLocationRuntimeStatus.securityStatus.monitoringEnabled -> tr("Policy Off", "Policy Nonaktif")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Critical -> tr("Spoof Critical", "Spoof Kritis")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Strong -> tr("Spoof Strong", "Spoof Kuat")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Warning -> tr("Package Warning", "Peringatan Paket")
        else -> tr("Clean", "Bersih")
    }
    val deviceTimeStatusLabel = when {
        deviceTimeBypassState == DeviceTimeBypassState.Tampered -> tr("Tampered", "Tampered")
        bypassDeviceTime -> tr("Bypassed", "Bypass")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.Safe -> tr("Safe", "Aman")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> tr("Auto Date/Time Off", "Tanggal/Waktu Otomatis Nonaktif")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> tr("Auto Time Zone Off", "Zona Waktu Otomatis Nonaktif")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected -> tr("Clock Change", "Perubahan Jam")
        else -> tr("Action needed", "Perlu aksi")
    }
    val deviceTimeDetail = when {
        deviceTimeBypassState == DeviceTimeBypassState.Tampered -> tr(
            "Open Admin Secret to review the bypass integrity.",
            "Buka Admin Secret untuk memeriksa integritas bypass."
        )
        bypassDeviceTime -> null
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.Safe -> null
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> tr(
            "Enable automatic date & time, then tap Refresh.",
            "Aktifkan tanggal & waktu otomatis, lalu tekan Refresh."
        )
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> tr(
            "Enable automatic time zone, then tap Refresh.",
            "Aktifkan zona waktu otomatis, lalu tekan Refresh."
        )
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected -> tr(
            "Enable automatic time, then refresh the check before starting the exam.",
            "Aktifkan waktu otomatis, lalu refresh pemeriksaan sebelum mulai ujian."
        )
        else -> null
    }
    val bluetoothStatusLabel = when {
        bypassBluetooth -> tr("Bypassed", "Bypass")
        needsBluetoothPermission && !bluetoothPermissionGranted -> tr("Permission needed", "Butuh izin")
        bluetoothEnabled -> tr("Action needed", "Perlu aksi")
        else -> tr("Safe", "Aman")
    }
    val developerStatusLabel = when {
        adbBypassState == AdbBypassState.Tampered -> tr("Warning", "Peringatan")
        bypassAdb -> tr("Bypassed", "Bypass")
        adbInspection.blocking -> tr("Action needed", "Perlu aksi")
        adbInspection.insecureSystemProperty -> tr("Warning", "Peringatan")
        else -> tr("Safe", "Aman")
    }
    val keyboardStatusLabel = when {
        bypassKeyboardPolicy -> tr("Bypassed", "Bypass")
        keyboardAllowed -> tr("Ready", "Siap")
        else -> tr("Built-in", "Bawaan")
    }
    val rootStatusLabel = when {
        rootBypassState == RootBypassState.Tampered -> tr("Warning", "Peringatan")
        bypassRoot -> tr("Bypassed", "Bypass")
        rootSecurityStatus.detected -> tr("Danger", "Bahaya")
        rootSecurityStatus.selinuxPermissive -> tr("Warning", "Peringatan")
        else -> tr("Safe", "Aman")
    }
    val signatureStatusLabel = when {
        signatureMismatchDetected -> tr("Danger", "Bahaya")
        else -> tr("Safe", "Aman")
    }
    val signatureValue = when {
        signatureMismatchDetected && reinstallApkFixNeeded -> tr(
            "Signature mismatch. Reinstall official APK.",
            "Signature tidak cocok. Instal ulang APK resmi."
        )
        signatureMismatchDetected -> tr(
            "Signature mismatch detected.",
            "Signature tidak cocok terdeteksi."
        )
        else -> tr(
            "Signature matches the official release.",
            "Signature cocok dengan rilis resmi."
        )
    }
    val virtualEnvironmentStatusLabel = when {
        bypassVirtualEnvironment -> tr("Bypassed", "Bypass")
        virtualEnvironmentDetected -> tr("Danger", "Bahaya")
        else -> tr("Safe", "Aman")
    }
    val screenPinningStatusLabel = when {
        bypassScreenPinning -> tr("Bypassed", "Bypass")
        isScreenPinningActive -> tr("Active", "Aktif")
        screenPinningFixNeeded -> tr("Start Required", "Perlu Start")
        screenPinningAvailable -> tr("Start Required", "Perlu Start")
        else -> tr("Unavailable", "Tidak tersedia")
    }
    val accessibilityGuardStatusLabel = when {
        bypassScreenPinning -> tr("Not required", "Tidak wajib")
        accessibilityGuardRequired && accessibilityGuardEnabled -> tr("Required Active", "Wajib Aktif")
        accessibilityGuardRequired -> tr("Action needed", "Perlu aksi")
        accessibilityGuardEnabled -> tr("Optional Active", "Opsional Aktif")
        else -> tr("Optional", "Opsional")
    }
    val appSwitchStatusLabel = when {
        bypassAppSwitch -> tr("Bypassed", "Bypass")
        appSwitchStatus.hasViolations -> tr("Warning", "Peringatan")
        appSwitchStatus.fallbackGuardActive -> tr("Fallback", "Fallback")
        else -> tr("Ready", "Siap")
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
