package com.example.coblaxexamlock.ui.preparation

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.DpcProtectionTier
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.OverlaySignal
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.accessibilityServiceFriendlySummary
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextMuted
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary

private const val PreparationRecomposeTag = "PreparationRecompose"

@Composable
private fun PreparationSectionRecompositionMarker(sectionName: String) {
    SideEffect {
        if (BuildConfig.DEBUG) {
            Log.d(PreparationRecomposeTag, "$sectionName recomposed")
        }
    }
}

@Composable
private fun PreparationChecklistSectionSurface(
    sectionName: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    PreparationSectionRecompositionMarker(sectionName)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutline.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
internal fun PreparationChecklistIntroItem(
    checklistTitle: String,
    checklistSubtitle: String,
    telegramHelperText: String,
    modifier: Modifier = Modifier
) {
    PreparationChecklistSectionSurface(
        sectionName = "checklist_intro",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = checklistTitle,
                    color = LockTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = checklistSubtitle,
                    color = LockTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, LockOutline.copy(alpha = 0.60f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = telegramHelperText,
                color = LockTextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
internal fun PreparationDeviceSetupSection(
    device: PreparationDeviceState,
    bypass: PreparationBypassState,
    text: PreparationChecklistText,
    sendingSection: DiagnosticSection?,
    needsBluetoothPermission: Boolean,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    modifier: Modifier = Modifier
) {
    PreparationChecklistSectionSurface(
        sectionName = "device_setup",
        modifier = modifier
    ) {
        SecurityChecklistItem(
            title = tr("Safe Keyboard", "Keyboard Aman"),
            value = when {
                bypass.bypassKeyboardPolicy -> tr("Bypass enabled", "Bypass aktif")
                device.usingBuiltInExamKeyboard -> "internal.coblax.exam"
                else -> device.keyboardPackage.ifBlank { tr("Not detected", "Tidak terdeteksi") }
            },
            detail = text.keyboardDetail,
            status = text.keyboardStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.Keyboard) },
            isSending = sendingSection == DiagnosticSection.Keyboard,
            sendEnabled = sendingSection == null
        )
        SecurityChecklistItem(
            title = tr("Bluetooth", "Bluetooth"),
            value = when {
                bypass.bypassBluetooth -> tr("Bypass enabled", "Bypass aktif")
                needsBluetoothPermission && !device.bluetoothPermissionGranted ->
                    tr("Bluetooth permission has not been granted.", "Izin Bluetooth belum diberikan")
                device.bluetoothEnabled -> tr("Still enabled", "Masih aktif")
                else -> tr("Disabled", "Nonaktif")
            },
            detail = text.bluetoothDetail,
            status = text.bluetoothStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.Bluetooth) },
            isSending = sendingSection == DiagnosticSection.Bluetooth,
            sendEnabled = sendingSection == null
        )
    }
}

@Composable
internal fun PreparationConnectivitySection(
    text: PreparationChecklistText,
    sendingSection: DiagnosticSection?,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    modifier: Modifier = Modifier
) {
    PreparationChecklistSectionSurface(
        sectionName = "connectivity",
        modifier = modifier
    ) {
        SecurityChecklistItem(
            title = tr("Network / Connectivity", "Network / Konektivitas"),
            value = text.networkValue,
            meta = text.networkMeta,
            detail = text.networkDetail,
            status = text.networkStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.Network) },
            isSending = sendingSection == DiagnosticSection.Network,
            sendEnabled = sendingSection == null
        )
    }
}

