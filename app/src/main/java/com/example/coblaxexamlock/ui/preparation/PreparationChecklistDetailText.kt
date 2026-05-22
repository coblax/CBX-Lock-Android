package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.AccessibilityInspectionResult
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
import com.example.coblaxexamlock.accessibilityBlockingCauseText
import com.example.coblaxexamlock.accessibilityBlockingFixText
import com.example.coblaxexamlock.accessibilityServiceFriendlySummary
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.model.UiLanguage

internal data class PreparationChecklistDetailText(
    val keyboardDetail: String?,
    val bluetoothDetail: String?,
    val accessibilityDetail: String?,
    val overlayDetail: String?,
    val developerDetail: String?,
    val rootDetail: String?,
    val signatureDetail: String?,
    val virtualEnvironmentDetail: String?,
    val clipboardDetail: String?,
    val screenPinningDetail: String?,
    val accessibilityGuardDetail: String?,
    val appSwitchDetail: String?
)

internal fun preparationDetailOrNull(
    showChecklistDetails: Boolean,
    uiLanguage: UiLanguage,
    english: () -> String,
    indonesian: () -> String
): String? {
    if (!showChecklistDetails) {
        return null
    }
    return if (uiLanguage == UiLanguage.English) english() else indonesian()
}

internal fun buildPreparationChecklistDetailText(
    state: PreparationScreenState,
    uiLanguage: UiLanguage,
    accessibilityInspection: AccessibilityInspectionResult,
    accessibilityGuardEnabled: Boolean,
    activeWizardStep: WizardStep? = null
): PreparationChecklistDetailText = with(state) {
    fun shouldBuildFor(step: WizardStep): Boolean =
        activeWizardStep == null || activeWizardStep == step
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
    fun preparationDetailOrNull(english: () -> String, indonesian: () -> String): String? =
        preparationDetailOrNull(showChecklistDetails, uiLanguage, english, indonesian)
    val keyboardDetail = if (shouldBuildFor(WizardStep.DeviceSetup)) preparationDetailOrNull(
        english = {
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
            "- If keyboard changes during exam -> violation + alarm"
        },
        indonesian = {
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
        }
    ) else null
    val bluetoothDetail = if (shouldBuildFor(WizardStep.DeviceSetup)) preparationDetailOrNull(
        english = {
        "Checked:\n" +
            "- Permission BLUETOOTH_CONNECT (Android 12+)\n" +
            "- Bluetooth adapter enabled state\n" +
            "- Listener for BluetoothAdapter.ACTION_STATE_CHANGED during exam\n" +
            "Impact:\n" +
            "- Start blocked if permission missing or Bluetooth enabled\n" +
            "- If enabled during exam -> violation + alarm"
        },
        indonesian = {
        "Dicek:\n" +
            "- Izin BLUETOOTH_CONNECT (Android 12+)\n" +
            "- Status adapter Bluetooth\n" +
            "- Listener BluetoothAdapter.ACTION_STATE_CHANGED saat ujian\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika izin belum ada atau Bluetooth aktif\n" +
            "- Jika aktif saat ujian -> pelanggaran + alarm"
        }
    ) else null
    val accessibilityActionDetail = if (
        shouldBuildFor(WizardStep.RuntimeInteraction) &&
        !bypassAccessibility &&
        accessibilityInspection.blockingServiceActive
    ) {
        accessibilityBlockingCauseText(accessibilityInspection, uiLanguage) +
            "\n" +
            accessibilityBlockingFixText(accessibilityInspection, uiLanguage)
    } else {
        null
    }
    val accessibilityAuditDetail = if (shouldBuildFor(WizardStep.RuntimeInteraction)) preparationDetailOrNull(
        english = {
        "Checked:\n" +
            "- AccessibilityManager.isEnabled\n" +
            "- Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES not empty\n" +
            "- Likely active service: ${
                accessibilityServiceFriendlySummary(
                    accessibilityInspection.effectiveServiceComponents,
                    includePackage = true
                )
            }\n" +
            "- Active packages: ${preparationListSummary(enabledAccessibilityPackages)}\n" +
            "- Allowed services: ${preparationListSummary(allowedAccessibilityServices)}\n" +
            "- Allowed packages: ${preparationListSummary(allowedAccessibilityPackages)}\n" +
            "- Effective packages after allowlist: ${preparationListSummary(effectiveAccessibilityPackages)}\n" +
            "- Risky keywords: ${preparationListSummary(RiskyAccessibilityKeywords)}\n" +
            "- Risky packages matched: ${preparationListSummary(riskyAccessibilityPackages)}\n" +
            "Common causes:\n" +
            "- Select to Speak, TalkBack, Switch Access, Voice Access\n" +
            "- Auto clicker, app lock, antivirus/cleaner, floating menu, or OEM assistant service\n" +
            "Impact:\n" +
            "- Start blocked if accessibility service active\n" +
            "- If enabled during exam -> warning + alarm"
        },
        indonesian = {
        "Dicek:\n" +
            "- AccessibilityManager.isEnabled\n" +
            "- Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES tidak kosong\n" +
            "- Kemungkinan service aktif: ${
                accessibilityServiceFriendlySummary(
                    accessibilityInspection.effectiveServiceComponents,
                    includePackage = true
                )
            }\n" +
            "- Paket aktif: ${preparationListSummary(enabledAccessibilityPackages)}\n" +
            "- Service yang diizinkan: ${preparationListSummary(allowedAccessibilityServices)}\n" +
            "- Paket yang diizinkan: ${preparationListSummary(allowedAccessibilityPackages)}\n" +
            "- Paket efektif setelah allowlist: ${preparationListSummary(effectiveAccessibilityPackages)}\n" +
            "- Keyword berisiko: ${preparationListSummary(RiskyAccessibilityKeywords)}\n" +
            "- Paket berisiko terdeteksi: ${preparationListSummary(riskyAccessibilityPackages)}\n" +
            "Kemungkinan penyebab:\n" +
            "- Select to Speak/Pilih untuk Diucapkan, TalkBack, Akses Tombol/Switch Access, Voice Access\n" +
            "- Auto clicker, app lock, antivirus/cleaner, floating menu, atau service bantuan bawaan vendor\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika aksesibilitas aktif\n" +
            "- Jika aktif saat ujian -> peringatan + alarm"
        }
    ) else null
    val accessibilityDetail = appendPreparationAuditDetail(
        actionDetail = accessibilityActionDetail,
        auditDetail = accessibilityAuditDetail
    )
    val overlayDetail = if (shouldBuildFor(WizardStep.RuntimeInteraction)) preparationDetailOrNull(
        english = {
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
            "- Confirmed obscured touch or suspicious focus loss triggers alarm + acknowledge dialog"
        },
        indonesian = {
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
        }
    ) else null
    val developerDetail = if (shouldBuildFor(WizardStep.DeviceIntegrity)) preparationDetailOrNull(
        english = {
        "Checked:\n" +
            "- Settings.Global.DEVELOPMENT_SETTINGS_ENABLED = ${adbInspection.developerOptionsRawValue}\n" +
            "- Settings.Global.ADB_ENABLED = ${adbInspection.adbRawValue}\n" +
            "- ro.adb.secure = ${adbInspection.adbSecureProperty}\n" +
            "- Integrity hint = ${adbInspection.integrityHintSummary}\n" +
            "Impact:\n" +
            "- Start blocked if Developer Mode or ADB enabled\n" +
            "- If enabled during exam -> warning + alarm"
        },
        indonesian = {
        "Dicek:\n" +
            "- Settings.Global.DEVELOPMENT_SETTINGS_ENABLED = ${adbInspection.developerOptionsRawValue}\n" +
            "- Settings.Global.ADB_ENABLED = ${adbInspection.adbRawValue}\n" +
            "- ro.adb.secure = ${adbInspection.adbSecureProperty}\n" +
            "- Hint integritas = ${adbInspection.integrityHintSummary}\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika Developer Mode atau ADB aktif\n" +
            "- Jika aktif saat ujian -> peringatan + alarm"
        }
    ) else null
    val rootDetail = if (shouldBuildFor(WizardStep.DeviceIntegrity)) preparationDetailOrNull(
        english = {
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
            "- If detected during exam -> warning + alarm"
        },
        indonesian = {
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
        }
    ) else null
    val signatureDetail = if (shouldBuildFor(WizardStep.DeviceIntegrity)) preparationDetailOrNull(
        english = {
        "Checked:\n" +
            "- SHA-256 fingerprint of signing certificate\n" +
            "- Expected fingerprints: release (and debug when BuildConfig.DEBUG)\n" +
            "Impact:\n" +
            "- Mismatch blocks start and prompts reinstall official APK"
        },
        indonesian = {
        "Dicek:\n" +
            "- Fingerprint SHA-256 sertifikat penandatangan APK\n" +
            "- Fingerprint expected: rilis (dan debug saat BuildConfig.DEBUG)\n" +
            "Dampak:\n" +
            "- Tidak cocok -> blok mulai ujian dan sarankan reinstall APK resmi"
        }
    ) else null
    val virtualEnvironmentDetail = if (shouldBuildFor(WizardStep.DeviceIntegrity)) preparationDetailOrNull(
        english = {
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
            "- If detected during exam -> warning + alarm"
        },
        indonesian = {
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
        }
    ) else null
    val clipboardDetail = if (shouldBuildFor(WizardStep.Clipboard)) preparationDetailOrNull(
        english = {
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
            "- Clipboard changes trigger alarm (does not block start)"
        },
        indonesian = {
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
        }
    ) else null
    val screenPinningDetail = if (shouldBuildFor(WizardStep.DeviceLock)) preparationDetailOrNull(
        english = {
        "Checked:\n" +
            "- lock_to_app_enabled from Settings.System then Settings.Secure\n" +
            "- ActivityManager.lockTaskModeState (LOCKED/PINNED)\n" +
            "- Screen Pinning support available: ${if (screenPinningAvailable) "Yes" else "No"}\n" +
            "Impact:\n" +
            "- Available but inactive -> Start Exam stays blocked until Start Screen Pinning succeeds\n" +
            "- If Android does not show the pinning dialog, use Screen Pinning settings as a fallback\n" +
            "- Unavailable -> Start Exam is blocked; use a supported device or Secret Admin bypass\n" +
            "- If bypass enabled -> skip pin/lock-task flow"
        },
        indonesian = {
        "Dicek:\n" +
            "- lock_to_app_enabled dari Settings.System lalu Settings.Secure\n" +
            "- ActivityManager.lockTaskModeState (LOCKED/PINNED)\n" +
            "- Dukungan Screen Pinning tersedia: ${if (screenPinningAvailable) "Ya" else "Tidak"}\n" +
            "Dampak:\n" +
            "- Tersedia tapi belum aktif -> Mulai Ujian tetap diblokir sampai Start Screen Pinning berhasil\n" +
            "- Jika Android tidak menampilkan dialog pinning, gunakan pengaturan Screen Pinning sebagai fallback\n" +
            "- Tidak tersedia -> Start Exam diblokir; gunakan perangkat yang mendukung atau bypass Secret Admin\n" +
            "- Jika bypass aktif -> lewati alur pin/lock-task"
        }
    ) else null
    val accessibilityGuardDetail = if (shouldBuildFor(WizardStep.DeviceLock)) preparationDetailOrNull(
        english = {
        "Checked:\n" +
            "- CBX Lock Exam Guard accessibility service enabled: ${if (accessibilityGuardEnabled) "Yes" else "No"}\n" +
            "- Required only when Screen Pinning is unavailable and Screen Pinning bypass is off\n" +
                "- Events monitored: TYPE_WINDOW_STATE_CHANGED, TYPE_WINDOWS_CHANGED, TYPE_NOTIFICATION_STATE_CHANGED\n" +
            "- Screen text is not read\n" +
            "Impact:\n" +
            "- If required and disabled -> Start Exam is blocked\n" +
            "- During fallback mode, app switches are logged and the app returns to the exam with escalating alarm"
        },
        indonesian = {
        "Dicek:\n" +
            "- Service aksesibilitas CBX Lock Exam Guard aktif: ${if (accessibilityGuardEnabled) "Ya" else "Tidak"}\n" +
            "- Wajib hanya saat Screen Pinning tidak tersedia dan bypass Screen Pinning nonaktif\n" +
                "- Event yang dipantau: TYPE_WINDOW_STATE_CHANGED, TYPE_WINDOWS_CHANGED, TYPE_NOTIFICATION_STATE_CHANGED\n" +
            "- Teks layar tidak dibaca\n" +
            "Dampak:\n" +
            "- Jika wajib tetapi nonaktif -> Start Exam diblokir\n" +
            "- Saat mode fallback, app switch dicatat dan app kembali ke ujian dengan alarm eskalatif"
        }
    ) else null
    val appSwitchDetail = if (shouldBuildFor(WizardStep.RuntimeSecurity)) preparationDetailOrNull(
        english = {
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
            "- If bypass enabled -> App Switch monitoring is skipped"
        },
        indonesian = {
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
        }
    ) else null


    PreparationChecklistDetailText(
        keyboardDetail = keyboardDetail,
        bluetoothDetail = bluetoothDetail,
        accessibilityDetail = accessibilityDetail,
        overlayDetail = overlayDetail,
        developerDetail = developerDetail,
        rootDetail = rootDetail,
        signatureDetail = signatureDetail,
        virtualEnvironmentDetail = virtualEnvironmentDetail,
        clipboardDetail = clipboardDetail,
        screenPinningDetail = screenPinningDetail,
        accessibilityGuardDetail = accessibilityGuardDetail,
        appSwitchDetail = appSwitchDetail
    )
}
