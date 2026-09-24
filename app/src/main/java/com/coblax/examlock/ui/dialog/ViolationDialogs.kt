package com.coblax.examlock.ui.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.WrongLocation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.coblax.examlock.AppSwitchStatus
import com.coblax.examlock.GeofenceSecurityStatus
import com.coblax.examlock.GeofenceSecurityVerdict
import com.coblax.examlock.LocationSpoofConfidenceTier
import com.coblax.examlock.LocationSpoofSecurityStatus
import com.coblax.examlock.LocationSpoofSecurityVerdict
import com.coblax.examlock.OverlaySignal
import com.coblax.examlock.diagnosticLabel
import com.coblax.examlock.format.formatLocationFixAge
import com.coblax.examlock.formatCoordinates
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.runtime.ExternalDisplayInfo
import com.coblax.examlock.runtime.MultiWindowModeInfo
import com.coblax.examlock.runtime.buildDisplayMirrorRuntimeEvidence
import com.coblax.examlock.runtime.buildMultiWindowRuntimeEvidence
import com.coblax.examlock.runtime.buildScreenRecorderRuntimeEvidence
import com.coblax.examlock.runtime.buildVpnRuntimeEvidence
import com.coblax.examlock.ui.LocalTelegramDiagnosticsEnabled
import com.coblax.examlock.ui.geofence.summarizeCircleCenters
import com.coblax.examlock.ui.theme.UiStatusTone
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
private fun violationBadge(count: Int): String =
    tr("Violation #$count", "Pelanggaran ke-$count")

@Composable
private fun yesNo(value: Boolean): String = if (value) tr("yes", "ya") else tr("no", "tidak")

@Composable
internal fun KeyboardViolationDialog(
    violationCount: Int,
    keyboardLabel: String,
    onAcknowledge: () -> Unit
) {
    AppAlertDialog(
        tone = UiStatusTone.Danger,
        icon = Icons.Rounded.Keyboard,
        title = tr("Keyboard not allowed", "Keyboard tidak diizinkan"),
        badge = violationBadge(violationCount),
        message = tr(
            "A non-standard keyboard was detected during the exam.",
            "Keyboard non-standar terdeteksi saat ujian berjalan."
        ),
        nextStep = tr(
            "Switch back to the device's default keyboard so the exam can continue.",
            "Kembali ke keyboard bawaan perangkat agar ujian bisa dilanjutkan."
        ),
        details = listOf(
            AppAlertDetail(
                tr("Detected keyboard", "Keyboard terdeteksi"),
                keyboardLabel.ifBlank { tr("Unknown", "Tidak diketahui") }
            )
        ),
        primaryAction = AppAlertAction(acknowledgeLabel(), onAcknowledge)
    )
}

@Composable
internal fun ExitExamDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isClearingSession: Boolean,
    onForceExit: () -> Unit = onConfirm
) {
    var forceExitEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(isClearingSession) {
        forceExitEnabled = false
        if (isClearingSession) {
            delay(15_000L)
            forceExitEnabled = true
        }
    }
    val message = when {
        isClearingSession && forceExitEnabled -> tr(
            "Session cleanup is taking longer than expected. You can force exit now.",
            "Pembersihan sesi lebih lama dari biasanya. Anda bisa keluar paksa sekarang."
        )
        isClearingSession -> tr(
            "Clearing exam session data before returning to Home.",
            "Membersihkan data sesi ujian sebelum kembali ke menu utama."
        )
        else -> tr(
            "You will leave the exam screen and the app lock will be turned off.",
            "Anda akan keluar dari layar ujian dan kunci aplikasi akan dimatikan."
        )
    }
    val primaryAction = when {
        isClearingSession && forceExitEnabled -> AppAlertAction(
            label = tr("Force exit", "Keluar paksa"),
            onClick = onForceExit,
            style = AppAlertStyle.Destructive
        )
        isClearingSession -> AppAlertAction(
            label = tr("Clearing…", "Membersihkan…"),
            onClick = {},
            enabled = false,
            loading = true
        )
        else -> AppAlertAction(
            label = tr("Exit exam", "Keluar ujian"),
            onClick = onConfirm,
            style = AppAlertStyle.Destructive
        )
    }
    AppAlertDialog(
        tone = UiStatusTone.Warning,
        icon = Icons.AutoMirrored.Rounded.ExitToApp,
        title = tr("Exit the exam?", "Keluar dari ujian?"),
        message = message,
        dismissible = !isClearingSession,
        onDismissRequest = onDismiss,
        progress = isClearingSession && !forceExitEnabled,
        primaryAction = primaryAction,
        secondaryActions = listOf(
            AppAlertAction(
                label = tr("Cancel", "Batal"),
                onClick = onDismiss,
                enabled = !isClearingSession || forceExitEnabled
            )
        )
    )
}

