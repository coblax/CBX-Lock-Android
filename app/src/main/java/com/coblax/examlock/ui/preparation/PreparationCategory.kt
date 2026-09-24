package com.coblax.examlock.ui.preparation

import com.coblax.examlock.DpcProtectionTier
import com.coblax.examlock.GeofenceSecurityVerdict
import com.coblax.examlock.LocationSpoofSecurityVerdict
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.WebViewHealthVerdict
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.model.NetworkReadinessVerdict
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.exam.formatApkIntegrityBlockReason
import com.coblax.examlock.ui.exam.formatReverseEngineeringBlockReason

/**
 * One group of pre-exam checks. Each category maps 1-to-1 to a checklist section
 * and to one tile on the preparation screen. Titles are student-facing; keep them
 * short enough to fit a three-column tile grid on a 320dp phone.
 */
internal enum class PreparationCategory(
    val key: String,
    private val titleEN: String,
    private val titleID: String,
    private val descEN: String,
    private val descID: String
) {
    DeviceSetup(
        key = "checklist_device_setup",
        titleEN = "Device",
        titleID = "Perangkat",
        descEN = "Keyboard and Bluetooth",
        descID = "Keyboard dan Bluetooth"
    ),
    Connectivity(
        key = "checklist_connectivity",
        titleEN = "Network",
        titleID = "Jaringan",
        descEN = "Internet and VPN",
        descID = "Internet dan VPN"
    ),
    DeviceHealth(
        key = "checklist_device_health",
        titleEN = "System",
        titleID = "Sistem",
        descEN = "Clock and exam browser",
        descID = "Jam dan browser ujian"
    ),
    RuntimeInteraction(
        key = "checklist_runtime_interaction",
        titleEN = "Accessibility",
        titleID = "Aksesibilitas",
        descEN = "Accessibility services and floating apps",
        descID = "Layanan aksesibilitas dan aplikasi melayang"
    ),
    DeviceIntegrity(
        key = "checklist_device_integrity",
        titleEN = "Integrity",
        titleID = "Integritas",
        descEN = "USB debugging, root, and official app",
        descID = "USB debugging, root, dan aplikasi resmi"
    ),
    Clipboard(
        key = "checklist_runtime_clipboard",
        titleEN = "Clipboard",
        titleID = "Clipboard",
        descEN = "Copy and paste monitoring",
        descID = "Pemantauan salin-tempel"
    ),
    Location(
        key = "checklist_location",
        titleEN = "Location",
        titleID = "Lokasi",
        descEN = "Exam area and fake location",
        descID = "Area ujian dan lokasi palsu"
    ),
    DeviceLock(
        key = "checklist_device_lock",
        titleEN = "Screen lock",
        titleID = "Kunci layar",
        descEN = "Screen pinning and Exam Guard",
        descID = "Screen pinning dan Exam Guard"
    ),
    RuntimeSecurity(
        key = "checklist_runtime_static_security",
        titleEN = "Screen capture",
        titleID = "Rekam layar",
        descEN = "Recorders, casting, and split screen",
        descID = "Perekam, cast, dan split screen"
    );

    fun title(lang: UiLanguage): String = localized(lang, titleEN, titleID)
    fun description(lang: UiLanguage): String = localized(lang, descEN, descID)
}

/**
 * Order in which categories needing attention are listed. Screen pinning comes
 * last on purpose: once pinning is active, Android blocks the external Settings
 * screens every other fix depends on.
 */
internal val PreparationCategoryFixOrder: List<PreparationCategory> = listOf(
    PreparationCategory.DeviceIntegrity,
    PreparationCategory.DeviceHealth,
    PreparationCategory.Connectivity,
    PreparationCategory.RuntimeInteraction,
    PreparationCategory.DeviceSetup,
    PreparationCategory.Location,
    PreparationCategory.RuntimeSecurity,
    PreparationCategory.Clipboard,
    PreparationCategory.DeviceLock
)

internal fun PreparationCategory.preparationSection(): PreparationSection {
    return when (this) {
        PreparationCategory.DeviceSetup -> PreparationSection.DeviceSetup
        PreparationCategory.Connectivity -> PreparationSection.Connectivity
        PreparationCategory.DeviceHealth -> PreparationSection.DeviceHealth
        PreparationCategory.RuntimeInteraction -> PreparationSection.RuntimeInteraction
        PreparationCategory.DeviceIntegrity -> PreparationSection.DeviceIntegrity
        PreparationCategory.Clipboard -> PreparationSection.Clipboard
        PreparationCategory.Location -> PreparationSection.Location
        PreparationCategory.DeviceLock -> PreparationSection.DeviceLock
        PreparationCategory.RuntimeSecurity -> PreparationSection.RuntimeSecurity
    }
}

