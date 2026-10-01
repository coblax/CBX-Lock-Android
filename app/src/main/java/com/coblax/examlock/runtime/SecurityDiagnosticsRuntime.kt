package com.coblax.examlock.runtime

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
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.view.accessibility.AccessibilityManager
import androidx.webkit.WebViewCompat
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.RuntimeStringDecoder
import com.coblax.examlock.config.EmulatorPackagePrefixes
import com.coblax.examlock.config.VirtualSpaceHostPackagePrefixes
import com.coblax.examlock.config.MagiskIndicatorPaths
import com.coblax.examlock.config.RootBinaryIndicatorPaths
import com.coblax.examlock.config.RootPackageNames
import com.coblax.examlock.config.VirtualFingerprintTokens
import com.coblax.examlock.config.VirtualHardwareTokens
import com.coblax.examlock.config.VirtualManufacturerTokens
import com.coblax.examlock.config.VirtualModelTokens
import com.coblax.examlock.config.VirtualProductTokens
import com.coblax.examlock.config.VirtualQemuFiles
import com.coblax.examlock.config.VirtualBoardTokens
import com.coblax.examlock.config.VirtualPresenceSystemPropertyKeys
import com.coblax.examlock.config.VirtualValueSystemProperties
import com.coblax.examlock.inspectAccessibility
import com.coblax.examlock.model.ClipboardDiagnostics
import com.coblax.examlock.model.RootDetectionDetails
import com.coblax.examlock.model.RootIndicatorType
import com.coblax.examlock.model.VirtualEnvironmentDiagnostics
import com.coblax.examlock.readClipboardSnapshot
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val virtualEnvDiagnosticsCache = AtomicReference<VirtualEnvironmentDiagnostics?>()

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
    val appContext = context.applicationContext
    return getRootDetectionDetails(
        context = appContext,
        packageInventory = SecurityDetectorCache.readPackageInventory(appContext)
    )
}

