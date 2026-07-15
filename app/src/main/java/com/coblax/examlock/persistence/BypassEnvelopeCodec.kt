package com.coblax.examlock.persistence

import android.util.Base64
import java.nio.charset.StandardCharsets

internal object BypassEnvelopeCodec {
    private const val UrlSafeBase64Flags = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

    fun encodeEnvelope(envelope: BypassEnvelope): String {
        val encodedPayload = encodeBase64Url(envelope.payload)
        return "$encodedPayload.${envelope.mac}"
    }

    fun decodeEnvelope(serialized: String): BypassEnvelope? {
        if (serialized.isBlank()) return null
        val separatorIndex = serialized.indexOf('.')
        if (separatorIndex <= 0 || separatorIndex == serialized.lastIndex) {
            return null
        }
        val encodedPayload = serialized.substring(0, separatorIndex)
        val mac = serialized.substring(separatorIndex + 1)
        if (mac.isBlank()) return null
        val payload = decodeBase64Url(encodedPayload) ?: return null
        return BypassEnvelope(payload = payload, mac = mac)
    }

    fun encodePayload(payload: BypassEnvelopePayload): String {
        val gateStates = payload.gateStates.toSortedMap().entries.joinToString(",") { (gateId, enabled) ->
            "$gateId:${if (enabled) 1 else 0}"
        }
        return listOf(
            "v=${payload.schemaVersion}",
            "ctr=${payload.monotonicCounter}",
            "crt=${payload.createdAtEpochMillis}",
            "upd=${payload.updatedAtEpochMillis}",
            "dev=${encodeField(payload.deviceBinding)}",
            "g=$gateStates"
        ).joinToString("|")
    }

    fun decodePayload(serialized: String): BypassEnvelopePayload? {
        if (serialized.isBlank()) return null
        val fields = serialized.split('|')
            .mapNotNull { entry ->
                val idx = entry.indexOf('=')
                if (idx <= 0 || idx == entry.lastIndex) {
                    null
                } else {
                    entry.substring(0, idx) to entry.substring(idx + 1)
                }
            }
            .toMap()
        val schemaVersion = fields["v"]?.toIntOrNull() ?: return null
        val counter = fields["ctr"]?.toLongOrNull() ?: return null
        val createdAt = fields["crt"]?.toLongOrNull() ?: return null
        val updatedAt = fields["upd"]?.toLongOrNull() ?: return null
        val deviceBinding = decodeField(fields["dev"].orEmpty()) ?: return null
        val gateStates = decodeGateStates(fields["g"].orEmpty()) ?: return null
        return BypassEnvelopePayload(
            schemaVersion = schemaVersion,
            monotonicCounter = counter,
            createdAtEpochMillis = createdAt,
            updatedAtEpochMillis = updatedAt,
            deviceBinding = deviceBinding,
            gateStates = gateStates
        )
    }

    private fun encodeField(value: String): String {
        return encodeBase64Url(value)
    }

    private fun decodeField(value: String): String? {
        if (value.isBlank()) return ""
        return decodeBase64Url(value)
    }

    private fun encodeBase64Url(value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        encodeBase64UrlWithJava(bytes)?.let { return it }
        return Base64.encodeToString(
            bytes,
            UrlSafeBase64Flags
        )
    }

    private fun decodeBase64Url(value: String): String? {
        val bytes = decodeBase64UrlWithJava(value)
            ?: runCatching { Base64.decode(value, UrlSafeBase64Flags) }.getOrNull()
            ?: return null
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun encodeBase64UrlWithJava(bytes: ByteArray): String? {
        return runCatching {
            val base64Class = Class.forName("java.util.Base64")
            val encoder = base64Class.getMethod("getUrlEncoder").invoke(null)
            val noPaddingEncoder = encoder.javaClass.getMethod("withoutPadding").invoke(encoder)
            noPaddingEncoder.javaClass
                .getMethod("encodeToString", ByteArray::class.java)
                .invoke(noPaddingEncoder, bytes) as String
        }.getOrNull()
    }

    private fun decodeBase64UrlWithJava(value: String): ByteArray? {
        return runCatching {
            val base64Class = Class.forName("java.util.Base64")
            val decoder = base64Class.getMethod("getUrlDecoder").invoke(null)
            decoder.javaClass
                .getMethod("decode", String::class.java)
                .invoke(decoder, value) as ByteArray
        }.getOrNull()
    }

    private fun decodeGateStates(serialized: String): Map<Int, Boolean>? {
        if (serialized.isBlank()) return emptyMap()
        val states = linkedMapOf<Int, Boolean>()
        serialized.split(',').forEach { entry ->
            val idx = entry.indexOf(':')
            if (idx <= 0 || idx == entry.lastIndex) {
                return null
            }
            val gateId = entry.substring(0, idx).toIntOrNull() ?: return null
            val enabled = when (entry.substring(idx + 1)) {
                "1" -> true
                "0" -> false
                else -> return null
            }
            states[gateId] = enabled
        }
        return states
    }
}