@Composable
internal fun PreparationDeviceHealthSection(
    device: PreparationDeviceState,
    bypass: PreparationBypassState,
    text: PreparationChecklistText,
    sendingSection: DiagnosticSection?,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    modifier: Modifier = Modifier
) {
    PreparationChecklistSectionSurface(
        sectionName = "device_health",
        modifier = modifier
    ) {
        SecurityChecklistItem(
            title = tr("WebView Provider", "WebView Provider"),
            value = text.webViewProviderValue,
            detail = text.webViewProviderDetail,
            status = text.webViewProviderStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.SecurityHealth) },
            isSending = sendingSection == DiagnosticSection.SecurityHealth,
            sendEnabled = sendingSection == null
        )
        SecurityChecklistItem(
            title = tr("Device Time", "Waktu Perangkat"),
            value = when {
                device.deviceTimeBypassState == DeviceTimeBypassState.Tampered -> tr(
                    "Bypass storage tamper detected. Device Time enforcement remains active.",
                    "Tamper pada storage bypass terdeteksi. Enforcement Waktu Perangkat tetap aktif."
                )
                bypass.bypassDeviceTime -> tr(
                    "Bypass active. Device Time checks are skipped.",
                    "Bypass aktif. Cek Waktu Perangkat dilewati."
                )
                device.deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.Safe -> tr(
                    "Automatic date & time and automatic time zone are enabled.",
                    "Tanggal & waktu otomatis dan zona waktu otomatis aktif."
                )
                device.deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> tr(
                    "Automatic date & time is off.",
                    "Tanggal & waktu otomatis nonaktif."
                )
                device.deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> tr(
                    "Automatic time zone is off.",
                    "Zona waktu otomatis nonaktif."
                )
                else -> tr(
                    "A suspicious clock change was detected.",
                    "Terdeteksi perubahan jam yang mencurigakan."
                )
            },
            detail = text.deviceTimeDetail,
            status = text.deviceTimeStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.DeviceTime) },
            isSending = sendingSection == DiagnosticSection.DeviceTime,
            sendEnabled = sendingSection == null
        )
    }
}

