package com.example.coblaxexamlock.nativebridge

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.coblaxexamlock.CertificateReader
import com.example.coblaxexamlock.FingerprintGenerator
import com.example.coblaxexamlock.PackageResolver

private const val AdminSecretTag = "AdminSecretBridge"

private object AdminSecretNativeRuntime {
    val loadFailure: Throwable?
    val isAvailable: Boolean

    init {
        var failure: Throwable? = null
        val available = try {
            System.loadLibrary("examlock_native")
            true
        } catch (throwable: Throwable) {
            failure = throwable
            false
        }
        loadFailure = failure
        isAvailable = available
        if (failure != null) {
            Log.w(AdminSecretTag, "Native admin secret library unavailable.", failure)
        }
    }
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
