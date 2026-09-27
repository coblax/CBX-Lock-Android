package com.coblax.examlock.installer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File

internal data class InstallResult(
    val sessionId: Int,
    val status: Int,
    val message: String?,
    val legacyStatus: Int?,
    val confirmIntent: Intent?,
    /** Set when the report was already sent while the installer was closed. */
    val alreadyReportedAs: String? = null
)

/**
 * Installs CBX through PackageInstaller. Unlike opening the APK file, a session reports back
 * the exact reason when Android refuses (INSTALL_PARSE_FAILED_…, INSTALL_FAILED_…).
 */
internal object SessionInstaller {
    private const val ActionInstallResult = "com.coblax.examlock.installer.INSTALL_RESULT"
    const val ExtraSessionId = "com.coblax.examlock.installer.SESSION_ID"

    /** Writes [apk] into a new session and commits it; returns the session id. Blocking. */
    fun install(context: Context, apk: File, writeLimitBytes: Long = apk.length()): Int {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(CbxPackageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 26) {
                setInstallReason(PackageManager.INSTALL_REASON_USER)
            }
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                session.openWrite("cbx.apk", 0, apk.length()).use { output ->
                    apk.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var remaining = writeLimitBytes
                        while (remaining > 0) {
                            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            remaining -= read
                        }
                    }
                    session.fsync(output)
                }
                val intent = Intent(context, InstallResultReceiver::class.java)
                    .setAction(ActionInstallResult)
                    .putExtra(ExtraSessionId, sessionId)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    // The system fills the status into this intent, so it must stay mutable.
                    (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                val callback = PendingIntent.getBroadcast(context, sessionId, intent, flags)
                session.commit(callback.intentSender)
            }
        } catch (error: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw error
        }
        return sessionId
    }

    fun abandon(context: Context, sessionId: Int) {
        runCatching { context.packageManager.packageInstaller.abandonSession(sessionId) }
    }

    /** The session as Android sees it now, or null once it is finished or gone. */
    fun sessionInfo(context: Context, sessionId: Int): PackageInstaller.SessionInfo? =
        runCatching { context.packageManager.packageInstaller.getSessionInfo(sessionId) }.getOrNull()
}

/** Hands install results to the open installer screen, or keeps the last one for later. */
internal object InstallResultBus {
    private const val PreferencesName = "installer_result"
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var listener: ((InstallResult) -> Unit)? = null

    /**
     * Returns true when the result still needs background work (a report); the caller then
     * keeps the receiver alive until [finishUnattended] is done.
     */
    fun post(context: Context, result: InstallResult): Boolean {
        val current = listener
        if (current != null) {
            mainHandler.post { current(result) }
            return false
        }
        if (result.status == SessionStatus.PendingUserAction) {
            // Nobody to ask the student: drop the session so it cannot hang around.
            SessionInstaller.abandon(context, result.sessionId)
            return false
        }
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).edit()
            .putInt("session", result.sessionId)
            .putInt("status", result.status)
            .putString("message", result.message)
            .putInt("legacy", result.legacyStatus ?: Int.MIN_VALUE)
            .commit()
        return true
    }

    /**
     * The installer was closed when Android answered: report a failure right away (not only
     * when the student reopens it, which may be never) and tell the student in a notification.
     */
    fun finishUnattended(context: Context, result: InstallResult) {
        val texts = Texts.forLocale()
        if (result.status == SessionStatus.Success) {
            InstallNotifier.show(
                context,
                success = true,
                title = texts.t("CBX Lock is installed", "CBX Lock berhasil dipasang"),
                body = texts.t("Tap to open CBX Installer.", "Ketuk untuk membuka CBX Installer.")
            )
            return
        }
        val explanation = explainInstallFailure(result.status, result.message, texts)
        val reportId = runCatching { Diagnostics.reportUnattendedResult(context, result) }.getOrNull()
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).edit()
            .putString("report_id", reportId)
            .commit()
        InstallNotifier.show(
            context,
            success = false,
            title = explanation.title,
            body = explanation.advice + (reportId?.let { "\n" + texts.t("Report ID: ", "ID laporan: ") + it } ?: "")
        )
    }

    /** A result that arrived while the screen was closed, handed over once. */
    fun takeStored(context: Context): InstallResult? {
        val prefs = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        if (!prefs.contains("status")) return null
        val legacy = prefs.getInt("legacy", Int.MIN_VALUE)
        val result = InstallResult(
            sessionId = prefs.getInt("session", -1),
            status = prefs.getInt("status", SessionStatus.Failure),
            message = prefs.getString("message", null),
            legacyStatus = legacy.takeIf { it != Int.MIN_VALUE },
            confirmIntent = null,
            alreadyReportedAs = prefs.getString("report_id", null)
        )
        prefs.edit().clear().apply()
        return result
    }
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val legacyKey = "android.content.pm.extra.LEGACY_STATUS"
        val appContext = context.applicationContext
        val result = InstallResult(
            sessionId = intent.getIntExtra(
                PackageInstaller.EXTRA_SESSION_ID,
                intent.getIntExtra(SessionInstaller.ExtraSessionId, -1)
            ),
            status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, SessionStatus.Failure),
            message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
            legacyStatus = if (intent.hasExtra(legacyKey)) intent.getIntExtra(legacyKey, 0) else null,
            confirmIntent = intent.confirmIntent()
        )
        if (InstallResultBus.post(appContext, result)) {
            val pending = goAsync()
            Thread {
                try {
                    InstallResultBus.finishUnattended(appContext, result)
                } finally {
                    pending.finish()
                }
            }.start()
        }
    }
}

@Suppress("DEPRECATION")
private fun Intent.confirmIntent(): Intent? =
    if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
    } else {
        getParcelableExtra(Intent.EXTRA_INTENT)
    }