@Composable
internal fun PreparationRuntimeInteractionSection(
    runtimeSecurity: PreparationRuntimeSecurityState,
    bypass: PreparationBypassState,
    text: PreparationChecklistText,
    sendingSection: DiagnosticSection?,
    accessibilityInspection: AccessibilityInspectionResult,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val blockingAccessibilitySummary = accessibilityServiceFriendlySummary(
        serviceComponents = accessibilityInspection.effectiveServiceComponents,
        maxItems = 1
    )
    PreparationChecklistSectionSurface(
        sectionName = "runtime_interaction",
        modifier = modifier
    ) {
        SecurityChecklistItem(
            title = tr("Accessibility Service", "Accessibility Service"),
            value = when {
                bypass.bypassAccessibility -> tr("Bypass enabled", "Bypass aktif")
                accessibilityInspection.allowedOnlyActive -> tr(
                    "Allowed service active: ${accessibilityInspection.allowedPackages.joinToString().ifBlank { "-" }}",
                    "Service yang diizinkan aktif: ${accessibilityInspection.allowedPackages.joinToString().ifBlank { "-" }}"
                )
                runtimeSecurity.accessibilityServiceEnabled -> tr(
                    "Turn off: $blockingAccessibilitySummary",
                    "Matikan: $blockingAccessibilitySummary"
                )
                else -> tr("Inactive", "Tidak aktif")
            },
            detail = text.accessibilityDetail,
            status = text.accessibilityStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.Accessibility) },
            isSending = sendingSection == DiagnosticSection.Accessibility,
            sendEnabled = sendingSection == null
        )
        SecurityChecklistItem(
            title = tr("Overlay / Floating App", "Overlay / Floating App"),
            value = when {
                bypass.bypassOverlay -> tr("Bypass enabled", "Bypass aktif")
                runtimeSecurity.overlayAppsDetected.isNotEmpty() -> {
                    val appNames = runtimeSecurity.overlayAppsDetected.joinToString(", ") { it.appLabel }
                    tr(
                        "Apps with overlay permission detected: $appNames. Disable 'Appear on Top' for these listed apps; do not enable it for CBX.",
                        "Aplikasi dengan izin overlay terdeteksi: $appNames. Matikan izin 'Tampilkan di Atas Aplikasi Lain' untuk app yang terdaftar; jangan aktifkan izin ini untuk CBX."
                    )
                }
                runtimeSecurity.overlayRiskResult.confirmedInteractionDetected -> tr(
                    "Overlay interaction was confirmed on the exam screen.",
                    "Interaksi overlay terkonfirmasi di layar ujian."
                )
                runtimeSecurity.overlayRiskResult.lastTrigger == OverlaySignal.WindowFocusLoss.diagnosticLabel() ->
                    tr(
                        "Window focus changed; this is warning-only unless another strong signal appears.",
                        "Fokus jendela berubah; ini warning-only kecuali ada sinyal kuat lain."
                    )
                runtimeSecurity.overlayRiskResult.shieldStatus.supported &&
                    runtimeSecurity.overlayRiskResult.shieldStatus.requested &&
                    runtimeSecurity.overlayRiskResult.shieldStatus.lastApplySucceeded == false -> tr(
                    "Overlay shield failed to apply; floating apps may still appear.",
                    "Overlay shield gagal aktif; floating app masih bisa muncul."
                )
                runtimeSecurity.dpcRuntimeStatus.protectionTier == DpcProtectionTier.DpcOverlayRestrictedWithShield -> tr(
                    "Device Owner restriction and overlay shield are available.",
                    "Restriction Device Owner dan overlay shield tersedia."
                )
                runtimeSecurity.dpcRuntimeStatus.protectionTier == DpcProtectionTier.DpcOverlayRestricted -> tr(
                    "Device Owner can restrict floating windows during the exam.",
                    "Device Owner dapat membatasi floating window saat ujian."
                )
                runtimeSecurity.dpcRuntimeStatus.protectionTier == DpcProtectionTier.LegacyDpcAndroid7 -> tr(
                    "Device Owner Lock Task is available with Android 7 legacy limitation.",
                    "Device Owner Lock Task tersedia dengan batasan legacy Android 7."
                )
                runtimeSecurity.overlayRiskResult.shieldStatus.active -> tr(
                    "Overlay shield is active for this exam session.",
                    "Overlay shield aktif untuk sesi ujian ini."
                )
                runtimeSecurity.dpcRuntimeStatus.protectionTier == DpcProtectionTier.NormalApk -> tr(
                    "Normal APK overlay shield is available on this Android version.",
                    "Overlay shield mode APK biasa tersedia pada versi Android ini."
                )
                runtimeSecurity.dpcRuntimeStatus.protectionTier == DpcProtectionTier.None &&
                    !runtimeSecurity.overlayRiskResult.shieldStatus.supported -> tr(
                    "Legacy Android normal APK cannot fully block floating apps.",
                    "Android legacy mode APK biasa tidak bisa memblokir floating app sepenuhnya."
                )
                runtimeSecurity.overlayRiskResult.riskyAccessibilityPackages.isNotEmpty() -> tr(
                    "Risky accessibility package detected: ${runtimeSecurity.overlayRiskResult.riskyAccessibilityPackages.joinToString()}",
                    "Paket accessibility berisiko terdeteksi: ${runtimeSecurity.overlayRiskResult.riskyAccessibilityPackages.joinToString()}"
                )
                runtimeSecurity.overlayRiskResult.heuristicRisk -> tr(
                    "Accessibility activity may create floating-app risk.",
                    "Aktivitas accessibility dapat menimbulkan risiko floating app."
                )
                else -> tr("No overlay risk detected", "Tidak ada risiko overlay terdeteksi")
            },
            detail = text.overlayDetail,
            status = text.overlayStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.Overlay) },
            isSending = sendingSection == DiagnosticSection.Overlay,
            sendEnabled = sendingSection == null
        )
    }
}

