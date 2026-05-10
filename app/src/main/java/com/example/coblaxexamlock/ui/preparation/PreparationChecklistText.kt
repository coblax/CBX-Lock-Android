package com.example.coblaxexamlock.ui.preparation

import androidx.compose.runtime.Composable
import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.OverlaySignal
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.config.AllowedExamKeyboardPackages
import com.example.coblaxexamlock.config.AllowedSystemKeyboardPackagePrefixes
import com.example.coblaxexamlock.config.BlockedExamKeyboardPackages
import com.example.coblaxexamlock.config.EmulatorPackagePrefixes
import com.example.coblaxexamlock.config.MagiskIndicatorPaths
import com.example.coblaxexamlock.config.RiskyAccessibilityKeywords
import com.example.coblaxexamlock.config.RootBinaryIndicatorPaths
import com.example.coblaxexamlock.config.RootPackageNames
import com.example.coblaxexamlock.config.SuspiciousKeyboardPackageTokens
import com.example.coblaxexamlock.config.TrustedOemKeyboardManufacturers
import com.example.coblaxexamlock.config.VirtualFingerprintTokens
import com.example.coblaxexamlock.config.VirtualHardwareTokens
import com.example.coblaxexamlock.config.VirtualManufacturerTokens
import com.example.coblaxexamlock.config.VirtualModelTokens
import com.example.coblaxexamlock.config.VirtualProductTokens
import com.example.coblaxexamlock.config.VirtualQemuFiles
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.format.formatGeofenceDistance
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizeCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizePolygonVertices
import java.util.Locale

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
): PreparationChecklistText = with(state) {
    fun preparationDetailOrNull(english: String, indonesian: String): String? =
        if (showChecklistDetails) localized(uiLanguage, english, indonesian) else null
    val enabledAccessibilityPackages =
        if (showChecklistDetails) accessibilityInspection.activePackages else emptyList()
    val allowedAccessibilityServices =
        if (showChecklistDetails) accessibilityInspection.allowedServiceComponents else emptyList()
    val allowedAccessibilityPackages =
        if (showChecklistDetails) accessibilityInspection.allowedPackages else emptyList()
    val effectiveAccessibilityPackages =
        if (showChecklistDetails) accessibilityInspection.effectivePackages else emptyList()
    val riskyAccessibilityPackages =
        if (showChecklistDetails) accessibilityInspection.riskyPackages else emptyList()
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
    val networkStatusLabel = when (networkReadinessStatus.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> tr("Stable", "Stabil")
        NetworkReadinessUserVerdict.Offline -> tr("Offline", "Offline")
        NetworkReadinessUserVerdict.Unvalidated -> tr("Unvalidated", "Belum Tervalidasi")
        NetworkReadinessUserVerdict.CaptivePortal -> tr("Captive Portal", "Captive Portal")
        NetworkReadinessUserVerdict.DnsFailed -> tr("DNS Failed", "DNS Gagal")
        NetworkReadinessUserVerdict.Slow -> tr("Slow", "Lambat")
        NetworkReadinessUserVerdict.AirplaneMode -> tr("Airplane Mode", "Mode Pesawat")
        NetworkReadinessUserVerdict.Unstable -> tr("Unstable", "Tidak Stabil")
    }
    val networkValue = when (networkReadinessStatus.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> tr(
            "Connected and ready on ${networkReadinessStatus.transportLabel}.",
            "Terhubung dan siap di ${networkReadinessStatus.transportLabel}."
        )
        NetworkReadinessUserVerdict.Offline -> tr(
            "No active internet connection is available right now.",
            "Saat ini belum ada koneksi internet aktif."
        )
        NetworkReadinessUserVerdict.Unvalidated -> tr(
            "A network is connected, but Android has not validated internet access yet.",
            "Jaringan sudah terhubung, tetapi Android belum memvalidasi akses internet."
        )
        NetworkReadinessUserVerdict.CaptivePortal -> tr(
            "This network may still require a portal or login step before internet works.",
            "Jaringan ini mungkin masih membutuhkan portal atau langkah login sebelum internet bisa dipakai."
        )
        NetworkReadinessUserVerdict.DnsFailed -> tr(
            "Internet is connected, but DNS did not answer the quick probe.",
            "Internet terhubung, tetapi DNS tidak menjawab probe cepat."
        )
        NetworkReadinessUserVerdict.Slow -> tr(
            "Internet works, but the quick probe is slow. A steadier network is recommended.",
            "Internet bisa dipakai, tetapi probe cepat lambat. Jaringan yang lebih stabil disarankan."
        )
        NetworkReadinessUserVerdict.AirplaneMode -> tr(
            "Airplane mode is on and no active connection is available.",
            "Mode pesawat aktif dan belum ada koneksi aktif."
        )
        NetworkReadinessUserVerdict.Unstable -> tr(
            "The connection has changed several times recently. A stable network is recommended before and during the exam.",
            "Koneksi berubah beberapa kali belakangan ini. Jaringan yang stabil disarankan sebelum dan selama ujian."
        )
    }
    val networkLastChangeSummary = lastNetworkChangeAt?.ifBlank { "-" } ?: "-"
    val networkFlapMeta = when {
        networkReadinessStatus.verdict == NetworkReadinessVerdict.Unstable ||
            networkUnstableRuntimeStatus.flapCount > 0 ->
            tr(
                "Last change: $networkLastChangeSummary | Changes: ${networkUnstableRuntimeStatus.flapCount}",
                "Perubahan terakhir: $networkLastChangeSummary | Perubahan: ${networkUnstableRuntimeStatus.flapCount}"
            )
        else -> null
    }
    val networkProbeMeta = networkReadinessStatus.dnsProbeStatus
        .takeIf { it.verdict.name !in setOf("NotRun", "Skipped") }
        ?.let { probe ->
            tr(
                "DNS probe: ${probe.verdict.name} | ${probe.latencyBucket.name.lowercase(Locale.US)}",
                "Probe DNS: ${probe.verdict.name} | ${probe.latencyBucket.name.lowercase(Locale.US)}"
            )
        }
    val networkMeta = listOfNotNull(networkFlapMeta, networkProbeMeta)
        .joinToString("\n")
        .ifBlank { null }
    val networkDetail = networkReadinessStatus.userFacingQuickFixText ?: when (networkReadinessStatus.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> null
        NetworkReadinessUserVerdict.Offline -> tr(
            "Check Wi-Fi or mobile data, then tap Refresh.",
            "Periksa Wi-Fi atau data seluler, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.Unvalidated -> tr(
            "Wait a moment or switch to a network with working internet, then tap Refresh.",
            "Tunggu sebentar atau pindah ke jaringan yang internetnya aktif, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.CaptivePortal -> tr(
            "Complete the network login page first, then return here and tap Refresh.",
            "Selesaikan halaman login jaringan dahulu, lalu kembali dan tekan Refresh."
        )
        NetworkReadinessUserVerdict.DnsFailed -> tr(
            "Try another network or DNS, disable VPN if needed, then tap Refresh.",
            "Coba jaringan atau DNS lain, matikan VPN bila perlu, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.Slow -> tr(
            "Move closer to Wi-Fi or switch network before starting.",
            "Dekatkan ke Wi-Fi atau pindah jaringan sebelum mulai."
        )
        NetworkReadinessUserVerdict.AirplaneMode -> tr(
            "Turn off airplane mode or enable Wi-Fi/mobile data, then tap Refresh.",
            "Matikan mode pesawat atau aktifkan Wi-Fi/data seluler, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.Unstable -> tr(
            "Use the most stable available network before starting the exam.",
            "Gunakan jaringan yang paling stabil sebelum mulai ujian."
        )
    }
    val webViewHealthItem = preExamHealthCheckSnapshot.items.firstOrNull {
        it.category == PreExamHealthCategory.WebView
    }
    val webViewProviderStatusLabel = when (webViewHealthItem?.verdict) {
        PreExamHealthVerdict.Blocking -> tr("Unavailable", "Tidak Tersedia")
        PreExamHealthVerdict.Warning -> tr("Needs Update", "Perlu Update")
        PreExamHealthVerdict.Stable -> tr("Ready", "Siap")
        null -> tr("Unknown", "Tidak Diketahui")
    }
    val webViewProviderValue = webViewHealthItem?.detail ?: tr(
        "WebView provider status is not available yet.",
        "Status WebView provider belum tersedia."
    )
    val webViewProviderDetail = webViewHealthItem?.quickFix
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
        else -> "Fallback"
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
        screenPinningAvailable -> tr("Available", "Tersedia")
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
        else -> tr("Monitored", "Dipantau")
    }
    val keyboardDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Default input method package from Settings.Secure.DEFAULT_INPUT_METHOD\n" +
            "- Allowlist packages: ${preparationListSummary(AllowedExamKeyboardPackages)}\n" +
            "- Blocklist packages: ${preparationListSummary(BlockedExamKeyboardPackages)}\n" +
            "- Suspicious tokens: ${preparationListSummary(SuspiciousKeyboardPackageTokens)}\n" +
            "- Must be system app OR trusted OEM keyboard\n" +
            "- Allowed system prefixes: ${preparationListSummary(AllowedSystemKeyboardPackagePrefixes)}\n" +
            "- Trusted OEMs: ${preparationListSummary(TrustedOemKeyboardManufacturers)}\n" +
            "Impact:\n" +
            "- Not allowed -> fallback to internal keyboard\n" +
            "- If keyboard changes during exam -> violation + alarm",
        "Dicek:\n" +
            "- Paket input method default dari Settings.Secure.DEFAULT_INPUT_METHOD\n" +
            "- Allowlist paket: ${preparationListSummary(AllowedExamKeyboardPackages)}\n" +
            "- Blocklist paket: ${preparationListSummary(BlockedExamKeyboardPackages)}\n" +
            "- Token mencurigakan: ${preparationListSummary(SuspiciousKeyboardPackageTokens)}\n" +
            "- Harus aplikasi sistem ATAU keyboard OEM tepercaya\n" +
            "- Prefix sistem yang diizinkan: ${preparationListSummary(AllowedSystemKeyboardPackagePrefixes)}\n" +
            "- OEM tepercaya: ${preparationListSummary(TrustedOemKeyboardManufacturers)}\n" +
            "Dampak:\n" +
            "- Tidak diizinkan -> fallback ke keyboard internal\n" +
            "- Jika berubah saat ujian -> pelanggaran + alarm"
    )
    val bluetoothDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Permission BLUETOOTH_CONNECT (Android 12+)\n" +
            "- Bluetooth adapter enabled state\n" +
            "- Listener for BluetoothAdapter.ACTION_STATE_CHANGED during exam\n" +
            "Impact:\n" +
            "- Start blocked if permission missing or Bluetooth enabled\n" +
            "- If enabled during exam -> violation + alarm",
        "Dicek:\n" +
            "- Izin BLUETOOTH_CONNECT (Android 12+)\n" +
            "- Status adapter Bluetooth\n" +
            "- Listener BluetoothAdapter.ACTION_STATE_CHANGED saat ujian\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika izin belum ada atau Bluetooth aktif\n" +
            "- Jika aktif saat ujian -> pelanggaran + alarm"
    )
    val accessibilityDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- AccessibilityManager.isEnabled\n" +
            "- Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES not empty\n" +
            "- Active packages: ${preparationListSummary(enabledAccessibilityPackages)}\n" +
            "- Allowed services: ${preparationListSummary(allowedAccessibilityServices)}\n" +
            "- Allowed packages: ${preparationListSummary(allowedAccessibilityPackages)}\n" +
            "- Effective packages after allowlist: ${preparationListSummary(effectiveAccessibilityPackages)}\n" +
            "- Risky keywords: ${preparationListSummary(RiskyAccessibilityKeywords)}\n" +
            "- Risky packages matched: ${preparationListSummary(riskyAccessibilityPackages)}\n" +
            "Impact:\n" +
            "- Start blocked if accessibility service active\n" +
            "- If enabled during exam -> warning + alarm",
        "Dicek:\n" +
            "- AccessibilityManager.isEnabled\n" +
            "- Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES tidak kosong\n" +
            "- Paket aktif: ${preparationListSummary(enabledAccessibilityPackages)}\n" +
            "- Service yang diizinkan: ${preparationListSummary(allowedAccessibilityServices)}\n" +
            "- Paket yang diizinkan: ${preparationListSummary(allowedAccessibilityPackages)}\n" +
            "- Paket efektif setelah allowlist: ${preparationListSummary(effectiveAccessibilityPackages)}\n" +
            "- Keyword berisiko: ${preparationListSummary(RiskyAccessibilityKeywords)}\n" +
            "- Paket berisiko terdeteksi: ${preparationListSummary(riskyAccessibilityPackages)}\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika aksesibilitas aktif\n" +
            "- Jika aktif saat ujian -> peringatan + alarm"
    )
    val overlayDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Confirmed signal: obscured/partially obscured touch on SecureExamWebView\n" +
            "- Confirmed signal: suspicious exam window focus loss while app stays visible\n" +
            "- Heuristic signal: active accessibility service\n" +
            "- Overlay shield supported: ${if (overlayRiskResult.shieldStatus.supported) "Yes" else "No"}\n" +
            "- Overlay shield requested: ${if (overlayRiskResult.shieldStatus.requested) "Yes" else "No"}\n" +
            "- Overlay shield active: ${if (overlayRiskResult.shieldStatus.active) "Yes" else "No"}\n" +
            "- Overlay shield last apply result: ${
                overlayRiskResult.shieldStatus.lastApplySucceeded?.let { if (it) "success" else "failed" } ?: "unsupported"
            }\n" +
            "- Overlay shield last apply time: ${overlayRiskResult.shieldStatus.lastApplyAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Risky accessibility keywords: ${preparationListSummary(RiskyAccessibilityKeywords)}\n" +
            "- Risky accessibility packages: ${preparationListSummary(overlayRiskResult.riskyAccessibilityPackages)}\n" +
            "- Overlay signals: ${preparationListSummary(overlayRiskResult.signals.map { it.diagnosticLabel() })}\n" +
            "- Overlay violations: ${overlayRiskResult.violationCount}\n" +
            "- Last trigger: ${overlayRiskResult.lastTrigger?.ifBlank { "-" } ?: "-"}\n" +
            "- Last timestamp: ${overlayRiskResult.lastDetectedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Last context: ${overlayRiskResult.lastContext?.ifBlank { "-" } ?: "-"}\n" +
            "Impact:\n" +
            "- Heuristic risk only updates warning status and quick fixes\n" +
            "- Confirmed obscured touch or suspicious focus loss triggers alarm + acknowledge dialog",
        "Dicek:\n" +
            "- Sinyal terkonfirmasi: touch obscured/partially obscured pada SecureExamWebView\n" +
            "- Sinyal terkonfirmasi: fokus jendela ujian hilang secara mencurigakan saat app masih terlihat\n" +
            "- Sinyal heuristik: accessibility service aktif\n" +
            "- Overlay shield didukung: ${if (overlayRiskResult.shieldStatus.supported) "Ya" else "Tidak"}\n" +
            "- Overlay shield diminta aktif: ${if (overlayRiskResult.shieldStatus.requested) "Ya" else "Tidak"}\n" +
            "- Overlay shield aktif: ${if (overlayRiskResult.shieldStatus.active) "Ya" else "Tidak"}\n" +
            "- Hasil apply overlay shield terakhir: ${
                overlayRiskResult.shieldStatus.lastApplySucceeded?.let { if (it) "berhasil" else "gagal" } ?: "tidak didukung"
            }\n" +
            "- Waktu apply overlay shield terakhir: ${overlayRiskResult.shieldStatus.lastApplyAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Keyword accessibility berisiko: ${preparationListSummary(RiskyAccessibilityKeywords)}\n" +
            "- Paket accessibility berisiko: ${preparationListSummary(overlayRiskResult.riskyAccessibilityPackages)}\n" +
            "- Sinyal overlay: ${preparationListSummary(overlayRiskResult.signals.map { it.diagnosticLabel() })}\n" +
            "- Jumlah pelanggaran overlay: ${overlayRiskResult.violationCount}\n" +
            "- Trigger terakhir: ${overlayRiskResult.lastTrigger?.ifBlank { "-" } ?: "-"}\n" +
            "- Waktu terakhir: ${overlayRiskResult.lastDetectedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Konteks terakhir: ${overlayRiskResult.lastContext?.ifBlank { "-" } ?: "-"}\n" +
            "Dampak:\n" +
            "- Risiko heuristik hanya mengubah status warning dan quick fix\n" +
            "- Obscured touch atau fokus hilang mencurigakan memicu alarm + dialog acknowledge"
    )
    val developerDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Settings.Global.DEVELOPMENT_SETTINGS_ENABLED = ${adbInspection.developerOptionsRawValue}\n" +
            "- Settings.Global.ADB_ENABLED = ${adbInspection.adbRawValue}\n" +
            "- ro.adb.secure = ${adbInspection.adbSecureProperty}\n" +
            "- Integrity hint = ${adbInspection.integrityHintSummary}\n" +
            "Impact:\n" +
            "- Start blocked if Developer Mode or ADB enabled\n" +
            "- If enabled during exam -> warning + alarm",
        "Dicek:\n" +
            "- Settings.Global.DEVELOPMENT_SETTINGS_ENABLED = ${adbInspection.developerOptionsRawValue}\n" +
            "- Settings.Global.ADB_ENABLED = ${adbInspection.adbRawValue}\n" +
            "- ro.adb.secure = ${adbInspection.adbSecureProperty}\n" +
            "- Hint integritas = ${adbInspection.integrityHintSummary}\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika Developer Mode atau ADB aktif\n" +
            "- Jika aktif saat ujian -> peringatan + alarm"
    )
    val rootDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Build tags contain test-keys\n" +
            "- su/busybox paths: ${preparationListSummary(RootBinaryIndicatorPaths)}\n" +
            "- Root app packages: ${preparationListSummary(RootPackageNames)}\n" +
            "- Magisk paths: ${preparationListSummary(MagiskIndicatorPaths)}\n" +
            "- Zygisk detection: /data/adb/zygisk or /proc/self/maps scan\n" +
            "- Bootloader state from ro.boot.verifiedbootstate, ro.boot.vbmeta.device_state, ro.boot.flash.locked\n" +
            "- Dangerous props: ro.debuggable, ro.secure, ro.adb.secure, ro.build.type\n" +
            "- SELinux enabled/enforced\n" +
            "- Current primary indicator: ${rootSecurityStatus.primaryIndicatorLabel}\n" +
            "- Current evidence summary: ${rootSecurityStatus.evidenceSummary}\n" +
            "Impact:\n" +
            "- Start blocked if root indicators found\n" +
            "- If detected during exam -> warning + alarm",
        "Dicek:\n" +
            "- Build tags mengandung test-keys\n" +
            "- Path su/busybox: ${preparationListSummary(RootBinaryIndicatorPaths)}\n" +
            "- Paket aplikasi root: ${preparationListSummary(RootPackageNames)}\n" +
            "- Path Magisk: ${preparationListSummary(MagiskIndicatorPaths)}\n" +
            "- Deteksi Zygisk: /data/adb/zygisk atau scan /proc/self/maps\n" +
            "- Status bootloader dari ro.boot.verifiedbootstate, ro.boot.vbmeta.device_state, ro.boot.flash.locked\n" +
            "- Properti berbahaya: ro.debuggable, ro.secure, ro.adb.secure, ro.build.type\n" +
            "- Status SELinux enabled/enforced\n" +
            "- Indikator utama saat ini: ${rootSecurityStatus.primaryIndicatorLabel}\n" +
            "- Ringkasan bukti saat ini: ${rootSecurityStatus.evidenceSummary}\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika indikator root ditemukan\n" +
            "- Jika terdeteksi saat ujian -> peringatan + alarm"
    )
    val signatureDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- SHA-256 fingerprint of signing certificate\n" +
            "- Expected fingerprints: release (and debug when BuildConfig.DEBUG)\n" +
            "Impact:\n" +
            "- Mismatch blocks start and prompts reinstall official APK",
        "Dicek:\n" +
            "- Fingerprint SHA-256 sertifikat penandatangan APK\n" +
            "- Fingerprint expected: rilis (dan debug saat BuildConfig.DEBUG)\n" +
            "Dampak:\n" +
            "- Tidak cocok -> blok mulai ujian dan sarankan reinstall APK resmi"
    )
    val virtualEnvironmentDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Build.FINGERPRINT tokens: ${preparationListSummary(VirtualFingerprintTokens)}\n" +
            "- Build.MODEL tokens: ${preparationListSummary(VirtualModelTokens)}\n" +
            "- Build.MANUFACTURER tokens: ${preparationListSummary(VirtualManufacturerTokens)}\n" +
            "- Build.BRAND/DEVICE generic prefix\n" +
            "- Build.PRODUCT tokens: ${preparationListSummary(VirtualProductTokens)}\n" +
            "- Build.HARDWARE tokens: ${preparationListSummary(VirtualHardwareTokens)}\n" +
            "- x86 ABIs in Build.SUPPORTED_ABIS\n" +
            "- ro.kernel.qemu=1\n" +
            "- QEMU files: ${preparationListSummary(VirtualQemuFiles)}\n" +
            "- Emulator package prefixes: ${preparationListSummary(EmulatorPackagePrefixes)}\n" +
            "Impact:\n" +
            "- Start blocked if emulator/VM detected\n" +
            "- If detected during exam -> warning + alarm",
        "Dicek:\n" +
            "- Token Build.FINGERPRINT: ${preparationListSummary(VirtualFingerprintTokens)}\n" +
            "- Token Build.MODEL: ${preparationListSummary(VirtualModelTokens)}\n" +
            "- Token Build.MANUFACTURER: ${preparationListSummary(VirtualManufacturerTokens)}\n" +
            "- Prefix generic pada Build.BRAND/DEVICE\n" +
            "- Token Build.PRODUCT: ${preparationListSummary(VirtualProductTokens)}\n" +
            "- Token Build.HARDWARE: ${preparationListSummary(VirtualHardwareTokens)}\n" +
            "- ABI x86 pada Build.SUPPORTED_ABIS\n" +
            "- ro.kernel.qemu=1\n" +
            "- File QEMU: ${preparationListSummary(VirtualQemuFiles)}\n" +
            "- Prefix paket emulator: ${preparationListSummary(EmulatorPackagePrefixes)}\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika emulator/VM terdeteksi\n" +
            "- Jika terdeteksi saat ujian -> peringatan + alarm"
    )
    val clipboardDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Clipboard monitoring arms as soon as START EXAM MODE is pressed\n" +
            "- ClipboardManager.OnPrimaryClipChangedListener during exam\n" +
            "- Snapshot includes all clipboard items: text | htmlText | uri | intent.action | intent.data | intent.component\n" +
            "- Diagnostics expose baseline vs detected semantic clipboard signatures for false-positive analysis\n" +
            "- Short settling window confirms the final clipboard state before raising a violation\n" +
            "- Clipboard is re-checked when the app returns after leaving the exam screen\n" +
            "- Ignore synthetic warmup callbacks right after listener registration\n" +
            "- Last confirmed change: ${clipboardRuntimeStatus.lastConfirmedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Last listener decision: ${clipboardRuntimeStatus.lastDecision}\n" +
            "- Baseline semantic signature: ${clipboardRuntimeStatus.baselineSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Detected semantic signature: ${clipboardRuntimeStatus.detectedSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Current semantic signature: ${clipboardRuntimeStatus.currentSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Violations: $clipboardViolationCount\n" +
            "Impact:\n" +
            "- Clipboard changes trigger alarm (does not block start)",
        "Dicek:\n" +
            "- Monitoring clipboard aktif sejak START EXAM MODE ditekan\n" +
            "- ClipboardManager.OnPrimaryClipChangedListener saat ujian\n" +
            "- Snapshot mencakup semua item clipboard: text | htmlText | uri | intent.action | intent.data | intent.component\n" +
            "- Diagnostics menampilkan semantic signature baseline vs detected untuk analisis false positive\n" +
            "- Ada jendela stabilisasi singkat untuk memastikan state akhir sebelum dianggap pelanggaran\n" +
            "- Clipboard dicek ulang saat aplikasi kembali setelah keluar dari layar ujian\n" +
            "- Abaikan callback warmup sintetis sesaat setelah listener dipasang\n" +
            "- Perubahan terkonfirmasi terakhir: ${clipboardRuntimeStatus.lastConfirmedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Keputusan listener terakhir: ${clipboardRuntimeStatus.lastDecision}\n" +
            "- Baseline semantic signature: ${clipboardRuntimeStatus.baselineSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Detected semantic signature: ${clipboardRuntimeStatus.detectedSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Current semantic signature: ${clipboardRuntimeStatus.currentSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Jumlah pelanggaran: $clipboardViolationCount\n" +
            "Dampak:\n" +
        "- Perubahan clipboard memicu alarm (tidak memblokir start)"
    )
    val geofenceDetail = preparationDetailOrNull(
        english =
            "- Location policy source: ${geofenceRuntimeStatus.policySource.diagnosticLabel()}\n" +
                "- Geofence enabled: ${if (geofenceRuntimeStatus.evaluation.enabled) "yes" else "no"}\n" +
                "- Shape: ${geofenceRuntimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US) ?: "-"}\n" +
                "- Polygon points: ${geofenceRuntimeStatus.evaluation.config?.vertices?.size ?: 0}\n" +
                "- Polygon vertices: ${summarizePolygonVertices(geofenceRuntimeStatus.evaluation.config?.vertices.orEmpty())}\n" +
                "- Circle centers: ${effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config).size}\n" +
                "- Circle centers summary: ${summarizeCircleCenters(effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config))}\n" +
                "- Bypass state: ${geofenceBypassState.name.lowercase(Locale.US)}\n" +
                "- Closest / primary center: ${
                    geofenceRuntimeStatus.evaluation.closestCircleCenter?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: geofenceRuntimeStatus.evaluation.config?.let {
                        formatCoordinates(it.centerLat, it.centerLng)
                    } ?: "-"
                }\n" +
                "- Shared radius: ${
                    geofenceRuntimeStatus.evaluation.config?.radiusMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Permission granted: ${if (geofenceRuntimeStatus.evaluation.permissionGranted) "yes" else "no"}\n" +
                "- Precise granted: ${if (geofenceRuntimeStatus.securityStatus.preciseLocationGranted) "yes" else "no"}\n" +
                "- Location services enabled: ${if (geofenceRuntimeStatus.evaluation.locationServicesEnabled) "yes" else "no"}\n" +
                "- Current coordinates: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: "-"
                }\n" +
                "- Provider: ${geofenceRuntimeStatus.evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"}\n" +
                "- Accuracy: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Fix quality: ${geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Fix age: ${formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs)}\n" +
                "- Snapshot used for geofence: ${if (geofenceRuntimeStatus.securityStatus.fixQualityStatus.usableForGeofence) "yes" else "no"}\n" +
                "- Distance from closest center: ${formatGeofenceDistance(geofenceRuntimeStatus.evaluation.distanceMeters)}\n" +
                "- Final verdict: ${geofenceRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Geofence verdict: ${geofenceRuntimeStatus.evaluation.verdict.diagnosticLabel()}\n" +
                "- Violations: ${geofenceRuntimeStatus.violationCount}\n" +
                "- Last trigger: ${geofenceRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}",
        indonesian =
            "- Sumber policy lokasi: ${geofenceRuntimeStatus.policySource.diagnosticLabel()}\n" +
                "- Geofence aktif: ${if (geofenceRuntimeStatus.evaluation.enabled) "ya" else "tidak"}\n" +
                "- Bentuk: ${geofenceRuntimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US) ?: "-"}\n" +
                "- Titik polygon: ${geofenceRuntimeStatus.evaluation.config?.vertices?.size ?: 0}\n" +
                "- Vertex polygon: ${summarizePolygonVertices(geofenceRuntimeStatus.evaluation.config?.vertices.orEmpty())}\n" +
                "- Jumlah center circle: ${effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config).size}\n" +
                "- Ringkasan center circle: ${summarizeCircleCenters(effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config))}\n" +
                "- Status bypass: ${geofenceBypassState.name.lowercase(Locale.US)}\n" +
                "- Center terdekat / utama: ${
                    geofenceRuntimeStatus.evaluation.closestCircleCenter?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: geofenceRuntimeStatus.evaluation.config?.let {
                        formatCoordinates(it.centerLat, it.centerLng)
                    } ?: "-"
                }\n" +
                "- Radius bersama: ${
                    geofenceRuntimeStatus.evaluation.config?.radiusMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Izin lokasi: ${if (geofenceRuntimeStatus.evaluation.permissionGranted) "diberikan" else "belum"}\n" +
                "- Lokasi presisi: ${if (geofenceRuntimeStatus.securityStatus.preciseLocationGranted) "ya" else "belum"}\n" +
                "- Layanan lokasi: ${if (geofenceRuntimeStatus.evaluation.locationServicesEnabled) "aktif" else "nonaktif"}\n" +
                "- Koordinat saat ini: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: "-"
                }\n" +
                "- Provider: ${geofenceRuntimeStatus.evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"}\n" +
                "- Akurasi: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Kualitas fix: ${geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Umur fix: ${formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs)}\n" +
                "- Snapshot dipakai untuk geofence: ${if (geofenceRuntimeStatus.securityStatus.fixQualityStatus.usableForGeofence) "ya" else "tidak"}\n" +
                "- Jarak dari center terdekat: ${formatGeofenceDistance(geofenceRuntimeStatus.evaluation.distanceMeters)}\n" +
                "- Verdict final: ${geofenceRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Verdict geofence: ${geofenceRuntimeStatus.evaluation.verdict.diagnosticLabel()}\n" +
                "- Jumlah pelanggaran: ${geofenceRuntimeStatus.violationCount}\n" +
                "- Trigger terakhir: ${geofenceRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}"
    )
    val fakeLocationDetail = preparationDetailOrNull(
        english =
            "- Monitoring enabled: ${if (fakeLocationRuntimeStatus.securityStatus.monitoringEnabled) "yes" else "no"}\n" +
                "- Permission granted: ${if (fakeLocationRuntimeStatus.securityStatus.permissionGranted) "yes" else "no"}\n" +
                "- Location services enabled: ${if (fakeLocationRuntimeStatus.securityStatus.locationServicesEnabled) "yes" else "no"}\n" +
                "- Snapshot available: ${if (fakeLocationRuntimeStatus.securityStatus.snapshotAvailable) "yes" else "no"}\n" +
                "- Bypass state: ${fakeLocationBypassState.name.lowercase(Locale.US)}\n" +
                "- Mock location flag: ${if (fakeLocationRuntimeStatus.securityStatus.mockLocationDetected) "yes" else "no"}\n" +
                "- Confidence tier: ${fakeLocationRuntimeStatus.securityStatus.confidenceTier.diagnosticLabel()}\n" +
                "- Final verdict: ${fakeLocationRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Developer options: ${if (fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled) "enabled" else "disabled"}\n" +
                "- Fix quality: ${fakeLocationRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Fix-quality eligible: ${if (fakeLocationRuntimeStatus.securityStatus.fixQualityEligible) "yes" else "no"}\n" +
                "- Suspicious packages: ${fakeLocationRuntimeStatus.securityStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" }}\n" +
                "- Supporting signals: ${fakeLocationRuntimeStatus.securityStatus.supportingSignals.map { it.diagnosticLabel() }.joinToString().ifBlank { "-" }}\n" +
                "- Violations: ${fakeLocationRuntimeStatus.violationCount}\n" +
                "- Last trigger: ${fakeLocationRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}",
        indonesian =
            "- Monitoring aktif: ${if (fakeLocationRuntimeStatus.securityStatus.monitoringEnabled) "ya" else "tidak"}\n" +
                "- Izin lokasi: ${if (fakeLocationRuntimeStatus.securityStatus.permissionGranted) "ya" else "tidak"}\n" +
                "- Layanan lokasi aktif: ${if (fakeLocationRuntimeStatus.securityStatus.locationServicesEnabled) "ya" else "tidak"}\n" +
                "- Snapshot tersedia: ${if (fakeLocationRuntimeStatus.securityStatus.snapshotAvailable) "ya" else "tidak"}\n" +
                "- Status bypass: ${fakeLocationBypassState.name.lowercase(Locale.US)}\n" +
                "- Flag mock location: ${if (fakeLocationRuntimeStatus.securityStatus.mockLocationDetected) "ya" else "tidak"}\n" +
                "- Confidence tier: ${fakeLocationRuntimeStatus.securityStatus.confidenceTier.diagnosticLabel()}\n" +
                "- Verdict final: ${fakeLocationRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Developer options: ${if (fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled) "aktif" else "nonaktif"}\n" +
                "- Kualitas fix: ${fakeLocationRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Fix layak dinilai: ${if (fakeLocationRuntimeStatus.securityStatus.fixQualityEligible) "ya" else "tidak"}\n" +
                "- Paket mencurigakan: ${fakeLocationRuntimeStatus.securityStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" }}\n" +
                "- Sinyal pendukung: ${fakeLocationRuntimeStatus.securityStatus.supportingSignals.map { it.diagnosticLabel() }.joinToString().ifBlank { "-" }}\n" +
                "- Jumlah pelanggaran: ${fakeLocationRuntimeStatus.violationCount}\n" +
                "- Trigger terakhir: ${fakeLocationRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}"
    )
    val screenPinningDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- lock_to_app_enabled from Settings.System then Settings.Secure\n" +
            "- ActivityManager.lockTaskModeState (LOCKED/PINNED)\n" +
            "- Screen Pinning support available: ${if (screenPinningAvailable) "Yes" else "No"}\n" +
            "Impact:\n" +
            "- Available but inactive -> Android pinning is requested only after START EXAM MODE is pressed\n" +
            "- Unavailable -> Start Exam is blocked; use a supported device or Secret Admin bypass\n" +
            "- If bypass enabled -> skip pin/lock-task flow",
        "Dicek:\n" +
            "- lock_to_app_enabled dari Settings.System lalu Settings.Secure\n" +
            "- ActivityManager.lockTaskModeState (LOCKED/PINNED)\n" +
            "- Dukungan Screen Pinning tersedia: ${if (screenPinningAvailable) "Ya" else "Tidak"}\n" +
            "Dampak:\n" +
            "- Tersedia tapi belum aktif -> pinning Android baru diminta setelah START EXAM MODE ditekan\n" +
            "- Tidak tersedia -> Start Exam diblokir; gunakan perangkat yang mendukung atau bypass Secret Admin\n" +
            "- Jika bypass aktif -> lewati alur pin/lock-task"
    )
    val accessibilityGuardDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- CBX Lock Exam Guard accessibility service enabled: ${if (accessibilityGuardEnabled) "Yes" else "No"}\n" +
            "- Required only when Screen Pinning is unavailable and Screen Pinning bypass is off\n" +
                "- Events monitored: TYPE_WINDOW_STATE_CHANGED, TYPE_WINDOWS_CHANGED, TYPE_NOTIFICATION_STATE_CHANGED\n" +
            "- Screen text is not read\n" +
            "Impact:\n" +
            "- If required and disabled -> Start Exam is blocked\n" +
            "- During fallback mode, app switches are logged and the app returns to the exam with escalating alarm",
        "Dicek:\n" +
            "- Service aksesibilitas CBX Lock Exam Guard aktif: ${if (accessibilityGuardEnabled) "Ya" else "Tidak"}\n" +
            "- Wajib hanya saat Screen Pinning tidak tersedia dan bypass Screen Pinning nonaktif\n" +
                "- Event yang dipantau: TYPE_WINDOW_STATE_CHANGED, TYPE_WINDOWS_CHANGED, TYPE_NOTIFICATION_STATE_CHANGED\n" +
            "- Teks layar tidak dibaca\n" +
            "Dampak:\n" +
            "- Jika wajib tetapi nonaktif -> Start Exam diblokir\n" +
            "- Saat mode fallback, app switch dicatat dan app kembali ke ujian dengan alarm eskalatif"
    )
    val appSwitchDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- App Switch monitoring arms as soon as START EXAM MODE is pressed\n" +
            "- onUserLeaveHint() callback from host activity\n" +
            "- Lifecycle stop/resume fallback when onUserLeaveHint() is skipped by the system\n" +
            "- Resume confirmation after leaving the app\n" +
            "- Suppressed internal-flow logging during allowed transitions\n" +
            "- Protection mode: ${appSwitchStatus.protectionMode.diagnosticLabel()}\n" +
            "- Lock task active now: ${if (appSwitchStatus.lockTaskActive) "Yes" else "No"}\n" +
            "- Fallback guard active: ${if (appSwitchStatus.fallbackGuardActive) "Yes" else "No"}\n" +
            "- Accessibility Guard enabled: ${if (appSwitchStatus.accessibilityGuardEnabled) "Yes" else "No"}\n" +
            "- Accessibility fallback active: ${if (appSwitchStatus.accessibilityFallbackActive) "Yes" else "No"}\n" +
            "- Accessibility violation count: ${appSwitchStatus.accessibilityViolationCount}\n" +
            "- Last accessibility reason: ${appSwitchStatus.accessibilityLastReason?.ifBlank { "-" } ?: "-"}\n" +
            "- Last foreign package: ${appSwitchStatus.accessibilityLastForeignPackage?.ifBlank { "-" } ?: "-"}\n" +
            "- Last accessibility event: ${appSwitchStatus.accessibilityLastEventType?.ifBlank { "-" } ?: "-"}\n" +
            "- Current alarm severity: ${appSwitchStatus.accessibilityAlarmSeverity?.ifBlank { "-" } ?: "-"}\n" +
            "- Last trigger: ${appSwitchStatus.lastTrigger?.ifBlank { "-" } ?: "-"}\n" +
            "- Last timestamp: ${appSwitchStatus.lastDetectedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Last context: ${appSwitchStatus.lastContext?.ifBlank { "-" } ?: "-"}\n" +
            "- Violations: ${appSwitchStatus.violationCount}\n" +
            "- Pending violation: ${if (appSwitchStatus.pendingViolation) "Yes" else "No"}\n" +
            "Impact:\n" +
            "- Leaving the app during exam triggers alarm + acknowledge dialog\n" +
            "- If screen pinning is bypassed/unavailable, App Switch stays active as the fallback guard\n" +
            "- If bypass enabled -> App Switch monitoring is skipped",
        "Dicek:\n" +
            "- Monitoring App Switch aktif sejak START EXAM MODE ditekan\n" +
            "- Callback onUserLeaveHint() dari host activity\n" +
            "- Fallback lifecycle stop/resume jika onUserLeaveHint() dilewati oleh sistem\n" +
            "- Konfirmasi resume setelah keluar dari aplikasi\n" +
            "- Logging suppressed internal-flow saat transisi yang diizinkan\n" +
            "- Mode proteksi: ${appSwitchStatus.protectionMode.diagnosticLabel()}\n" +
            "- Lock task aktif saat ini: ${if (appSwitchStatus.lockTaskActive) "Ya" else "Tidak"}\n" +
            "- Fallback guard aktif: ${if (appSwitchStatus.fallbackGuardActive) "Ya" else "Tidak"}\n" +
            "- Accessibility Guard aktif: ${if (appSwitchStatus.accessibilityGuardEnabled) "Ya" else "Tidak"}\n" +
            "- Fallback accessibility aktif: ${if (appSwitchStatus.accessibilityFallbackActive) "Ya" else "Tidak"}\n" +
            "- Jumlah pelanggaran accessibility: ${appSwitchStatus.accessibilityViolationCount}\n" +
            "- Alasan accessibility terakhir: ${appSwitchStatus.accessibilityLastReason?.ifBlank { "-" } ?: "-"}\n" +
            "- Paket asing terakhir: ${appSwitchStatus.accessibilityLastForeignPackage?.ifBlank { "-" } ?: "-"}\n" +
            "- Event accessibility terakhir: ${appSwitchStatus.accessibilityLastEventType?.ifBlank { "-" } ?: "-"}\n" +
            "- Severity alarm saat ini: ${appSwitchStatus.accessibilityAlarmSeverity?.ifBlank { "-" } ?: "-"}\n" +
            "- Trigger terakhir: ${appSwitchStatus.lastTrigger?.ifBlank { "-" } ?: "-"}\n" +
            "- Waktu terakhir: ${appSwitchStatus.lastDetectedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Konteks terakhir: ${appSwitchStatus.lastContext?.ifBlank { "-" } ?: "-"}\n" +
            "- Jumlah pelanggaran: ${appSwitchStatus.violationCount}\n" +
            "- Pending violation: ${if (appSwitchStatus.pendingViolation) "Ya" else "Tidak"}\n" +
            "Dampak:\n" +
            "- Keluar dari aplikasi saat ujian memicu alarm + dialog acknowledge\n" +
            "- Jika screen pinning dibypass/tidak aktif, App Switch tetap aktif sebagai fallback guard\n" +
            "- Jika bypass aktif -> monitoring App Switch dilewati"
    )

    PreparationChecklistText(
        accessibilityStatusLabel = accessibilityStatusLabel,
        overlayStatusLabel = overlayStatusLabel,
        geofenceStatusLabel = geofenceStatusLabel,
        geofenceMeta = geofenceMeta,
        fakeLocationStatusLabel = fakeLocationStatusLabel,
        deviceTimeStatusLabel = deviceTimeStatusLabel,
        networkStatusLabel = networkStatusLabel,
        networkValue = networkValue,
        networkMeta = networkMeta,
        networkDetail = networkDetail,
        webViewProviderStatusLabel = webViewProviderStatusLabel,
        webViewProviderValue = webViewProviderValue,
        webViewProviderDetail = webViewProviderDetail,
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
        appSwitchStatusLabel = appSwitchStatusLabel,
        keyboardDetail = keyboardDetail,
        bluetoothDetail = bluetoothDetail,
        accessibilityDetail = accessibilityDetail,
        overlayDetail = overlayDetail,
        developerDetail = developerDetail,
        rootDetail = rootDetail,
        signatureDetail = signatureDetail,
        virtualEnvironmentDetail = virtualEnvironmentDetail,
        clipboardDetail = clipboardDetail,
        geofenceDetail = geofenceDetail,
        fakeLocationDetail = fakeLocationDetail,
        screenPinningDetail = screenPinningDetail,
        accessibilityGuardDetail = accessibilityGuardDetail,
        appSwitchDetail = appSwitchDetail
    )
}
