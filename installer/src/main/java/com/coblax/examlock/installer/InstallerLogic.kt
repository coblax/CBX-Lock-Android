package com.coblax.examlock.installer

import java.util.Locale

internal const val CbxPackageName = "com.coblax.examlock"

/** The CBX APK this installer carries, as measured when it was built. */
internal data class BundledCbx(
    val size: Long,
    val sha256: String,
    val versionCode: Long,
    val versionName: String,
    val minSdk: Int,
    val abis: Set<String>
) {
    companion object {
        fun parse(properties: Map<String, String>): BundledCbx = BundledCbx(
            size = properties.getValue("size").toLong(),
            sha256 = normalizeHex(properties.getValue("sha256")),
            versionCode = properties.getValue("versionCode").toLong(),
            versionName = properties.getValue("versionName"),
            minSdk = properties.getValue("minSdk").toInt(),
            abis = properties.getValue("abis").split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        )
    }
}

internal data class DeviceFacts(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val sdkInt: Int,
    val release: String,
    val supportedAbis: List<String>,
    val freeBytes: Long,
    val romDisplay: String,
    val securityPatch: String = "",
    val fingerprint: String = "",
    val hardware: String = "",
    val socModel: String = "",
    val totalRamBytes: Long = -1L,
    val locale: String = "",
    val timezone: String = "",
    /** The app that installed this installer (WhatsApp, a file manager, …), when Android says. */
    val installerSource: String = "",
    /** Whether this installer may install apps; null before Android 8, where it is global. */
    val canInstallPackages: Boolean? = null,
    /** Device-policy restrictions that forbid installing apps (MDM, parental, work profile). */
    val installRestrictions: List<String> = emptyList()
)

/** CBX already on the phone, if any. */
internal data class InstalledCbx(
    val versionCode: Long,
    val versionName: String?,
    val certSha256: String?,
    /** False when the phone froze or disabled it (cleaner apps, "app freeze" features). */
    val enabled: Boolean = true,
    val installerSource: String? = null
)

/** What reading the bundled APK on this phone found. */
internal data class BundleInspection(
    val actualSha256: String?,
    /** The phone's own parser could read the APK (PackageManager.getPackageArchiveInfo). */
    val parsedByPhone: Boolean,
    val certSha256: String?,
    val error: String? = null
)

internal enum class PrecheckProblem(val code: String) {
    AndroidTooOld("android_too_old"),
    CpuNotSupported("cpu_not_supported"),
    NotEnoughStorage("storage_low"),
    BundleDamaged("bundle_damaged"),
    BundleNotOfficial("bundle_not_official"),
    UnreadableOnThisPhone("unreadable_on_this_phone"),
    OtherCbxSignature("other_cbx_signature"),
    NewerCbxInstalled("newer_cbx_installed"),
    InstallBlockedByPolicy("install_blocked_by_policy"),
    CbxDisabled("cbx_disabled")
}

/** Space the install needs: the session copy, the installed APK and room to optimize it. */
internal fun requiredFreeBytes(bundle: BundledCbx): Long = bundle.size * 3 + 20L * 1024 * 1024

/**
 * Problems that make the install fail for certain, found before trying, so the student
 * gets the real reason instead of "there was a problem parsing the package".
 */
