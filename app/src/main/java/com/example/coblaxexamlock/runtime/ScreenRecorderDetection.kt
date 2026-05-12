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

internal data class ScreenRecorderPackageMatch(
    val packageName: String,
    val flags: Int,
    val enabled: Boolean,
    val source: ScreenRecorderDetectionSource
) {
    val systemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_SYSTEM != 0
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
    return inspectScreenRecorderAppsFromInventory(
        context = context,
        inventory = readInstalledPackageInventory(context)
    )
}

internal fun detectScreenRecorderPackagesFromInventory(
    context: Context,
    inventory: InstalledPackageInventory,
    metadataResolver: (String) -> InstalledPackageMetadata? = { packageName ->
        resolveInstalledPackageMetadata(
            context = context,
            packageName = packageName,
            packageInventory = inventory,
            includeDisplayMetadata = true
        )
    }
): List<String> {
    return inspectScreenRecorderAppsFromInventory(
        context = context,
        inventory = inventory,
        metadataResolver = metadataResolver
    )
        .map { report -> report.displayLabel }
}

internal fun inspectScreenRecorderAppsFromInventory(
    context: Context,
    inventory: InstalledPackageInventory,
    metadataResolver: (String) -> InstalledPackageMetadata? = { packageName ->
        resolveInstalledPackageMetadata(
            context = context,
            packageName = packageName,
            packageInventory = inventory,
            includeDisplayMetadata = true
        )
    }
): List<ScreenRecorderAppReport> {
    val packageManager = context.packageManager
    return findScreenRecorderMatchesFromInventory(
        inventory = inventory,
        fallbackRecordProvider = { packageName ->
            packageManager.loadInstalledPackageRecord(packageName)
        }
    ).map { match ->
        buildScreenRecorderAppReport(
            match = match,
            metadata = metadataResolver(match.packageName)
        )
    }
}

internal fun findScreenRecorderMatchesFromInventory(
    inventory: InstalledPackageInventory,
    fallbackRecordProvider: (String) -> InstalledPackageRecord? = { null }
): List<ScreenRecorderPackageMatch> {
    val results = linkedMapOf<String, ScreenRecorderPackageMatch>()

    // Phase 1: Check known package names (fast exact-match lookup).
    for (packageName in KnownScreenRecorderPackages) {
        val record = inventory.get(packageName) ?: fallbackRecordProvider(packageName) ?: continue
        results[packageName] = ScreenRecorderPackageMatch(
            packageName = packageName,
            flags = record.flags,
            enabled = record.enabled,
            source = ScreenRecorderDetectionSource.KnownPackage
        )
    }

    // Phase 2: Keyword-based scan of all installed user (non-system) packages.
    for (record in inventory.records) {
        // Only user-installed apps are suspicious in keyword scan.
        if (record.systemApp) continue
        val pkg = record.packageName.lowercase()
        if (ScreenRecorderKeywords.any { keyword -> pkg.contains(keyword) }) {
            results.putIfAbsent(
                record.packageName,
                ScreenRecorderPackageMatch(
                    packageName = record.packageName,
                    flags = record.flags,
                    enabled = record.enabled,
                    source = ScreenRecorderDetectionSource.KeywordScan
                )
            )
        }
    }

    return results.values.toList()
}

private fun buildScreenRecorderAppReport(
    match: ScreenRecorderPackageMatch,
    metadata: InstalledPackageMetadata?
): ScreenRecorderAppReport {
    return ScreenRecorderAppReport(
        label = metadata?.label?.ifBlank { match.packageName } ?: match.packageName,
        packageName = match.packageName,
        versionName = metadata?.versionName?.ifBlank { "-" } ?: "-",
        systemApp = metadata?.systemOrUpdatedSystemApp ?: (
            match.systemApp ||
                (match.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            ),
        enabled = metadata?.enabled ?: match.enabled,
        source = match.source
    )
}

private fun PackageManager.loadInstalledPackageRecord(packageName: String): InstalledPackageRecord? {
    return loadApplicationInfo(packageName)?.toInstalledPackageRecord()
}

private fun PackageManager.loadApplicationInfo(packageName: String): ApplicationInfo? {
    return runCatching {
        @Suppress("DEPRECATION")
        getApplicationInfo(packageName, 0)
    }.getOrNull()
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
