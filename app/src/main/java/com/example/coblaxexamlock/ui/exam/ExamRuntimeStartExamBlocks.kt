package com.example.coblaxexamlock.ui.exam

import com.example.coblaxexamlock.GeofenceSecurityStatus
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.ExamScheduleValidationResult
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityStatus
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.runtime.buildRootIssueMessage
import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.SplitLocationSecurityStatus
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.UiLanguage
import java.util.Locale

internal data class StartExamBlockMessage(
    val code: String,
    val details: String = "-",
    val title: String,
    val message: String
)

internal fun resolveStartExamUnexpectedFailureBlockMessage(
    uiLanguage: UiLanguage,
    phase: String,
    throwable: Throwable
): StartExamBlockMessage {
    val errorClass = throwable.javaClass.simpleName.ifBlank { "Throwable" }
    val safeMessage = throwable.message
        ?.lineSequence()
        ?.joinToString(" ")
        ?.take(160)
        ?.ifBlank { null }
        ?: "-"
    return StartExamBlockMessage(
        code = "START_EXAM_FAILED_UNEXPECTED",
        details = "phase=$phase | error=$errorClass | message=$safeMessage",
        title = localized(uiLanguage, "Start Exam Failed", "Mulai Ujian Gagal"),
        message = localized(
            uiLanguage,
            "The app hit an internal error while preparing the exam. The exam has not started yet. Press Start Exam again. If it repeats, export diagnostics and contact the admin.\n\nTechnical detail: $errorClass",
            "Aplikasi mengalami kendala internal saat menyiapkan ujian. Ujian belum dimulai. Tekan Mulai Ujian lagi. Jika berulang, export diagnostik dan hubungi admin.\n\nDetail teknis: $errorClass"
        )
    )
}

internal fun resolveStartExamTamperBlockMessage(
    uiLanguage: UiLanguage,
    reverseEngineeringDetected: Boolean,
    reverseEngineeringSummary: String,
    reverseEngineeringBypassActive: Boolean,
    apkIntegrityDetected: Boolean,
    apkIntegritySummary: String,
    apkIntegrityBypassActive: Boolean
): StartExamBlockMessage? {
    if (reverseEngineeringDetected && !reverseEngineeringBypassActive) {
        return StartExamBlockMessage(
            code = "START_EXAM_BLOCKED_REVERSE_ENGINEERING",
            details = reverseEngineeringSummary.ifBlank { "-" },
            title = localized(
                uiLanguage,
                "Reverse Engineering Check Failed",
                "Cek Reverse Engineering Gagal"
            ),
            message = localized(
                uiLanguage,
                "Debugger, tracer, hooking memory, hook class, or root/hooking package was detected. Close the related tool, uninstall it, or use the Secret Admin bypass only for approved troubleshooting.\n\nReason: ${formatReverseEngineeringBlockReason(reverseEngineeringSummary)}",
                "Debugger, tracer, memory hooking, hook class, atau package root/hooking terdeteksi. Tutup tool terkait, hapus aplikasinya, atau gunakan bypass Secret Admin hanya untuk troubleshooting resmi.\n\nAlasan: ${formatReverseEngineeringBlockReason(reverseEngineeringSummary)}"
            )
        )
    }
    if (apkIntegrityDetected && !apkIntegrityBypassActive) {
        return StartExamBlockMessage(
            code = "START_EXAM_BLOCKED_APK_INTEGRITY",
            details = apkIntegritySummary.ifBlank { "-" },
            title = localized(uiLanguage, "APK Integrity Check Failed", "Cek Integritas APK Gagal"),
            message = localized(
                uiLanguage,
                "The APK signature, hash, or device integrity signal is not trusted. Reinstall the official APK or use the Secret Admin bypass only for approved troubleshooting.\n\nReason: ${formatApkIntegrityBlockReason(apkIntegritySummary)}",
                "Signature, hash, atau sinyal integritas perangkat tidak terpercaya. Instal ulang APK resmi atau gunakan bypass Secret Admin hanya untuk troubleshooting resmi.\n\nAlasan: ${formatApkIntegrityBlockReason(apkIntegritySummary)}"
            )
        )
    }
    return null
}