@Composable
internal fun OverlayViolationDialog(
    violationCount: Int,
    trigger: String?,
    onAcknowledge: () -> Unit
) {
    val reasonText = when (trigger) {
        OverlaySignal.WindowFocusLoss.diagnosticLabel() -> tr(
            "The exam window lost focus in a suspicious way, which often means a floating app took focus.",
            "Jendela ujian kehilangan fokus secara mencurigakan, yang sering menandakan aplikasi melayang mengambil fokus."
        )
        else -> tr(
            "Touch input on the exam screen was covered by another window.",
            "Sentuhan pada layar ujian tertutup oleh jendela lain."
        )
    }
    AppAlertDialog(
        tone = UiStatusTone.Danger,
        icon = Icons.Rounded.Layers,
        title = tr("Floating app detected", "Aplikasi melayang terdeteksi"),
        badge = violationBadge(violationCount),
        message = reasonText,
        nextStep = tr(
            "Close the floating app (chat bubbles, recorders, and similar), then continue the exam.",
            "Tutup aplikasi melayang (bubble chat, perekam, dan sejenisnya), lalu lanjutkan ujian."
        ),
        details = listOf(AppAlertDetail(tr("Trigger", "Pemicu"), trigger ?: "-")),
        primaryAction = AppAlertAction(acknowledgeLabel(), onAcknowledge)
    )
}

@Composable
internal fun OfflineTooLongDialog(
    durationText: String,
    onAcknowledge: () -> Unit
) {
    AppAlertDialog(
        tone = UiStatusTone.Warning,
        icon = Icons.Rounded.CloudOff,
        title = tr("Offline for too long", "Offline terlalu lama"),
        badge = tr("Offline for $durationText", "Offline selama $durationText"),
        message = tr(
            "The exam device has been without a connection for too long.",
            "Perangkat ujian sudah terlalu lama tanpa koneksi."
        ),
        nextStep = tr(
            "Check Wi-Fi or mobile data, then continue once the connection is stable.",
            "Periksa Wi-Fi atau data seluler, lalu lanjutkan setelah koneksi stabil."
        ),
        primaryAction = AppAlertAction(acknowledgeLabel(), onAcknowledge)
    )
}

@Composable
internal fun NetworkUnstableDialog(
    transportLabel: String,
    flapCount: Int,
    onAcknowledge: () -> Unit
) {
    AppAlertDialog(
        tone = UiStatusTone.Warning,
        icon = Icons.Rounded.SyncProblem,
        title = tr("Connection unstable", "Koneksi tidak stabil"),
        badge = tr("$flapCount changes detected", "$flapCount perubahan terdeteksi"),
        message = tr(
            "The exam connection changed several times in a short period.",
            "Koneksi ujian berubah beberapa kali dalam waktu singkat."
        ),
        nextStep = tr(
            "Move to a more stable Wi-Fi or mobile connection if the exam needs internet.",
            "Pindah ke Wi-Fi atau data seluler yang lebih stabil jika ujian butuh internet."
        ),
        details = listOf(AppAlertDetail(tr("Last connection", "Koneksi terakhir"), transportLabel)),
        primaryAction = AppAlertAction(acknowledgeLabel(), onAcknowledge)
    )
}

