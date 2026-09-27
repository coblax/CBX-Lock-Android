package com.coblax.examlock.installer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * One screen: check the phone, install CBX, and when anything goes wrong, show the student
 * what to do and send the admins the exact reason. Every failure is reported, every state has
 * a way forward or back, and a report ID ties the student's screen to the admin's message.
 */
class InstallerActivity : Activity() {
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val texts = Texts.forLocale()
    private lateinit var inspector: PhoneInspector

    private lateinit var statusTitle: TextView
    private lateinit var statusBody: TextView
    private lateinit var progress: ProgressBar
    private lateinit var technical: TextView
    private lateinit var reportLine: TextView
    private lateinit var primaryButton: Button
    private lateinit var secondaryButton: Button
    private lateinit var shareButton: Button
    private lateinit var deviceLine: TextView
    private lateinit var subtitle: TextView

    private var bundle: BundledCbx? = null
    private var device: DeviceFacts? = null
    private var installed: InstalledCbx? = null
    private var inspection: BundleInspection? = null
    private var bundleFile: File? = null
    private var problems: List<PrecheckProblem> = emptyList()

    /** Set while checks or an install run, so returning to the screen does not restart them. */
    private var busy = false
    private var resumed = false
    /**
     * An install result or failure is on screen. Coming back from Android's own install or
     * Play Protect screen must not re-run the checks over it, or the student never sees why.
     */
    private var holdResultOnResume = false
    private var activeSessionId: Int? = null
    private var lastReportText: String? = null
    private val resultListener: (InstallResult) -> Unit = { onInstallResult(it) }
    private val noResultWatchdog = Runnable { onNoResult() }
    private val sessionOutcomeCheck = Runnable { verifySessionOutcome() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inspector = PhoneInspector(this)
        setContentView(buildLayout())
        InstallResultBus.listener = resultListener
        askForNotificationsOnce()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        val stored = InstallResultBus.takeStored(this)
        when {
            stored != null -> runChecks(thenShow = stored)
            activeSessionId != null -> {
                // Back from Android's install screen: if it ended without telling us, find out.
                main.removeCallbacks(sessionOutcomeCheck)
                main.postDelayed(sessionOutcomeCheck, SessionOutcomeCheckDelayMillis)
            }
            holdResultOnResume -> Unit
            !busy -> runChecks()
        }
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    override fun onDestroy() {
        if (InstallResultBus.listener === resultListener) {
            InstallResultBus.listener = null
        }
        main.removeCallbacksAndMessages(null)
        worker.shutdown()
        super.onDestroy()
    }

    private fun askForNotificationsOnce() {
        if (Build.VERSION.SDK_INT < 33) return
        val prefs = getSharedPreferences(StatePreferences, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KeyAskedNotifications, false)) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        prefs.edit().putBoolean(KeyAskedNotifications, true).apply()
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    // ── Checks ──────────────────────────────────────────────────────

    private fun runChecks(thenShow: InstallResult? = null) {
        busy = true
        holdResultOnResume = false
        showState(
            tone = Tone.Info,
            title = texts.t("Checking this phone…", "Memeriksa HP ini…"),
            body = texts.t("Making sure CBX Lock can be installed.", "Memastikan CBX Lock bisa dipasang."),
            working = true
        )
        worker.execute {
            val result = runCatching {
                val bundle = inspector.bundle()
                val device = inspector.device()
                val installed = inspector.installedCbx()
                val (file, inspection) = inspector.inspectBundle()
                val problems = evaluatePrechecks(
                    bundle = bundle,
                    device = device,
                    installed = installed,
                    inspection = inspection,
                    officialCertSha256 = BuildConfig.CBX_OFFICIAL_CERT_SHA256
                )
                Checked(bundle, device, installed, inspection, file, problems)
            }
            main.post {
                busy = false
                result.fold(
                    onSuccess = { checked ->
                        applyChecked(checked)
                        if (thenShow != null) onInstallResult(thenShow) else showCheckedState()
                    },
                    onFailure = { error -> showCheckCrashed(error) }
                )
            }
            runCatching { TelegramReporter.flushPending(this) }
        }
    }

    private class Checked(
        val bundle: BundledCbx,
        val device: DeviceFacts,
        val installed: InstalledCbx?,
        val inspection: BundleInspection,
        val file: File?,
        val problems: List<PrecheckProblem>
    )

    private fun applyChecked(checked: Checked) {
        bundle = checked.bundle
        device = checked.device
        installed = checked.installed
        inspection = checked.inspection
        bundleFile = checked.file
        problems = checked.problems
        subtitle.text = texts.t(
            "Installs CBX Lock v${checked.bundle.versionName}",
            "Memasang CBX Lock v${checked.bundle.versionName}"
        )
        deviceLine.text = with(checked.device) {
            "$manufacturer $model · Android $release (API $sdkInt) · ${supportedAbis.firstOrNull().orEmpty()}"
        }
    }

    private fun showCheckedState() {
        val bundle = bundle ?: return
        val device = device ?: return
        // A phone that cannot parse an intact file may still install through a session, so it
        // is reported but not a stop.
        val blocking = problems - PrecheckProblem.UnreadableOnThisPhone
        if (blocking.isNotEmpty()) {
            val explanations = blocking.map { explainPrecheck(it, bundle, device, texts) }
            val first = explanations.first()
            val extra = explanations.drop(1).joinToString("\n") { "• ${it.title}" }
            showState(
                tone = if (explanations.any { it.needsAdmin }) Tone.Danger else Tone.Warning,
                title = first.title,
                body = if (extra.isEmpty()) first.advice else first.advice + "\n\n" + extra,
                technicalText = blocking.joinToString { it.code },
                primary = when {
                    PrecheckProblem.OtherCbxSignature in blocking || PrecheckProblem.NewerCbxInstalled in blocking ->
                        texts.t("Uninstall the CBX Lock on this phone", "Uninstall CBX Lock yang ada") to ::uninstallCbx
                    PrecheckProblem.CbxDisabled in blocking ->
                        texts.t("Open CBX Lock app info", "Buka Info aplikasi CBX Lock") to ::openCbxAppInfo
                    else -> texts.t("Check again", "Periksa lagi") to { runChecks() }
                },
                secondary = if (PrecheckProblem.NewerCbxInstalled in blocking) {
                    texts.t("Open CBX Lock", "Buka CBX Lock") to ::openCbx
                } else {
                    null
                }
            )
            report(
                stage = "precheck",
                code = blocking.joinToString("+") { it.code },
                studentTitle = first.title,
                needsAdmin = explanations.any { it.needsAdmin },
                problemCodes = blocking.map { it.code }
            )
            return
        }
        val current = installed
        if (current != null && current.versionCode == bundle.versionCode) {
            showState(
                tone = Tone.Success,
                title = texts.t("CBX Lock is already installed", "CBX Lock sudah terpasang"),
                body = texts.t(
                    "Version ${bundle.versionName} is on this phone. You can delete this installer.",
                    "Versi ${bundle.versionName} sudah ada di HP ini. Installer ini boleh dihapus."
                ),
                primary = texts.t("Open CBX Lock", "Buka CBX Lock") to ::openCbx,
                secondary = texts.t("Install again", "Pasang ulang") to ::startInstall
            )
            return
        }
        if (device.canInstallPackages == false) {
            val attempts = statePrefs().getInt(KeyPermissionAttempts, 0)
            val explanation = explainEvent(InstallerEvent.PermissionNotGranted, texts)
            showState(
                tone = Tone.Warning,
                title = texts.t("Allow CBX Installer to install apps", "Izinkan CBX Installer memasang aplikasi"),
                body = texts.t(
                    "Android asks once. Turn on \"Allow from this source\", then come back here.",
                    "Android hanya bertanya sekali. Nyalakan \"Izinkan dari sumber ini\", lalu kembali ke sini."
                ),
                primary = texts.t("Open the permission", "Buka izin") to ::openInstallPermission,
                secondary = texts.t("Check again", "Periksa lagi") to { runChecks() }
            )
            // Not a failure the first time; after a trip to Settings it means the student is stuck.
            if (attempts > 0) {
                report(
                    stage = "permission",
                    code = explanation.code,
                    studentTitle = explanation.title,
                    needsAdmin = false,
                    extra = "Permission screen opened $attempts time(s) without the permission being granted."
                )
            }
            return
        }
        val unreadable = PrecheckProblem.UnreadableOnThisPhone in problems
        showState(
            tone = if (unreadable) Tone.Warning else Tone.Info,
            title = if (current == null) {
                texts.t("Ready to install", "Siap memasang")
            } else {
                texts.t("Ready to update", "Siap memperbarui")
            },
            body = when {
                unreadable -> explainPrecheck(PrecheckProblem.UnreadableOnThisPhone, bundle, device, texts).advice
                current == null -> texts.t(
                    "CBX Lock v${bundle.versionName} will be installed. Choose Install on the next screen.",
                    "CBX Lock v${bundle.versionName} akan dipasang. Pilih Instal di layar berikutnya."
                )
                else -> texts.t(
                    "CBX Lock v${current.versionName} will be updated to v${bundle.versionName}.",
                    "CBX Lock v${current.versionName} akan diperbarui ke v${bundle.versionName}."
                )
            },
            technicalText = if (unreadable) PrecheckProblem.UnreadableOnThisPhone.code else null,
            primary = texts.t("Install CBX Lock", "Pasang CBX Lock") to ::startInstall
        )
        if (unreadable) {
            val explanation = explainPrecheck(PrecheckProblem.UnreadableOnThisPhone, bundle, device, texts)
            report(
                stage = "precheck",
                code = explanation.code,
                studentTitle = explanation.title,
                needsAdmin = true,
                problemCodes = listOf(explanation.code)
            )
        }
    }

    private fun showCheckCrashed(error: Throwable) {
        val explanation = explainEvent(InstallerEvent.CheckCrashed, texts)
        showState(
            tone = Tone.Danger,
            title = explanation.title,
            body = explanation.advice,
            technicalText = "${error.javaClass.simpleName}: ${error.message}",
            primary = texts.t("Check again", "Periksa lagi") to { runChecks() }
        )
        report(
            stage = "precheck",
            code = explanation.code,
            studentTitle = explanation.title,
            needsAdmin = true,
            extra = stackTraceOf(error)
        )
    }

    // ── Install ─────────────────────────────────────────────────────

    private fun startInstall() {
        val file = bundleFile?.takeIf { it.exists() } ?: return runChecks()
        busy = true
        showInstalling()
        val writeLimit = debugTruncatedWriteLimit(file)
        worker.execute {
            val outcome = runCatching { SessionInstaller.install(this, file, writeLimit) }
            main.post {
                outcome.fold(
                    onSuccess = { sessionId ->
                        activeSessionId = sessionId
                        armNoResultWatchdog()
                    },
                    onFailure = { error ->
                        onInstallResult(
                            InstallResult(
                                sessionId = -1,
                                status = SessionStatus.Failure,
                                message = "session: ${error.javaClass.simpleName}: ${error.message}",
                                legacyStatus = null,
                                confirmIntent = null
                            )
                        )
                    }
                )
            }
        }
    }

    private fun showInstalling() {
        showState(
            tone = Tone.Info,
            title = texts.t("Installing…", "Memasang…"),
            body = texts.t("Choose Install when Android asks.", "Pilih Instal saat Android bertanya."),
            working = true,
            secondary = texts.t("Cancel", "Batal") to ::cancelInstall
        )
    }

    /** Debug builds can install a cut-off copy on purpose, to see a real parse failure end to end. */
    private fun debugTruncatedWriteLimit(file: File): Long =
        if (BuildConfig.DEBUG && intent?.getBooleanExtra(DebugTruncateExtra, false) == true) {
            file.length() / 2
        } else {
            file.length()
        }

    private fun cancelInstall() {
        activeSessionId?.let { SessionInstaller.abandon(this, it) }
        clearSession()
        busy = false
        showCheckedState()
    }

    private fun clearSession() {
        activeSessionId = null
        main.removeCallbacks(noResultWatchdog)
        main.removeCallbacks(sessionOutcomeCheck)
    }

    private fun armNoResultWatchdog() {
        main.removeCallbacks(noResultWatchdog)
        main.postDelayed(noResultWatchdog, NoResultTimeoutMillis)
    }

    /** Android never answered: say so, report it, and offer to wait or start over. */
    private fun onNoResult() {
        val sessionId = activeSessionId ?: return
        val info = SessionInstaller.sessionInfo(this, sessionId)
        if (info == null) {
            verifySessionOutcome()
            return
        }
        val explanation = explainEvent(InstallerEvent.NoResult, texts)
        showState(
            tone = Tone.Warning,
            title = explanation.title,
            body = explanation.advice,
            technicalText = "${explanation.code} | session $sessionId | ${(info.progress * 100).toInt()}%",
            primary = texts.t("Cancel and install again", "Batalkan dan pasang lagi") to {
                cancelInstall()
                startInstall()
            },
            secondary = texts.t("Keep waiting", "Tunggu lagi") to {
                showInstalling()
                armNoResultWatchdog()
            }
        )
        report(
            stage = "install",
            code = explanation.code,
            studentTitle = explanation.title,
            needsAdmin = true,
            sessionId = sessionId,
            sessionProgress = info.progress,
            extra = "No result from Android ${NoResultTimeoutMillis / 1000} s after commit. " +
                "Session active=${info.isActive}, sealed=${info.isSealed}."
        )
    }

    /** The session is gone but no result came: tell success from a silent failure. */
    private fun verifySessionOutcome() {
        val sessionId = activeSessionId ?: return
        if (SessionInstaller.sessionInfo(this, sessionId) != null) return
        val bundle = bundle
        val nowInstalled = runCatching { inspector.installedCbx() }.getOrNull()
        if (bundle != null && nowInstalled != null && nowInstalled.versionCode == bundle.versionCode) {
            onInstallResult(InstallResult(sessionId, SessionStatus.Success, null, null, null))
            return
        }
        clearSession()
        busy = false
        val explanation = explainEvent(InstallerEvent.SessionEndedWithoutResult, texts)
        holdResultOnResume = true
        showState(
            tone = Tone.Danger,
            title = explanation.title,
            body = explanation.advice,
            technicalText = "${explanation.code} | session $sessionId",
            primary = texts.t("Install again", "Pasang lagi") to { runChecks() }
        )
        report(
            stage = "install",
            code = explanation.code,
            studentTitle = explanation.title,
            needsAdmin = true,
            sessionId = sessionId,
            extra = "Android closed the install session without sending a result."
        )
    }

    private fun onInstallResult(result: InstallResult) {
        val active = activeSessionId
        if (result.sessionId != -1 && active != null && result.sessionId != active) {
            return
        }
        if (result.status == SessionStatus.PendingUserAction) {
            val confirm = result.confirmIntent
            if (confirm == null) {
                onInstallResult(result.copy(status = SessionStatus.Failure, message = "confirm: no confirmation screen from Android"))
                return
            }
            runCatching { startActivity(confirm) }.onFailure {
                onInstallResult(result.copy(status = SessionStatus.Failure, message = "confirm: ${it.javaClass.simpleName}: ${it.message}"))
            }
            return
        }
        busy = false
        clearSession()
        holdResultOnResume = true
        if (result.status == SessionStatus.Success) {
            bundleFile?.delete()
            installed = runCatching { inspector.installedCbx() }.getOrNull()
            showState(
                tone = Tone.Success,
                title = texts.t("CBX Lock is installed", "CBX Lock berhasil dipasang"),
                body = texts.t(
                    "You can open it now. This installer is no longer needed.",
                    "Silakan buka sekarang. Installer ini sudah tidak diperlukan."
                ),
                primary = texts.t("Open CBX Lock", "Buka CBX Lock") to ::openCbx,
                secondary = texts.t("Delete this installer", "Hapus installer ini") to ::uninstallSelf
            )
            if (!resumed) {
                InstallNotifier.show(
                    this,
                    success = true,
                    title = texts.t("CBX Lock is installed", "CBX Lock berhasil dipasang"),
                    body = texts.t("Tap to open CBX Installer.", "Ketuk untuk membuka CBX Installer.")
                )
            }
            return
        }
        val explanation = explainInstallFailure(result.status, result.message, texts)
        showState(
            tone = if (explanation.needsAdmin) Tone.Danger else Tone.Warning,
            title = explanation.title,
            body = explanation.advice,
            technicalText = listOfNotNull(
                explanation.code,
                result.message?.takeIf { it.isNotBlank() }?.take(300)
            ).joinToString("\n"),
            primary = if (explanation.code == "INSTALL_FAILED_UPDATE_INCOMPATIBLE") {
                texts.t("Uninstall the CBX Lock on this phone", "Uninstall CBX Lock yang ada") to ::uninstallCbx
            } else {
                texts.t("Install again", "Pasang lagi") to { runChecks() }
            }
        )
        val alreadyReported = result.alreadyReportedAs
        if (alreadyReported != null) {
            setReportLine(texts.t("Report ID: ", "ID laporan: ") + alreadyReported)
        } else {
            report(
                stage = "install",
                code = explanation.code,
                studentTitle = explanation.title,
                needsAdmin = explanation.needsAdmin,
                result = result
            )
        }
        if (!resumed) {
            InstallNotifier.show(this, success = false, title = explanation.title, body = explanation.advice)
        }
    }

    // ── Reports ─────────────────────────────────────────────────────

    /**
     * Reports one failure to the admins, shows its report ID, and offers sharing it by hand
     * in case Telegram cannot be reached. The same failure is sent once a day per phone.
     */
    private fun report(
        stage: String,
        code: String,
        studentTitle: String,
        needsAdmin: Boolean,
        problemCodes: List<String> = problems.map { it.code },
        result: InstallResult? = null,
        sessionId: Int? = result?.sessionId?.takeIf { it != -1 },
        sessionProgress: Float? = null,
        extra: String? = null
    ) {
        val device = device ?: runCatching { inspector.device() }.getOrNull() ?: return
        val reportId = newReportId()
        val text = formatInstallReport(
            InstallReport(
                reportId = reportId,
                stage = stage,
                code = code,
                studentTitle = studentTitle,
                needsAdmin = needsAdmin,
                statusName = result?.let { SessionStatus.name(it.status) },
                statusMessage = result?.message,
                legacyStatus = result?.legacyStatus,
                problems = problemCodes,
                bundle = bundle,
                device = device,
                installed = installed,
                inspection = inspection,
                sessionId = sessionId,
                sessionProgress = sessionProgress,
                extra = extra,
                installerVersion = BuildConfig.VERSION_NAME,
                timestamp = Diagnostics.timestamp()
            )
        )
        lastReportText = text
        showShareButton(true)
        val idLine = texts.t("Report ID: ", "ID laporan: ") + reportId
        setReportLine(idLine + "\n" + texts.t("Sending to the admin…", "Mengirim ke admin…"))
        val dedupeKey = "$stage|$code|${bundle?.versionCode}|${Diagnostics.today()}"
        worker.execute {
            val outcome = runCatching { TelegramReporter.report(this, dedupeKey, text) }
                .getOrDefault(ReportOutcome.Queued)
            main.post {
                if (lastReportText !== text) return@post
                setReportLine(idLine + "\n" + reportOutcomeLine(outcome))
            }
        }
    }

    private fun reportOutcomeLine(outcome: ReportOutcome): String = when (outcome) {
        ReportOutcome.Sent, ReportOutcome.Duplicate ->
            texts.t("The admin has received the report.", "Laporan sudah diterima admin.")
        ReportOutcome.Queued -> texts.t(
            "No internet yet: the report is sent automatically once the phone is online.",
            "Belum ada internet: laporan terkirim otomatis begitu HP tersambung internet."
        )
        is ReportOutcome.Rejected -> texts.t(
            "The report could not be sent (Telegram ${outcome.httpCode}). Use \"Share error details\" to send it to the admin.",
            "Laporan tidak bisa dikirim (Telegram ${outcome.httpCode}). Pakai \"Bagikan detail error\" untuk mengirimnya ke admin."
        )
        ReportOutcome.NotConfigured -> texts.t(
            "Test build: the report is not sent. Use \"Share error details\".",
            "Build uji: laporan tidak dikirim. Pakai \"Bagikan detail error\"."
        )
    }

    private fun shareReport() {
        val text = lastReportText ?: return
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "CBX Installer")
            .putExtra(Intent.EXTRA_TEXT, text)
        runCatching {
            startActivity(Intent.createChooser(send, texts.t("Share error details", "Bagikan detail error")))
        }
    }

