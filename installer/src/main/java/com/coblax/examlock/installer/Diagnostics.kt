package com.coblax.examlock.installer

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds reports where the install screen is not there to supply its details: a result that
 * arrives after the student closed the installer, or the installer crashing.
 */
internal object Diagnostics {
    fun timestamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())

    fun today(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    /** Reports an install result that no open screen received. Blocking; off the main thread. */
    fun reportUnattendedResult(context: Context, result: InstallResult): String {
        val texts = Texts.forLocale()
        val explanation = explainInstallFailure(result.status, result.message, texts)
        val inspector = PhoneInspector(context)
        val bundle = runCatching { inspector.bundle() }.getOrNull()
        val reportId = newReportId()
        val text = formatInstallReport(
            InstallReport(
                reportId = reportId,
                stage = "install (installer closed)",
                code = explanation.code,
                studentTitle = explanation.title,
                needsAdmin = explanation.needsAdmin,
                statusName = SessionStatus.name(result.status),
                statusMessage = result.message,
                legacyStatus = result.legacyStatus,
                bundle = bundle,
                device = inspector.device(),
                installed = runCatching { inspector.installedCbx() }.getOrNull(),
                sessionId = result.sessionId,
                installerVersion = BuildConfig.VERSION_NAME,
                timestamp = timestamp()
            )
        )
        TelegramReporter.report(context, "install|${explanation.code}|${bundle?.versionCode}|${today()}", text)
        return reportId
    }

    /** Keeps a crash report for the retry job; the process is about to die, so no network. */
    fun recordCrash(context: Context, thread: Thread, error: Throwable) {
        val inspector = PhoneInspector(context)
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
            .lineSequence().take(25).joinToString("\n")
        val explanation = explainEvent(InstallerEvent.InstallerCrashed, Texts.forLocale())
        val text = formatInstallReport(
            InstallReport(
                reportId = newReportId(),
                stage = "installer",
                code = explanation.code,
                studentTitle = explanation.title,
                needsAdmin = true,
                bundle = runCatching { inspector.bundle() }.getOrNull(),
                device = inspector.device(),
                installed = runCatching { inspector.installedCbx() }.getOrNull(),
                extra = "Thread: ${thread.name}\n$stack",
                installerVersion = BuildConfig.VERSION_NAME,
                timestamp = timestamp()
            )
        )
        TelegramReporter.enqueueNow(context, text)
    }
}