@Composable
internal fun VpnDetectedDialog(
    transportLabel: String,
    interfaceName: String,
    bypassActive: Boolean,
    bypassTampered: Boolean,
    onOpenVpnSettings: () -> Unit,
    onRefreshStatus: () -> Unit,
    onSendReport: () -> Unit
) {
    AppAlertDialog(
        tone = UiStatusTone.Danger,
        icon = Icons.Rounded.VpnKey,
        title = tr("VPN is on", "VPN aktif"),
        message = tr(
            "A VPN connection is active while the exam is running.",
            "Koneksi VPN aktif saat ujian berjalan."
        ),
        nextStep = tr(
            "Turn off the VPN, then tap Check again.",
            "Matikan VPN, lalu ketuk Cek ulang."
        ),
        details = listOf(
            AppAlertDetail(
                tr("Evidence", "Bukti"),
                buildVpnRuntimeEvidence(
                    transportLabel = transportLabel,
                    interfaceName = interfaceName,
                    bypassActive = bypassActive,
                    bypassTampered = bypassTampered
                )
            )
        ),
        primaryAction = AppAlertAction(tr("Open VPN settings", "Buka setelan VPN"), onOpenVpnSettings),
        secondaryActions = buildList {
            add(AppAlertAction(tr("Check again", "Cek ulang"), onRefreshStatus))
            if (LocalTelegramDiagnosticsEnabled.current) {
                add(AppAlertAction(tr("Send network report", "Kirim report jaringan"), onSendReport))
            }
        }
    )
}

@Composable
internal fun GeofenceViolationDialog(
    locationStatus: GeofenceSecurityStatus,
    violationCount: Int,
    onAcknowledge: () -> Unit
) {
    val evaluation = locationStatus.geofenceEvaluation
    val titleText = when (locationStatus.finalVerdict) {
        GeofenceSecurityVerdict.Outside -> tr("Outside the exam area", "Di luar area ujian")
        GeofenceSecurityVerdict.PreciseRequired -> tr("Precise location required", "Lokasi presisi diperlukan")
        GeofenceSecurityVerdict.StaleFix -> tr("Location is out of date", "Lokasi sudah kedaluwarsa")
        GeofenceSecurityVerdict.LowAccuracy -> tr("Location not accurate enough", "Lokasi kurang akurat")
        GeofenceSecurityVerdict.MissingAccuracy -> tr("Location accuracy missing", "Akurasi lokasi belum ada")
        else -> tr("Location check failed", "Validasi lokasi gagal")
    }
    val primaryMessage = when (locationStatus.finalVerdict) {
        GeofenceSecurityVerdict.Outside -> tr(
            "The device moved outside the exam area.",
            "Perangkat keluar dari area ujian."
        )
        GeofenceSecurityVerdict.PreciseRequired -> tr(
            "The exam needs precise location, but only approximate location is allowed.",
            "Ujian butuh lokasi presisi, tetapi yang diizinkan hanya lokasi perkiraan."
        )
        GeofenceSecurityVerdict.StaleFix -> tr(
            "The latest location reading is too old to check the exam area.",
            "Pembacaan lokasi terakhir terlalu lama untuk memeriksa area ujian."
        )
        GeofenceSecurityVerdict.LowAccuracy -> tr(
            "The latest location reading is too inaccurate to check the exam area.",
            "Pembacaan lokasi terakhir terlalu tidak akurat untuk memeriksa area ujian."
        )
        GeofenceSecurityVerdict.MissingAccuracy -> tr(
            "The latest location reading has no accuracy value yet.",
            "Pembacaan lokasi terakhir belum punya nilai akurasi."
        )
        else -> tr(
            "The app could not check the exam location while the exam was running.",
            "Aplikasi tidak dapat memeriksa lokasi ujian saat ujian berjalan."
        )
    }
    val nextStep = when (locationStatus.finalVerdict) {
        GeofenceSecurityVerdict.Outside -> tr(
            "Go back inside the exam area.",
            "Kembali ke dalam area ujian."
        )
        GeofenceSecurityVerdict.PreciseRequired -> tr(
            "Allow precise location for CBX Lock.",
            "Izinkan lokasi presisi untuk CBX Lock."
        )
        GeofenceSecurityVerdict.StaleFix,
        GeofenceSecurityVerdict.LowAccuracy,
        GeofenceSecurityVerdict.MissingAccuracy -> tr(
            "Move near a window or outdoors so the location becomes accurate.",
            "Pindah ke dekat jendela atau tempat terbuka agar lokasi lebih akurat."
        )
        else -> tr(
            "Tell the proctor if this keeps happening.",
            "Beri tahu pengawas jika masalah ini terus muncul."
        )
    }
    val circleCenters = evaluation.config?.circleCenters.orEmpty()
    val meters = { value: Double? -> value?.let { String.format(Locale.US, "%.1f m", it) } ?: "-" }
    val details = listOf(
        AppAlertDetail(tr("Verdict", "Verdict"), locationStatus.finalVerdict.diagnosticLabel()),
        AppAlertDetail(
            tr("Current coordinates", "Koordinat saat ini"),
            evaluation.locationSnapshot?.let { formatCoordinates(it.latitude, it.longitude) } ?: "-"
        ),
        AppAlertDetail(
            tr("Closest / primary center", "Center terdekat / utama"),
            evaluation.closestCircleCenter?.let { formatCoordinates(it.latitude, it.longitude) }
                ?: evaluation.config?.let { formatCoordinates(it.centerLat, it.centerLng) }
                ?: "-"
        ),
        AppAlertDetail(
            tr("Circle centers", "Center circle"),
            "${circleCenters.size} | ${summarizeCircleCenters(circleCenters)}"
        ),
        AppAlertDetail(tr("Shared radius", "Radius bersama"), meters(evaluation.config?.radiusMeters)),
        AppAlertDetail(tr("Distance from closest center", "Jarak dari center terdekat"), meters(evaluation.distanceMeters)),
        AppAlertDetail(
            tr("Provider / accuracy", "Provider / akurasi"),
            "${evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"} / " +
                meters(evaluation.locationSnapshot?.accuracyMeters?.toDouble())
        ),
        AppAlertDetail(
            tr("Fix quality / age", "Kualitas fix / umur"),
            "${locationStatus.fixQualityStatus.verdict.diagnosticLabel()} / " +
                formatLocationFixAge(locationStatus.fixQualityStatus.ageMs)
        ),
        AppAlertDetail(
            tr("Permission / precise / services", "Izin / presisi / layanan"),
            "${yesNo(evaluation.permissionGranted)} / ${yesNo(locationStatus.preciseLocationGranted)} / " +
                yesNo(evaluation.locationServicesEnabled)
        )
    )
    AppAlertDialog(
        tone = UiStatusTone.Danger,
        icon = Icons.Rounded.LocationOff,
        title = titleText,
        badge = violationBadge(violationCount),
        message = primaryMessage,
        nextStep = nextStep,
        details = details,
        primaryAction = AppAlertAction(acknowledgeLabel(), onAcknowledge)
    )
}