internal fun formatReverseEngineeringBlockReason(summary: String): String {
    val normalized = summary.ifBlank { "-" }
    val lower = normalized.lowercase(Locale.US)
    val reasons = mutableListOf<String>()
    if ("debugger" in lower || "tracerpid" in lower || "tracer" in lower) {
        reasons.add("Debugger atau tracing aktif")
    }
    if ("maps:" in lower || "proc_maps" in lower || "memory" in lower) {
        reasons.add("Hooking/root framework terdeteksi di memory")
    }
    if ("class:" in lower || "hook_class" in lower || "xposed" in lower || "lsposed" in lower || "substrate" in lower) {
        reasons.add("Class Xposed/LSPosed/Substrate terdeteksi")
    }
    if ("pkg:" in lower || "package" in lower || "magisk" in lower || "kernelsu" in lower) {
        reasons.add("Package tool hooking/root terpasang")
    }
    return reasons.joinToString("; ").ifBlank { normalized }
}

internal fun formatApkIntegrityBlockReason(summary: String): String {
    val normalized = summary.ifBlank { "-" }
    val lower = normalized.lowercase(Locale.US)
    val reasons = mutableListOf<String>()
    if ("signature_changed" in lower || "signature" in lower) {
        reasons.add("Signature APK berubah")
    }
    if ("dex_hash_mismatch" in lower || "hash" in lower) {
        reasons.add("Hash APK berubah")
    }
    if ("sysprop_" in lower || "test_keys" in lower || "system property" in lower) {
        reasons.add("System property tidak aman")
    }
    if ("hook_class" in lower) {
        reasons.add("Hook class terdeteksi oleh IntegrityGuard")
    }
    return reasons.joinToString("; ").ifBlank { normalized }
}

internal fun resolveStartExamScreenPinningBlockMessage(
    uiLanguage: UiLanguage,
    screenPinningMode: ScreenPinningMode,
    screenPinningAvailable: Boolean,
    screenPinningActive: Boolean,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardEnabled: Boolean,
    phaseSuffix: String = ""
): StartExamBlockMessage? {
    if (screenPinningMode != ScreenPinningMode.Enforced) {
        return null
    }

    val suffix = phaseSuffix.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()
    if (screenPinningAvailable && !screenPinningActive) {
        return StartExamBlockMessage(
            code = ExamRuntimeHardeningDiagnostics.StartExamBlockedScreenPinningInactive,
            details = "screen_pinning_available=true | lock_task_active=false | bypass=false$suffix",
            title = localized(uiLanguage, "Start Screen Pinning First", "Start Screen Pinning Dulu"),
            message = localized(
                uiLanguage,
                "Start Screen Pinning first from Preparation, confirm the Android dialog, then press Start Exam.",
                "Jalankan Start Screen Pinning dulu dari Preparation, konfirmasi dialog Android, lalu tekan Mulai Ujian."
            )
        )
    }
    if (screenPinningAvailable) {
        return null
    }

    return when {
        accessibilityGuardAvailable && !accessibilityGuardEnabled ->
            StartExamBlockMessage(
                code = "ACCESSIBILITY_GUARD_MISSING_BLOCKED",
                details = "screen_pinning_available=false | accessibility_guard_available=true | accessibility_guard_enabled=false | bypass=false$suffix",
                title = localized(
                    uiLanguage,
                    "Accessibility Exam Guard Required",
                    "Accessibility Exam Guard Diperlukan"
                ),
                message = localized(
                    uiLanguage,
                    "This device does not support Screen Pinning. Enable CBX Lock Exam Guard in Accessibility Settings, or use the Secret Admin Screen Pinning bypass.",
                    "Perangkat ini tidak mendukung Screen Pinning. Aktifkan CBX Lock Exam Guard di Pengaturan Aksesibilitas, atau gunakan bypass Screen Pinning melalui Secret Admin."
                )
            )

        !accessibilityGuardAvailable ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_SCREEN_PINNING_UNAVAILABLE",
                details = "screen_pinning_available=false | accessibility_guard_available=false | bypass=false$suffix",
                title = localized(uiLanguage, "Screen Pinning Unavailable", "Screen Pinning Tidak Tersedia"),
                message = localized(
                    uiLanguage,
                    "This device does not support Screen Pinning. Use a supported device or Secret Admin Screen Pinning bypass.",
                    "Perangkat ini tidak mendukung Screen Pinning. Gunakan perangkat yang mendukung atau bypass Screen Pinning melalui Secret Admin."
                )
            )

        else -> null
    }
}