@Composable
internal fun PreparationDeviceIntegritySection(
    device: PreparationDeviceState,
    bypass: PreparationBypassState,
    text: PreparationChecklistText,
    sendingSection: DiagnosticSection?,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    modifier: Modifier = Modifier
) {
    PreparationChecklistSectionSurface(
        sectionName = "device_integrity",
        modifier = modifier
    ) {
        SecurityChecklistItem(
            title = tr("Developer Mode / ADB", "Developer Mode / ADB"),
            value = when {
                device.adbBypassState == AdbBypassState.Tampered -> tr(
                    "Bypass tamper detected; ADB checks stay active",
                    "Tamper bypass terdeteksi; cek ADB tetap aktif"
                )
                bypass.bypassAdb -> tr("Bypass enabled", "Bypass aktif")
                device.adbInspection.developerOptionsEnabled && device.adbInspection.adbEnabled ->
                    tr("Developer mode and USB debugging are enabled", "Mode developer dan USB debugging aktif")
                device.adbInspection.developerOptionsEnabled ->
                    tr("Developer mode is enabled", "Mode developer aktif")
                device.adbInspection.adbEnabled -> tr("USB debugging is enabled", "USB debugging aktif")
                device.adbInspection.insecureSystemProperty -> tr(
                    "ADB security property is unsafe",
                    "Properti keamanan ADB tidak aman"
                )
                else -> tr("Disabled", "Nonaktif")
            },
            detail = text.developerDetail,
            status = text.developerStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.DeveloperAdb) },
            isSending = sendingSection == DiagnosticSection.DeveloperAdb,
            sendEnabled = sendingSection == null
        )
        SecurityChecklistItem(
            title = tr("Root Device", "Root Device"),
            value = when {
                device.rootBypassState == RootBypassState.Tampered -> tr(
                    "Bypass tamper detected; root checks stay active",
                    "Tamper bypass terdeteksi; cek root tetap aktif"
                )
                bypass.bypassRoot -> tr("Bypass enabled", "Bypass aktif")
                device.rootSecurityStatus.detected -> device.rootSecurityStatus.primaryIndicatorLabel
                device.rootSecurityStatus.selinuxPermissive -> tr("SELinux permissive", "SELinux permisif")
                else -> tr("Not detected", "Tidak terdeteksi")
            },
            detail = text.rootDetail,
            status = text.rootStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.Root) },
            isSending = sendingSection == DiagnosticSection.Root,
            sendEnabled = sendingSection == null
        )
        SecurityChecklistItem(
            title = tr("Official APK Signature", "Signature APK Resmi"),
            value = text.signatureValue,
            detail = text.signatureDetail,
            status = text.signatureStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.Signature) },
            isSending = sendingSection == DiagnosticSection.Signature,
            sendEnabled = sendingSection == null
        )
        SecurityChecklistItem(
            title = tr("Virtual Environment", "Virtual Environment"),
            value = if (bypass.bypassVirtualEnvironment) {
                tr("Bypass enabled", "Bypass aktif")
            } else if (device.virtualEnvironmentDetected) {
                tr("Emulator/VM detected", "Emulator/VM terdeteksi")
            } else {
                tr("Not detected", "Tidak terdeteksi")
            },
            detail = text.virtualEnvironmentDetail,
            status = text.virtualEnvironmentStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.VirtualEnvironment) },
            isSending = sendingSection == DiagnosticSection.VirtualEnvironment,
            sendEnabled = sendingSection == null
        )
    }
}

@Composable
internal fun PreparationRuntimeClipboardSection(
    runtimeSecurity: PreparationRuntimeSecurityState,
    bypass: PreparationBypassState,
    text: PreparationChecklistText,
    sendingSection: DiagnosticSection?,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    modifier: Modifier = Modifier
) {
    PreparationChecklistSectionSurface(
        sectionName = "runtime_clipboard",
        modifier = modifier
    ) {
        SecurityChecklistItem(
            title = tr("Clipboard", "Clipboard"),
            value = if (runtimeSecurity.clipboardBypassState == ClipboardBypassState.Tampered) {
                tr(
                    "Bypass tamper detected; clipboard monitoring stays active",
                    "Tamper bypass terdeteksi; monitoring clipboard tetap aktif"
                )
            } else if (runtimeSecurity.clipboardViolationCount > 0) {
                tr(
                    "Clipboard changes were confirmed during the exam",
                    "Perubahan clipboard terkonfirmasi saat ujian"
                )
            } else if (bypass.bypassClipboard) {
                tr("Bypass enabled", "Bypass aktif")
            } else {
                tr(
                    "Clipboard monitoring activates when the exam session starts",
                    "Monitoring clipboard aktif saat sesi ujian dimulai"
                )
            },
            detail = text.clipboardDetail,
            status = when {
                runtimeSecurity.clipboardBypassState == ClipboardBypassState.Tampered ->
                    tr("Warning", "Peringatan")

                bypass.bypassClipboard -> tr("Bypassed", "Bypass")
                runtimeSecurity.clipboardViolationCount > 0 -> tr("Warning", "Peringatan")
                else -> tr("Ready", "Siap")
            },
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.Clipboard) },
            isSending = sendingSection == DiagnosticSection.Clipboard,
            sendEnabled = sendingSection == null
        )
    }
}