@Composable
internal fun FakeLocationViolationDialog(
    fakeLocationStatus: LocationSpoofSecurityStatus,
    violationCount: Int,
    onAcknowledge: () -> Unit
) {
    val titleText = when (fakeLocationStatus.finalVerdict) {
        LocationSpoofSecurityVerdict.PermissionRequired ->
            tr("Location permission required", "Izin lokasi diperlukan")
        LocationSpoofSecurityVerdict.LocationServicesDisabled ->
            tr("Location is turned off", "Lokasi dimatikan")
        LocationSpoofSecurityVerdict.LocationUnavailable ->
            tr("Location not available yet", "Lokasi belum tersedia")
        else -> when (fakeLocationStatus.confidenceTier) {
            LocationSpoofConfidenceTier.Critical -> tr("Fake location detected (critical)", "Lokasi palsu terdeteksi (kritis)")
            else -> tr("Fake location detected", "Lokasi palsu terdeteksi")
        }
    }
    val primaryMessage = when (fakeLocationStatus.finalVerdict) {
        LocationSpoofSecurityVerdict.PermissionRequired -> tr(
            "Location permission was removed during the exam, so fake-location protection cannot continue safely.",
            "Izin lokasi dicabut saat ujian, sehingga perlindungan lokasi palsu tidak bisa berjalan dengan aman."
        )
        LocationSpoofSecurityVerdict.LocationServicesDisabled -> tr(
            "Location was turned off during the exam, so fake-location protection cannot continue safely.",
            "Lokasi dimatikan saat ujian, sehingga perlindungan lokasi palsu tidak bisa berjalan dengan aman."
        )
        LocationSpoofSecurityVerdict.LocationUnavailable -> tr(
            "The app could not get a usable location while the exam was running.",
            "Aplikasi tidak bisa mendapatkan lokasi yang bisa dipakai saat ujian berjalan."
        )
        else -> when (fakeLocationStatus.confidenceTier) {
            LocationSpoofConfidenceTier.Critical -> tr(
                "Several strong fake-location signals were detected during the exam.",
                "Beberapa sinyal lokasi palsu yang kuat terdeteksi saat ujian."
            )
            else -> tr(
                "Strong fake-location or mock-location signals were detected during the exam.",
                "Sinyal lokasi palsu atau mock location yang kuat terdeteksi saat ujian."
            )
        }
    }
    val nextStep = when (fakeLocationStatus.finalVerdict) {
        LocationSpoofSecurityVerdict.PermissionRequired -> tr(
            "Allow location access for CBX Lock again.",
            "Izinkan kembali akses lokasi untuk CBX Lock."
        )
        LocationSpoofSecurityVerdict.LocationServicesDisabled -> tr(
            "Turn location back on.",
            "Nyalakan kembali lokasi (GPS)."
        )
        LocationSpoofSecurityVerdict.LocationUnavailable -> tr(
            "Move near a window or outdoors and wait for the location.",
            "Pindah ke dekat jendela atau tempat terbuka dan tunggu lokasi terbaca."
        )
        else -> tr(
            "Turn off fake location apps and mock location, then tell the proctor.",
            "Matikan aplikasi lokasi palsu dan mock location, lalu beri tahu pengawas."
        )
    }
    val status = fakeLocationStatus
    val details = listOf(
        AppAlertDetail(tr("Verdict", "Verdict"), status.finalVerdict.diagnosticLabel()),
        AppAlertDetail(tr("Confidence tier", "Tingkat keyakinan"), status.confidenceTier.diagnosticLabel()),
        AppAlertDetail(
            tr("Fix quality eligible", "Fix layak dinilai"),
            "${yesNo(status.fixQualityEligible)} (${status.fixQualityStatus.verdict.diagnosticLabel()})"
        ),
        AppAlertDetail(tr("Location permission", "Izin lokasi"), yesNo(status.permissionGranted)),
        AppAlertDetail(tr("Location services", "Layanan lokasi"), yesNo(status.locationServicesEnabled)),
        AppAlertDetail(tr("Snapshot available", "Snapshot tersedia"), yesNo(status.snapshotAvailable)),
        AppAlertDetail(tr("Mock flag", "Flag mock"), yesNo(status.mockLocationDetected)),
        AppAlertDetail(tr("Developer options", "Opsi pengembang"), yesNo(status.developerOptionsEnabled)),
        AppAlertDetail(
            tr("Suspicious packages", "Paket mencurigakan"),
            status.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" }
        ),
        AppAlertDetail(
            tr("Supporting signals", "Sinyal pendukung"),
            status.supportingSignals.joinToString { it.diagnosticLabel() }.ifBlank { "-" }
        )
    )
    AppAlertDialog(
        tone = UiStatusTone.Danger,
        icon = Icons.Rounded.WrongLocation,
        title = titleText,
        badge = violationBadge(violationCount),
        message = primaryMessage,
        nextStep = nextStep,
        details = details,
        primaryAction = AppAlertAction(acknowledgeLabel(), onAcknowledge)
    )
}