internal fun evaluatePrechecks(
    bundle: BundledCbx,
    device: DeviceFacts,
    installed: InstalledCbx?,
    inspection: BundleInspection,
    officialCertSha256: String
): List<PrecheckProblem> {
    val problems = mutableListOf<PrecheckProblem>()
    val tooOld = device.sdkInt < bundle.minSdk
    if (tooOld) problems += PrecheckProblem.AndroidTooOld
    if (bundle.abis.isNotEmpty() && device.supportedAbis.none { it in bundle.abis }) {
        problems += PrecheckProblem.CpuNotSupported
    }
    if (device.freeBytes in 0 until requiredFreeBytes(bundle)) {
        problems += PrecheckProblem.NotEnoughStorage
    }
    if (device.installRestrictions.isNotEmpty()) {
        problems += PrecheckProblem.InstallBlockedByPolicy
    }
    val intact = inspection.actualSha256 != null && normalizeHex(inspection.actualSha256) == bundle.sha256
    if (!intact) {
        problems += PrecheckProblem.BundleDamaged
    } else if (!inspection.parsedByPhone && !tooOld) {
        // The bytes are exactly what was built, yet this phone cannot read them: the
        // phone's installer is the problem, not the download.
        problems += PrecheckProblem.UnreadableOnThisPhone
    }
    val official = normalizeHex(officialCertSha256)
    val bundleCert = inspection.certSha256?.let(::normalizeHex)
    if (intact && official.isNotEmpty() && bundleCert != null && bundleCert != official) {
        problems += PrecheckProblem.BundleNotOfficial
    }
    if (installed != null) {
        val expectedCert = official.ifEmpty { bundleCert.orEmpty() }
        val installedCert = installed.certSha256?.let(::normalizeHex)
        if (expectedCert.isNotEmpty() && installedCert != null && installedCert != expectedCert) {
            problems += PrecheckProblem.OtherCbxSignature
        } else if (installed.versionCode > bundle.versionCode) {
            problems += PrecheckProblem.NewerCbxInstalled
        }
        if (!installed.enabled) {
            problems += PrecheckProblem.CbxDisabled
        }
    }
    return problems
}

internal fun normalizeHex(value: String): String =
    value.uppercase(Locale.US).filter { it in '0'..'9' || it in 'A'..'F' }

internal data class Explanation(
    val code: String,
    val title: String,
    val advice: String,
    /**
     * Every failure is reported. This only sorts them: true when the student cannot fix it
     * alone and an admin has to act, false when the advice on screen is enough.
     */
    val needsAdmin: Boolean = true
)

internal class Texts(private val indonesian: Boolean) {
    fun t(english: String, indonesianText: String): String = if (indonesian) indonesianText else english

    companion object {
        fun forLocale(locale: Locale = Locale.getDefault()): Texts =
            Texts(indonesian = !locale.language.equals("en", ignoreCase = true))
    }
}

