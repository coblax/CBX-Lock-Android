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

    /**
     * [isUsable] lets a caller reject a native result that arrived without an exception
     * but is not actually an answer. Some native paths report failure by returning an
     * empty value, and treating that as the verdict skipped the Kotlin fallback
     * entirely. ForceNative deliberately keeps the raw result so parity tests still
     * compare the two implementations rather than Kotlin against itself.
     */
    private fun <T> invokeOrFallback(
        operation: String,
        fallback: () -> T,
        isUsable: (T) -> Boolean = { true },
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
                    val nativeResult = runCatching(nativeCall)
                        .onFailure { throwable ->
                            logNativeWarning("Native $operation failed; using Kotlin fallback.", throwable)
                        }
                        .getOrNull()
                    if (nativeResult != null && isUsable(nativeResult)) {
                        nativeResult
                    } else {
                        fallback()
                    }
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
        invokeOrFallback(
            operation = "readDexHash",
            fallback = fallback,
            // The native reader buffers the whole APK before hashing it and reports
            // any failure as an empty string, so on a low-RAM device it could hand
            // back "no hash" without ever throwing. Kotlin's ZipFile streams instead,
            // so falling through to it is both cheaper and more likely to succeed.
            isUsable = { hash -> hash.isNotBlank() }
        ) {
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
