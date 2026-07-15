package com.coblax.examlock

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.coblax.examlock.nativebridge.NativeBridgeBackendMode
import com.coblax.examlock.nativebridge.NativeBridgeTestControl
import com.coblax.examlock.nativebridge.NativeSecurityBridge
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile


data class IntegrityCheckResult(
    val ok: Boolean,
    val issues: List<String>,
    val details: String,
    val currentFingerprint: String,
    val expectedDexHash: String,
    val actualDexHash: String
)

object IntegrityGuard {
    private val extraHookClasses = listOf(
        "de.robv.android.xposed.XposedBridge",
        "de.robv.android.xposed.XC_MethodHook",
        "com.saurik.substrate.SubstrateHooker",
        "org.lsposed.lspd.core.Main",
        "org.lsposed.lspd.nativebridge.LspNative",
        "org.lsposed.lspd.service.LSPSystemServer",
        "io.github.lsposed.lspd.nativebridge.LspNativeBridge",
        "io.github.libxposed.api.XposedInterface",
        "io.github.libxposed.api.XposedModule"
    )

    fun expectedDexHash(context: Context): String {
        val assetHash = runCatching {
            context.assets.open("dex.sha256").bufferedReader().use { it.readText() }
        }.getOrDefault("")
        return assetHash.replace(" ", "").trim().uppercase(Locale.US)
    }

    fun check(context: Context, baselineFingerprint: String?): IntegrityCheckResult {
        val issues = mutableListOf<String>()
        val expectedHash = expectedDexHash(context)
        val actualHash = readDexHash(context)
            .replace(" ", "")
            .uppercase(Locale.US)

        if (expectedHash.isNotBlank() && actualHash.isNotBlank() && expectedHash != actualHash) {
            issues.add("dex_hash_mismatch")
        }

        val currentFingerprint = readSigningFingerprint(context)
        if (!baselineFingerprint.isNullOrBlank() &&
            currentFingerprint.isNotBlank() &&
            !currentFingerprint.equals(baselineFingerprint, ignoreCase = true)
        ) {
            issues.add("signature_changed")
        }

        val debuggable = getSystemProperty("ro.debuggable")
        val secure = getSystemProperty("ro.secure")
        val adbSecure = getSystemProperty("ro.adb.secure")
        if (debuggable == "1") {
            issues.add("sysprop_debuggable")
        }
        if (secure == "0") {
            issues.add("sysprop_secure")
        }
        if (adbSecure == "0") {
            issues.add("sysprop_adb_secure")
        }
        if (Build.TAGS?.contains("test-keys") == true) {
            issues.add("test_keys")
        }

        if (hasHookClasses(context)) {
            issues.add("hook_class")
        }

        val detailSummary = buildString {
            if (issues.isEmpty()) {
                append("ok")
                return@buildString
            }
            append("issues=").append(issues.joinToString())
            if (issues.contains("dex_hash_mismatch")) {
                append(" | dex=").append(shorten(actualHash))
                append("/exp=").append(shorten(expectedHash))
            }
            if (issues.contains("signature_changed")) {
                append(" | sig=").append(shorten(currentFingerprint))
                append("/base=").append(shorten(baselineFingerprint.orEmpty()))
            }
            if (issues.any { it.startsWith("sysprop_") } || issues.contains("test_keys")) {
                append(" | props=")
                append("dbg=").append(debuggable.ifBlank { "-" })
                append(",sec=").append(secure.ifBlank { "-" })
                append(",adb=").append(adbSecure.ifBlank { "-" })
                append(",tags=").append(Build.TAGS?.ifBlank { "-" } ?: "-")
            }
        }

        return IntegrityCheckResult(
            ok = issues.isEmpty(),
            issues = issues,
            details = detailSummary,
            currentFingerprint = currentFingerprint,
            expectedDexHash = expectedHash,
            actualDexHash = actualHash
        )
    }

    internal object ParityAccess {
        fun readDexHashWithBackend(
            context: Context,
            backendMode: NativeBridgeBackendMode
        ): String = NativeBridgeTestControl.withBackendMode(backendMode) {
            readDexHash(context)
        }

        fun readDexHashReference(context: Context): String {
            val apkPath = context.packageCodePath
            if (apkPath.isNullOrBlank()) return ""
            return readDexHashKotlin(apkPath)
        }

        fun getSystemPropertyWithBackend(
            key: String,
            backendMode: NativeBridgeBackendMode
        ): String = NativeBridgeTestControl.withBackendMode(backendMode) {
            getSystemProperty(key)
        }

        fun getSystemPropertyReference(key: String): String = getSystemPropertyKotlin(key)

        fun checkWithBackend(
            context: Context,
            baselineFingerprint: String?,
            backendMode: NativeBridgeBackendMode
        ): IntegrityCheckResult = NativeBridgeTestControl.withBackendMode(backendMode) {
            check(context, baselineFingerprint)
        }
    }

    private fun readDexHash(context: Context): String {
        val apkPath = context.packageCodePath
        if (apkPath.isNullOrBlank()) return ""
        return NativeSecurityBridge.readDexHash(apkPath) {
            readDexHashKotlin(apkPath)
        }
    }

    private fun readDexHashKotlin(apkPath: String): String {
        return runCatching {
            ZipFile(File(apkPath)).use { zip ->
                val dexEntries = zip.entries().asSequence()
                    .filter { it.name.endsWith(".dex") }
                    .sortedBy { it.name }
                    .toList()
                if (dexEntries.isEmpty()) return@use ""

                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8 * 1024)
                dexEntries.forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        var read = input.read(buffer)
                        while (read > 0) {
                            digest.update(buffer, 0, read)
                            read = input.read(buffer)
                        }
                    }
                }
                digest.digest().joinToString("") { byte -> "%02X".format(byte) }
            }
        }.getOrDefault("")
    }

    private fun readSigningFingerprint(context: Context): String {
        val packageInfo = PackageResolver.resolve(context)
        val certificateBytes = packageInfo?.let { CertificateReader.readPrimaryCertificate(it) }
        return certificateBytes?.let { FingerprintGenerator.sha256Fingerprint(it) }.orEmpty()
            .ifBlank { "-" }
    }

    private fun hasHookClasses(context: Context): Boolean {
        val loader = context.classLoader
        return extraHookClasses.any { name ->
            runCatching { Class.forName(name, false, loader) }.isSuccess
        }
    }

    private fun shorten(value: String): String {
        val clean = value.trim()
        if (clean.length <= 10) return clean
        return clean.take(6) + ".." + clean.takeLast(4)
    }

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String): String {
        return NativeSecurityBridge.getSystemProperty(key) {
            getSystemPropertyKotlin(key)
        }
    }

    @SuppressLint("PrivateApi")
    private fun getSystemPropertyKotlin(key: String): String {
        return runCatching {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java, String::class.java)
            (getMethod.invoke(null, key, "") as? String).orEmpty()
        }.getOrDefault("")
    }
}