/**
 * Filters quick fix actions relevant to one category. Section metadata is the source
 * of truth; target/code-prefix matching is only a fallback for actions that have not
 * been tagged, so a tagged action never shows up in two categories.
 */
internal fun filterQuickFixActionsForCategory(
    category: PreparationCategory,
    allActions: List<PreparationQuickFixAction>
): List<PreparationQuickFixAction> {
    val relevantSection = category.preparationSection()
    val relevantTargets = when (category) {
        PreparationCategory.Connectivity -> setOf(QuickFixTarget.Network)
        PreparationCategory.DeviceHealth -> setOf(QuickFixTarget.DeviceTime, QuickFixTarget.WebView)
        PreparationCategory.Location -> setOf(QuickFixTarget.Location)
        PreparationCategory.DeviceLock -> setOf(QuickFixTarget.ScreenPinning)
        PreparationCategory.RuntimeSecurity -> setOf(
            QuickFixTarget.ScreenRecorder,
            QuickFixTarget.DisplayMirror,
            QuickFixTarget.MultiWindow
        )
        PreparationCategory.DeviceSetup,
        PreparationCategory.RuntimeInteraction,
        PreparationCategory.DeviceIntegrity,
        PreparationCategory.Clipboard -> emptySet()
    }
    val relevantCodePrefixes = when (category) {
        PreparationCategory.DeviceSetup -> listOf("quick_fix_60", "quick_fix_65", "quick_fix_200", "quick_fix_205")
        PreparationCategory.RuntimeInteraction -> listOf("quick_fix_35", "quick_fix_220", "quick_fix_225")
        PreparationCategory.DeviceIntegrity -> listOf(
            "quick_fix_20", "adb_insecure_property", "root_detected",
            "selinux_permissive", "virtual_env_detected", "quick_fix_15"
        )
        PreparationCategory.Clipboard -> listOf("clipboard")
        PreparationCategory.RuntimeSecurity -> listOf("app_switch_violations")
        PreparationCategory.DeviceLock -> listOf("quick_fix_36")
        else -> emptyList()
    }

    return allActions.filter { action ->
        // The global "check all again" lives in the top bar, not in a category.
        if (action.code == QuickFixRefreshAllSecurityChecksCode) return@filter false
        if (action.section != null) return@filter action.section == relevantSection
        if (action.target != null && action.target in relevantTargets) return@filter true
        relevantCodePrefixes.any { prefix -> action.code.startsWith(prefix) }
    }
}

/**
 * Quick fixes whose only effect is re-running the checks, yet whose label is advice
 * ("Use a physical device", "Contact admin"). As buttons they promise something they
 * cannot do; the issue text already says what to do and the top bar re-checks.
 */
internal val AdviceOnlyQuickFixCodes: Set<String> = setOf("virtual_env_detected", "selinux_permissive")

/** Label for a quick fix button: the short field label when one exists. */
internal fun PreparationQuickFixAction.buttonLabel(): String = fieldText ?: text

internal enum class PreparationTone {
    Clear,
    Warning,
    Blocking
}

/**
 * One problem shown to the student. [blocking] issues mirror the readiness flags that
 * gate Start Exam exactly, so the "must fix" count always agrees with the Start button.
 */
internal data class PreparationIssue(
    val key: String,
    val title: String,
    val message: String?,
    val blocking: Boolean
)

internal data class PreparationCategoryStatus(
    val category: PreparationCategory,
    val issues: List<PreparationIssue>,
    val actions: List<PreparationQuickFixAction>
) {
    val blockingCount: Int get() = issues.count { it.blocking }
    val warningCount: Int get() = issues.count { !it.blocking }
    val tone: PreparationTone
        get() = when {
            blockingCount > 0 -> PreparationTone.Blocking
            issues.isNotEmpty() -> PreparationTone.Warning
            else -> PreparationTone.Clear
        }
}

internal enum class PreparationOverallState {
    Scanning,
    Blocked,
    ReadyWithWarnings,
    Ready
}

internal data class PreparationOverview(
    /** Every category in grid order. */
    val categories: List<PreparationCategoryStatus>,
    /** Categories that need attention: blocking first, each group in fix order. */
    val attention: List<PreparationCategoryStatus>,
    val canStartExam: Boolean,
    val scanPending: Boolean
) {
    val blockingCount: Int get() = categories.sumOf { it.blockingCount }
    val warningCount: Int get() = categories.sumOf { it.warningCount }
    val clearCategoryCount: Int get() = categories.count { it.tone == PreparationTone.Clear }
    val overallState: PreparationOverallState
        get() = when {
            canStartExam && warningCount > 0 -> PreparationOverallState.ReadyWithWarnings
            canStartExam -> PreparationOverallState.Ready
            scanPending && blockingCount == 0 -> PreparationOverallState.Scanning
            else -> PreparationOverallState.Blocked
        }

    fun status(category: PreparationCategory): PreparationCategoryStatus =
        categories.first { it.category == category }
}