@Composable
internal fun BluetoothViolationDialog(
    bluetoothEnabled: Boolean,
    violationCount: Int,
    onOpenBluetoothSettings: () -> Unit,
    onAcknowledge: () -> Unit
) {
    AppAlertDialog(
        tone = UiStatusTone.Danger,
        icon = Icons.Rounded.BluetoothDisabled,
        title = tr("Bluetooth is on", "Bluetooth aktif"),
        badge = violationBadge(violationCount),
        message = if (bluetoothEnabled) {
            tr("Bluetooth was turned on during the exam.", "Bluetooth dinyalakan saat ujian berjalan.")
        } else {
            tr(
                "Bluetooth must be confirmed off before the exam continues.",
                "Bluetooth harus dipastikan mati sebelum ujian dilanjutkan."
            )
        },
        nextStep = tr("Turn off Bluetooth, then continue.", "Matikan Bluetooth, lalu lanjutkan ujian."),
        primaryAction = AppAlertAction(tr("Open Bluetooth", "Buka Bluetooth"), onOpenBluetoothSettings),
        secondaryActions = listOf(AppAlertAction(acknowledgeLabel(), onAcknowledge))
    )
}

@Composable
internal fun ClipboardViolationDialog(
    violationCount: Int,
    lastConfirmedAt: String?,
    lastDecision: String,
    onAcknowledge: () -> Unit
) {
    AppAlertDialog(
        tone = UiStatusTone.Danger,
        icon = Icons.Rounded.ContentPaste,
        title = tr("Clipboard changed", "Clipboard berubah"),
        badge = violationBadge(violationCount),
        message = tr(
            "The device clipboard changed while the exam was running.",
            "Clipboard perangkat berubah saat ujian berjalan."
        ),
        nextStep = tr(
            "Do not copy or paste text from other apps during the exam.",
            "Jangan menyalin atau menempel teks dari aplikasi lain selama ujian."
        ),
        details = listOf(
            AppAlertDetail(
                tr("Last confirmed change", "Perubahan terakhir"),
                lastConfirmedAt?.ifBlank { "-" } ?: "-"
            ),
            AppAlertDetail(tr("Listener decision", "Keputusan listener"), lastDecision)
        ),
        primaryAction = AppAlertAction(acknowledgeLabel(), onAcknowledge)
    )
}

