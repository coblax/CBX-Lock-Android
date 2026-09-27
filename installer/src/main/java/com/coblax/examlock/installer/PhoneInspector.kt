package com.coblax.examlock.installer

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.StatFs
import android.os.UserManager
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.Properties
import java.util.TimeZone

internal const val BundledApkAsset = "cbx.apk"
private const val BundledPropertiesAsset = "cbx.properties"

/** Reads the phone, the bundled CBX file and any CBX already installed. */
internal class PhoneInspector(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    fun bundle(): BundledCbx {
        val properties = Properties()
        context.assets.open(BundledPropertiesAsset).use(properties::load)
        return BundledCbx.parse(properties.stringPropertyNames().associateWith { properties.getProperty(it) })
    }

    fun device(): DeviceFacts = DeviceFacts(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        brand = Build.BRAND.orEmpty(),
        model = Build.MODEL.orEmpty(),
        device = Build.DEVICE.orEmpty(),
        sdkInt = Build.VERSION.SDK_INT,
        release = Build.VERSION.RELEASE.orEmpty(),
        supportedAbis = Build.SUPPORTED_ABIS.toList(),
        freeBytes = runCatching { StatFs(context.filesDir.path).availableBytes }.getOrDefault(-1L),
        romDisplay = Build.DISPLAY.orEmpty(),
        securityPatch = if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH.orEmpty() else "",
        fingerprint = Build.FINGERPRINT.orEmpty(),
        hardware = Build.HARDWARE.orEmpty(),
        socModel = if (Build.VERSION.SDK_INT >= 31) "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}".trim() else "",
        totalRamBytes = runCatching {
            val memory = ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
            memory.totalMem
        }.getOrDefault(-1L),
        locale = Locale.getDefault().toLanguageTag(),
        timezone = TimeZone.getDefault().id,
        installerSource = installSource(context.packageName).orEmpty(),
        canInstallPackages = if (Build.VERSION.SDK_INT >= 26) {
            runCatching { packageManager.canRequestPackageInstalls() }.getOrNull()
        } else {
            null
        },
        installRestrictions = installRestrictions()
    )

    fun installedCbx(): InstalledCbx? {
        val info = try {
            packageInfo(CbxPackageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        return InstalledCbx(
            versionCode = info.longVersionCodeCompat(),
            versionName = info.versionName,
            certSha256 = info.certSha256(),
            enabled = info.applicationInfo?.enabled ?: true,
            installerSource = installSource(CbxPackageName)
        )
    }

    /**
     * Copies the bundled APK out (the phone's parser needs a file), hashing it on the way, then
     * asks this phone's PackageManager to read it: exactly what the system installer does first.
     */
    fun inspectBundle(): Pair<File?, BundleInspection> {
        val copy = File(context.cacheDir, "cbx-install.apk")
        val sha256 = try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.assets.open(BundledApkAsset).use { input ->
                copy.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = input.read(buffer)
                    while (read > 0) {
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                        read = input.read(buffer)
                    }
                }
            }
            digest.digest().toHex()
        } catch (error: Exception) {
            copy.delete()
            return null to BundleInspection(
                actualSha256 = null,
                parsedByPhone = false,
                certSha256 = null,
                error = "copy: ${error.javaClass.simpleName}: ${error.message}"
            )
        }
        val archive = runCatching { archiveInfo(copy.path) }
        val info = archive.getOrNull()
        return copy to BundleInspection(
            actualSha256 = sha256,
            parsedByPhone = info != null,
            certSha256 = info?.certSha256(),
            error = archive.exceptionOrNull()?.let { "parse: ${it.javaClass.simpleName}: ${it.message}" }
        )
    }

    /** Which app installed [packageName], and which app the student started it from. */
    @Suppress("DEPRECATION")
    private fun installSource(packageName: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= 30) {
            val info = packageManager.getInstallSourceInfo(packageName)
            listOfNotNull(
                info.initiatingPackageName?.let { "from $it" },
                info.installingPackageName?.let { "via $it" }
            ).joinToString(" ").ifBlank { null }
        } else {
            packageManager.getInstallerPackageName(packageName)
        }
    }.getOrNull()

    /** Device policies that stop this app from installing anything. */
    private fun installRestrictions(): List<String> {
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager ?: return emptyList()
        val keys = buildList {
            add(UserManager.DISALLOW_INSTALL_APPS)
            add(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
            if (Build.VERSION.SDK_INT >= 29) add(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)
        }
        return keys.filter { runCatching { userManager.hasUserRestriction(it) }.getOrDefault(false) }
    }

    @Suppress("DEPRECATION")
    private fun packageInfo(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= 28) {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        }

    @Suppress("DEPRECATION")
    private fun archiveInfo(path: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= 28) {
            packageManager.getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            packageManager.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES)
        }
}

@Suppress("DEPRECATION")
private fun PackageInfo.longVersionCodeCompat(): Long =
    if (Build.VERSION.SDK_INT >= 28) longVersionCode else versionCode.toLong()

/** SHA-256 of the current signing certificate, or null where this Android does not expose it. */
@Suppress("DEPRECATION")
private fun PackageInfo.certSha256(): String? {
    val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= 28) {
        signingInfo?.let { info ->
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        }
    } else {
        this.signatures
    }
    val current = signatures?.lastOrNull() ?: return null
    return MessageDigest.getInstance("SHA-256").digest(current.toByteArray()).toHex()
}

private fun ByteArray.toHex(): String = joinToString("") { String.format(Locale.US, "%02X", it) }