internal fun explainPrecheck(
    problem: PrecheckProblem,
    bundle: BundledCbx,
    device: DeviceFacts,
    texts: Texts
): Explanation = with(texts) {
    when (problem) {
        PrecheckProblem.AndroidTooOld -> Explanation(
            problem.code,
            t("Android is too old for CBX Lock", "Android HP ini terlalu lama untuk CBX Lock"),
            t(
                "This phone runs Android ${device.release} (API ${device.sdkInt}); CBX Lock needs API ${bundle.minSdk} (Android 7.0) or newer. Use another phone.",
                "HP ini memakai Android ${device.release} (API ${device.sdkInt}), sedangkan CBX Lock butuh API ${bundle.minSdk} (Android 7.0) ke atas. Gunakan HP lain."
            )
        )
        PrecheckProblem.CpuNotSupported -> Explanation(
            problem.code,
            t("This phone's processor is not supported", "Prosesor HP ini tidak didukung"),
            t(
                "Phone: ${device.supportedAbis.joinToString()}. CBX Lock supports: ${bundle.abis.joinToString()}. The admin has been told.",
                "HP: ${device.supportedAbis.joinToString()}. CBX Lock mendukung: ${bundle.abis.joinToString()}. Admin sudah diberi tahu."
            )
        )
        PrecheckProblem.NotEnoughStorage -> Explanation(
            problem.code,
            t("Not enough free storage", "Memori HP tidak cukup"),
            t(
                "Free up at least ${formatMegabytes(requiredFreeBytes(bundle))} (now ${formatMegabytes(device.freeBytes)}), then tap Check again.",
                "Kosongkan memori minimal ${formatMegabytes(requiredFreeBytes(bundle))} (sekarang ${formatMegabytes(device.freeBytes)}), lalu tekan Periksa lagi."
            ),
            needsAdmin = false
        )
        PrecheckProblem.BundleDamaged -> Explanation(
            problem.code,
            t("The installer file is damaged", "File installer rusak"),
            t(
                "Delete this installer, download it again completely, then open it again.",
                "Hapus installer ini, unduh ulang sampai selesai, lalu buka lagi."
            )
        )
        PrecheckProblem.BundleNotOfficial -> Explanation(
            problem.code,
            t("This installer is not the official one", "Installer ini bukan yang resmi"),
            t(
                "The CBX Lock inside is not signed by the school. Get the installer from the admin.",
                "CBX Lock di dalamnya tidak ditandatangani sekolah. Minta installer dari admin."
            )
        )
        PrecheckProblem.UnreadableOnThisPhone -> Explanation(
            problem.code,
            t("This phone cannot read the CBX Lock package", "HP ini tidak bisa membaca paket CBX Lock"),
            t(
                "The file is intact, so this is a phone compatibility problem. The admin has been told; you can still try installing.",
                "File-nya utuh, jadi ini masalah kecocokan HP. Admin sudah diberi tahu; Anda tetap bisa mencoba memasang."
            )
        )
        PrecheckProblem.OtherCbxSignature -> Explanation(
            problem.code,
            t("A different CBX Lock is already installed", "Sudah ada CBX Lock lain di HP ini"),
            t(
                "It is a test or unofficial build, and Android will not replace it. Uninstall it first, then install again.",
                "Itu versi uji atau tidak resmi, dan Android tidak mau menimpanya. Uninstall dulu, lalu pasang lagi."
            ),
            needsAdmin = false
        )
        PrecheckProblem.NewerCbxInstalled -> Explanation(
            problem.code,
            t("A newer CBX Lock is already installed", "CBX Lock yang lebih baru sudah terpasang"),
            t(
                "This installer carries v${bundle.versionName}. Open the installed CBX Lock, or uninstall it first to go back to this version.",
                "Installer ini membawa v${bundle.versionName}. Buka CBX Lock yang sudah ada, atau uninstall dulu untuk kembali ke versi ini."
            ),
            needsAdmin = false
        )
        PrecheckProblem.InstallBlockedByPolicy -> Explanation(
            problem.code,
            t("Installing apps is blocked on this phone", "Memasang aplikasi diblokir di HP ini"),
            t(
                "A device policy forbids it (${device.installRestrictions.joinToString()}), set by a school/work profile, parental control or phone management app. Ask whoever manages this phone, or use another phone.",
                "Kebijakan perangkat melarangnya (${device.installRestrictions.joinToString()}), dari profil kerja/sekolah, kontrol orang tua, atau aplikasi pengelola HP. Minta pengelola HP ini membukanya, atau gunakan HP lain."
            )
        )
        PrecheckProblem.CbxDisabled -> Explanation(
            problem.code,
            t("CBX Lock is turned off on this phone", "CBX Lock dinonaktifkan di HP ini"),
            t(
                "It is installed but disabled (often by a cleaner or \"app freeze\" feature). Open its App info and tap Enable, then check again.",
                "Aplikasinya ada tetapi dinonaktifkan (sering oleh aplikasi pembersih atau fitur \"bekukan aplikasi\"). Buka Info aplikasi CBX Lock, tekan Aktifkan, lalu periksa lagi."
            ),
            needsAdmin = false
        )
    }
}

/** Failures outside Android's install result: the installer's own steps going wrong. */
internal enum class InstallerEvent(val code: String) {
    CheckCrashed("check_crashed"),
    InstallerCrashed("installer_crashed"),
    NoResult("install_no_result"),
    SessionEndedWithoutResult("install_session_vanished"),
    PermissionNotGranted("install_permission_not_granted"),
    PermissionScreenUnavailable("install_permission_screen_unavailable"),
    CbxNotLaunchable("cbx_not_launchable"),
    UninstallScreenUnavailable("uninstall_screen_unavailable")
}

