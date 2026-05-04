package com.example.coblaxexamlock.runtime

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.webkit.WebViewCompat
import com.example.coblaxexamlock.config.EmulatorPackagePrefixes
import com.example.coblaxexamlock.config.MagiskIndicatorPaths
import com.example.coblaxexamlock.config.RootBinaryIndicatorPaths
import com.example.coblaxexamlock.config.RootPackageNames
import com.example.coblaxexamlock.config.VirtualFingerprintTokens
import com.example.coblaxexamlock.config.VirtualHardwareTokens
import com.example.coblaxexamlock.config.VirtualManufacturerTokens
import com.example.coblaxexamlock.config.VirtualModelTokens
import com.example.coblaxexamlock.config.VirtualProductTokens
import com.example.coblaxexamlock.config.VirtualQemuFiles
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.model.ClipboardDiagnostics
import com.example.coblaxexamlock.model.RootDetectionDetails
import com.example.coblaxexamlock.model.RootIndicatorType
import com.example.coblaxexamlock.model.VirtualEnvironmentDiagnostics
import com.example.coblaxexamlock.readClipboardSnapshot
import java.util.Locale
import java.util.TimeZone


internal fun isAccessibilityServiceEnabled(context: Context): Boolean {
    return inspectAccessibility(context).blockingServiceActive
}

internal fun isAccessibilityManagerEnabled(context: Context): Boolean {
    val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
    return accessibilityManager?.isEnabled == true
}

internal fun isTouchExplorationEnabled(context: Context): Boolean {
    return inspectAccessibility(context).touchExplorationEnabled
}

internal fun getEnabledAccessibilityServicesRawValue(context: Context): String {
    return runCatching {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
    }.getOrDefault("").ifBlank { "-" }
}

internal fun isDeveloperOptionsEnabled(context: Context): Boolean {
    return runCatching {
        Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1
    }.getOrDefault(false)
}

internal fun getDeveloperOptionsRawValue(context: Context): String {
    return runCatching {
        Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            -1
        ).toString()
    }.getOrDefault("-")
}

internal fun isAdbEnabled(context: Context): Boolean {
    return runCatching {
        Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        ) == 1
    }.getOrDefault(false)
}

internal fun getAdbRawValue(context: Context): String {
    return runCatching {
        Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            -1
        ).toString()
    }.getOrDefault("-")
}

internal fun getRootDetectionDetails(context: Context): RootDetectionDetails {
    val hasTestKeys = Build.TAGS?.contains("test-keys") == true
    val rootBinaryPaths = RootBinaryIndicatorPaths.distinct().filter(::safeFileExists)
    val hasSuBinary = rootBinaryPaths.any { path -> path.endsWith("/su") }
    val foundRootPackages = RootPackageNames.filter { packageName ->
        runCatching { context.packageManager.getApplicationInfo(packageName, 0) }.isSuccess
    }
    val magiskPaths = MagiskIndicatorPaths.distinct().filter(::safeFileExists)
    val zygiskDetected = safeFileExists("/data/adb/zygisk") || scanProcSelfMapsForZygisk()
    val verifiedBootStateRaw = getSystemProperty("ro.boot.verifiedbootstate").trim()
    val vbmetaDeviceStateRaw = getSystemProperty("ro.boot.vbmeta.device_state").trim()
    val flashLockedRaw = getSystemProperty("ro.boot.flash.locked").trim()
    val bootloaderUnlocked = isBootloaderUnlocked(
        verifiedBootState = verifiedBootStateRaw,
        vbmetaDeviceState = vbmetaDeviceStateRaw,
        flashLocked = flashLockedRaw
    )
    val roDebuggableRaw = getSystemProperty("ro.debuggable").trim()
    val roSecureRaw = getSystemProperty("ro.secure").trim()
    val roAdbSecureRaw = getSystemProperty("ro.adb.secure").trim()
    val roBuildTypeRaw = getSystemProperty("ro.build.type").trim()
    val dangerousSystemProperties = buildList {
        if (roDebuggableRaw == "1") add("ro.debuggable=1")
        if (roSecureRaw == "0") add("ro.secure=0")
        if (roAdbSecureRaw == "0") add("ro.adb.secure=0")
        if (roBuildTypeRaw.isNotBlank() && !roBuildTypeRaw.equals("user", ignoreCase = true)) {
            add("ro.build.type=$roBuildTypeRaw")
        }
    }
    val selinuxEnabled = readSelinuxEnabled()
    val selinuxEnforced = readSelinuxEnforced()

    return RootDetectionDetails(
        hasTestKeys = hasTestKeys,
        hasSuBinary = hasSuBinary,
        foundRootPackages = foundRootPackages,
        rootBinaryPaths = rootBinaryPaths,
        magiskPaths = magiskPaths,
        zygiskDetected = zygiskDetected,
        verifiedBootState = verifiedBootStateRaw.ifBlank { "-" },
        vbmetaDeviceState = vbmetaDeviceStateRaw.ifBlank { "-" },
        flashLocked = flashLockedRaw.ifBlank { "-" },
        bootloaderUnlocked = bootloaderUnlocked,
        selinuxEnabled = selinuxEnabled,
        selinuxEnforced = selinuxEnforced,
        dangerousSystemProperties = dangerousSystemProperties,
        roDebuggable = roDebuggableRaw.ifBlank { "-" },
        roSecure = roSecureRaw.ifBlank { "-" },
        roAdbSecure = roAdbSecureRaw.ifBlank { "-" },
        roBuildType = roBuildTypeRaw.ifBlank { "-" }
    )
}