internal fun resolveStartExamDeviceTimeBlockMessage(
    uiLanguage: UiLanguage,
    trigger: String,
    status: DeviceTimeSecurityStatus
): StartExamBlockMessage? {
    if (!status.blocking) {
        return null
    }
    return StartExamBlockMessage(
        code = "START_EXAM_BLOCKED_DEVICE_TIME",
        details = buildDeviceTimeEventDetails(trigger, status),
        title = deviceTimeBlockedTitle(uiLanguage),
        message = deviceTimeBlockedMessage(uiLanguage, status)
    )
}

internal fun resolveStartExamVpnBlockMessage(
    uiLanguage: UiLanguage,
    status: NetworkReadinessStatus
): StartExamBlockMessage? {
    if (!status.diagnostics.isVpnActive) {
        return null
    }
    return StartExamBlockMessage(
        code = ExamRuntimeHardeningDiagnostics.StartExamBlockedVpn,
        details = buildNetworkEventDetails("start_exam_precheck", status),
        title = localized(uiLanguage, "VPN Active", "VPN Aktif"),
        message = localized(
            uiLanguage,
            "Turn off VPN before starting the exam, then refresh Network status.",
            "Matikan VPN sebelum memulai ujian, lalu refresh status Network."
        )
    )
}

internal fun resolveStartExamNetworkReachabilityBlockMessage(
    uiLanguage: UiLanguage,
    status: NetworkReadinessStatus
): StartExamBlockMessage? {
    val shouldBlock = when (status.userFacingVerdict) {
        NetworkReadinessUserVerdict.Offline,
        NetworkReadinessUserVerdict.AirplaneMode -> true
        NetworkReadinessUserVerdict.Stable,
        NetworkReadinessUserVerdict.CaptivePortal,
        NetworkReadinessUserVerdict.Unvalidated,
        NetworkReadinessUserVerdict.DnsFailed,
        NetworkReadinessUserVerdict.Slow,
        NetworkReadinessUserVerdict.VpnActive,
        NetworkReadinessUserVerdict.Unstable -> false
    }
    if (!shouldBlock) {
        return null
    }
    val host = status.dnsProbeStatus.host.ifBlank { "-" }
    return StartExamBlockMessage(
        code = "START_EXAM_BLOCKED_NETWORK_REACHABILITY",
        details = buildNetworkEventDetails(
            trigger = "start_exam_precheck",
            status = status,
            extraContext = "probe_host=$host | global_dns_error=${status.globalDnsProbeStatus.error ?: "-"} | " +
                "exam_dns_error=${status.dnsProbeStatus.error ?: "-"}"
        ),
        title = localized(uiLanguage, "Exam Network Not Ready", "Network Ujian Belum Siap"),
        message = buildStartExamNetworkReachabilityMessage(uiLanguage, status, host)
    )
}