internal fun explainEvent(event: InstallerEvent, texts: Texts): Explanation = with(texts) {
    when (event) {
        InstallerEvent.CheckCrashed -> Explanation(
            event.code,
            t("The check could not run", "Pemeriksaan tidak bisa berjalan"),
            t(
                "Close this installer and open it again. If it keeps happening, download it again.",
                "Tutup installer ini lalu buka lagi. Jika terus terjadi, unduh ulang installer-nya."
            )
        )
        InstallerEvent.InstallerCrashed -> Explanation(
            event.code,
            t("CBX Installer stopped unexpectedly", "CBX Installer berhenti tiba-tiba"),
            t("Open it again. The admin has the details.", "Buka lagi. Admin sudah menerima detailnya.")
        )
        InstallerEvent.NoResult -> Explanation(
            event.code,
            t("Android has not finished the installation", "Android belum menyelesaikan pemasangan"),
            t(
                "If an Android screen asked to install, answer it. Otherwise cancel and install again.",
                "Jika ada layar Android yang meminta konfirmasi, jawab dulu. Jika tidak ada, batalkan lalu pasang lagi."
            )
        )
        InstallerEvent.SessionEndedWithoutResult -> Explanation(
            event.code,
            t("The installation ended without an answer", "Pemasangan berhenti tanpa jawaban dari Android"),
            t("Tap Install again.", "Tekan Pasang lagi.")
        )
        InstallerEvent.PermissionNotGranted -> Explanation(
            event.code,
            t("CBX Installer is not allowed to install apps yet", "CBX Installer belum diizinkan memasang aplikasi"),
            t(
                "Open the permission and turn on \"Allow from this source\" for CBX Installer.",
                "Buka izin lalu nyalakan \"Izinkan dari sumber ini\" untuk CBX Installer."
            ),
            needsAdmin = false
        )
        InstallerEvent.PermissionScreenUnavailable -> Explanation(
            event.code,
            t("The permission screen could not be opened", "Layar izin tidak bisa dibuka"),
            t(
                "Open Settings > Apps > CBX Installer > Install unknown apps, and allow it.",
                "Buka Setelan > Aplikasi > CBX Installer > Instal aplikasi tidak dikenal, lalu izinkan."
            )
        )
        InstallerEvent.CbxNotLaunchable -> Explanation(
            event.code,
            t("CBX Lock could not be opened", "CBX Lock tidak bisa dibuka"),
            t("Open it from the app list. The admin has been told.", "Buka dari daftar aplikasi. Admin sudah diberi tahu.")
        )
        InstallerEvent.UninstallScreenUnavailable -> Explanation(
            event.code,
            t("The uninstall screen could not be opened", "Layar uninstall tidak bisa dibuka"),
            t(
                "Uninstall CBX Lock from Settings > Apps, then open this installer again.",
                "Uninstall CBX Lock lewat Setelan > Aplikasi, lalu buka installer ini lagi."
            )
        )
    }
}

/** PackageInstaller status codes, including ones newer than this app's minSdk. */
internal object SessionStatus {
    const val PendingUserAction = -1
    const val Success = 0
    const val Failure = 1
    const val Blocked = 2
    const val Aborted = 3
    const val Invalid = 4
    const val Conflict = 5
    const val Storage = 6
    const val Incompatible = 7
    const val Timeout = 8

    fun name(status: Int): String = when (status) {
        PendingUserAction -> "STATUS_PENDING_USER_ACTION"
        Success -> "STATUS_SUCCESS"
        Failure -> "STATUS_FAILURE"
        Blocked -> "STATUS_FAILURE_BLOCKED"
        Aborted -> "STATUS_FAILURE_ABORTED"
        Invalid -> "STATUS_FAILURE_INVALID"
        Conflict -> "STATUS_FAILURE_CONFLICT"
        Storage -> "STATUS_FAILURE_STORAGE"
        Incompatible -> "STATUS_FAILURE_INCOMPATIBLE"
        Timeout -> "STATUS_FAILURE_TIMEOUT"
        else -> "STATUS_$status"
    }
}