@Composable
internal fun PreparationLocationSection(
    location: PreparationLocationState,
    bypass: PreparationBypassState,
    text: PreparationChecklistText,
    sendingSection: DiagnosticSection?,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    modifier: Modifier = Modifier
) {
    PreparationChecklistSectionSurface(
        sectionName = "location",
        modifier = modifier
    ) {
        SecurityChecklistItem(
            title = tr("Geofence", "Geofence"),
            value = when {
                location.geofenceBypassState == GeofenceBypassState.Tampered -> tr(
                    "Bypass seal mismatch was detected. Geofence enforcement stays active.",
                    "Seal bypass tidak cocok terdeteksi. Enforcement geofence tetap aktif."
                )
                bypass.bypassGeofence -> tr(
                    "Exam-area position checks are bypassed by admin.",
                    "Pemeriksaan posisi area ujian dibypass oleh admin."
                )
                !location.geofenceRuntimeStatus.evaluation.enabled -> tr(
                    "This exam policy does not require a geofence.",
                    "Policy ujian ini tidak mewajibkan geofence."
                )
                location.geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.ConfigInvalid -> tr(
                    "The geofence policy from QR or Direct Link is invalid.",
                    "Policy geofence dari QR atau Direct Link tidak valid."
                )
                location.geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.PermissionMissing -> tr(
                    "Location permission is still missing for geofence validation.",
                    "Izin lokasi masih kurang untuk validasi geofence."
                )
                location.geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.PreciseRequired -> tr(
                    "Precise location is required to validate the exam area.",
                    "Lokasi presisi diperlukan untuk memvalidasi area ujian."
                )
                location.geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LocationDisabled -> tr(
                    "Location services are off, so the exam area cannot be validated.",
                    "Layanan lokasi mati sehingga area ujian tidak bisa divalidasi."
                )
                location.geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.NoFix -> tr(
                    "A location fix is not available yet.",
                    "Fix lokasi belum tersedia."
                )
                location.geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.StaleFix -> tr(
                    "The last location fix is too old to validate the exam area reliably. Refresh the location first.",
                    "Fix lokasi terakhir terlalu lama untuk memvalidasi area ujian dengan andal. Refresh lokasi terlebih dahulu."
                )
                location.geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LowAccuracy -> tr(
                    "The current location accuracy is still too weak for strict geofence validation. Move to a more open area, then refresh location.",
                    "Akurasi lokasi saat ini masih terlalu lemah untuk validasi geofence ketat. Pindah ke area yang lebih terbuka lalu refresh lokasi."
                )
                location.geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.MissingAccuracy -> tr(
                    "The current location fix has no usable accuracy value yet. Refresh location to get a better fix.",
                    "Fix lokasi saat ini belum punya nilai akurasi yang bisa dipakai. Refresh lokasi untuk mendapatkan fix yang lebih baik."
                )
                location.geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Outside -> tr(
                    "This device is currently outside the allowed exam area. Open the geofence map to compare the current position with the exam area.",
                    "Perangkat ini saat ini berada di luar area ujian yang diizinkan. Buka peta geofence untuk membandingkan posisi saat ini dengan area ujian."
                )
                else -> tr(
                    "This device is inside the configured exam area.",
                    "Perangkat ini berada di dalam area ujian yang dikonfigurasi."
                )
            },
            meta = text.geofenceMeta,
            detail = text.geofenceDetail,
            status = text.geofenceStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.Geofence) },
            isSending = sendingSection == DiagnosticSection.Geofence,
            sendEnabled = sendingSection == null
        )
        SecurityChecklistItem(
            title = tr("Anti-Fake-Location", "Anti-Fake-Location"),
            value = when {
                location.fakeLocationBypassState == FakeLocationBypassState.Tampered -> tr(
                    "Bypass seal mismatch was detected. Anti-fake-location stays active.",
                    "Seal bypass tidak cocok terdeteksi. Anti-fake-location tetap aktif."
                )
                bypass.bypassFakeLocation -> tr(
                    "Mock-location and fake GPS checks are bypassed by admin.",
                    "Pemeriksaan mock-location dan fake GPS dibypass oleh admin."
                )
                location.fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired -> tr(
                    "Location permission is required before anti-fake-location can validate this exam.",
                    "Izin lokasi wajib tersedia sebelum anti-fake-location bisa memvalidasi ujian ini."
                )
                location.fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled -> tr(
                    "Location services must be turned on before anti-fake-location can validate this exam.",
                    "Layanan lokasi harus diaktifkan sebelum anti-fake-location bisa memvalidasi ujian ini."
                )
                location.fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable -> tr(
                    "Anti-fake-location is still waiting for a usable location snapshot.",
                    "Anti-fake-location masih menunggu snapshot lokasi yang bisa dipakai."
                )
                !location.fakeLocationRuntimeStatus.securityStatus.monitoringEnabled -> tr(
                    "Anti-fake-location monitoring is currently inactive for this exam.",
                    "Monitoring anti-fake-location saat ini nonaktif untuk ujian ini."
                )
                location.fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Critical -> tr(
                    "Critical fake-location confidence was reached from combined spoof signals.",
                    "Confidence fake-location kritis tercapai dari kombinasi sinyal spoof."
                )
                location.fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Strong -> tr(
                    "Strong mock-location or fake GPS signals were detected.",
                    "Terdeteksi sinyal mock-location atau fake GPS yang kuat."
                )
                location.fakeLocationRuntimeStatus.securityStatus.warningOnly -> tr(
                    "A suspicious fake-location app was found, but no strong spoof signal yet.",
                    "Aplikasi fake-location mencurigakan ditemukan, tetapi belum ada sinyal spoof kuat."
                )
                else -> tr(
                    "No strong fake-location signal is currently detected.",
                    "Saat ini tidak ada sinyal fake-location kuat yang terdeteksi."
                )
            },
            detail = text.fakeLocationDetail,
            status = text.fakeLocationStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.FakeLocation) },
            isSending = sendingSection == DiagnosticSection.FakeLocation,
            sendEnabled = sendingSection == null
        )
    }
}