internal fun isDeviceRooted(details: RootDetectionDetails): Boolean {
    return details.hasTestKeys ||
        details.hasSuBinary ||
        details.rootBinaryPaths.isNotEmpty() ||
        details.foundRootPackages.isNotEmpty() ||
        details.magiskPaths.isNotEmpty() ||
        details.zygiskDetected ||
        details.bootloaderUnlocked ||
        details.dangerousSystemProperties.isNotEmpty() ||
        details.selinuxEnabled == false
}

internal fun isBootloaderUnlocked(
    verifiedBootState: String,
    vbmetaDeviceState: String,
    flashLocked: String
): Boolean {
    val verified = verifiedBootState.trim()
    val vbmeta = vbmetaDeviceState.trim()
    val flash = flashLocked.trim()
    val verifiedIndicator = verified.isNotBlank() && !verified.equals("green", ignoreCase = true)
    val vbmetaIndicator = vbmeta.equals("unlocked", ignoreCase = true)
    val flashIndicator = flash == "0"
    return verifiedIndicator || vbmetaIndicator || flashIndicator
}

internal fun resolvePrimaryRootIndicator(details: RootDetectionDetails): RootIndicatorType? {
    return when {
        details.zygiskDetected -> RootIndicatorType.Zygisk
        details.magiskPaths.isNotEmpty() -> RootIndicatorType.Magisk
        details.rootBinaryPaths.isNotEmpty() || details.hasSuBinary -> RootIndicatorType.RootBinary
        details.selinuxEnabled == false -> RootIndicatorType.SelinuxDisabled
        details.bootloaderUnlocked -> RootIndicatorType.Bootloader
        details.dangerousSystemProperties.isNotEmpty() -> RootIndicatorType.DangerousProps
        details.hasTestKeys -> RootIndicatorType.TestKeys
        details.selinuxEnforced == false -> RootIndicatorType.SelinuxPermissive
        else -> null
    }
}

internal fun isSelinuxPermissive(details: RootDetectionDetails): Boolean {
    return details.selinuxEnabled == true && details.selinuxEnforced == false
}

internal fun buildRootIndicatorLabel(
    details: RootDetectionDetails,
    indicator: RootIndicatorType
): String {
    return when (indicator) {
        RootIndicatorType.Zygisk -> "Zygisk terdeteksi"
        RootIndicatorType.Magisk -> {
            val path = details.magiskPaths.firstOrNull()
            if (path == null) {
                "Folder Magisk terdeteksi"
            } else {
                "Folder Magisk terdeteksi: $path"
            }
        }
        RootIndicatorType.RootBinary -> {
            val path = details.rootBinaryPaths.firstOrNull()
            if (path == null) {
                "Binary root ditemukan"
            } else {
                "Binary root ditemukan: $path"
            }
        }
        RootIndicatorType.SelinuxDisabled -> "SELinux nonaktif"
        RootIndicatorType.SelinuxPermissive -> "SELinux permissive"
        RootIndicatorType.Bootloader -> {
            val info = listOfNotNull(
                details.verifiedBootState.takeIf { it != "-" }?.let { "verifiedbootstate=$it" },
                details.vbmetaDeviceState.takeIf { it != "-" }?.let { "vbmeta=$it" },
                details.flashLocked.takeIf { it != "-" }?.let { "flash.locked=$it" }
            ).joinToString()
            if (info.isBlank()) {
                "Verified boot/bootloader tidak terkunci"
            } else {
                "Verified boot/bootloader: $info"
            }
        }
        RootIndicatorType.DangerousProps -> {
            val prop = details.dangerousSystemProperties.firstOrNull()
            if (prop == null) {
                "Properti sistem berbahaya terdeteksi"
            } else {
                "Properti sistem berbahaya: $prop"
            }
        }
        RootIndicatorType.TestKeys -> "Build menggunakan test-keys"
    }
}