private fun buildStartExamNetworkReachabilityMessage(
    uiLanguage: UiLanguage,
    status: NetworkReadinessStatus,
    host: String
): String {
    val globalDnsVerdict = status.globalDnsProbeStatus.verdict.name
    val globalDnsError = status.globalDnsProbeStatus.error?.ifBlank { null } ?: "-"
    val examDnsVerdict = status.dnsProbeStatus.verdict.name
    val examDnsError = status.dnsProbeStatus.error?.ifBlank { null } ?: "-"
    val quickFix = status.userFacingQuickFixText?.ifBlank { null }
    return localized(
        uiLanguage,
        buildString {
            appendLine("The app could not confirm that the device network is ready for the exam.")
            appendLine()
            appendLine("Status: ${status.userFacingVerdict.name}")
            appendLine("Transport: ${status.transportLabel.ifBlank { "-" }}")
            appendLine("Exam host: $host")
            appendLine("Global DNS: $globalDnsVerdict (${status.globalDnsProbeStatus.host.ifBlank { "-" }})")
            appendLine("Global DNS error: $globalDnsError")
            appendLine("Exam host DNS: $examDnsVerdict")
            appendLine("Exam host DNS error: $examDnsError")
            appendLine("Android validated: ${if (status.diagnostics.isValidated) "yes" else "no"}")
            appendLine("Captive portal: ${if (status.diagnostics.isCaptivePortal) "yes" else "no"}")
            appendLine()
            append(quickFix ?: "Turn Wi-Fi/data back on, wait a few seconds, then refresh Network status.")
        },
        buildString {
            appendLine("Aplikasi belum bisa memastikan jaringan perangkat siap untuk ujian.")
            appendLine()
            appendLine("Status: ${status.userFacingVerdict.name}")
            appendLine("Koneksi: ${status.transportLabel.ifBlank { "-" }}")
            appendLine("Host ujian: $host")
            appendLine("DNS global: $globalDnsVerdict (${status.globalDnsProbeStatus.host.ifBlank { "-" }})")
            appendLine("Error DNS global: $globalDnsError")
            appendLine("DNS host ujian: $examDnsVerdict")
            appendLine("Error DNS host ujian: $examDnsError")
            appendLine("Validasi Android: ${if (status.diagnostics.isValidated) "ya" else "tidak"}")
            appendLine("Captive portal: ${if (status.diagnostics.isCaptivePortal) "ya" else "tidak"}")
            appendLine()
            append(quickFix ?: "Aktifkan ulang Wi-Fi/data, tunggu beberapa detik, lalu refresh status Network.")
        }
    )
}

@Suppress("UNUSED_PARAMETER")
internal fun resolveStartExamServerProbeBlockMessage(
    uiLanguage: UiLanguage,
    result: ExamServerProbeResult
): StartExamBlockMessage? {
    // The HTTP preflight runs through HttpURLConnection, while the exam itself loads
    // through Android WebView. Several field devices recover from stale DNS/TCP state
    // only when WebView retries or after radio reset/reboot, so a single failed
    // preflight must not block an otherwise connected student from entering the exam.
    return null
}

internal fun resolveStartExamScheduleBlockMessage(
    uiLanguage: UiLanguage,
    payload: ExamQrPayload,
    validationResult: ExamScheduleValidationResult,
    networkNowMillis: Long?,
    deviceTimeStatus: DeviceTimeSecurityStatus
): StartExamBlockMessage? {
    if (validationResult == ExamScheduleValidationResult.Valid) {
        return null
    }
    return StartExamBlockMessage(
        code = "START_EXAM_BLOCKED_DEVICE_TIME",
        details =
            "schedule_result=${validationResult.name.lowercase(Locale.US)} | " +
                "network_now_ms=${networkNowMillis ?: "unavailable"} | " +
                buildDeviceTimeEventDetails("start_exam_schedule", deviceTimeStatus),
        title = if (validationResult == ExamScheduleValidationResult.TimeSpoofDetected) {
            deviceTimeBlockedTitle(uiLanguage)
        } else {
            localized(uiLanguage, "Exam Schedule Not Active", "Jadwal Ujian Tidak Aktif")
        },
        message = scheduleBlockedMessage(
            uiLanguage = uiLanguage,
            payload = payload,
            validationResult = validationResult
        )
    )
}