@Composable
internal fun PreparationDeviceLockSection(
    device: PreparationDeviceState,
    bypass: PreparationBypassState,
    text: PreparationChecklistText,
    sendingSection: DiagnosticSection?,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardRequired: Boolean,
    accessibilityGuardEnabled: Boolean,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    modifier: Modifier = Modifier
) {
    PreparationChecklistSectionSurface(
        sectionName = "device_lock",
        modifier = modifier
    ) {
        SecurityChecklistItem(
            title = tr("Screen Pinning", "Screen Pinning"),
            value = if (bypass.bypassScreenPinning) {
                tr("Bypass enabled", "Bypass aktif")
            } else if (device.isScreenPinningActive) {
                tr("Already active", "Sudah aktif")
            } else if (!device.screenPinningAvailable) {
                tr(
                    "Screen Pinning is not available on this device. Use another supported device, or ask Secret Admin to enable Screen Pinning bypass.",
                    "Screen Pinning tidak tersedia di perangkat ini. Gunakan perangkat lain yang mendukung, atau minta Secret Admin mengaktifkan bypass Screen Pinning."
                )
            } else if (device.screenPinningFixNeeded) {
                tr(
                    "Screen Pinning is available but not active yet. Press Start Screen Pinning first; if Android still does not show the pinning dialog, use the settings fallback.",
                    "Screen Pinning tersedia tetapi belum aktif. Tekan Start Screen Pinning dulu; jika Android tetap tidak memunculkan dialog pinning, gunakan fallback pengaturan."
                )
            } else if (device.screenPinningAvailable) {
                tr(
                    "Screen Pinning is available but not active yet. Press Start Screen Pinning, confirm the Android dialog, then Start Exam will become available.",
                    "Screen Pinning tersedia tetapi belum aktif. Tekan Start Screen Pinning, konfirmasi dialog Android, lalu tombol Mulai Ujian akan tersedia."
                )
            } else {
                tr(
                    "Screen Pinning status is not ready yet.",
                    "Status Screen Pinning belum siap."
                )
            },
            detail = text.screenPinningDetail,
            status = text.screenPinningStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.ScreenPinning) },
            isSending = sendingSection == DiagnosticSection.ScreenPinning,
            sendEnabled = sendingSection == null
        )
        if (!device.screenPinningAvailable && accessibilityGuardAvailable) {
            SecurityChecklistItem(
                title = tr("Accessibility Exam Guard", "Accessibility Exam Guard"),
                value = when {
                    bypass.bypassScreenPinning -> tr(
                        "Screen Pinning bypass is active, so this fallback guard is not required.",
                        "Bypass Screen Pinning aktif, jadi guard fallback ini tidak wajib."
                    )
                    accessibilityGuardRequired && accessibilityGuardEnabled -> tr(
                        "Required fallback is enabled for this device.",
                        "Fallback wajib sudah aktif untuk perangkat ini."
                    )
                    accessibilityGuardRequired -> tr(
                        "Enable CBX Lock Exam Guard in Accessibility Settings before starting.",
                        "Aktifkan CBX Lock Exam Guard di Pengaturan Aksesibilitas sebelum mulai."
                    )
                    else -> tr(
                        "This fallback guard is only needed when Screen Pinning is unavailable.",
                        "Guard fallback ini hanya diperlukan saat Screen Pinning tidak tersedia."
                    )
                },
                detail = text.accessibilityGuardDetail,
                status = text.accessibilityGuardStatusLabel,
                onSendTelegram = { onRequestSectionReport(DiagnosticSection.AppSwitch) },
                isSending = sendingSection == DiagnosticSection.AppSwitch,
                sendEnabled = sendingSection == null
            )
        }
    }
}

