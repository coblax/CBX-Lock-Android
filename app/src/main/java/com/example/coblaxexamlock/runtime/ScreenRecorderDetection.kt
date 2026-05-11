package com.example.coblaxexamlock.runtime

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * Detects installed screen recorder applications.
 *
 * Phase 1: Exact-match lookup of known screen recorder package names.
 * Phase 2: Keyword-based scan of all installed user (non-system) packages.
 *
 * System built-in recorders (Samsung Game Tools, Xiaomi built-in, Google Recorder)
 * are excluded because they ship with the OEM ROM and are not cheating indicators.
 */

internal val KnownScreenRecorderPackages = listOf(
    // AZ Screen Recorder
    "com.kimcy929.screenrecorder",
    // Screen Recorder - No Ads
    "com.hecorat.screenrecorder.free",
    // Screen Recorder - XRecorder
    "screenrecorder.recorder.editor",
    // DU Recorder
    "com.duapps.recorder",
    // Mobizen Screen Recorder
    "com.mobizen.recorder",
    // Mobizen (Samsung variant)
    "com.rsupport.mvagent",
    // ADV Screen Recorder
    "com.appsmartz.screenrecorder",
    // Vidma Recorder
    "com.vidma.recorder.pro",
    // Super Screen Recorder
    "com.rec.screen.recorder",
    // Screen Recorder Pro
    "com.screenrecord.pro",
    // ilos Screen Recorder
    "com.ilos.screen",
    // NLL Screen Recorder
    "com.nll.screenrecorder",
    // Screen Recorder by InShot
    "com.xinternalstudio.screenrecord",
    // REC Screen Recorder
    "com.spectrl.rec"
)

internal enum class ScreenRecorderDetectionSource {
    KnownPackage,
    KeywordScan
}

internal data class ScreenRecorderAppReport(
    val label: String,
    val packageName: String,
    val versionName: String,
    val systemApp: Boolean,
    val enabled: Boolean,
    val source: ScreenRecorderDetectionSource
) {
    val displayLabel: String
        get() = formatScreenRecorderLabel(label, packageName)
}

private val ScreenRecorderKeywords = listOf(
    "screenrecorder",
    "screen.recorder",
    "screen_recorder",
    "screenrecord",
    "screen.record",
    "screen_record",
    "screencapture",
    "screen.capture",
    "screen_capture"
)

internal fun detectScreenRecorderPackages(context: Context): List<String> {
    return inspectScreenRecorderApps(context).map { report -> report.displayLabel }
}

internal fun inspectScreenRecorderApps(context: Context): List<ScreenRecorderAppReport> {
    val packageManager = context.packageManager
    val results = linkedMapOf<String, ScreenRecorderAppReport>()

    // Phase 1: Check known package names (fast exact-match lookup).
    for (packageName in KnownScreenRecorderPackages) {
        val appInfo = runCatching {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }.getOrNull() ?: continue
        results[packageName] = buildScreenRecorderAppReport(
            packageManager = packageManager,
            appInfo = appInfo,
            packageName = packageName,
            source = ScreenRecorderDetectionSource.KnownPackage
        )
    }

    // Phase 2: Keyword-based scan of all installed user (non-system) packages.
    val installedPackages = runCatching {
        @Suppress("DEPRECATION", "QueryPermissionsNeeded")
        packageManager.getInstalledApplications(0)
    }.getOrDefault(emptyList())
    for (appInfo in installedPackages) {
        // Only user-installed apps are suspicious in keyword scan.
        if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) continue
        val pkg = appInfo.packageName.lowercase()
        if (ScreenRecorderKeywords.any { keyword -> pkg.contains(keyword) }) {
            results.putIfAbsent(
                appInfo.packageName,
                buildScreenRecorderAppReport(
                    packageManager = packageManager,
                    appInfo = appInfo,
                    packageName = appInfo.packageName,
                    source = ScreenRecorderDetectionSource.KeywordScan
                )
            )
        }
    }

    return results.values.toList()
}

private fun buildScreenRecorderAppReport(
    packageManager: PackageManager,
    appInfo: ApplicationInfo,
    packageName: String,
    source: ScreenRecorderDetectionSource
): ScreenRecorderAppReport {
    val label = runCatching {
        packageManager.getApplicationLabel(appInfo).toString().trim()
    }.getOrDefault("")
    val versionName = runCatching {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull().orEmpty()
    val isSystemApp =
        appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    return ScreenRecorderAppReport(
        label = label.ifBlank { packageName },
        packageName = packageName,
        versionName = versionName.ifBlank { "-" },
        systemApp = isSystemApp,
        enabled = appInfo.enabled,
        source = source
    )
}

private fun formatScreenRecorderLabel(
    label: String,
    packageName: String
): String {
    return if (label.isNotBlank() && !label.equals(packageName, ignoreCase = true)) {
        "$label ($packageName)"
    } else {
        packageName
    }
}
