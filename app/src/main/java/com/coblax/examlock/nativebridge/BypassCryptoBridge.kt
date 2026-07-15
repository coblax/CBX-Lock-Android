package com.coblax.examlock.nativebridge

import android.content.Context
import android.util.Log

private const val BypassCryptoTag = "BypassCryptoBridge"

internal object BypassCryptoBridge {
    fun buildDeviceBinding(context: Context): String {
        if (!NativeSecurityBridge.isNativeAvailableForTests()) {
            Log.w(BypassCryptoTag, "Native library unavailable; bypass crypto binding cannot be derived.")
            return ""
        }
        val binding = AdminSecretBinding.from(context)
        return runCatching {
            nativeBuildBypassDeviceBinding(
                binding.packageName,
                binding.dataDir,
                binding.androidId,
                binding.signingFingerprint,
                com.coblax.examlock.BuildConfig.VERSION_CODE
            )
        }.onFailure { throwable ->
            Log.w(BypassCryptoTag, "Failed to derive bypass device binding.", throwable)
        }.getOrDefault("")
    }

    fun computeEnvelopeMac(payload: String, deviceBinding: String): String {
        if (payload.isBlank() || deviceBinding.isBlank()) {
            return ""
        }
        if (!NativeSecurityBridge.isNativeAvailableForTests()) {
            Log.w(BypassCryptoTag, "Native library unavailable; bypass MAC cannot be computed.")
            return ""
        }
        return runCatching {
            nativeComputeBypassMac(payload, deviceBinding)
        }.onFailure { throwable ->
            Log.w(BypassCryptoTag, "Failed to compute bypass MAC.", throwable)
        }.getOrDefault("")
    }
}
