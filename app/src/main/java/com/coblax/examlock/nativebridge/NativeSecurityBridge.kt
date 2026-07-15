package com.coblax.examlock.nativebridge

import android.util.Log
import com.coblax.examlock.ClipboardNormalizedItemInput
import com.coblax.examlock.ClipboardSnapshotMode
import com.coblax.examlock.NativeClipboardSnapshotCore

private const val NativeSecurityTag = "NativeSecurityBridge"

private fun logNativeWarning(message: String, throwable: Throwable) {
    runCatching {
        Log.w(NativeSecurityTag, message, throwable)
    }
}

private object NativeLibraryRuntime {
    val isAvailable: Boolean get() = NativeLibraryLoader.isAvailable
}

internal object NativeSecurityBridge {
    fun isNativeAvailableForTests(): Boolean = NativeLibraryRuntime.isAvailable

    private fun <T> invokeOrFallback(
        operation: String,
        fallback: () -> T,
        nativeCall: () -> T
    ): T {
        return when (NativeBridgeTestControl.currentMode) {
            NativeBridgeBackendMode.ForceKotlinFallback -> fallback()
            NativeBridgeBackendMode.ForceNative -> {
                check(NativeLibraryRuntime.isAvailable) {
                    "Native library is unavailable while ForceNative mode is active for $operation."
                }
                nativeCall()
            }
            NativeBridgeBackendMode.Auto -> {
                if (!NativeLibraryRuntime.isAvailable) {
                    fallback()
                } else {
                    runCatching(nativeCall)
                        .onFailure { throwable ->
                            logNativeWarning("Native $operation failed; using Kotlin fallback.", throwable)
                        }
                        .getOrElse { fallback() }
                }
            }
        }
    }

    fun readTracerPid(fallback: () -> Int): Int =
        invokeOrFallback(operation = "readTracerPid", fallback = fallback) {
            nativeReadTracerPid()
        }

    fun scanProcMaps(markers: List<String>, fallback: () -> Set<String>): Set<String> =
        invokeOrFallback(operation = "scanProcMaps", fallback = fallback) {
            val results = nativeScanProcMaps(markers.toTypedArray())
            linkedSetOf<String>().apply {
                markers.forEach { marker ->
                    if (results.contains(marker)) {
                        add(marker)
                    }
                }
            }
        }

    fun readDexHash(apkPath: String, fallback: () -> String): String =
        invokeOrFallback(operation = "readDexHash", fallback = fallback) {
            nativeReadDexHash(apkPath)
        }

    fun getSystemProperty(key: String, fallback: () -> String): String =
        invokeOrFallback(operation = "getSystemProperty", fallback = fallback) {
            nativeGetSystemProperty(key)
        }

    fun decodeBase64Xor(obfuscated: String, fallback: () -> String): String =
        invokeOrFallback(operation = "decodeBase64Xor", fallback = fallback) {
            nativeDecodeBase64Xor(obfuscated)
        }

    fun encryptQrPayload(plaintext: ByteArray, fallback: () -> ByteArray): ByteArray =
        invokeOrFallback(operation = "encryptQrPayload", fallback = fallback) {
            nativeEncryptQrPayload(plaintext)
        }

    fun decryptQrPayload(packed: ByteArray, fallback: () -> ByteArray): ByteArray =
        invokeOrFallback(operation = "decryptQrPayload", fallback = fallback) {
            nativeDecryptQrPayload(packed)
        }

    fun buildClipboardSnapshotCore(
        mode: ClipboardSnapshotMode,
        items: Array<ClipboardNormalizedItemInput>,
        fallback: () -> NativeClipboardSnapshotCore
    ): NativeClipboardSnapshotCore =
        invokeOrFallback(operation = "buildClipboardSnapshotCore", fallback = fallback) {
            nativeBuildClipboardSnapshotCore(mode.ordinal, items)
        }
}