internal fun resolveStartExamStaticSecurityBlockMessage(
    bypassAccessibility: Boolean,
    accessibilityServiceEnabled: Boolean,
    bypassAdb: Boolean,
    developerOptionsEnabled: Boolean,
    bypassVirtualEnvironment: Boolean,
    virtualEnvironmentDetected: Boolean,
    adbEnabled: Boolean,
    adbInsecureSystemProperty: Boolean,
    bypassRoot: Boolean,
    rootSecurityStatus: RootSecurityStatus,
    bypassScreenRecorder: Boolean,
    screenRecorderPackages: List<String>,
    bypassDisplayMirror: Boolean,
    externalDisplayDetected: Boolean,
    bypassMultiWindow: Boolean,
    multiWindowDetected: Boolean
): StartExamBlockMessage? {
    return when {
        !bypassAccessibility && accessibilityServiceEnabled ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_ACCESSIBILITY",
                title = "Accessibility Service Masih Aktif",
                message = "Nonaktifkan accessibility service sebelum memulai ujian."
            )

        !bypassAdb && developerOptionsEnabled ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_DEVELOPER_OPTIONS",
                title = "Developer Mode Masih Aktif",
                message = "Nonaktifkan Developer Mode sebelum memulai ujian."
            )

        !bypassVirtualEnvironment && virtualEnvironmentDetected ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_VIRTUAL_ENV",
                title = "Virtual Environment Terdeteksi",
                message = "Perangkat ini terdeteksi berjalan di emulator/VM. Gunakan perangkat fisik untuk melanjutkan ujian."
            )

        !bypassAdb && adbEnabled ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_ADB",
                title = "USB Debugging (ADB) Masih Aktif",
                message = "USB debugging terdeteksi aktif. Nonaktifkan ADB sebelum memulai ujian."
            )

        !bypassAdb && adbInsecureSystemProperty ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_ADB_INSECURE_PROPERTY",
                title = "ADB Security Property Tidak Aman",
                message = "Properti keamanan ADB sistem terdeteksi dalam kondisi tidak aman. Restart perangkat dan pastikan USB debugging dinonaktifkan."
            )

        !bypassRoot && rootSecurityStatus.detected ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_ROOT",
                title = "Root Device Terdeteksi",
                message = buildRootIssueMessage(rootSecurityStatus.details)
            )

        !bypassScreenRecorder && screenRecorderPackages.isNotEmpty() ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_SCREEN_RECORDER",
                details = "packages=${screenRecorderPackages.joinToString()}",
                title = "Screen Recorder Terdeteksi",
                message = "Hapus aplikasi screen recorder (${screenRecorderPackages.size} app) sebelum memulai ujian."
            )

        !bypassDisplayMirror && externalDisplayDetected ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_DISPLAY_MIRROR",
                title = "Display Eksternal Terdeteksi",
                message = "Putuskan koneksi display eksternal / screen casting sebelum memulai ujian."
            )

        !bypassMultiWindow && multiWindowDetected ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_MULTI_WINDOW",
                title = "Mode Split-Screen Aktif",
                message = "Keluar dari mode split-screen atau picture-in-picture sebelum memulai ujian."
            )

        else -> null
    }
}

internal fun resolveStartExamGeofenceConfigBlockMessage(
    uiLanguage: UiLanguage,
    details: String
): StartExamBlockMessage {
    return StartExamBlockMessage(
        code = "START_EXAM_BLOCKED_GEOFENCE_CONFIG",
        details = details,
        title = localized(uiLanguage, "Geofence Configuration Invalid", "Konfigurasi Geofence Tidak Valid"),
        message = localized(
            uiLanguage,
            "Check the geofence latitude, longitude, and radius in the Custom QR before starting the exam.",
            "Periksa latitude, longitude, dan radius geofence di Custom QR sebelum memulai ujian."
        )
    )
}

internal fun resolveStartExamGeofenceLocationDisabledBlockMessage(
    uiLanguage: UiLanguage,
    details: String
): StartExamBlockMessage {
    return StartExamBlockMessage(
        code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_DISABLED",
        details = details,
        title = localized(uiLanguage, "Location Services Disabled", "Layanan Lokasi Nonaktif"),
        message = localized(
            uiLanguage,
            "Turn on location services before starting the exam.",
            "Aktifkan layanan lokasi sebelum memulai ujian."
        )
    )
}

internal fun resolveStartExamFakeLocationServicesDisabledBlockMessage(
    uiLanguage: UiLanguage,
    details: String
): StartExamBlockMessage {
    return StartExamBlockMessage(
        code = "START_EXAM_BLOCKED_FAKE_LOCATION_LOCATION_DISABLED",
        details = details,
        title = localized(uiLanguage, "Location Services Disabled", "Layanan Lokasi Nonaktif"),
        message = localized(
            uiLanguage,
            "Turn on location services so anti-fake-location can validate the exam before it starts.",
            "Aktifkan layanan lokasi agar anti-fake-location bisa memvalidasi ujian sebelum dimulai."
        )
    )
}