internal fun buildPreparationOverview(
    state: PreparationScreenState,
    readiness: PreparationChecklistReadiness,
    quickFixActions: List<PreparationQuickFixAction>,
    needsBluetoothPermission: Boolean,
    accessibilityGuardAvailable: Boolean,
    uiLanguage: UiLanguage,
    accessibilityStateLoaded: Boolean = true
): PreparationOverview {
    val issuesByCategory = buildPreparationIssues(
        state = state,
        readiness = readiness,
        needsBluetoothPermission = needsBluetoothPermission,
        accessibilityGuardAvailable = accessibilityGuardAvailable,
        accessibilityStateLoaded = accessibilityStateLoaded,
        uiLanguage = uiLanguage
    )
    val categories = PreparationCategory.entries.map { category ->
        val categoryActions = filterQuickFixActionsForCategory(category, quickFixActions)
            .filterNot { it.code in AdviceOnlyQuickFixCodes }
        val explicitIssues = issuesByCategory[category].orEmpty()
        // An action without a matching readiness issue is still a recommendation the
        // student should see, so surface it as a warning instead of dropping it.
        val fallbackIssue = categoryActions
            .firstOrNull { !it.isNotice }
            ?.takeIf { explicitIssues.isEmpty() }
            ?.let { action ->
                PreparationIssue(
                    key = "recommended_${action.code}",
                    title = localized(uiLanguage, "Recommended check", "Pemeriksaan disarankan"),
                    message = action.reason ?: action.text,
                    blocking = false
                )
            }
        PreparationCategoryStatus(
            category = category,
            issues = explicitIssues + listOfNotNull(fallbackIssue),
            actions = categoryActions
        )
    }.toMutableList()

    // Defensive: Start Exam must never be blocked without a visible reason.
    val scanPending = !readiness.staticSecurityInitialScanComplete || !accessibilityStateLoaded
    if (!readiness.canStartExam && !scanPending && categories.none { it.blockingCount > 0 }) {
        val index = categories.indexOfFirst { it.category == PreparationCategory.RuntimeSecurity }
        val target = categories[index]
        categories[index] = target.copy(
            issues = target.issues + PreparationIssue(
                key = "unresolved_blocker",
                title = resolveFirstBlockingReason(readiness, en = uiLanguage == UiLanguage.English)
                    ?: localized(uiLanguage, "Device check not passed", "Pemeriksaan perangkat belum lulus"),
                message = localized(
                    uiLanguage,
                    "Check all again. If this stays, ask the proctor for help.",
                    "Cek ulang semua. Jika tetap muncul, minta bantuan pengawas."
                ),
                blocking = true
            )
        )
    }

    val attention = PreparationCategoryFixOrder
        .map { category -> categories.first { it.category == category } }
        .filter { it.tone != PreparationTone.Clear }
        .sortedBy { if (it.tone == PreparationTone.Blocking) 0 else 1 }

    return PreparationOverview(
        categories = categories,
        attention = attention,
        canStartExam = readiness.canStartExam,
        scanPending = scanPending
    )
}