@Composable
internal fun PreparationRuntimeStaticSecuritySection(
    runtimeSecurity: PreparationRuntimeSecurityState,
    bypass: PreparationBypassState,
    text: PreparationChecklistText,
    sendingSection: DiagnosticSection?,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    modifier: Modifier = Modifier
) {
    PreparationChecklistSectionSurface(
        sectionName = "runtime_static_security",
        modifier = modifier
    ) {
        SecurityChecklistItem(
            title = tr("Screen Recorder", "Screen Recorder"),
            value = when {
                bypass.bypassScreenRecorder -> tr("Bypass enabled", "Bypass aktif")
                runtimeSecurity.screenRecorderPackages.isNotEmpty() -> tr(
                    "Detected: ${runtimeSecurity.screenRecorderPackages.size} app(s)",
                    "Terdeteksi: ${runtimeSecurity.screenRecorderPackages.size} aplikasi"
                )
                else -> tr("No screen recorder apps detected", "Tidak ada app screen recorder terdeteksi")
            },
            detail = text.screenRecorderDetail,
            status = when {
                bypass.bypassScreenRecorder -> tr("Bypassed", "Dibypass")
                runtimeSecurity.screenRecorderPackages.isNotEmpty() -> tr("Not Ready", "Belum Siap")
                else -> tr("Ready", "Siap")
            },
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.ScreenRecorder) },
            isSending = sendingSection == DiagnosticSection.ScreenRecorder,
            sendEnabled = sendingSection == null
        )
        SecurityChecklistItem(
            title = tr("Display Mirror", "Display Mirror"),
            value = when {
                bypass.bypassDisplayMirror -> tr("Bypass enabled", "Bypass aktif")
                runtimeSecurity.externalDisplayDetected -> tr(
                    "External display detected",
                    "Display eksternal terdeteksi"
                )
                else -> tr("No external display connected", "Tidak ada display eksternal terhubung")
            },
            detail = text.displayMirrorDetail,
            status = when {
                bypass.bypassDisplayMirror -> tr("Bypassed", "Dibypass")
                runtimeSecurity.externalDisplayDetected -> tr("Not Ready", "Belum Siap")
                else -> tr("Ready", "Siap")
            },
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.DisplayMirror) },
            isSending = sendingSection == DiagnosticSection.DisplayMirror,
            sendEnabled = sendingSection == null
        )
        SecurityChecklistItem(
            title = tr("Multi-Window", "Multi-Window"),
            value = when {
                bypass.bypassMultiWindow -> tr("Bypass enabled", "Bypass aktif")
                runtimeSecurity.multiWindowDetected -> tr(
                    "Split-screen or PiP mode active",
                    "Mode split-screen atau PiP aktif"
                )
                else -> tr("Normal single-app mode", "Mode single-app normal")
            },
            detail = text.multiWindowDetail,
            status = when {
                bypass.bypassMultiWindow -> tr("Bypassed", "Dibypass")
                runtimeSecurity.multiWindowDetected -> tr("Not Ready", "Belum Siap")
                else -> tr("Ready", "Siap")
            },
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.MultiWindow) },
            isSending = sendingSection == DiagnosticSection.MultiWindow,
            sendEnabled = sendingSection == null
        )
        SecurityChecklistItem(
            title = tr("App Switch", "App Switch"),
            value = when {
                bypass.bypassAppSwitch -> tr("Bypass enabled", "Bypass aktif")
                runtimeSecurity.appSwitchStatus.hasViolations -> tr(
                    "App switch violations recorded: ${runtimeSecurity.appSwitchStatus.violationCount}",
                    "Pelanggaran App Switch tercatat: ${runtimeSecurity.appSwitchStatus.violationCount}"
                )
                runtimeSecurity.appSwitchStatus.fallbackGuardActive -> tr(
                    "Fallback guard is active because screen pinning is bypassed or inactive.",
                    "Fallback guard aktif karena screen pinning dibypass atau tidak aktif."
                )
                runtimeSecurity.appSwitchStatus.runtimeMonitoringActive -> tr(
                    "Monitoring is active for this exam session.",
                    "Monitoring aktif untuk sesi ujian ini."
                )
                else -> tr(
                    "Monitoring will activate when the exam session starts.",
                    "Monitoring akan aktif saat sesi ujian dimulai."
                )
            },
            detail = text.appSwitchDetail,
            status = text.appSwitchStatusLabel,
            onSendTelegram = { onRequestSectionReport(DiagnosticSection.AppSwitch) },
            isSending = sendingSection == DiagnosticSection.AppSwitch,
            sendEnabled = sendingSection == null
        )
    }
}