internal fun buildRootIssueMessage(details: RootDetectionDetails): String {
    val indicator = resolvePrimaryRootIndicator(details)
    val indicatorLabel = indicator?.let { buildRootIndicatorLabel(details, it) }
    return if (indicatorLabel.isNullOrBlank()) {
        "Perangkat ini terdeteksi memiliki indikator root. " +
            "Demi keamanan ujian, gunakan perangkat non-root untuk melanjutkan ujian."
    } else {
        "Perangkat ini terdeteksi memiliki indikator root ($indicatorLabel). " +
            "Demi keamanan ujian, gunakan perangkat non-root untuk melanjutkan ujian."
    }
}

internal fun formatYesNo(value: Boolean?): String {
    return when (value) {
        null -> "Tidak diketahui"
        true -> "Ya"
        false -> "Tidak"
    }
}

internal fun safeFileExists(path: String): Boolean {
    return runCatching { java.io.File(path).exists() }.getOrDefault(false)
}

internal fun scanProcSelfMapsForZygisk(): Boolean {
    return runCatching {
        val mapsFile = java.io.File("/proc/self/maps")
        if (!mapsFile.canRead()) {
            return@runCatching false
        }
        mapsFile.useLines { lines ->
            lines.any { line ->
                line.contains("zygisk", ignoreCase = true) ||
                    line.contains("libzygisk", ignoreCase = true)
            }
        }
    }.getOrDefault(false)
}

@SuppressLint("PrivateApi")
internal fun readSelinuxEnabled(): Boolean? {
    return runCatching {
        val selinuxClass = Class.forName("android.os.SELinux")
        val method = selinuxClass.getMethod("isSELinuxEnabled")
        method.invoke(null) as? Boolean
    }.getOrNull()
}

@SuppressLint("PrivateApi")
internal fun readSelinuxEnforced(): Boolean? {
    return runCatching {
        val selinuxClass = Class.forName("android.os.SELinux")
        val method = selinuxClass.getMethod("isSELinuxEnforced")
        method.invoke(null) as? Boolean
    }.getOrNull()
}

@SuppressLint("PrivateApi")
internal fun getSystemProperty(key: String): String {
    return runCatching {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val getMethod = systemProperties.getMethod("get", String::class.java, String::class.java)
        (getMethod.invoke(null, key, "") as? String).orEmpty()
    }.getOrDefault("")
}

@SuppressLint("QueryPermissionsNeeded")
internal fun getVirtualEnvironmentDiagnostics(context: Context): VirtualEnvironmentDiagnostics {
    val indicators = mutableListOf<String>()
    val fingerprint = Build.FINGERPRINT.orEmpty()
    if (VirtualFingerprintTokens.any { token ->
            fingerprint.contains(token, ignoreCase = true)
        }
    ) {
        indicators.add("fingerprint:$fingerprint")
    }

    val model = Build.MODEL.orEmpty()
    if (VirtualModelTokens.any { token ->
            model.contains(token, ignoreCase = true)
        }
    ) {
        indicators.add("model:$model")
    }

    val manufacturer = Build.MANUFACTURER.orEmpty()
    if (VirtualManufacturerTokens.any { token ->
            manufacturer.contains(token, ignoreCase = true)
        }
    ) {
        indicators.add("manufacturer:$manufacturer")
    }

    val brand = Build.BRAND.orEmpty()
    val device = Build.DEVICE.orEmpty()
    if (brand.startsWith("generic", ignoreCase = true) ||
        device.startsWith("generic", ignoreCase = true)
    ) {
        indicators.add("generic_brand_device:${brand}/${device}")
    }

    val product = Build.PRODUCT.orEmpty()
    if (VirtualProductTokens.any { token ->
            product.contains(token, ignoreCase = true)
        }
    ) {
        indicators.add("product:$product")
    }

    val hardware = Build.HARDWARE.orEmpty()
    if (VirtualHardwareTokens.any { token ->
            hardware.contains(token, ignoreCase = true)
        }
    ) {
        indicators.add("hardware:$hardware")
    }

    val abis = Build.SUPPORTED_ABIS?.toList() ?: emptyList()
    if (abis.any { it.contains("x86", ignoreCase = true) }) {
        indicators.add("abis:${abis.joinToString()}")
    }

    val qemuProperty = getSystemProperty("ro.kernel.qemu").trim()
    if (qemuProperty == "1") {
        indicators.add("ro.kernel.qemu=1")
    }

    val qemuFiles = VirtualQemuFiles.filter { path ->
        runCatching { java.io.File(path).exists() }.getOrDefault(false)
    }
    if (qemuFiles.isNotEmpty()) {
        indicators.add("qemu_files:${qemuFiles.joinToString()}")
    }

    val installedPackages = runCatching {
        context.packageManager.getInstalledPackages(0).map { it.packageName }
    }.getOrDefault(emptyList())
    val emulatorPackages = installedPackages.filter { packageName ->
        EmulatorPackagePrefixes.any { prefix ->
            packageName.startsWith(prefix, ignoreCase = true)
        }
    }
    if (emulatorPackages.isNotEmpty()) {
        indicators.add("packages:${emulatorPackages.joinToString()}")
    }

    return VirtualEnvironmentDiagnostics(
        detected = indicators.isNotEmpty(),
        indicators = indicators,
        qemuProperty = qemuProperty,
        emulatorPackages = emulatorPackages,
        qemuFiles = qemuFiles,
        abis = abis
    )
}

