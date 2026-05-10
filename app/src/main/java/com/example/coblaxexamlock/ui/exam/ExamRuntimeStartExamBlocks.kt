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
import com.example.coblaxexamlock.model.UiLanguage
import java.util.Locale

internal data class StartExamBlockMessage(
    val code: String,
    val details: String = "-",
    val title: String,
    val message: String
)

internal fun resolveStartExamTamperBlockMessage(uiLanguage: UiLanguage): StartExamBlockMessage {
    return StartExamBlockMessage(
        code = "START_EXAM_BLOCKED_TAMPER",
        title = localized(uiLanguage, "Security Check Failed", "Pemeriksaan Keamanan Gagal"),
        message = localized(
            uiLanguage,
            "Security checks failed. Close debugging or hooking tools and reopen the app.",
            "Pemeriksaan keamanan gagal. Tutup tool debugging/hooking lalu buka ulang aplikasi."
        )
    )
}

internal fun resolveStartExamScreenPinningBlockMessage(
    uiLanguage: UiLanguage,
    screenPinningMode: ScreenPinningMode,
    screenPinningAvailable: Boolean,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardEnabled: Boolean,
    phaseSuffix: String = ""
): StartExamBlockMessage? {
    if (screenPinningMode != ScreenPinningMode.Enforced || screenPinningAvailable) {
        return null
    }

    val suffix = phaseSuffix.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()
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
    rootSecurityStatus: RootSecurityStatus
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

        !bypassRoot && rootSecurityStatus.selinuxPermissive ->
            StartExamBlockMessage(
                code = "START_EXAM_BLOCKED_SELINUX_PERMISSIVE",
                title = "SELinux Permissive Terdeteksi",
                message = "SELinux perangkat ini dalam mode permissive, yang mengurangi keamanan sistem. Gunakan perangkat dengan SELinux enforcing, atau minta admin mengaktifkan bypass Root."
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