@Composable
internal fun PreparationChecklistItemsCard(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    text: PreparationChecklistText,
    needsBluetoothPermission: Boolean,
    accessibilityInspection: AccessibilityInspectionResult,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardRequired: Boolean,
    accessibilityGuardEnabled: Boolean,
    checklistTitle: String,
    checklistSubtitle: String,
    telegramHelperText: String
) {
    PreparationChecklistIntroItem(
        checklistTitle = checklistTitle,
        checklistSubtitle = checklistSubtitle,
        telegramHelperText = telegramHelperText
    )
    PreparationDeviceSetupSection(
        device = state.device,
        bypass = state.bypass,
        text = text,
        sendingSection = state.session.sendingSection,
        needsBluetoothPermission = needsBluetoothPermission,
        onRequestSectionReport = actions.session.onRequestSectionReport
    )
    PreparationConnectivitySection(
        text = text,
        sendingSection = state.session.sendingSection,
        onRequestSectionReport = actions.session.onRequestSectionReport
    )
    PreparationDeviceHealthSection(
        device = state.device,
        bypass = state.bypass,
        text = text,
        sendingSection = state.session.sendingSection,
        onRequestSectionReport = actions.session.onRequestSectionReport
    )
    PreparationRuntimeInteractionSection(
        runtimeSecurity = state.runtimeSecurity,
        bypass = state.bypass,
        text = text,
        sendingSection = state.session.sendingSection,
        accessibilityInspection = accessibilityInspection,
        onRequestSectionReport = actions.session.onRequestSectionReport
    )
    PreparationDeviceIntegritySection(
        device = state.device,
        bypass = state.bypass,
        text = text,
        sendingSection = state.session.sendingSection,
        onRequestSectionReport = actions.session.onRequestSectionReport
    )
    PreparationRuntimeClipboardSection(
        runtimeSecurity = state.runtimeSecurity,
        bypass = state.bypass,
        text = text,
        sendingSection = state.session.sendingSection,
        onRequestSectionReport = actions.session.onRequestSectionReport
    )
    PreparationLocationSection(
        location = state.location,
        bypass = state.bypass,
        text = text,
        sendingSection = state.session.sendingSection,
        onRequestSectionReport = actions.session.onRequestSectionReport
    )
    PreparationDeviceLockSection(
        device = state.device,
        bypass = state.bypass,
        text = text,
        sendingSection = state.session.sendingSection,
        accessibilityGuardAvailable = accessibilityGuardAvailable,
        accessibilityGuardRequired = accessibilityGuardRequired,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        onRequestSectionReport = actions.session.onRequestSectionReport
    )
    PreparationRuntimeStaticSecuritySection(
        runtimeSecurity = state.runtimeSecurity,
        bypass = state.bypass,
        text = text,
        sendingSection = state.session.sendingSection,
        onRequestSectionReport = actions.session.onRequestSectionReport
    )
}