private val InstallErrorToken = Regex("""INSTALL_(?:PARSE_)?FAILED_[A-Z_]+""")

/** The INSTALL_… code Android put in the status message, if any. */
internal fun installErrorToken(message: String?): String? =
    message?.let { InstallErrorToken.find(it)?.value }

/** Turns what PackageInstaller returned into what the student should do. */
internal fun explainInstallFailure(status: Int, message: String?, texts: Texts): Explanation = with(texts) {
    val token = installErrorToken(message)
    val code = token ?: SessionStatus.name(status)
    when {
        // Google Play Protect asks about apps it has not seen; "Don't install" comes back as an
        // abort with this code. The student has to pick the other button, so say which.
        token == "INSTALL_FAILED_VERIFICATION_FAILURE" && status != SessionStatus.Blocked -> Explanation(
            code,
            t("Google Play Protect stopped the installation", "Google Play Protect menahan pemasangan"),
            t(
                "Tap Install again. When Google Play Protect appears, choose \"Install without scanning\" (or \"Scan app\" and wait), not \"Don't install app\".",
                "Tekan Pasang lagi. Saat Google Play Protect muncul, pilih \"Instal tanpa memindai\" (atau \"Pindai aplikasi\" lalu tunggu), jangan \"Jangan instal aplikasi\"."
            ),
            needsAdmin = false
        )
        status == SessionStatus.Aborted || token == "INSTALL_FAILED_ABORTED" -> Explanation(
            code,
            t("Installation was cancelled", "Pemasangan dibatalkan"),
            t("Tap Install again and choose Install on the next screen.", "Tekan Pasang lagi, lalu pilih Instal di layar berikutnya."),
            needsAdmin = false
        )
        token == "INSTALL_FAILED_UPDATE_INCOMPATIBLE" || token == "INSTALL_FAILED_SHARED_USER_INCOMPATIBLE" -> Explanation(
            code,
            t("A different CBX Lock is already installed", "Sudah ada CBX Lock lain di HP ini"),
            t("Uninstall the CBX Lock on this phone first, then install again.", "Uninstall CBX Lock yang ada di HP ini dulu, lalu pasang lagi."),
            needsAdmin = false
        )
        token == "INSTALL_FAILED_VERSION_DOWNGRADE" -> Explanation(
            code,
            t("A newer CBX Lock is already installed", "CBX Lock yang lebih baru sudah terpasang"),
            t("Open the installed CBX Lock, or uninstall it first.", "Buka CBX Lock yang sudah ada, atau uninstall dulu."),
            needsAdmin = false
        )
        token == "INSTALL_FAILED_OLDER_SDK" -> Explanation(
            code,
            t("Android is too old for CBX Lock", "Android HP ini terlalu lama untuk CBX Lock"),
            t("CBX Lock needs Android 7.0 or newer. Use another phone.", "CBX Lock butuh Android 7.0 ke atas. Gunakan HP lain.")
        )
        token == "INSTALL_FAILED_NO_MATCHING_ABIS" || token == "INSTALL_FAILED_CPU_ABI_INCOMPATIBLE" -> Explanation(
            code,
            t("This phone's processor is not supported", "Prosesor HP ini tidak didukung"),
            t("The admin has been told.", "Admin sudah diberi tahu.")
        )
        token == "INSTALL_FAILED_INSUFFICIENT_STORAGE" || status == SessionStatus.Storage -> Explanation(
            code,
            t("Not enough free storage", "Memori HP tidak cukup"),
            t("Free up some space (delete videos or unused apps), then install again.", "Kosongkan memori (hapus video atau aplikasi yang tidak dipakai), lalu pasang lagi."),
            needsAdmin = false
        )
        token == "INSTALL_FAILED_USER_RESTRICTED" || token == "INSTALL_FAILED_VERIFICATION_FAILURE" ||
            token == "INSTALL_FAILED_VERIFICATION_TIMEOUT" || status == SessionStatus.Blocked -> Explanation(
            code,
            t("The phone blocked the installation", "Pemasangan diblokir oleh HP"),
            t(
                "A security setting refused it (for example Play Protect, the phone's security app, or a parental/work profile). Allow installing from CBX Installer and try again; the admin has been told.",
                "Setelan keamanan HP menolaknya (misalnya Play Protect, aplikasi keamanan HP, atau profil kerja/orang tua). Izinkan pemasangan dari CBX Installer lalu coba lagi; admin sudah diberi tahu."
            )
        )
        token != null && token.startsWith("INSTALL_PARSE_FAILED") || token == "INSTALL_FAILED_INVALID_APK" ||
            status == SessionStatus.Invalid -> Explanation(
            code,
            t("This phone could not read the CBX Lock package", "HP ini tidak bisa membaca paket CBX Lock"),
            t("The admin has been told the exact reason. Try again later or use another phone.", "Admin sudah diberi tahu penyebab persisnya. Coba lagi nanti atau gunakan HP lain.")
        )
        token == "INSTALL_FAILED_CONFLICTING_PROVIDER" || token == "INSTALL_FAILED_DUPLICATE_PERMISSION" ||
            status == SessionStatus.Conflict -> Explanation(
            code,
            t("Another app conflicts with CBX Lock", "Ada aplikasi lain yang bentrok dengan CBX Lock"),
            t("The admin has been told which one.", "Admin sudah diberi tahu aplikasi mana.")
        )
        status == SessionStatus.Incompatible -> Explanation(
            code,
            t("CBX Lock is not compatible with this phone", "CBX Lock tidak cocok dengan HP ini"),
            t("The admin has been told the exact reason.", "Admin sudah diberi tahu penyebab persisnya.")
        )
        status == SessionStatus.Timeout -> Explanation(
            code,
            t("The installation took too long", "Pemasangan terlalu lama"),
            t("Tap Install again.", "Tekan Pasang lagi.")
        )
        else -> Explanation(
            code,
            t("Installation failed", "Pemasangan gagal"),
            t("The admin has been told the exact reason. Tap Install to try again.", "Admin sudah diberi tahu penyebab persisnya. Tekan Pasang untuk mencoba lagi.")
        )
    }
}