private fun buildPreparationIssues(
    state: PreparationScreenState,
    readiness: PreparationChecklistReadiness,
    needsBluetoothPermission: Boolean,
    accessibilityGuardAvailable: Boolean,
    accessibilityStateLoaded: Boolean,
    uiLanguage: UiLanguage
): Map<PreparationCategory, List<PreparationIssue>> {
    fun t(english: String, indonesian: String): String = localized(uiLanguage, english, indonesian)
    val result = mutableMapOf<PreparationCategory, MutableList<PreparationIssue>>()
    fun add(category: PreparationCategory, issue: PreparationIssue) {
        val list = result.getOrPut(category) { mutableListOf() }
        if (list.none { it.key == issue.key }) {
            list += issue
        }
    }
    fun blocking(category: PreparationCategory, key: String, title: String, message: String?) =
        add(category, PreparationIssue(key, title, message, blocking = true))
    fun warning(category: PreparationCategory, key: String, title: String, message: String?) =
        add(category, PreparationIssue(key, title, message, blocking = false))

    val device = state.device
    val bypass = state.bypass
    val runtime = state.runtimeSecurity
    val network = state.network

    // ── Device setup ─────────────────────────────────────────────
    if (!readiness.bluetoothReady) {
        if (needsBluetoothPermission && !device.bluetoothPermissionGranted) {
            blocking(
                PreparationCategory.DeviceSetup,
                "bluetooth_permission",
                t("Bluetooth permission not granted", "Izin Bluetooth belum diberikan"),
                t(
                    "Needed to confirm no Bluetooth device is connected.",
                    "Diperlukan untuk memastikan tidak ada perangkat Bluetooth tersambung."
                )
            )
        }
        if (device.bluetoothEnabled) {
            blocking(
                PreparationCategory.DeviceSetup,
                "bluetooth_on",
                t("Bluetooth is on", "Bluetooth masih aktif"),
                t("Turn Bluetooth off before the exam.", "Matikan Bluetooth sebelum ujian.")
            )
        }
    }
    if (!readiness.keyboardReady) {
        warning(
            PreparationCategory.DeviceSetup,
            "keyboard_not_allowed",
            t("Keyboard not allowed", "Keyboard belum diizinkan"),
            t("Choose an allowed system keyboard.", "Pilih keyboard sistem yang diizinkan.")
        )
    } else if (device.usingBuiltInExamKeyboard && !bypass.bypassKeyboardPolicy) {
        warning(
            PreparationCategory.DeviceSetup,
            "keyboard_builtin",
            t("System keyboard not compatible", "Keyboard sistem tidak cocok"),
            t(
                "CBX Lock will switch to its internal keyboard when the exam starts.",
                "CBX Lock akan memakai keyboard internal saat ujian dimulai."
            )
        )
    }

    // ── Connectivity ─────────────────────────────────────────────
    val verdict = network.networkReadinessStatus.verdict
    if (!readiness.vpnReady) {
        blocking(
            PreparationCategory.Connectivity,
            "vpn_active",
            t("VPN is on", "VPN masih aktif"),
            t("Turn off the VPN, then check the network again.", "Matikan VPN, lalu cek ulang jaringan.")
        )
    } else if (verdict == NetworkReadinessVerdict.VpnActive) {
        warning(
            PreparationCategory.Connectivity,
            "vpn_bypassed",
            t("VPN allowed by admin", "VPN diizinkan admin"),
            t("Use it only for approved troubleshooting.", "Gunakan hanya untuk troubleshooting resmi.")
        )
    }
    when (verdict) {
        // Start Exam refuses offline and in airplane mode (no bypass), so both are required.
        NetworkReadinessVerdict.Offline -> blocking(
            PreparationCategory.Connectivity,
            "network_offline",
            t("No internet connection", "Tidak ada koneksi internet"),
            t(
                "Turn on Wi-Fi or mobile data, then check the network again.",
                "Nyalakan Wi-Fi atau data seluler, lalu cek ulang jaringan."
            )
        )
        NetworkReadinessVerdict.Unstable -> warning(
            PreparationCategory.Connectivity,
            "network_unstable",
            t("Connection is unstable", "Koneksi tidak stabil"),
            t("A more stable network is recommended.", "Jaringan yang lebih stabil sangat disarankan.")
        )
        NetworkReadinessVerdict.CaptivePortal -> warning(
            PreparationCategory.Connectivity,
            "network_captive_portal",
            t("Network needs a sign-in", "Jaringan butuh login"),
            t(
                "Finish the Wi-Fi sign-in page before starting.",
                "Selesaikan halaman login Wi-Fi sebelum mulai."
            )
        )
        NetworkReadinessVerdict.Unvalidated -> warning(
            PreparationCategory.Connectivity,
            "network_unvalidated",
            t("Internet not confirmed yet", "Internet belum terkonfirmasi"),
            t("Internet access may still be limited.", "Akses internet mungkin masih terbatas.")
        )
        NetworkReadinessVerdict.AirplaneMode -> blocking(
            PreparationCategory.Connectivity,
            "network_airplane",
            t("Airplane mode is on", "Mode pesawat aktif"),
            t(
                "Turn off airplane mode, then connect to Wi-Fi or mobile data.",
                "Matikan mode pesawat, lalu sambungkan Wi-Fi atau data seluler."
            )
        )
        NetworkReadinessVerdict.ConnectedStable,
        NetworkReadinessVerdict.VpnActive -> Unit
    }

    // ── Device health ────────────────────────────────────────────
    if (!readiness.scheduleReady) {
        val endedAt = state.session.examEndDateTime.trim()
        blocking(
            PreparationCategory.DeviceHealth,
            "exam_schedule_ended",
            t("The exam has ended", "Waktu ujian sudah berakhir"),
            if (endedAt.isNotBlank()) {
                t("It closed at $endedAt. Ask the proctor.", "Ujian ditutup pukul $endedAt. Hubungi pengawas.")
            } else {
                t("Ask the proctor.", "Hubungi pengawas.")
            }
        )
    }
    if (!readiness.deviceTimeReady) {
        blocking(
            PreparationCategory.DeviceHealth,
            "device_time",
            t("Automatic date and time is off", "Tanggal dan waktu otomatis belum aktif"),
            t("Turn on automatic date and time.", "Aktifkan tanggal dan waktu otomatis.")
        )
    }
    val webViewHealth = state.preExamHealthCheckSnapshot.items
        .firstOrNull { it.category == PreExamHealthCategory.WebView }
    when (webViewHealth?.verdict) {
        // Start Exam refuses on this (preExamHealthStartBlocker), so it is not optional.
        PreExamHealthVerdict.Blocking -> blocking(
            PreparationCategory.DeviceHealth,
            "webview_unavailable",
            t("Exam browser (WebView) not available", "Browser ujian (WebView) tidak tersedia"),
            webViewProviderNote(state.webViewCompatibilityStatus, uiLanguage)
        )
        PreExamHealthVerdict.Warning -> warning(
            PreparationCategory.DeviceHealth,
            "webview_health",
            t("Exam browser needs attention", "Browser ujian perlu diperiksa"),
            // The health item detail is English-only, so it stays in the technical sheet.
            webViewProviderNote(state.webViewCompatibilityStatus, uiLanguage) ?: t(
                "The exam browser is being reset or was just recovered.",
                "Browser ujian sedang disiapkan ulang atau baru dipulihkan."
            )
        )
        PreExamHealthVerdict.Stable, null -> Unit
    }

    // ── Runtime interaction ──────────────────────────────────────
    if (!readiness.accessibilityReady) {
        blocking(
            PreparationCategory.RuntimeInteraction,
            "accessibility_service",
            t("Accessibility service is on", "Layanan aksesibilitas aktif"),
            t(
                "Other apps could read the exam. Turn off third-party accessibility services.",
                "Aplikasi lain bisa membaca isi ujian. Matikan layanan aksesibilitas pihak ketiga."
            )
        )
    }
    if (!readiness.overlayReady) {
        blocking(
            PreparationCategory.RuntimeInteraction,
            "overlay_risk",
            t("Floating app detected", "Aplikasi melayang terdeteksi"),
            t(
                "An app covered the exam screen earlier. Close chat bubbles and floating windows, then tap \"Closed, Check Again\".",
                "Ada aplikasi yang menutupi layar ujian sebelumnya. Tutup bubble chat dan jendela melayang, lalu ketuk \"Sudah Ditutup, Cek Ulang\"."
            )
        )
    } else {
        val overlayHealthBlocking = state.preExamHealthCheckSnapshot.items.any {
            it.category == PreExamHealthCategory.FloatingAppOverlay &&
                it.verdict == PreExamHealthVerdict.Blocking
        }
        val shield = runtime.overlayRiskResult.shieldStatus
        val overlayBypassed = bypass.bypassOverlay || runtime.overlayRiskResult.bypassed
        when {
            // Start Exam refuses on these too; say so here instead of after the tap.
            overlayHealthBlocking && shield.supported && shield.requested &&
                shield.lastApplySucceeded == false -> blocking(
                PreparationCategory.RuntimeInteraction,
                "overlay_shield_failed",
                t("Floating-app protection failed to start", "Perlindungan aplikasi melayang gagal aktif"),
                t(
                    "Check all again. If it stays, reinstall the official app or ask the proctor.",
                    "Cek ulang semua. Jika tetap muncul, instal ulang aplikasi resmi atau hubungi pengawas."
                )
            )
            overlayHealthBlocking -> blocking(
                PreparationCategory.RuntimeInteraction,
                "overlay_protection_not_ready",
                t("Floating-app protection is not ready", "Perlindungan aplikasi melayang belum siap"),
                t(
                    "Check all again. If it stays, ask the proctor.",
                    "Cek ulang semua. Jika tetap muncul, hubungi pengawas."
                )
            )
            // Android 7-11 on a personal phone: floating apps cannot be hidden, only
            // detected. Allowed by policy, so the student is told rather than stopped.
            !overlayBypassed &&
                runtime.dpcRuntimeStatus.protectionTier == DpcProtectionTier.None &&
                !shield.supported -> warning(
                PreparationCategory.RuntimeInteraction,
                "overlay_protection_limited",
                t(
                    "Floating apps can't be hidden on this Android",
                    "Android ini tidak bisa menyembunyikan aplikasi melayang"
                ),
                t(
                    "Close chat bubbles and floating windows before starting. The exam can still start.",
                    "Tutup bubble chat dan jendela mengambang sebelum mulai. Ujian tetap bisa dimulai."
                )
            )
        }
    }

    // ── Device integrity ─────────────────────────────────────────
    if (!readiness.adbReady) {
        if (device.adbInspection.blocking) {
            // Start Exam refuses on Developer options alone, so turning off only USB
            // debugging is not enough; switching Developer options off clears every case.
            val adb = device.adbInspection
            blocking(
                PreparationCategory.DeviceIntegrity,
                "adb_enabled",
                when {
                    adb.adbEnabled -> t("USB debugging is on", "USB debugging masih aktif")
                    adb.wirelessAdbEnabled -> t("Wireless debugging is on", "Wireless debugging masih aktif")
                    else -> t("Developer options are on", "Opsi pengembang masih aktif")
                },
                t(
                    "Turn off the \"Developer options\" switch at the top of Developer options.",
                    "Matikan tombol \"Opsi pengembang\" di bagian atas halaman Opsi pengembang."
                )
            )
        } else {
            blocking(
                PreparationCategory.DeviceIntegrity,
                "adb_insecure_property",
                t("ADB system setting is not secure", "Pengaturan sistem ADB tidak aman"),
                t("Check Developer options or ask the admin.", "Periksa Opsi pengembang atau hubungi admin.")
            )
        }
    }
    if (!readiness.rootReady) {
        blocking(
            PreparationCategory.DeviceIntegrity,
            "root_detected",
            t("Device is rooted", "Perangkat di-root"),
            t("Use a device that is not rooted.", "Gunakan HP yang tidak di-root.")
        )
    } else if (!bypass.bypassRoot && device.rootSecurityStatus.detected) {
        warning(
            PreparationCategory.DeviceIntegrity,
            "root_signal",
            t("Root signs found", "Tanda root ditemukan"),
            t(
                "The exam can continue, but device security is reduced.",
                "Ujian bisa lanjut, tetapi keamanan HP berkurang."
            )
        )
    } else if (!bypass.bypassRoot && device.rootSecurityStatus.selinuxPermissive) {
        warning(
            PreparationCategory.DeviceIntegrity,
            "selinux_permissive",
            t("SELinux is permissive", "SELinux permisif"),
            t(
                "The exam can continue, but device security is reduced.",
                "Ujian bisa lanjut, tetapi keamanan HP berkurang."
            )
        )
    }
    if (!readiness.virtualEnvironmentReady) {
        blocking(
            PreparationCategory.DeviceIntegrity,
            "virtual_environment",
            t("Emulator detected", "Emulator terdeteksi"),
            t("The exam must run on a physical phone.", "Ujian harus memakai HP fisik.")
        )
    }
    val reverseEngineeringReason = formatReverseEngineeringBlockReason(runtime.reverseEngineeringSummary)
    if (!readiness.reverseEngineeringReady) {
        blocking(
            PreparationCategory.DeviceIntegrity,
            "reverse_engineering",
            t("Debugging or hooking tool detected", "Tool debugging atau hooking terdeteksi"),
            t(
                "Reason: $reverseEngineeringReason. Close debugger, tracer, hooking or root tools, then reopen the app.",
                "Alasan: $reverseEngineeringReason. Tutup debugger, tracer, tool hooking/root, lalu buka ulang aplikasi."
            )
        )
    } else if (runtime.reverseEngineeringDetected) {
        warning(
            PreparationCategory.DeviceIntegrity,
            "reverse_engineering_bypassed",
            t("Reverse engineering check skipped by admin", "Cek reverse engineering dilewati admin"),
            t(
                "Reason: $reverseEngineeringReason. Detection is still logged.",
                "Alasan: $reverseEngineeringReason. Deteksi tetap dicatat."
            )
        )
    }
    val integritySummary = runtime.integritySummary
        .takeIf { it.isNotBlank() && it != "-" }
        ?: if (device.signatureMismatchDetected) "signature_changed" else "-"
    val integrityReason = formatApkIntegrityBlockReason(integritySummary)
    if (!readiness.integrityReady) {
        blocking(
            PreparationCategory.DeviceIntegrity,
            "apk_integrity",
            t("App is not the official version", "Aplikasi bukan versi resmi"),
            t(
                "Reason: $integrityReason. Reinstall the official APK, then reopen the app.",
                "Alasan: $integrityReason. Instal ulang APK resmi, lalu buka ulang aplikasi."
            )
        )
    } else if (runtime.integrityDetected || device.signatureMismatchDetected) {
        warning(
            PreparationCategory.DeviceIntegrity,
            "apk_integrity_bypassed",
            t("App integrity check skipped by admin", "Cek integritas aplikasi dilewati admin"),
            t(
                "Reason: $integrityReason. Detection is still logged.",
                "Alasan: $integrityReason. Deteksi tetap dicatat."
            )
        )
    }

    // ── Location ─────────────────────────────────────────────────
    val permissionTitle = t("Location permission not granted", "Izin lokasi belum diberikan")
    val permissionMessage = t(
        "Allow location so your exam position can be verified.",
        "Izinkan lokasi agar posisi ujian bisa diverifikasi."
    )
    val servicesTitle = t("Location is off", "Lokasi (GPS) mati")
    val servicesMessage = t("Turn on location in quick settings.", "Nyalakan lokasi di pengaturan cepat.")
    val waitForFixMessage = t(
        "Move near a window or outdoors, then check the location again.",
        "Pindah ke dekat jendela atau tempat terbuka, lalu cek ulang lokasi."
    )
    if (!readiness.geofenceReady) {
        when (state.location.geofenceRuntimeStatus.securityStatus.finalVerdict) {
            GeofenceSecurityVerdict.PermissionMissing -> blocking(
                PreparationCategory.Location, "location_permission", permissionTitle, permissionMessage
            )
            GeofenceSecurityVerdict.PreciseRequired -> blocking(
                PreparationCategory.Location,
                "location_precise",
                t("Precise location not allowed", "Lokasi presisi belum diizinkan"),
                t("Turn on precise location for CBX Lock.", "Aktifkan lokasi presisi untuk CBX Lock.")
            )
            GeofenceSecurityVerdict.LocationDisabled -> blocking(
                PreparationCategory.Location, "location_services", servicesTitle, servicesMessage
            )
            GeofenceSecurityVerdict.Outside -> blocking(
                PreparationCategory.Location,
                "geofence_outside",
                t("Outside the exam area", "Di luar area ujian"),
                t(
                    "Move inside the exam area, then check the location again.",
                    "Pindah ke dalam area ujian, lalu cek ulang lokasi."
                )
            )
            GeofenceSecurityVerdict.NoFix,
            GeofenceSecurityVerdict.StaleFix -> blocking(
                PreparationCategory.Location,
                "geofence_no_fix",
                t("Location not found yet", "Lokasi belum didapat"),
                waitForFixMessage
            )
            GeofenceSecurityVerdict.LowAccuracy,
            GeofenceSecurityVerdict.MissingAccuracy -> blocking(
                PreparationCategory.Location,
                "geofence_accuracy",
                t("Location is not accurate enough", "Lokasi kurang akurat"),
                waitForFixMessage
            )
            GeofenceSecurityVerdict.ConfigInvalid -> blocking(
                PreparationCategory.Location,
                "geofence_config",
                t("Exam area setup is invalid", "Pengaturan area ujian tidak valid"),
                t("Ask the proctor or admin.", "Hubungi pengawas atau admin.")
            )
            GeofenceSecurityVerdict.Bypassed,
            GeofenceSecurityVerdict.Disabled,
            GeofenceSecurityVerdict.Inside -> blocking(
                PreparationCategory.Location,
                "geofence_failed",
                t("Exam area check not passed", "Pemeriksaan area ujian belum lulus"),
                waitForFixMessage
            )
        }
    }
    if (!readiness.fakeLocationReady) {
        when (state.location.fakeLocationRuntimeStatus.securityStatus.finalVerdict) {
            LocationSpoofSecurityVerdict.PermissionRequired -> blocking(
                PreparationCategory.Location, "location_permission", permissionTitle, permissionMessage
            )
            LocationSpoofSecurityVerdict.LocationServicesDisabled -> blocking(
                PreparationCategory.Location, "location_services", servicesTitle, servicesMessage
            )
            LocationSpoofSecurityVerdict.LocationUnavailable -> blocking(
                PreparationCategory.Location,
                "fake_location_unavailable",
                t("Location not available yet", "Lokasi belum tersedia"),
                waitForFixMessage
            )
            // Only blocks while Developer options are on (see fakeLocationReady), so turning
            // mock location off alone does not clear it; say what actually does.
            LocationSpoofSecurityVerdict.PackageWarning -> {
                val packages = state.location.fakeLocationRuntimeStatus.securityStatus
                    .suspiciousFakeLocationPackages
                    .take(2)
                    .joinToString(", ")
                blocking(
                    PreparationCategory.Location,
                    "fake_location_app",
                    t("Fake location app found", "Aplikasi lokasi palsu terpasang"),
                    if (packages.isBlank()) {
                        t(
                            "Uninstall the fake GPS app, or turn off Developer options.",
                            "Hapus aplikasi GPS palsu, atau matikan Opsi pengembang."
                        )
                    } else {
                        t(
                            "Uninstall $packages, or turn off Developer options.",
                            "Hapus $packages, atau matikan Opsi pengembang."
                        )
                    }
                )
            }
            LocationSpoofSecurityVerdict.SpoofDetected,
            LocationSpoofSecurityVerdict.Bypassed,
            LocationSpoofSecurityVerdict.Disabled,
            LocationSpoofSecurityVerdict.Safe -> blocking(
                PreparationCategory.Location,
                "fake_location",
                t("Fake location detected", "Lokasi palsu terdeteksi"),
                t(
                    "Turn off fake location apps and mock location.",
                    "Matikan aplikasi lokasi palsu dan mock location."
                )
            )
        }
    }

    // ── Device lock ──────────────────────────────────────────────
    if (!readiness.screenPinningReady) {
        when {
            device.screenPinningAvailable -> blocking(
                PreparationCategory.DeviceLock,
                "screen_pinning",
                t("Screen pinning not active", "Screen pinning belum aktif"),
                t(
                    "Turn it on as the last step, after the other fixes.",
                    "Aktifkan sebagai langkah terakhir, setelah perbaikan lain beres."
                )
            )
            // Exam Guard is the fallback and its own issue below explains the fix. Until the
            // first accessibility inspection lands, availability is unknown, not "missing".
            accessibilityGuardAvailable || !accessibilityStateLoaded -> Unit
            else -> blocking(
                PreparationCategory.DeviceLock,
                "screen_lock_unavailable",
                t("Screen lock not available", "Kunci layar tidak tersedia"),
                t(
                    "This phone supports neither screen pinning nor Exam Guard. Ask the proctor.",
                    "HP ini tidak mendukung screen pinning maupun Exam Guard. Hubungi pengawas."
                )
            )
        }
    }
    if (!readiness.accessibilityGuardReady) {
        blocking(
            PreparationCategory.DeviceLock,
            "exam_guard",
            t("Exam Guard is off", "Exam Guard belum aktif"),
            t(
                "Turn on CBX Lock Exam Guard in Accessibility settings.",
                "Aktifkan CBX Lock Exam Guard di pengaturan Aksesibilitas."
            )
        )
    }

    // ── Runtime security ─────────────────────────────────────────
    if (!readiness.screenRecorderReady) {
        // Detection is about installed apps, so closing the recorder does not clear it.
        val recorders = runtime.screenRecorderPackages.take(3).joinToString(", ")
        blocking(
            PreparationCategory.RuntimeSecurity,
            "screen_recorder",
            t("Screen recorder app installed", "Aplikasi perekam layar terpasang"),
            if (recorders.isBlank()) {
                t("Uninstall or disable the screen recorder app.", "Hapus atau nonaktifkan aplikasi perekam layar.")
            } else {
                t("Uninstall or disable: $recorders.", "Hapus atau nonaktifkan: $recorders.")
            }
        )
    }
    if (!readiness.displayMirrorReady) {
        blocking(
            PreparationCategory.RuntimeSecurity,
            "display_mirror",
            t("Screen is being cast or mirrored", "Layar sedang di-cast atau mirror"),
            t(
                "Turn off casting, screen mirroring, or external displays.",
                "Matikan cast, screen mirroring, atau layar eksternal."
            )
        )
    }
    if (!readiness.multiWindowReady) {
        blocking(
            PreparationCategory.RuntimeSecurity,
            "multi_window",
            t("Split screen is on", "Split screen aktif"),
            t(
                "Exit split screen or floating windows.",
                "Keluar dari split screen atau jendela mengambang."
            )
        )
    }
    if (!readiness.appSwitchReady) {
        warning(
            PreparationCategory.RuntimeSecurity,
            "app_switch",
            t("App switch violation recorded", "Pelanggaran pindah aplikasi tercatat"),
            t("Check again to clear it.", "Cek ulang untuk menghapusnya.")
        )
    }

    return result
}

/**
 * The student-facing line for the WebView provider, in the UI language. Null when the
 * provider is fine; the English health detail stays in the technical sheet.
 */
internal fun webViewProviderNote(status: WebViewCompatibilityStatus, uiLanguage: UiLanguage): String? {
    fun t(english: String, indonesian: String): String = localized(uiLanguage, english, indonesian)
    return when (status.verdict) {
        WebViewHealthVerdict.Unavailable -> t(
            "Install or enable Android System WebView or Chrome, then reopen CBX Lock.",
            "Pasang atau aktifkan Android System WebView atau Chrome, lalu buka ulang CBX Lock."
        )
        WebViewHealthVerdict.NeedsUpdate -> t(
            "${status.providerLabel} ${status.versionLabel} is old. Update it from Play Store before a long exam.",
            "${status.providerLabel} ${status.versionLabel} sudah lama. Perbarui di Play Store sebelum ujian panjang."
        )
        WebViewHealthVerdict.Unknown -> t(
            "${status.providerLabel} version could not be checked. Updating it from Play Store is recommended.",
            "Versi ${status.providerLabel} tidak bisa dicek. Disarankan perbarui di Play Store."
        )
        WebViewHealthVerdict.Ready -> null
    }
}