@Composable
private fun RuntimeStaticSecurityDialog(
    icon: ImageVector,
    title: String,
    message: String,
    detail: String,
    nextStep: String,
    primaryActionLabel: String?,
    onPrimaryAction: (() -> Unit)?,
    onRefreshStatus: () -> Unit,
    onSendReport: () -> Unit
) {
    val refresh = AppAlertAction(tr("Check again", "Cek ulang"), onRefreshStatus)
    val report = AppAlertAction(tr("Send report", "Kirim report"), onSendReport)
        .takeIf { LocalTelegramDiagnosticsEnabled.current }
    val primary = if (primaryActionLabel != null && onPrimaryAction != null) {
        AppAlertAction(primaryActionLabel, onPrimaryAction)
    } else {
        refresh
    }
    AppAlertDialog(
        tone = UiStatusTone.Danger,
        icon = icon,
        title = title,
        message = message,
        nextStep = nextStep,
        details = listOf(AppAlertDetail(tr("Evidence", "Bukti"), detail)),
        primaryAction = primary,
        secondaryActions = listOfNotNull(refresh.takeIf { primary !== refresh }, report)
    )
}

@Composable
internal fun ScreenRecorderRuntimeViolationDialog(
    packages: List<String>,
    violationCount: Int,
    onOpenAppSettings: () -> Unit,
    onRefreshStatus: () -> Unit,
    onSendReport: () -> Unit
) {
    RuntimeStaticSecurityDialog(
        icon = Icons.Rounded.Videocam,
        title = tr("Screen recorder detected", "Perekam layar terdeteksi"),
        message = tr(
            "A screen recorder app is present while the exam is running.",
            "Aplikasi perekam layar terdeteksi saat ujian berjalan."
        ),
        detail = buildScreenRecorderRuntimeEvidence(
            packages = packages,
            violationCount = violationCount
        ),
        nextStep = tr(
            "Open App settings, disable or uninstall the recorder, then tap Check again.",
            "Buka Setelan App, nonaktifkan atau hapus aplikasi perekam, lalu ketuk Cek ulang."
        ),
        primaryActionLabel = tr("Open App settings", "Buka setelan App"),
        onPrimaryAction = onOpenAppSettings,
        onRefreshStatus = onRefreshStatus,
        onSendReport = onSendReport
    )
}

@Composable
internal fun DisplayMirrorRuntimeViolationDialog(
    externalDisplayCount: Int,
    externalDisplayInfoList: List<ExternalDisplayInfo>,
    violationCount: Int,
    onOpenCastSettings: () -> Unit,
    onRefreshStatus: () -> Unit,
    onSendReport: () -> Unit
) {
    RuntimeStaticSecurityDialog(
        icon = Icons.Rounded.ScreenShare,
        title = tr("Screen is being mirrored", "Layar sedang di-mirror"),
        message = tr(
            "An external display or casting is active while the exam is running.",
            "Layar eksternal atau casting aktif saat ujian berjalan."
        ),
        detail = buildDisplayMirrorRuntimeEvidence(
            externalDisplayCount = externalDisplayCount,
            externalDisplayInfoList = externalDisplayInfoList,
            violationCount = violationCount
        ),
        nextStep = tr(
            "Disconnect the external display or casting, then tap Check again.",
            "Putuskan layar eksternal atau casting, lalu ketuk Cek ulang."
        ),
        primaryActionLabel = tr("Open Cast settings", "Buka setelan Cast"),
        onPrimaryAction = onOpenCastSettings,
        onRefreshStatus = onRefreshStatus,
        onSendReport = onSendReport
    )
}

