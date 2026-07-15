package com.coblax.examlock.nativebridge

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.coblax.examlock.CertificateReader
import com.coblax.examlock.FingerprintGenerator
import com.coblax.examlock.PackageResolver

private const val AdminSecretTag = "AdminSecretBridge"

private object AdminSecretNativeRuntime {
    val isAvailable: Boolean get() = NativeLibraryLoader.isAvailable
}

internal object AdminSecretBridge {
    fun isNativeAvailableForTests(): Boolean = AdminSecretNativeRuntime.isAvailable

    fun verify(context: Context, candidate: String): Boolean {
        if (!AdminSecretNativeRuntime.isAvailable) {
            return false
        }
        val input = candidate.trim()
        if (input.isBlank()) {
            return false
        }
        val binding = AdminSecretBinding.from(context)
        return runCatching {
            nativeVerifyAdminSecret(
                binding.packageName,
                binding.dataDir,
                binding.androidId,
                binding.signingFingerprint,
                input
            )
        }.onFailure { throwable ->
            Log.w(AdminSecretTag, "Native admin verify failed.", throwable)
        }.getOrDefault(false)
    }
}

internal data class AdminSecretBinding(
    val packageName: String,
    val dataDir: String,
    val androidId: String,
    val signingFingerprint: String
) {
    companion object {
        @SuppressLint("HardwareIds")
        fun from(context: Context): AdminSecretBinding {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ).orEmpty()
            val packageInfo = PackageResolver.resolve(context)
            val certificateBytes = packageInfo?.let { CertificateReader.readPrimaryCertificate(it) }
            val fingerprint = certificateBytes?.let { FingerprintGenerator.sha256Fingerprint(it) }
                .orEmpty()
                .ifBlank { "-" }
            return AdminSecretBinding(
                packageName = context.packageName,
                dataDir = context.applicationInfo?.dataDir.orEmpty(),
                androidId = androidId,
                signingFingerprint = fingerprint
            )
        }
    }
}
