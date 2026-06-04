package com.example.coblaxexamlock.runtime

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Proactively scans installed packages that currently hold an active
 * SYSTEM_ALERT_WINDOW (Appear on Top) permission.
 *
 * This is the **pre-emptive** companion to the runtime obscured-touch and
 * window-focus-loss detectors: it identifies the *source* of potential
 * overlay cheating before the floating window actually appears on the
 * exam screen.
 */

internal data class OverlayAppInfo(
    val packageName: String,
    val appLabel: String,
    val isSystemApp: Boolean
)

internal data class OverlayAppScanResult(
    val packagesWithOverlayPermission: List<OverlayAppInfo>,
    val totalCount: Int
) {
    val hasRiskyApps: Boolean
        get() = packagesWithOverlayPermission.isNotEmpty()
}

/**
 * Well-known system packages whose overlay capability is benign and should
 * never be flagged to the student.  Keep the list minimal — only packages
 * that ship with the ROM itself and are required for normal system operation.
 */
private val OverlaySafeSystemPackages = setOf(
    "com.android.systemui",
    "com.android.settings",
    "com.android.shell",
    "com.android.providers.settings",
    "com.android.inputdevices",
    "com.google.android.inputmethod.latin",       // Gboard
    "com.samsung.android.honeyboard",             // Samsung keyboard
    "com.google.android.gms",                     // Google Play Services
    "com.google.android.gsf",                     // Google Services Framework
    "com.google.android.permissioncontroller",
    "com.samsung.android.incallui",               // Samsung in-call UI
    "com.samsung.android.smartswitchassistant",
    "com.android.bluetooth",
    "com.android.nfc"
)

internal fun scanOverlayApps(context: Context): OverlayAppScanResult {
    val appContext = context.applicationContext
    val appOpsManager = appContext.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        ?: return OverlayAppScanResult(emptyList(), 0)
    val pm = appContext.packageManager
    val ownPackageName = appContext.packageName

    val installedApps = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
    }.getOrDefault(emptyList())

    val riskyApps = mutableListOf<OverlayAppInfo>()

    for (appInfo in installedApps) {
        val packageName = appInfo.packageName ?: continue

        // Skip our own package
        if (packageName == ownPackageName) continue

        // Skip safe system packages
        if (packageName in OverlaySafeSystemPackages) continue

        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        // Check if SYSTEM_ALERT_WINDOW is granted
        val overlayAllowed = isOverlayPermissionGranted(
            appOpsManager = appOpsManager,
            packageName = packageName,
            uid = appInfo.uid
        )

        if (overlayAllowed) {
            val label = runCatching {
                pm.getApplicationLabel(appInfo).toString()
            }.getOrDefault(packageName)

            riskyApps.add(
                OverlayAppInfo(
                    packageName = packageName,
                    appLabel = label,
                    isSystemApp = isSystemApp
                )
            )
        }
    }

    return OverlayAppScanResult(
        packagesWithOverlayPermission = riskyApps,
        totalCount = riskyApps.size
    )
}

@Suppress("DEPRECATION")
private fun isOverlayPermissionGranted(
    appOpsManager: AppOpsManager,
    packageName: String,
    uid: Int
): Boolean {
    return runCatching {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                uid,
                packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                uid,
                packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)
}