internal fun getEnabledAccessibilityServicePackages(context: Context): List<String> {
    return inspectAccessibility(context).activePackages
}

internal fun getRiskyAccessibilityPackages(context: Context): List<String> {
    return inspectAccessibility(context).riskyPackages
}

internal fun isUsbConnected(context: Context): Boolean {
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    return plugged and BatteryManager.BATTERY_PLUGGED_USB != 0
}

@Suppress("DEPRECATION")
internal fun getInstallSourceSummary(context: Context): String {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val info = context.packageManager.getInstallSourceInfo(context.packageName)
            listOfNotNull(
                info.initiatingPackageName,
                info.installingPackageName,
                info.originatingPackageName
            ).distinct().joinToString().ifBlank { "-" }
        } else {
            context.packageManager.getInstallerPackageName(context.packageName).orEmpty().ifBlank { "-" }
        }
    }.getOrDefault("-")
}

internal fun isAppDebuggable(context: Context): Boolean {
    val appInfo = context.applicationInfo
    return appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}

internal fun getSecurityPatchLevel(): String {
    return Build.VERSION.SECURITY_PATCH.takeIf(String::isNotBlank) ?: "-"
}

internal fun getCurrentWebViewPackageSummary(context: Context): String {
    val packageInfo = WebViewCompat.getCurrentWebViewPackage(context)

    if (packageInfo != null) {
        return "${packageInfo.packageName} ${packageInfo.versionName ?: ""}".trim()
    }

    val knownPackages = listOf(
        "com.google.android.webview",
        "com.android.webview",
        "com.sec.android.app.sbrowser"
    )

    knownPackages.forEach { packageName ->
        val found = runCatching { context.packageManager.getPackageInfo(packageName, 0) }.getOrNull()
        if (found != null) {
            return "${found.packageName} ${found.versionName ?: ""}".trim()
        }
    }

    return "-"
}

internal fun formatBytesToReadable(byteCount: Long): String {
    val gigaByte = 1024L * 1024L * 1024L
    val megaByte = 1024L * 1024L
    return when {
        byteCount >= gigaByte -> String.format(Locale.US, "%.2f GB", byteCount.toDouble() / gigaByte)
        byteCount >= megaByte -> String.format(Locale.US, "%.0f MB", byteCount.toDouble() / megaByte)
        else -> "$byteCount B"
    }
}

internal fun getMemorySummary(context: Context): String {
    val activityManager = context.getSystemService(ActivityManager::class.java) ?: return "-"
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    return "avail ${formatBytesToReadable(memoryInfo.availMem)} / total ${formatBytesToReadable(memoryInfo.totalMem)}"
}

internal fun getStorageSummary(context: Context): String {
    return runCatching {
        val statFs = StatFs(context.filesDir.absolutePath)
        val availableBytes = statFs.availableBytes
        val totalBytes = statFs.totalBytes
        "avail ${formatBytesToReadable(availableBytes)} / total ${formatBytesToReadable(totalBytes)}"
    }.getOrDefault("-")
}

internal fun getLocaleSummary(context: Context): String {
    val configuration = context.resources.configuration
    val locale =
        if (!configuration.locales.isEmpty) {
            configuration.locales.get(0)
        } else {
            Locale.getDefault()
        }
    return locale.toLanguageTag()
}

internal fun getTimezoneSummary(): String {
    val timeZone = TimeZone.getDefault()
    return "${timeZone.id} (${timeZone.displayName})"
}

internal fun getClipboardDiagnostics(context: Context): ClipboardDiagnostics {
    val snapshot = readClipboardSnapshot(context)
    return ClipboardDiagnostics(
        hasData = !snapshot.isEmpty,
        itemCount = snapshot.itemCount,
        currentSemanticSignature = snapshot.semanticSignature
    )
}