    // ── Actions ─────────────────────────────────────────────────────

    private fun openInstallPermission() {
        if (Build.VERSION.SDK_INT < 26) return
        val prefs = statePrefs()
        prefs.edit().putInt(KeyPermissionAttempts, prefs.getInt(KeyPermissionAttempts, 0) + 1).apply()
        val opened = launchFirst(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        )
        if (!opened) reportEvent(InstallerEvent.PermissionScreenUnavailable)
    }

    private fun uninstallCbx() {
        val opened = launchFirst(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$CbxPackageName")),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$CbxPackageName"))
        )
        if (!opened) reportEvent(InstallerEvent.UninstallScreenUnavailable)
    }

    private fun openCbxAppInfo() {
        val opened = launchFirst(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$CbxPackageName")),
            Intent(Settings.ACTION_APPLICATION_SETTINGS)
        )
        if (!opened) reportEvent(InstallerEvent.UninstallScreenUnavailable)
    }

    private fun uninstallSelf() {
        launchFirst(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        )
    }

    private fun openCbx() {
        val launch = packageManager.getLaunchIntentForPackage(CbxPackageName)
        if (launch == null || !launchFirst(launch)) {
            reportEvent(InstallerEvent.CbxNotLaunchable)
        }
    }

    /** Shows and reports a failure of one of the installer's own steps. */
    private fun reportEvent(event: InstallerEvent) {
        val explanation = explainEvent(event, texts)
        holdResultOnResume = true
        showState(
            tone = if (explanation.needsAdmin) Tone.Danger else Tone.Warning,
            title = explanation.title,
            body = explanation.advice,
            technicalText = explanation.code,
            primary = texts.t("Check again", "Periksa lagi") to { runChecks() }
        )
        report(stage = "action", code = explanation.code, studentTitle = explanation.title, needsAdmin = explanation.needsAdmin)
    }

    /** Starts the first intent some app on this phone can handle; false if none could. */
    private fun launchFirst(vararg intents: Intent): Boolean {
        for (intent in intents) {
            if (runCatching { startActivity(intent) }.isSuccess) return true
        }
        return false
    }

    private fun statePrefs() = getSharedPreferences(StatePreferences, Context.MODE_PRIVATE)

    private fun stackTraceOf(error: Throwable): String =
        StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
            .lineSequence().take(20).joinToString("\n")

    // ── Layout ──────────────────────────────────────────────────────

    private enum class Tone { Info, Success, Warning, Danger }

    private fun showState(
        tone: Tone,
        title: String,
        body: String,
        working: Boolean = false,
        technicalText: String? = null,
        primary: Pair<String, () -> Unit>? = null,
        secondary: Pair<String, () -> Unit>? = null
    ) {
        statusTitle.text = title
        statusTitle.setTextColor(color(
            when (tone) {
                Tone.Info -> R.color.installer_text
                Tone.Success -> R.color.installer_success
                Tone.Warning -> R.color.installer_warning
                Tone.Danger -> R.color.installer_danger
            }
        ))
        statusBody.text = body
        progress.visibility = if (working) View.VISIBLE else View.GONE
        technical.text = technicalText.orEmpty()
        technical.visibility = if (technicalText.isNullOrEmpty()) View.GONE else View.VISIBLE
        // A new state starts without a report; report() adds it back when this state has one.
        lastReportText = null
        setReportLine(null)
        showShareButton(false)
        bindButton(primaryButton, primary)
        bindButton(secondaryButton, secondary)
    }

    private fun setReportLine(text: String?) {
        reportLine.text = text.orEmpty()
        reportLine.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    private fun showShareButton(visible: Boolean) {
        bindButton(
            shareButton,
            if (visible) texts.t("Share error details", "Bagikan detail error") to ::shareReport else null
        )
    }

    private fun bindButton(button: Button, action: Pair<String, () -> Unit>?) {
        if (action == null) {
            button.visibility = View.GONE
            button.setOnClickListener(null)
            return
        }
        button.visibility = View.VISIBLE
        button.text = action.first
        button.setOnClickListener { action.second() }
    }

    private fun buildLayout(): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = null
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        titles.addView(text(22f, R.color.installer_text, bold = true).apply { text = getString(R.string.installer_app_name) })
        subtitle = text(14f, R.color.installer_text_secondary)
        titles.addView(subtitle)
        header.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        column.addView(header)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(color(R.color.installer_surface))
                setStroke(dp(1), color(R.color.installer_outline))
            }
        }
        statusTitle = text(18f, R.color.installer_text, bold = true)
        statusBody = text(15f, R.color.installer_text).apply { setLineSpacing(0f, 1.15f) }
        progress = ProgressBar(this).apply { isIndeterminate = true }
        technical = text(12f, R.color.installer_text_secondary).apply {
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        reportLine = text(13f, R.color.installer_text_secondary).apply { setTextIsSelectable(true) }
        card.addView(statusTitle)
        card.addView(statusBody, spaced(8))
        card.addView(progress, spaced(12).apply { gravity = Gravity.CENTER_HORIZONTAL })
        card.addView(technical, spaced(12))
        card.addView(reportLine, spaced(8))
        column.addView(card, spaced(24))

        primaryButton = button(filled = true)
        secondaryButton = button(filled = false)
        shareButton = button(filled = false)
        column.addView(primaryButton, spaced(20))
        column.addView(secondaryButton, spaced(10))
        column.addView(shareButton, spaced(10))

        deviceLine = text(12f, R.color.installer_text_secondary).apply { gravity = Gravity.CENTER_HORIZONTAL }
        column.addView(deviceLine, spaced(24))

        return ScrollView(this).apply {
            isFillViewport = true
            addView(column)
            applySystemBarInsets(this)
        }
    }

    /** Android 15 draws apps under the system bars; keep the content clear of them. */
    private fun applySystemBarInsets(view: View) {
        view.setOnApplyWindowInsetsListener { target, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                target.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                target.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }
            insets
        }
    }

    private fun text(sizeSp: Float, colorRes: Int, bold: Boolean = false) = TextView(this).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color(colorRes))
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun button(filled: Boolean) = Button(this).apply {
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        minHeight = dp(52)
        stateListAnimator = null
        setTextColor(color(if (filled) R.color.installer_on_accent else R.color.installer_accent))
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            if (filled) {
                setColor(color(R.color.installer_accent))
            } else {
                setColor(0)
                setStroke(dp(1), color(R.color.installer_outline))
            }
        }
        visibility = View.GONE
    }

    private fun spaced(topDp: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(topDp) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    @Suppress("DEPRECATION")
    private fun color(res: Int): Int =
        if (Build.VERSION.SDK_INT >= 23) getColor(res) else resources.getColor(res)

    companion object {
        /** Debug builds only: install a cut-off copy to exercise a real parse failure. */
        const val DebugTruncateExtra = "debug_truncate"

        private const val StatePreferences = "installer_state"
        private const val KeyPermissionAttempts = "permission_attempts"
        private const val KeyAskedNotifications = "asked_notifications"

        /** Long enough for Play Protect scans on slow phones; Android usually answers in seconds. */
        private const val NoResultTimeoutMillis = 180_000L
        private const val SessionOutcomeCheckDelayMillis = 5_000L
    }
}