internal fun resolveStartExamLocationBlockMessage(
    uiLanguage: UiLanguage,
    latestLocationStatus: SplitLocationSecurityStatus,
    bypassGeofence: Boolean,
    bypassFakeLocation: Boolean,
    geofenceDetails: (GeofenceSecurityStatus) -> String,
    fakeLocationDetails: (LocationSpoofSecurityStatus) -> String
): StartExamBlockMessage? {
    val geofenceStatus = latestLocationStatus.geofenceStatus
    val fakeLocationStatus = latestLocationStatus.fakeLocationStatus

    return when {
        !bypassGeofence && geofenceStatus.finalVerdict == GeofenceSecurityVerdict.Outside ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_GEOFENCE_OUTSIDE",
                details = geofenceDetails(geofenceStatus),
                title = localized(uiLanguage, "Outside Allowed Exam Area", "Di Luar Area Ujian"),
                message = localized(
                    uiLanguage,
                    "This device is outside the allowed exam radius. Move into the approved area before starting the exam.",
                    "Perangkat ini berada di luar radius ujian yang diizinkan. Masuk ke area yang disetujui sebelum memulai ujian."
                )
            )

        !bypassGeofence && geofenceStatus.finalVerdict == GeofenceSecurityVerdict.PermissionMissing ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_GEOFENCE_PERMISSION",
                details = geofenceDetails(geofenceStatus),
                title = localized(uiLanguage, "Location Permission Required", "Izin Lokasi Diperlukan"),
                message = localized(
                    uiLanguage,
                    "Location permission must be granted before the exam can start.",
                    "Izin lokasi harus diberikan sebelum ujian bisa dimulai."
                )
            )

        !bypassGeofence && geofenceStatus.finalVerdict == GeofenceSecurityVerdict.PreciseRequired ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_GEOFENCE_PRECISE_REQUIRED",
                details = geofenceDetails(geofenceStatus),
                title = localized(uiLanguage, "Precise Location Required", "Lokasi Presisi Diperlukan"),
                message = localized(
                    uiLanguage,
                    "Precise location must be granted before the exam can start.",
                    "Lokasi presisi harus diberikan sebelum ujian bisa dimulai."
                )
            )

        !bypassGeofence && geofenceStatus.finalVerdict == GeofenceSecurityVerdict.LocationDisabled ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_DISABLED",
                details = geofenceDetails(geofenceStatus),
                title = localized(uiLanguage, "Location Services Disabled", "Layanan Lokasi Nonaktif"),
                message = localized(
                    uiLanguage,
                    "Turn on location services before starting the exam.",
                    "Aktifkan layanan lokasi sebelum memulai ujian."
                )
            )

        !bypassGeofence && geofenceStatus.finalVerdict == GeofenceSecurityVerdict.NoFix ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_UNAVAILABLE",
                details = geofenceDetails(geofenceStatus),
                title = localized(uiLanguage, "Location Not Available", "Lokasi Belum Tersedia"),
                message = localized(
                    uiLanguage,
                    "The device location could not be validated yet. Wait for a location fix, then try again.",
                    "Lokasi perangkat belum bisa divalidasi. Tunggu hingga lokasi tersedia lalu coba lagi."
                )
            )

        !bypassGeofence && geofenceStatus.finalVerdict == GeofenceSecurityVerdict.StaleFix ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_UNAVAILABLE",
                details = geofenceDetails(geofenceStatus),
                title = localized(uiLanguage, "Location Fix Too Old", "Fix Lokasi Terlalu Lama"),
                message = localized(
                    uiLanguage,
                    "The latest location fix is too old. Wait for a fresh location update, then try again.",
                    "Fix lokasi terbaru terlalu lama. Tunggu pembaruan lokasi yang baru lalu coba lagi."
                )
            )

        !bypassGeofence && geofenceStatus.finalVerdict == GeofenceSecurityVerdict.LowAccuracy ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_UNAVAILABLE",
                details = geofenceDetails(geofenceStatus),
                title = localized(uiLanguage, "Location Accuracy Too Low", "Akurasi Lokasi Terlalu Rendah"),
                message = localized(
                    uiLanguage,
                    "The current location accuracy is still too weak for strict geofence validation. Wait for a better fix, then try again.",
                    "Akurasi lokasi saat ini masih terlalu lemah untuk validasi geofence ketat. Tunggu fix yang lebih baik lalu coba lagi."
                )
            )

        !bypassGeofence && geofenceStatus.finalVerdict == GeofenceSecurityVerdict.MissingAccuracy ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_UNAVAILABLE",
                details = geofenceDetails(geofenceStatus),
                title = localized(uiLanguage, "Location Accuracy Missing", "Akurasi Lokasi Belum Ada"),
                message = localized(
                    uiLanguage,
                    "The current location fix does not include a usable accuracy value yet. Wait for a better fix, then try again.",
                    "Fix lokasi saat ini belum memiliki nilai akurasi yang bisa dipakai. Tunggu fix yang lebih baik lalu coba lagi."
                )
            )

        !bypassFakeLocation && fakeLocationStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_FAKE_LOCATION_PERMISSION",
                details = fakeLocationDetails(fakeLocationStatus),
                title = localized(uiLanguage, "Location Permission Required", "Izin Lokasi Diperlukan"),
                message = localized(
                    uiLanguage,
                    "Location access is required so anti-fake-location can validate the exam before it starts.",
                    "Akses lokasi wajib tersedia agar anti-fake-location bisa memvalidasi ujian sebelum dimulai."
                )
            )

        !bypassFakeLocation &&
            fakeLocationStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_FAKE_LOCATION_LOCATION_DISABLED",
                details = fakeLocationDetails(fakeLocationStatus),
                title = localized(uiLanguage, "Location Services Disabled", "Layanan Lokasi Nonaktif"),
                message = localized(
                    uiLanguage,
                    "Turn on location services so anti-fake-location can validate the exam before it starts.",
                    "Aktifkan layanan lokasi agar anti-fake-location bisa memvalidasi ujian sebelum dimulai."
                )
            )

        !bypassFakeLocation && fakeLocationStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_FAKE_LOCATION_LOCATION_UNAVAILABLE",
                details = fakeLocationDetails(fakeLocationStatus),
                title = localized(uiLanguage, "Location Not Available", "Lokasi Belum Tersedia"),
                message = localized(
                    uiLanguage,
                    "Anti-fake-location is still waiting for a usable location snapshot. Refresh the location, then try again.",
                    "Anti-fake-location masih menunggu snapshot lokasi yang bisa dipakai. Refresh lokasi lalu coba lagi."
                )
            )

        !bypassFakeLocation && fakeLocationStatus.finalVerdict == LocationSpoofSecurityVerdict.SpoofDetected ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_FAKE_LOCATION_SPOOF",
                details = fakeLocationDetails(fakeLocationStatus),
                title = localized(
                    uiLanguage,
                    if (fakeLocationStatus.confidenceTier == LocationSpoofConfidenceTier.Critical) {
                        "Critical Fake Location Detected"
                    } else {
                        "Mock Location Detected"
                    },
                    if (fakeLocationStatus.confidenceTier == LocationSpoofConfidenceTier.Critical) {
                        "Fake Location Kritis Terdeteksi"
                    } else {
                        "Lokasi Palsu Terdeteksi"
                    }
                ),
                message = localized(
                    uiLanguage,
                    if (fakeLocationStatus.confidenceTier == LocationSpoofConfidenceTier.Critical) {
                        "Critical combined fake-location signals were detected. Disable Fake GPS, mock providers, or related developer tools before starting the exam."
                    } else {
                        "Location spoofing or mock-location signals were detected. Disable Fake GPS or developer mock providers before starting the exam."
                    },
                    if (fakeLocationStatus.confidenceTier == LocationSpoofConfidenceTier.Critical) {
                        "Terdeteksi kombinasi sinyal fake-location kritis. Nonaktifkan Fake GPS, mock provider, atau alat developer terkait sebelum memulai ujian."
                    } else {
                        "Terdeteksi sinyal spoofing lokasi atau mock location. Nonaktifkan Fake GPS atau mock provider developer sebelum memulai ujian."
                    }
                )
            )

        !bypassGeofence && geofenceStatus.finalVerdict == GeofenceSecurityVerdict.ConfigInvalid ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_GEOFENCE_CONFIG",
                details = geofenceDetails(geofenceStatus),
                title = localized(uiLanguage, "Geofence Configuration Invalid", "Konfigurasi Geofence Tidak Valid"),
                message = localized(
                    uiLanguage,
                    "Check the geofence latitude, longitude, and radius in the Custom QR before starting the exam.",
                    "Periksa latitude, longitude, dan radius geofence di Custom QR sebelum memulai ujian."
                )
            )

        else -> null
    }
}