internal fun getRootDetectionDetails(
    context: Context,
    packageInventory: InstalledPackageInventory
): RootDetectionDetails {
    val hasTestKeys = Build.TAGS?.contains("test-keys") == true
    val rootBinaryPaths = RootBinaryIndicatorPaths.distinct().filter(::safeFileExists)
    val hasSuBinary = rootBinaryPaths.any { path -> path.endsWith("/su") }
    val foundRootPackages = findRootPackagesFromInventory(packageInventory) { packageName ->
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(packageName, 0)
        }.isSuccess
    }
    val magiskPaths = MagiskIndicatorPaths.distinct().filter { safeFileExists(it) }
    val zygiskDetected = safeFileExists(HookProbeStrings.zygiskAdbPath) || scanProcSelfMapsForZygisk()
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
        // ro.adb.secure=0 is already monitored via AdbInspection.insecureSystemProperty;
        // some Samsung/Oppo OEM devices ship with this value on stock ROM — excluded to avoid
        // false-positive root detection.
        // ro.build.type is kept in evidenceSummary for diagnostics but excluded here because
        // Xiaomi MIUI Developer ROM legitimately ships as "userdebug" without root.
    }
    val selinuxEnabled = readSelinuxEnabled()
    val selinuxEnforced = readSelinuxEnforced()
    val xposedBridgeDetected = isXposedBridgeActive()

    return RootDetectionDetails(
        hasTestKeys = hasTestKeys,
        hasSuBinary = hasSuBinary,
        foundRootPackages = foundRootPackages,
        rootBinaryPaths = rootBinaryPaths,
        magiskPaths = magiskPaths,
        zygiskDetected = zygiskDetected,
        xposedBridgeDetected = xposedBridgeDetected,
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

/**
 * busybox is a general-purpose applet bundle that ships on plenty of stock ROMs —
 * Android TV boxes and several MediaTek vendor images carry it — so finding one says
 * nothing about root on its own. Every other watched path (su, daemonsu, Superuser.apk)
 * only exists because someone rooted the device.
 */
internal fun isCorroboratingRootBinaryPath(path: String): Boolean =
    path.substringAfterLast('/').equals("busybox", ignoreCase = true)

internal fun isDeviceRooted(details: RootDetectionDetails): Boolean {
    val conclusiveBinaries = details.rootBinaryPaths.filterNot { path ->
        isCorroboratingRootBinaryPath(path)
    }
    val corroboration = conclusiveBinaries.size < details.rootBinaryPaths.size ||
        details.selinuxEnforced == false
    return details.hasSuBinary ||
        conclusiveBinaries.isNotEmpty() ||
        details.foundRootPackages.isNotEmpty() ||
        details.magiskPaths.isNotEmpty() ||
        details.zygiskDetected ||
        details.xposedBridgeDetected ||
        details.bootloaderUnlocked ||
        details.dangerousSystemProperties.isNotEmpty() ||
        details.selinuxEnabled == false ||
        // ro.build.tags=test-keys is normal on genuine budget retail ROMs from smaller
        // OEMs, which is most of this app's install base, so on its own it refused the
        // exam on perfectly stock devices. It now has to be corroborated.
        (details.hasTestKeys && corroboration)
}

@Suppress("TooGenericExceptionCaught")
internal fun isXposedBridgeActive(): Boolean {
    // Check 1: XposedBridge class injected into this process (Xposed / LSPosed active)
    if (runCatching { Class.forName(HookProbeStrings.xposedBridgeClass) }.isSuccess) return true
    // Check 2: XposedBridge JAR on disk (classic Xposed installed at system level)
    if (safeFileExists(HookProbeStrings.xposedBridgeFrameworkJar)) return true
    if (safeFileExists(HookProbeStrings.xposedBridgeLibJar)) return true
    return false
}

internal fun isBootloaderUnlocked(
    verifiedBootState: String,
    vbmetaDeviceState: String,
    flashLocked: String
): Boolean {
    val verified = verifiedBootState.trim()
    val vbmeta = vbmetaDeviceState.trim()
    val flash = flashLocked.trim()
    // "green"  = verified boot with OEM production key (safe)
    // "yellow" = verified boot with custom device-specific key (safe on stock OEM ROMs;
    //             some Oppo/Realme/MediaTek devices return this without user unlocking the bootloader)
    // "orange" = user-unlocked bootloader (unsafe)
    // "red"    = boot verification failed (unsafe)
    val verifiedIndicator = verified.isNotBlank() &&
        !verified.equals("green", ignoreCase = true) &&
        !verified.equals("yellow", ignoreCase = true)
    val vbmetaIndicator = vbmeta.equals("unlocked", ignoreCase = true)
    val flashIndicator = flash == "0"
    return verifiedIndicator || vbmetaIndicator || flashIndicator
}

internal fun resolvePrimaryRootIndicator(details: RootDetectionDetails): RootIndicatorType? {
    return when {
        details.zygiskDetected -> RootIndicatorType.Zygisk
        details.xposedBridgeDetected -> RootIndicatorType.XposedBridge
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
        RootIndicatorType.XposedBridge -> "Xposed/LSPosed framework aktif"
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

// Hook/root probe inputs are stored obfuscated. In plaintext a release DEX answered
// `strings classes.dex | grep xposed` with this detector's exact shopping list, which
// is all an attacker needs to find the check and patch it out.
private object HookProbeStrings {
    private fun decode(value: String) = RuntimeStringDecoder.decodeBase64Xor(value)

    private const val XPOSED_BRIDGE_CLASS = "FxZdARwRBV0SHRcBHBoXXQsDHAAWF10rAxwAFhcxARoXFBY="  // de.robv.android.xposed.XposedBridge
    private const val XPOSED_BRIDGE_FRAMEWORK_JAR = "XAAKAAcWHlwVARIeFgQcARhcKwMcABYXMQEaFxQWXRkSAQ=="  // /system/framework/XposedBridge.jar
    private const val XPOSED_BRIDGE_LIB_JAR = "XAAKAAcWHlwfGhFcKwMcABYXMQEaFxQWXRkSAQ=="  // /system/lib/XposedBridge.jar
    private const val ZYGISK_ADB_PATH = "XBcSBxJcEhcRXAkKFBoAGA=="  // /data/adb/zygisk

    val xposedBridgeClass: String by lazy { decode(XPOSED_BRIDGE_CLASS) }
    val xposedBridgeFrameworkJar: String by lazy { decode(XPOSED_BRIDGE_FRAMEWORK_JAR) }
    val xposedBridgeLibJar: String by lazy { decode(XPOSED_BRIDGE_LIB_JAR) }
    val zygiskAdbPath: String by lazy { decode(ZYGISK_ADB_PATH) }

    val injectionMapMarkers: List<String> by lazy {
        listOf(
            "CQoUGgAY",  // zygisk
            "HxoRCQoUGgAY",  // libzygisk
            "HxoRARoBBg==",  // libriru
            "HwADHAAWFw==",  // lsposed
            "FhcLAxwAFhc=",  // edxposed
            "HxoRAAYRAAcBEgcW"  // libsubstrate
        ).map { decode(it) }
    }
}

internal fun scanProcSelfMapsForZygisk(): Boolean {
    return runCatching {
        val mapsFile = java.io.File("/proc/self/maps")
        if (!mapsFile.canRead()) {
            return@runCatching false
        }
        mapsFile.useLines { lines ->
            lines.any { line ->
                HookProbeStrings.injectionMapMarkers.any { marker ->
                    line.contains(marker, ignoreCase = true)
                }
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

private const val SelinuxEnforceNode = "/sys/fs/selinux/enforce"

/**
 * android.os.SELinux.isSELinuxEnforced() reads [SelinuxEnforceNode] and answers false
 * whenever that read fails. Apps lost read access to the node in Android 9, so the
 * hidden call reported every enforcing Android 9+ phone as permissive (and, on test-keys
 * ROMs, as rooted). Reading the node directly tells the two apart.
 */
internal fun readSelinuxEnforced(): Boolean? {
    var nodeValue: String? = null
    var accessDenied = false
    try {
        val fd = Os.open(SelinuxEnforceNode, OsConstants.O_RDONLY, 0)
        try {
            val buffer = ByteArray(4)
            val count = Os.read(fd, buffer, 0, buffer.size)
            if (count > 0) nodeValue = String(buffer, 0, count, Charsets.US_ASCII)
        } finally {
            runCatching { Os.close(fd) }
        }
    } catch (error: ErrnoException) {
        accessDenied = error.errno == OsConstants.EACCES
    } catch (_: Exception) {
        // Unreadable for another reason: leave the reading unknown.
    }
    return resolveSelinuxEnforced(nodeValue, accessDenied)
}

/**
 * The node is world-readable, so only the SELinux policy itself can refuse the read, and
 * a permissive policy merely logs that refusal and lets the read through. A denied read
 * therefore means enforcing.
 */
internal fun resolveSelinuxEnforced(nodeValue: String?, accessDenied: Boolean): Boolean? =
    when (nodeValue?.trim()) {
        "1" -> true
        "0" -> false
        else -> if (accessDenied) true else null
    }

@SuppressLint("PrivateApi")
internal fun getSystemProperty(key: String): String {
    return runCatching {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val getMethod = systemProperties.getMethod("get", String::class.java, String::class.java)
        (getMethod.invoke(null, key, "") as? String).orEmpty()
    }.getOrDefault("")
}

internal fun getVirtualEnvironmentDiagnostics(
    context: Context,
    forceRefresh: Boolean = false
): VirtualEnvironmentDiagnostics {
    if (!forceRefresh) {
        getCachedVirtualEnvironmentDiagnostics()?.let { return it }
    }

    val appContext = context.applicationContext
    val result = computeVirtualEnvironmentDiagnostics(
        context = appContext,
        packageInventory = SecurityDetectorCache.readPackageInventory(
            context = appContext,
            forceRefresh = forceRefresh
        )
    )
    if (forceRefresh) {
        virtualEnvDiagnosticsCache.set(result)
        return result
    }
    return if (virtualEnvDiagnosticsCache.compareAndSet(null, result)) result else {
        virtualEnvDiagnosticsCache.get() ?: result
    }
}

internal fun getCachedVirtualEnvironmentDiagnostics(): VirtualEnvironmentDiagnostics? {
    return virtualEnvDiagnosticsCache.get()
}

internal fun invalidateVirtualEnvironmentDiagnosticsCache() {
    virtualEnvDiagnosticsCache.set(null)
}

internal suspend fun getVirtualEnvironmentDiagnosticsOnIo(
    context: Context,
    forceRefresh: Boolean = false
): VirtualEnvironmentDiagnostics = withContext(LowRamDispatchers.detectorIo) {
    getVirtualEnvironmentDiagnostics(
        context = context.applicationContext,
        forceRefresh = forceRefresh
    )
}

internal fun findRootPackagesFromInventory(
    packageInventory: InstalledPackageInventory,
    fallbackPackageExists: (String) -> Boolean = { false }
): List<String> {
    return RootPackageNames.filter { packageName ->
        packageInventory.hasPackage(packageName) || fallbackPackageExists(packageName)
    }
}

internal fun findEmulatorPackagesFromInventory(
    inventory: InstalledPackageInventory
): List<String> = findPackagesWithPrefixes(inventory, EmulatorPackagePrefixes)

internal fun findVirtualSpaceHostPackagesFromInventory(
    inventory: InstalledPackageInventory
): List<String> = findPackagesWithPrefixes(inventory, VirtualSpaceHostPackagePrefixes)

private fun findPackagesWithPrefixes(
    inventory: InstalledPackageInventory,
    prefixes: List<String>
): List<String> {
    return inventory.records
        .asSequence()
        .map { record -> record.packageName }
        .filter { packageName ->
            prefixes.any { prefix -> packageName.startsWith(prefix, ignoreCase = true) }
        }
        .toList()
}

private val InstalledDataDirPattern = Regex(
    "^/(data/data|data/user(_de)?/\\d+|mnt/expand/[^/]+/user(_de)?/\\d+)/([^/]+)/?$"
)
private val InstalledSourceDirPrefixes = listOf("/data/app/", "/mnt/expand/")

/**
 * Evidence that this app is running inside a clone / virtual-space app rather than as
 * itself. Those hosts load the APK into their own process and redirect its storage into
 * their own data folder, which a normal install never has. Separate Android users (OEM
 * "dual apps", work profiles) keep the standard layout and are not flagged.
 */
internal fun resolveVirtualContainerIndicators(
    expectedPackageName: String,
    dataDir: String?,
    sourceDir: String?,
    processName: String?
): List<String> = buildList {
    val data = dataDir?.trim().orEmpty()
    if (data.isNotEmpty()) {
        val owner = InstalledDataDirPattern.matchEntire(data)?.groupValues?.lastOrNull()
        if (owner != expectedPackageName) {
            add("data_dir:$data")
        }
    }
    val source = sourceDir?.trim().orEmpty()
    if (source.isNotEmpty() && InstalledSourceDirPrefixes.none { source.startsWith(it) }) {
        add("source_dir:$source")
    }
    val process = processName?.trim().orEmpty()
    if (
        process.isNotEmpty() &&
        process != expectedPackageName &&
        !process.startsWith("$expectedPackageName:")
    ) {
        add("process:$process")
    }
}

private fun readCurrentProcessName(): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching { android.app.Application.getProcessName() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
    }
    return runCatching {
        java.io.File("/proc/self/cmdline").readText()
            .substringBefore('\u0000')
            .trim()
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

/**
 * Decides whether the signal counts add up to an emulator.
 *
 * The old rule was `score >= 2` with every strong signal worth 2, which meant any two
 * weak signals also crossed the line. A genuine budget tablet reporting `Build.BOARD`
 * of "unknown" with a short sensor list, or an x86 Chromebook, scored exactly 2 and was
 * refused the exam as an emulator. Weak signals now have to corroborate each other
 * three ways before they count on their own, while one unambiguous strong signal is
 * still conclusive — a real emulator trips several of those at once.
 */
internal fun resolveVirtualEnvironmentDetected(strongCount: Int, weakCount: Int): Boolean =
    strongCount >= 1 || weakCount >= 3

private fun computeVirtualEnvironmentDiagnostics(
    context: Context,
    packageInventory: InstalledPackageInventory
): VirtualEnvironmentDiagnostics {
    val indicators = mutableListOf<String>()
    var score = 0
    // Strong signals name an emulator outright (goldfish hardware, qemu files, a
    // BlueStacks package). Weak ones are merely consistent with one and are also
    // true of real budget hardware, so they are counted apart.
    var strongCount = 0
    var weakCount = 0

    // --- Build field checks (strong signals, +2 each) ---

    val fingerprint = Build.FINGERPRINT.orEmpty()
    if (VirtualFingerprintTokens.any { token ->
            fingerprint.contains(token, ignoreCase = true)
        }
    ) {
        indicators.add("fingerprint:$fingerprint")
        score += 2
        strongCount++
    }

    val model = Build.MODEL.orEmpty()
    if (VirtualModelTokens.any { token ->
            model.contains(token, ignoreCase = true)
        }
    ) {
        indicators.add("model:$model")
        score += 2
        strongCount++
    }

    val manufacturer = Build.MANUFACTURER.orEmpty()
    if (VirtualManufacturerTokens.any { token ->
            manufacturer.contains(token, ignoreCase = true)
        }
    ) {
        indicators.add("manufacturer:$manufacturer")
        score += 2
        strongCount++
    }

    val brand = Build.BRAND.orEmpty()
    val device = Build.DEVICE.orEmpty()
    if (brand.startsWith("generic", ignoreCase = true) ||
        device.startsWith("generic", ignoreCase = true)
    ) {
        indicators.add("generic_brand_device:${brand}/${device}")
        score += 2
        strongCount++
    }

    val product = Build.PRODUCT.orEmpty()
    if (VirtualProductTokens.any { token ->
            product.contains(token, ignoreCase = true)
        }
    ) {
        indicators.add("product:$product")
        score += 2
        strongCount++
    }

    val hardware = Build.HARDWARE.orEmpty()
    if (VirtualHardwareTokens.any { token ->
            hardware.contains(token, ignoreCase = true)
        }
    ) {
        indicators.add("hardware:$hardware")
        score += 2
        strongCount++
    }

    val board = Build.BOARD.orEmpty()
    if (VirtualBoardTokens.any { token ->
            board.equals(token, ignoreCase = true)
        }
    ) {
        indicators.add("board:$board")
        score += 1
        weakCount++
    }

    // --- ABI check (weak signal, +1) ---

    val abis = Build.SUPPORTED_ABIS?.toList() ?: emptyList()
    if (abis.any { it.contains("x86", ignoreCase = true) }) {
        indicators.add("abis:${abis.joinToString()}")
        score += 1
        weakCount++
    }

    // --- System properties (strong signal, +2 per match) ---

    val qemuProperty = getSystemProperty("ro.kernel.qemu").trim()
    if (qemuProperty == "1") {
        indicators.add("ro.kernel.qemu=1")
        score += 2
        strongCount++
    }

    val suspiciousSystemProperties = mutableListOf<String>()
    // Emulator-exclusive properties: their mere presence is a signal.
    for (key in VirtualPresenceSystemPropertyKeys) {
        val value = getSystemProperty(key).trim()
        if (value.isNotBlank()) {
            suspiciousSystemProperties.add("$key=$value")
        }
    }
    // Properties that also exist on real devices: only an emulator-shaped value counts,
    // so a genuine phone (e.g. Samsung A55) is not flagged just for having them set.
    for ((key, tokens) in VirtualValueSystemProperties) {
        val value = getSystemProperty(key).trim()
        if (value.isNotBlank() && tokens.any { value.contains(it, ignoreCase = true) }) {
            suspiciousSystemProperties.add("$key=$value")
        }
    }
    if (suspiciousSystemProperties.isNotEmpty()) {
        indicators.add("sysprops:${suspiciousSystemProperties.joinToString()}")
        score += 2
        strongCount++
    }

    // --- QEMU / emulator filesystem artifacts (strong signal, +2) ---

    val qemuFiles = VirtualQemuFiles.filter { path ->
        runCatching { java.io.File(path).exists() }.getOrDefault(false)
    }
    if (qemuFiles.isNotEmpty()) {
        indicators.add("qemu_files:${qemuFiles.joinToString()}")
        score += 2
        strongCount++
    }

    // --- Emulator packages (strong signal, +2) ---

    val emulatorPackages = findEmulatorPackagesFromInventory(packageInventory)
    if (emulatorPackages.isNotEmpty()) {
        indicators.add("packages:${emulatorPackages.joinToString()}")
        score += 2
        strongCount++
    }

    // --- Running inside a clone / virtual-space app (strong signal, +2) ---
    // Installed clone apps are only reported below; this is what makes one matter.

    val containerIndicators = resolveVirtualContainerIndicators(
        expectedPackageName = BuildConfig.APPLICATION_ID,
        dataDir = runCatching { context.applicationInfo.dataDir }.getOrNull(),
        sourceDir = runCatching { context.applicationInfo.sourceDir }.getOrNull(),
        processName = readCurrentProcessName()
    )
    if (containerIndicators.isNotEmpty()) {
        indicators.add("virtual_container:${containerIndicators.joinToString()}")
        score += 2
        strongCount++
    }
    val virtualSpaceHostPackages = findVirtualSpaceHostPackagesFromInventory(packageInventory)
    if (virtualSpaceHostPackages.isNotEmpty()) {
        indicators.add("clone_app_installed_info:${virtualSpaceHostPackages.joinToString()}")
    }

    // --- Hardware sensor count (weak signal, +1) ---
    // Real devices have 10+ sensors; emulators typically report 0-4.

    val sensorCount = runCatching {
        val sensorManager = context.getSystemService(android.hardware.SensorManager::class.java)
        sensorManager?.getSensorList(android.hardware.Sensor.TYPE_ALL)?.size ?: 0
    }.getOrDefault(-1)
    if (sensorCount in 0..4) {
        indicators.add("low_sensors:$sensorCount")
        score += 1
        weakCount++
    }

    // --- Battery presence (weak signal, +1) ---
    // Emulators often report no battery or STATUS_UNKNOWN.

    val hasBattery = runCatching {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val batteryPresent = batteryIntent?.getBooleanExtra(
            BatteryManager.EXTRA_PRESENT, true
        ) ?: true
        val batteryStatus = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_STATUS, -1
        ) ?: -1
        batteryPresent && batteryStatus != BatteryManager.BATTERY_STATUS_UNKNOWN
    }.getOrDefault(true)
    if (!hasBattery) {
        indicators.add("no_battery")
        score += 1
        weakCount++
    }

    return VirtualEnvironmentDiagnostics(
        detected = resolveVirtualEnvironmentDetected(strongCount, weakCount),
        indicators = indicators,
        score = score,
        qemuProperty = qemuProperty,
        emulatorPackages = emulatorPackages,
        qemuFiles = qemuFiles,
        suspiciousSystemProperties = suspiciousSystemProperties,
        abis = abis,
        sensorCount = sensorCount,
        hasBattery = hasBattery
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
