package com.coblax.examlock

import android.util.Base64
import android.util.Log
import com.coblax.examlock.nativebridge.NativeBridgeBackendMode
import com.coblax.examlock.nativebridge.NativeLibraryLoader
import com.coblax.examlock.nativebridge.NativeBridgeTestControl
import com.coblax.examlock.nativebridge.NativeSecurityBridge

internal object RuntimeStringDecoder {
    private const val Tag = "RuntimeStringDecoder"
    private const val FallbackKeyFragmentA = 0x12
    private const val FallbackKeyFragmentB = 0x34
    private const val FallbackKeyFragmentC = 0x41
    private const val FallbackKeyFragmentD = 0x14

    private val fallbackXorKey: Int by lazy {
        FallbackKeyFragmentA xor FallbackKeyFragmentB xor FallbackKeyFragmentC xor FallbackKeyFragmentD
    }

    fun decodeBase64Xor(obfuscated: String): String {
        if (obfuscated.isBlank()) {
            return ""
        }
        return NativeSecurityBridge.decodeBase64Xor(obfuscated) {
            if (!NativeLibraryLoader.isAvailable) {
                Log.w(Tag, "Using Kotlin fallback for string decoding. Native library unavailable.")
            }
            decodeBase64XorReference(obfuscated)
        }
    }

    internal fun decodeBase64XorReference(obfuscated: String): String {
        if (obfuscated.isBlank()) {
            return ""
        }
        return try {
            val bytes = decodeBase64Bytes(obfuscated)
            for (index in bytes.indices) {
                bytes[index] = (bytes[index].toInt() xor fallbackXorKey).toByte()
            }
            String(bytes, Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            ""
        }
    }

    private fun decodeBase64Bytes(obfuscated: String): ByteArray {
        return runCatching {
            val base64Class = Class.forName("java.util.Base64")
            val decoder = base64Class.getMethod("getDecoder").invoke(null)
            decoder.javaClass
                .getMethod("decode", String::class.java)
                .invoke(decoder, obfuscated) as ByteArray
        }.getOrElse {
            Base64.decode(obfuscated, Base64.DEFAULT)
        }
    }
}

internal object RuntimeStringDecoderParityAccess {
    fun decodeWithBackend(obfuscated: String, backendMode: NativeBridgeBackendMode): String =
        NativeBridgeTestControl.withBackendMode(backendMode) {
            RuntimeStringDecoder.decodeBase64Xor(obfuscated)
        }

    fun decodeReference(obfuscated: String): String =
        RuntimeStringDecoder.decodeBase64XorReference(obfuscated)
}