@Composable
internal fun MultiWindowRuntimeViolationDialog(
    modeInfo: MultiWindowModeInfo,
    runtimeDetected: Boolean,
    violationCount: Int,
    onRefreshStatus: () -> Unit,
    onSendReport: () -> Unit
) {
    RuntimeStaticSecurityDialog(
        icon = Icons.Rounded.VerticalSplit,
        title = tr("Split screen is on", "Split screen aktif"),
        message = tr(
            "The exam app is in split-screen or picture-in-picture mode.",
            "Aplikasi ujian berada di mode split screen atau picture-in-picture."
        ),
        detail = buildMultiWindowRuntimeEvidence(
            modeInfo = modeInfo,
            runtimeDetected = runtimeDetected,
            violationCount = violationCount
        ),
        nextStep = tr(
            "Return to full-screen single-app mode, then tap Check again.",
            "Kembali ke mode layar penuh satu aplikasi, lalu ketuk Cek ulang."
        ),
        primaryActionLabel = null,
        onPrimaryAction = null,
        onRefreshStatus = onRefreshStatus,
        onSendReport = onSendReport
    )
}

internal data class ExamRuntimeDialogsState(
    val showForcedExitAlarm: Boolean,
    val forcedExitViolationCount: Int,
    val appSwitchStatus: AppSwitchStatus,
    val showKeyboardViolationDialog: Boolean,
    val keyboardViolationCount: Int,
    val currentKeyboardLabel: String,
    val showOverlayViolationDialog: Boolean,
    val overlayViolationCount: Int,
    val overlayTrigger: String?,
    val showOfflineWarningDialog: Boolean,
    val offlineDurationText: String,
    val showVpnDetectedDialog: Boolean,
    val vpnTransportLabel: String,
    val vpnInterfaceName: String,
    val vpnBypassActive: Boolean,
    val vpnBypassTampered: Boolean,
    val showNetworkUnstableDialog: Boolean,
    val networkTransportLabel: String,
    val networkUnstableFlapCount: Int,
    val showGeofenceViolationDialog: Boolean,
    val geofenceStatus: GeofenceSecurityStatus,
    val geofenceViolationCount: Int,
    val showFakeLocationViolationDialog: Boolean,
    val fakeLocationStatus: LocationSpoofSecurityStatus,
    val fakeLocationViolationCount: Int,
    val showBluetoothViolationDialog: Boolean,
    val bluetoothEnabled: Boolean,
    val bluetoothViolationCount: Int,
    val showClipboardViolationDialog: Boolean,
    val clipboardViolationCount: Int,
    val clipboardLastConfirmedAt: String?,
    val clipboardLastDecision: String,
    val showExitExamDialog: Boolean,
    val exitSessionClearInFlight: Boolean
)

internal data class ExamRuntimeDialogsActions(
    val onAcknowledgeForcedExit: () -> Unit,
    val onAcknowledgeKeyboard: () -> Unit,
    val onAcknowledgeOverlay: () -> Unit,
    val onAcknowledgeOffline: () -> Unit,
    val onOpenVpnSettings: () -> Unit,
    val onRefreshVpnStatus: () -> Unit,
    val onSendVpnReport: () -> Unit,
    val onAcknowledgeNetworkUnstable: () -> Unit,
    val onAcknowledgeGeofence: () -> Unit,
    val onAcknowledgeFakeLocation: () -> Unit,
    val onOpenBluetoothSettings: () -> Unit,
    val onAcknowledgeBluetooth: () -> Unit,
    val onAcknowledgeClipboard: () -> Unit,
    val onDismissExitExam: () -> Unit,
    val onConfirmExitExam: () -> Unit
)

