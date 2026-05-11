package com.example.coblaxexamlock.ui.preparation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextMuted
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary

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
    with(state) {
        with(actions) {
            val accessibilityStatusLabel = text.accessibilityStatusLabel
            val overlayStatusLabel = text.overlayStatusLabel
            val geofenceStatusLabel = text.geofenceStatusLabel
            val geofenceMeta = text.geofenceMeta
            val fakeLocationStatusLabel = text.fakeLocationStatusLabel
            val deviceTimeStatusLabel = text.deviceTimeStatusLabel
            val networkStatusLabel = text.networkStatusLabel
            val networkValue = text.networkValue
            val networkMeta = text.networkMeta
            val networkDetail = text.networkDetail
            val webViewProviderStatusLabel = text.webViewProviderStatusLabel
            val webViewProviderValue = text.webViewProviderValue
            val webViewProviderDetail = text.webViewProviderDetail
            val deviceTimeDetail = text.deviceTimeDetail
            val bluetoothStatusLabel = text.bluetoothStatusLabel
            val developerStatusLabel = text.developerStatusLabel
            val keyboardStatusLabel = text.keyboardStatusLabel
            val rootStatusLabel = text.rootStatusLabel
            val signatureStatusLabel = text.signatureStatusLabel
            val signatureValue = text.signatureValue
            val virtualEnvironmentStatusLabel = text.virtualEnvironmentStatusLabel
            val screenPinningStatusLabel = text.screenPinningStatusLabel
            val accessibilityGuardStatusLabel = text.accessibilityGuardStatusLabel
            val appSwitchStatusLabel = text.appSwitchStatusLabel
            val keyboardDetail = text.keyboardDetail
            val bluetoothDetail = text.bluetoothDetail
            val accessibilityDetail = text.accessibilityDetail
            val overlayDetail = text.overlayDetail
            val developerDetail = text.developerDetail
            val rootDetail = text.rootDetail
            val signatureDetail = text.signatureDetail
            val virtualEnvironmentDetail = text.virtualEnvironmentDetail
            val clipboardDetail = text.clipboardDetail
            val geofenceDetail = text.geofenceDetail
            val fakeLocationDetail = text.fakeLocationDetail
            val screenPinningDetail = text.screenPinningDetail
            val accessibilityGuardDetail = text.accessibilityGuardDetail
            val screenRecorderDetail = text.screenRecorderDetail
            val displayMirrorDetail = text.displayMirrorDetail
            val multiWindowDetail = text.multiWindowDetail
            val appSwitchDetail = text.appSwitchDetail
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = LockSurfaceSoft,
            border = BorderStroke(1.dp, LockOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.75f))
                ) {
                    Text(
                        text = telegramHelperText,
                        color = LockTextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                SecurityChecklistItem(
                    title = tr("Safe Keyboard", "Keyboard Aman"),
                    value = when {
                        bypassKeyboardPolicy -> tr("Bypass enabled", "Bypass aktif")
                        usingBuiltInExamKeyboard -> "internal.coblax.exam"
                        else -> keyboardPackage.ifBlank { tr("Not detected", "Tidak terdeteksi") }
                    },
                    detail = keyboardDetail,
                    status = keyboardStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Keyboard) },
                    isSending = sendingSection == DiagnosticSection.Keyboard,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Bluetooth", "Bluetooth"),
                    value = when {
                        bypassBluetooth -> tr("Bypass enabled", "Bypass aktif")
                        needsBluetoothPermission && !bluetoothPermissionGranted ->
                            tr("Bluetooth permission has not been granted.", "Izin Bluetooth belum diberikan")
                        bluetoothEnabled -> tr("Still enabled", "Masih aktif")
                        else -> tr("Disabled", "Nonaktif")
                    },
                    detail = bluetoothDetail,
                    status = bluetoothStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Bluetooth) },
                    isSending = sendingSection == DiagnosticSection.Bluetooth,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Network / Connectivity", "Network / Konektivitas"),
                    value = networkValue,
                    meta = networkMeta,
                    detail = networkDetail,
                    status = networkStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Network) },
                    isSending = sendingSection == DiagnosticSection.Network,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("WebView Provider", "WebView Provider"),
                    value = webViewProviderValue,
                    detail = webViewProviderDetail,
                    status = webViewProviderStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.SecurityHealth) },
                    isSending = sendingSection == DiagnosticSection.SecurityHealth,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Device Time", "Waktu Perangkat"),
                    value = when {
                        deviceTimeBypassState == DeviceTimeBypassState.Tampered -> tr(
                            "Bypass storage tamper detected. Device Time enforcement remains active.",
                            "Tamper pada storage bypass terdeteksi. Enforcement Waktu Perangkat tetap aktif."
                        )
                        bypassDeviceTime -> tr(
                            "Bypass active. Device Time checks are skipped.",
                            "Bypass aktif. Cek Waktu Perangkat dilewati."
                        )
                        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.Safe -> tr(
                            "Automatic date & time and automatic time zone are enabled.",
                            "Tanggal & waktu otomatis dan zona waktu otomatis aktif."
                        )
                        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> tr(
                            "Automatic date & time is off.",
                            "Tanggal & waktu otomatis nonaktif."
                        )
                        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> tr(
                            "Automatic time zone is off.",
                            "Zona waktu otomatis nonaktif."
                        )
                        else -> tr(
                            "A suspicious clock change was detected.",
                            "Terdeteksi perubahan jam yang mencurigakan."
                        )
                    },
                    detail = deviceTimeDetail,
                    status = deviceTimeStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.DeviceTime) },
                    isSending = sendingSection == DiagnosticSection.DeviceTime,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Accessibility Service", "Accessibility Service"),
                    value = when {
                        bypassAccessibility -> tr("Bypass enabled", "Bypass aktif")
                        accessibilityInspection.allowedOnlyActive -> tr(
                            "Allowed service active: ${accessibilityInspection.allowedPackages.joinToString().ifBlank { "-" }}",
                            "Service yang diizinkan aktif: ${accessibilityInspection.allowedPackages.joinToString().ifBlank { "-" }}"
                        )
                        accessibilityServiceEnabled -> tr(
                            "Detected as active on this device",
                            "Terdeteksi aktif di perangkat"
                        )
                        else -> tr("Inactive", "Tidak aktif")
                    },
                    detail = accessibilityDetail,
                    status = accessibilityStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Accessibility) },
                    isSending = sendingSection == DiagnosticSection.Accessibility,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Overlay / Floating App", "Overlay / Floating App"),
                    value = when {
                        bypassOverlay -> tr("Bypass enabled", "Bypass aktif")
                        overlayRiskResult.lastTrigger == OverlaySignal.WindowFocusLoss.diagnosticLabel() ->
                            tr(
                                "A floating app likely stole exam-window focus.",
                                "Floating app kemungkinan mengambil fokus jendela ujian."
                            )
                        overlayRiskResult.confirmedInteractionDetected -> tr(
                            "Overlay interaction was confirmed on the exam screen.",
                            "Interaksi overlay terkonfirmasi di layar ujian."
                        )
                        overlayRiskResult.shieldStatus.active -> tr(
                            "Overlay shield is active for this exam session.",
                            "Overlay shield aktif untuk sesi ujian ini."
                        )
                        overlayRiskResult.riskyAccessibilityPackages.isNotEmpty() -> tr(
                            "Risky accessibility package detected: ${overlayRiskResult.riskyAccessibilityPackages.joinToString()}",
                            "Paket accessibility berisiko terdeteksi: ${overlayRiskResult.riskyAccessibilityPackages.joinToString()}"
                        )
                        overlayRiskResult.heuristicRisk -> tr(
                            "Accessibility activity may create floating-app risk.",
                            "Aktivitas accessibility dapat menimbulkan risiko floating app."
                        )
                        else -> tr("No overlay risk detected", "Tidak ada risiko overlay terdeteksi")
                    },
                    detail = overlayDetail,
                    status = overlayStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Overlay) },
                    isSending = sendingSection == DiagnosticSection.Overlay,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Developer Mode / ADB", "Developer Mode / ADB"),
                    value = when {
                        adbBypassState == AdbBypassState.Tampered -> tr(
                            "Bypass tamper detected; ADB checks stay active",
                            "Tamper bypass terdeteksi; cek ADB tetap aktif"
                        )
                        bypassAdb -> tr("Bypass enabled", "Bypass aktif")
                        adbInspection.developerOptionsEnabled && adbInspection.adbEnabled ->
                            tr("Developer mode and USB debugging are enabled", "Mode developer dan USB debugging aktif")
                        adbInspection.developerOptionsEnabled ->
                            tr("Developer mode is enabled", "Mode developer aktif")
                        adbInspection.adbEnabled -> tr("USB debugging is enabled", "USB debugging aktif")
                        adbInspection.insecureSystemProperty -> tr(
                            "ADB security property is unsafe",
                            "Properti keamanan ADB tidak aman"
                        )
                        else -> tr("Disabled", "Nonaktif")
                    },
                    detail = developerDetail,
                    status = developerStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.DeveloperAdb) },
                    isSending = sendingSection == DiagnosticSection.DeveloperAdb,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Root Device", "Root Device"),
                    value = when {
                        rootBypassState == RootBypassState.Tampered -> tr(
                            "Bypass tamper detected; root checks stay active",
                            "Tamper bypass terdeteksi; cek root tetap aktif"
                        )
                        bypassRoot -> tr("Bypass enabled", "Bypass aktif")
                        rootSecurityStatus.detected -> rootSecurityStatus.primaryIndicatorLabel
                        rootSecurityStatus.selinuxPermissive -> tr("SELinux permissive", "SELinux permisif")
                        else -> tr("Not detected", "Tidak terdeteksi")
                    },
                    detail = rootDetail,
                    status = rootStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Root) },
                    isSending = sendingSection == DiagnosticSection.Root,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Official APK Signature", "Signature APK Resmi"),
                    value = signatureValue,
                    detail = signatureDetail,
                    status = signatureStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Signature) },
                    isSending = sendingSection == DiagnosticSection.Signature,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Virtual Environment", "Virtual Environment"),
                    value = if (bypassVirtualEnvironment) {
                        tr("Bypass enabled", "Bypass aktif")
                    } else if (virtualEnvironmentDetected) {
                        tr("Emulator/VM detected", "Emulator/VM terdeteksi")
                    } else {
                        tr("Not detected", "Tidak terdeteksi")
                    },
                    detail = virtualEnvironmentDetail,
                    status = virtualEnvironmentStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.VirtualEnvironment) },
                    isSending = sendingSection == DiagnosticSection.VirtualEnvironment,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Clipboard", "Clipboard"),
                    value = if (clipboardBypassState == ClipboardBypassState.Tampered) {
                        tr(
                            "Bypass tamper detected; clipboard monitoring stays active",
                            "Tamper bypass terdeteksi; monitoring clipboard tetap aktif"
                        )
                    } else if (clipboardViolationCount > 0) {
                        tr(
                            "Clipboard changes were confirmed during the exam",
                            "Perubahan clipboard terkonfirmasi saat ujian"
                        )
                    } else if (bypassClipboard) {
                        tr("Bypass enabled", "Bypass aktif")
                    } else {
                        tr(
                            "Clipboard monitoring activates when the exam session starts",
                            "Monitoring clipboard aktif saat sesi ujian dimulai"
                        )
                    },
                    detail = clipboardDetail,
                    status = when {
                        clipboardBypassState == ClipboardBypassState.Tampered ->
                            tr("Warning", "Peringatan")

                        bypassClipboard -> tr("Bypassed", "Bypass")
                        clipboardViolationCount > 0 -> tr("Warning", "Peringatan")
                        else -> tr("Ready", "Siap")
                    },
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Clipboard) },
                    isSending = sendingSection == DiagnosticSection.Clipboard,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Geofence", "Geofence"),
                    value = when {
                        geofenceBypassState == GeofenceBypassState.Tampered -> tr(
                            "Bypass seal mismatch was detected. Geofence enforcement stays active.",
                            "Seal bypass tidak cocok terdeteksi. Enforcement geofence tetap aktif."
                        )
                        bypassGeofence -> tr(
                            "Exam-area position checks are bypassed by admin.",
                            "Pemeriksaan posisi area ujian dibypass oleh admin."
                        )
                        !geofenceRuntimeStatus.evaluation.enabled -> tr(
                            "This exam policy does not require a geofence.",
                            "Policy ujian ini tidak mewajibkan geofence."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.ConfigInvalid -> tr(
                            "The geofence policy from QR or Direct Link is invalid.",
                            "Policy geofence dari QR atau Direct Link tidak valid."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.PermissionMissing -> tr(
                            "Location permission is still missing for geofence validation.",
                            "Izin lokasi masih kurang untuk validasi geofence."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.PreciseRequired -> tr(
                            "Precise location is required to validate the exam area.",
                            "Lokasi presisi diperlukan untuk memvalidasi area ujian."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LocationDisabled -> tr(
                            "Location services are off, so the exam area cannot be validated.",
                            "Layanan lokasi mati sehingga area ujian tidak bisa divalidasi."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.NoFix -> tr(
                            "A location fix is not available yet.",
                            "Fix lokasi belum tersedia."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.StaleFix -> tr(
                            "The last location fix is too old to validate the exam area reliably. Refresh the location first.",
                            "Fix lokasi terakhir terlalu lama untuk memvalidasi area ujian dengan andal. Refresh lokasi terlebih dahulu."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LowAccuracy -> tr(
                            "The current location accuracy is still too weak for strict geofence validation. Move to a more open area, then refresh location.",
                            "Akurasi lokasi saat ini masih terlalu lemah untuk validasi geofence ketat. Pindah ke area yang lebih terbuka lalu refresh lokasi."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.MissingAccuracy -> tr(
                            "The current location fix has no usable accuracy value yet. Refresh location to get a better fix.",
                            "Fix lokasi saat ini belum punya nilai akurasi yang bisa dipakai. Refresh lokasi untuk mendapatkan fix yang lebih baik."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Outside -> tr(
                            "This device is currently outside the allowed exam area. Open the geofence map to compare the current position with the exam area.",
                            "Perangkat ini saat ini berada di luar area ujian yang diizinkan. Buka peta geofence untuk membandingkan posisi saat ini dengan area ujian."
                        )
                        else -> tr(
                            "This device is inside the configured exam area.",
                            "Perangkat ini berada di dalam area ujian yang dikonfigurasi."
                        )
                    },
                    meta = geofenceMeta,
                    detail = geofenceDetail,
                    status = geofenceStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Geofence) },
                    isSending = sendingSection == DiagnosticSection.Geofence,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Anti-Fake-Location", "Anti-Fake-Location"),
                    value = when {
                        fakeLocationBypassState == FakeLocationBypassState.Tampered -> tr(
                            "Bypass seal mismatch was detected. Anti-fake-location stays active.",
                            "Seal bypass tidak cocok terdeteksi. Anti-fake-location tetap aktif."
                        )
                        bypassFakeLocation -> tr(
                            "Mock-location and fake GPS checks are bypassed by admin.",
                            "Pemeriksaan mock-location dan fake GPS dibypass oleh admin."
                        )
                        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired -> tr(
                            "Location permission is required before anti-fake-location can validate this exam.",
                            "Izin lokasi wajib tersedia sebelum anti-fake-location bisa memvalidasi ujian ini."
                        )
                        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled -> tr(
                            "Location services must be turned on before anti-fake-location can validate this exam.",
                            "Layanan lokasi harus diaktifkan sebelum anti-fake-location bisa memvalidasi ujian ini."
                        )
                        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable -> tr(
                            "Anti-fake-location is still waiting for a usable location snapshot.",
                            "Anti-fake-location masih menunggu snapshot lokasi yang bisa dipakai."
                        )
                        !fakeLocationRuntimeStatus.securityStatus.monitoringEnabled -> tr(
                            "Anti-fake-location monitoring is currently inactive for this exam.",
                            "Monitoring anti-fake-location saat ini nonaktif untuk ujian ini."
                        )
                        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Critical -> tr(
                            "Critical fake-location confidence was reached from combined spoof signals.",
                            "Confidence fake-location kritis tercapai dari kombinasi sinyal spoof."
                        )
                        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Strong -> tr(
                            "Strong mock-location or fake GPS signals were detected.",
                            "Terdeteksi sinyal mock-location atau fake GPS yang kuat."
                        )
                        fakeLocationRuntimeStatus.securityStatus.warningOnly -> tr(
                            "A suspicious fake-location app was found, but no strong spoof signal yet.",
                            "Aplikasi fake-location mencurigakan ditemukan, tetapi belum ada sinyal spoof kuat."
                        )
                        else -> tr(
                            "No strong fake-location signal is currently detected.",
                            "Saat ini tidak ada sinyal fake-location kuat yang terdeteksi."
                        )
                    },
                    detail = fakeLocationDetail,
                    status = fakeLocationStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.FakeLocation) },
                    isSending = sendingSection == DiagnosticSection.FakeLocation,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Screen Pinning", "Screen Pinning"),
                    value = if (bypassScreenPinning) {
                        tr("Bypass enabled", "Bypass aktif")
                    } else if (isScreenPinningActive) {
                        tr("Already active", "Sudah aktif")
                    } else if (!screenPinningAvailable) {
                        tr(
                            "Screen Pinning is not available on this device. Use another supported device, or ask Secret Admin to enable Screen Pinning bypass.",
                            "Screen Pinning tidak tersedia di perangkat ini. Gunakan perangkat lain yang mendukung, atau minta Secret Admin mengaktifkan bypass Screen Pinning."
                        )
                    } else if (screenPinningFixNeeded) {
                        tr(
                            "Screen Pinning is available but not active yet. Press Start Screen Pinning first; if Android still does not show the pinning dialog, use the settings fallback.",
                            "Screen Pinning tersedia tetapi belum aktif. Tekan Start Screen Pinning dulu; jika Android tetap tidak memunculkan dialog pinning, gunakan fallback pengaturan."
                        )
                    } else if (screenPinningAvailable) {
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
                    detail = screenPinningDetail,
                    status = screenPinningStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.ScreenPinning) },
                    isSending = sendingSection == DiagnosticSection.ScreenPinning,
                    sendEnabled = sendingSection == null
                )
                if (!screenPinningAvailable && accessibilityGuardAvailable) {
                    SecurityChecklistItem(
                        title = tr("Accessibility Exam Guard", "Accessibility Exam Guard"),
                        value = when {
                            bypassScreenPinning -> tr(
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
                        detail = accessibilityGuardDetail,
                        status = accessibilityGuardStatusLabel,
                        onSendTelegram = { onRequestSectionReport(DiagnosticSection.AppSwitch) },
                        isSending = sendingSection == DiagnosticSection.AppSwitch,
                        sendEnabled = sendingSection == null
                    )
                }

                SecurityChecklistItem(
                    title = tr("Screen Recorder", "Screen Recorder"),
                    value = when {
                        bypassScreenRecorder -> tr("Bypass enabled", "Bypass aktif")
                        screenRecorderPackages.isNotEmpty() -> tr(
                            "Detected: ${screenRecorderPackages.size} app(s)",
                            "Terdeteksi: ${screenRecorderPackages.size} aplikasi"
                        )
                        else -> tr("No screen recorder apps detected", "Tidak ada app screen recorder terdeteksi")
                    },
                    detail = screenRecorderDetail,
                    status = when {
                        bypassScreenRecorder -> tr("Bypassed", "Dibypass")
                        screenRecorderPackages.isNotEmpty() -> tr("Not Ready", "Belum Siap")
                        else -> tr("Ready", "Siap")
                    },
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.ScreenRecorder) },
                    isSending = sendingSection == DiagnosticSection.ScreenRecorder,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Display Mirror", "Display Mirror"),
                    value = when {
                        bypassDisplayMirror -> tr("Bypass enabled", "Bypass aktif")
                        externalDisplayDetected -> tr(
                            "External display detected",
                            "Display eksternal terdeteksi"
                        )
                        else -> tr("No external display connected", "Tidak ada display eksternal terhubung")
                    },
                    detail = displayMirrorDetail,
                    status = when {
                        bypassDisplayMirror -> tr("Bypassed", "Dibypass")
                        externalDisplayDetected -> tr("Not Ready", "Belum Siap")
                        else -> tr("Ready", "Siap")
                    },
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.DisplayMirror) },
                    isSending = sendingSection == DiagnosticSection.DisplayMirror,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Multi-Window", "Multi-Window"),
                    value = when {
                        bypassMultiWindow -> tr("Bypass enabled", "Bypass aktif")
                        multiWindowDetected -> tr(
                            "Split-screen or PiP mode active",
                            "Mode split-screen atau PiP aktif"
                        )
                        else -> tr("Normal single-app mode", "Mode single-app normal")
                    },
                    detail = multiWindowDetail,
                    status = when {
                        bypassMultiWindow -> tr("Bypassed", "Dibypass")
                        multiWindowDetected -> tr("Not Ready", "Belum Siap")
                        else -> tr("Ready", "Siap")
                    },
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.MultiWindow) },
                    isSending = sendingSection == DiagnosticSection.MultiWindow,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("App Switch", "App Switch"),
                    value = when {
                        bypassAppSwitch -> tr("Bypass enabled", "Bypass aktif")
                        appSwitchStatus.hasViolations -> tr(
                            "App switch violations recorded: ${appSwitchStatus.violationCount}",
                            "Pelanggaran App Switch tercatat: ${appSwitchStatus.violationCount}"
                        )
                        appSwitchStatus.fallbackGuardActive -> tr(
                            "Fallback guard is active because screen pinning is bypassed or inactive.",
                            "Fallback guard aktif karena screen pinning dibypass atau tidak aktif."
                        )
                        appSwitchStatus.runtimeMonitoringActive -> tr(
                            "Monitoring is active for this exam session.",
                            "Monitoring aktif untuk sesi ujian ini."
                        )
                        else -> tr(
                            "Monitoring will activate when the exam session starts.",
                            "Monitoring akan aktif saat sesi ujian dimulai."
                        )
                    },
                    detail = appSwitchDetail,
                    status = appSwitchStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.AppSwitch) },
                    isSending = sendingSection == DiagnosticSection.AppSwitch,
                    sendEnabled = sendingSection == null
                )
            }
        }

        }
    }
}