internal data class InstallReport(
    /** Short id shown to the student, so a screenshot can be matched to this message. */
    val reportId: String,
    val stage: String,
    val code: String,
    /** What the student was told on screen. */
    val studentTitle: String,
    val needsAdmin: Boolean,
    val statusName: String? = null,
    val statusMessage: String? = null,
    val legacyStatus: Int? = null,
    val problems: List<String> = emptyList(),
    val bundle: BundledCbx?,
    val device: DeviceFacts,
    val installed: InstalledCbx?,
    val inspection: BundleInspection? = null,
    val sessionId: Int? = null,
    val sessionProgress: Float? = null,
    /** Anything else worth reading, such as a stack trace for a crash. */
    val extra: String? = null,
    val installerVersion: String,
    val timestamp: String
)

/**
 * Plain text for Telegram: what failed, on which phone, with what file, so the admin can act
 * without asking the student anything. No personal data: no names, numbers or accounts.
 */
internal fun formatInstallReport(report: InstallReport): String = buildString {
    appendLine(
        if (report.needsAdmin) {
            "[ADMIN ACTION] CBX Installer: install failed"
        } else {
            "[INFO] CBX Installer: student can fix it"
        }
    )
    appendLine("Report ID: ${report.reportId}")
    appendLine("Student saw: ${report.studentTitle}")
    appendLine("Stage: ${report.stage} | Code: ${report.code}")
    report.statusName?.let { appendLine("Status: $it" + (report.legacyStatus?.let { legacy -> " | legacy $legacy" } ?: "")) }
    report.statusMessage?.takeIf { it.isNotBlank() }?.let { appendLine("Android said: ${it.take(700)}") }
    if (report.problems.isNotEmpty()) appendLine("Prechecks: ${report.problems.joinToString()}")
    if (report.sessionId != null) {
        appendLine(
            "Session: ${report.sessionId}" +
                (report.sessionProgress?.let { " | progress ${(it * 100).toInt()}%" } ?: "")
        )
    }

    val device = report.device
    appendLine("")
    appendLine(
        "Phone: ${device.manufacturer} ${device.model} (brand ${device.brand}, device ${device.device}" +
            (if (device.hardware.isNotBlank()) ", hw ${device.hardware}" else "") +
            (if (device.socModel.isNotBlank()) ", SoC ${device.socModel}" else "") + ")"
    )
    appendLine(
        "Android: ${device.release} (API ${device.sdkInt})" +
            (if (device.securityPatch.isNotBlank()) " | patch ${device.securityPatch}" else "") +
            " | ROM ${device.romDisplay}"
    )
    if (device.fingerprint.isNotBlank()) appendLine("Build: ${device.fingerprint}")
    appendLine(
        "CPU ABIs: ${device.supportedAbis.joinToString(",")}" +
            " | RAM ${formatGigabytes(device.totalRamBytes)} | free storage ${formatMegabytes(device.freeBytes)}"
    )
    appendLine(
        "May install apps: " + when (device.canInstallPackages) {
            true -> "yes"
            false -> "no"
            null -> "system-wide setting (Android < 8)"
        } + " | restrictions: " + device.installRestrictions.ifEmpty { listOf("none") }.joinToString()
    )
    appendLine(
        "Installer came from: ${device.installerSource.ifBlank { "unknown" }}" +
            (if (device.locale.isNotBlank()) " | ${device.locale}" else "") +
            (if (device.timezone.isNotBlank()) " | ${device.timezone}" else "")
    )

    appendLine("")
    val bundle = report.bundle
    appendLine(
        bundle?.let {
            "Bundled CBX: v${it.versionName} (${it.versionCode}) | ${it.size} B | sha256 ${it.sha256.take(16)}"
        } ?: "Bundled CBX: unreadable"
    )
    report.inspection?.let { inspection ->
        appendLine(
            "File check: sha256 ${inspection.actualSha256?.take(16) ?: "unreadable"}" +
                " | parsed by phone ${if (inspection.parsedByPhone) "yes" else "no"}" +
                " | cert ${inspection.certSha256?.take(16) ?: "-"}" +
                (inspection.error?.let { " | error ${it.take(200)}" } ?: "")
        )
    }
    appendLine(
        report.installed?.let {
            "CBX on phone: v${it.versionName ?: "?"} (${it.versionCode}) | cert ${it.certSha256?.take(16) ?: "-"}" +
                " | ${if (it.enabled) "enabled" else "DISABLED"}" +
                " | installed by ${it.installerSource ?: "unknown"}"
        } ?: "CBX on phone: none"
    )
    report.extra?.takeIf { it.isNotBlank() }?.let {
        appendLine("")
        appendLine(it.take(1500))
    }
    appendLine("")
    appendLine("Installer v${report.installerVersion} | ${report.timestamp}")
}.trimEnd().take(TelegramMessageLimit)

internal const val TelegramMessageLimit = 4000

internal fun formatMegabytes(bytes: Long): String =
    if (bytes < 0) "?" else String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))

internal fun formatGigabytes(bytes: Long): String =
    if (bytes < 0) "?" else String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))

/** A short id, e.g. "IR-7F3A2C", readable over the phone and unique enough for a day. */
internal fun newReportId(random: java.util.Random = java.security.SecureRandom()): String =
    "IR-" + String.format(Locale.US, "%06X", random.nextInt(0x1000000))