@Composable
internal fun ExamRuntimeDialogsHost(
    state: ExamRuntimeDialogsState,
    actions: ExamRuntimeDialogsActions
) {
    if (state.showForcedExitAlarm) {
        SecurityViolationDialog(
            violationCount = state.forcedExitViolationCount,
            fallbackGuardActive = state.appSwitchStatus.fallbackGuardActive,
            lastTrigger = state.appSwitchStatus.lastTrigger,
            lastDetectedAt = state.appSwitchStatus.lastDetectedAt,
            lastContext = state.appSwitchStatus.lastContext,
            onAcknowledge = actions.onAcknowledgeForcedExit
        )
    }

    if (state.showKeyboardViolationDialog) {
        KeyboardViolationDialog(
            violationCount = state.keyboardViolationCount,
            keyboardLabel = state.currentKeyboardLabel,
            onAcknowledge = actions.onAcknowledgeKeyboard
        )
    }

    if (state.showOverlayViolationDialog) {
        OverlayViolationDialog(
            violationCount = state.overlayViolationCount,
            trigger = state.overlayTrigger,
            onAcknowledge = actions.onAcknowledgeOverlay
        )
    }

    if (state.showOfflineWarningDialog) {
        OfflineTooLongDialog(
            durationText = state.offlineDurationText,
            onAcknowledge = actions.onAcknowledgeOffline
        )
    }

    if (state.showVpnDetectedDialog && !state.showOfflineWarningDialog) {
        VpnDetectedDialog(
            transportLabel = state.vpnTransportLabel,
            interfaceName = state.vpnInterfaceName,
            bypassActive = state.vpnBypassActive,
            bypassTampered = state.vpnBypassTampered,
            onOpenVpnSettings = actions.onOpenVpnSettings,
            onRefreshStatus = actions.onRefreshVpnStatus,
            onSendReport = actions.onSendVpnReport
        )
    }

    if (state.showNetworkUnstableDialog && !state.showOfflineWarningDialog && !state.showVpnDetectedDialog) {
        NetworkUnstableDialog(
            transportLabel = state.networkTransportLabel,
            flapCount = state.networkUnstableFlapCount,
            onAcknowledge = actions.onAcknowledgeNetworkUnstable
        )
    }

    if (state.showGeofenceViolationDialog) {
        GeofenceViolationDialog(
            locationStatus = state.geofenceStatus,
            violationCount = state.geofenceViolationCount,
            onAcknowledge = actions.onAcknowledgeGeofence
        )
    }

    if (state.showFakeLocationViolationDialog) {
        FakeLocationViolationDialog(
            fakeLocationStatus = state.fakeLocationStatus,
            violationCount = state.fakeLocationViolationCount,
            onAcknowledge = actions.onAcknowledgeFakeLocation
        )
    }

    if (state.showBluetoothViolationDialog) {
        BluetoothViolationDialog(
            bluetoothEnabled = state.bluetoothEnabled,
            violationCount = state.bluetoothViolationCount,
            onOpenBluetoothSettings = actions.onOpenBluetoothSettings,
            onAcknowledge = actions.onAcknowledgeBluetooth
        )
    }

    if (state.showClipboardViolationDialog) {
        ClipboardViolationDialog(
            violationCount = state.clipboardViolationCount,
            lastConfirmedAt = state.clipboardLastConfirmedAt,
            lastDecision = state.clipboardLastDecision,
            onAcknowledge = actions.onAcknowledgeClipboard
        )
    }

    if (state.showExitExamDialog) {
        ExitExamDialog(
            onDismiss = actions.onDismissExitExam,
            onConfirm = actions.onConfirmExitExam,
            isClearingSession = state.exitSessionClearInFlight
        )
    }
}

@Composable
internal fun SecurityViolationDialog(
    violationCount: Int,
    fallbackGuardActive: Boolean,
    lastTrigger: String?,
    lastDetectedAt: String?,
    lastContext: String?,
    onAcknowledge: () -> Unit
) {
    AppAlertDialog(
        tone = UiStatusTone.Danger,
        icon = Icons.Rounded.GppBad,
        title = tr("You left the exam screen", "Anda keluar dari layar ujian"),
        badge = violationBadge(violationCount),
        message = tr(
            "The app detected that you left the exam screen or the app lost focus.",
            "Aplikasi mendeteksi Anda meninggalkan layar ujian atau aplikasi kehilangan fokus."
        ),
        nextStep = tr(
            "Stay on the exam screen. Every exit is recorded and reported.",
            "Tetap di layar ujian. Setiap kali keluar akan tercatat dan dilaporkan."
        ),
        details = listOf(
            AppAlertDetail(tr("Fallback guard active", "Fallback guard aktif"), yesNo(fallbackGuardActive)),
            AppAlertDetail(tr("Last trigger", "Pemicu terakhir"), lastTrigger?.ifBlank { "-" } ?: "-"),
            AppAlertDetail(tr("Last detected at", "Terdeteksi terakhir"), lastDetectedAt?.ifBlank { "-" } ?: "-"),
            AppAlertDetail(tr("Context", "Konteks"), lastContext?.ifBlank { "-" } ?: "-")
        ),
        primaryAction = AppAlertAction(acknowledgeLabel(), onAcknowledge)
    )
}
