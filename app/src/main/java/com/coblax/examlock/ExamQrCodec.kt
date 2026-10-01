package com.coblax.examlock

import android.util.Base64
import android.util.Log
import com.coblax.examlock.nativebridge.NativeLibraryLoader
import com.coblax.examlock.nativebridge.NativeBridgeBackendMode
import com.coblax.examlock.nativebridge.NativeBridgeTestControl
import com.coblax.examlock.nativebridge.NativeSecurityBridge
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


data class ExamQrPayload(
    val examUrl: String,
    val examName: String,
    val startDateTime: String,
    val endDateTime: String,
    val saveToDirectLink: Boolean = false,
    val issuedAt: Long = System.currentTimeMillis(),
    val locationPolicy: ExamQrLocationPolicy? = null,
    val locationPolicySource: LocationPolicySource = LocationPolicySource.DisabledNoPolicy,
    val timezoneId: String = TimeZone.getDefault().id,
    /** Checks this exam runs without, on top of the device's own Secret Admin bypasses. */
    val securityBypasses: Set<ExamQrSecurityBypass> = emptySet(),
    /**
     * Oldest CBX Lock build (versionCode) allowed to sit this exam, or 0 for any. Lets an
     * admin make sure students run a build that has the fixes the exam relies on.
     */
    val minAppVersionCode: Int = 0,
    /** Display name of [minAppVersionCode], shown to the student who has to update. */
    val minAppVersionName: String = "",
    /** Where a student who has to update downloads CBX Lock; blank uses the device setting. */
    val appUpdateUrl: String = ""
)

/** True when this device's CBX Lock is older than the exam QR allows. */
internal fun ExamQrPayload.requiresAppUpdate(currentVersionCode: Int = BuildConfig.VERSION_CODE): Boolean =
    minAppVersionCode > 0 && currentVersionCode < minAppVersionCode

enum class GeofenceShapeType {
    Disabled,
    Circle,
    Polygon
}

data class GeofenceVertex(
    val latitude: String,
    val longitude: String
)

data class ExamQrLocationPolicy(
    val shapeType: GeofenceShapeType = GeofenceShapeType.Disabled,
    val centerLat: String = "",
    val centerLng: String = "",
    val radiusMeters: String = "",
    val vertices: List<GeofenceVertex> = emptyList(),
    val circleCenters: List<GeofenceVertex> = emptyList()
) {
    val geofenceEnabled: Boolean
        get() = shapeType != GeofenceShapeType.Disabled

    val effectiveCircleCenters: List<GeofenceVertex>
        get() = when {
            shapeType != GeofenceShapeType.Circle -> emptyList()
            circleCenters.isNotEmpty() -> circleCenters
            centerLat.isBlank() || centerLng.isBlank() -> emptyList()
            else -> listOf(
                GeofenceVertex(
                    latitude = centerLat.trim(),
                    longitude = centerLng.trim()
                )
            )
        }
}

object ExamQrCodec {
    private const val PAYLOAD_PREFIX = "CBXEL2:"
    private const val LEGACY_PAYLOAD_PREFIX = "CBXEL1:"
    private const val IV_LENGTH = 12
    private const val AUTH_TAG_LENGTH_BITS = 128
    private const val PAYLOAD_VERSION = "7"
    // v8 adds the bypass bitmask. A QR without bypasses stays v7 so older apps still read it.
    private const val BYPASS_PAYLOAD_VERSION = "8"
    // v9 adds the minimum app version. Only a QR that asks for one uses it; builds from
    // before v9 refuse it outright, which is the point: they are older than the minimum.
    private const val MIN_VERSION_PAYLOAD_VERSION = "9"
    private const val KotlinQrSeedXorKeyPartOne = 0x23
    private const val KotlinQrSeedXorKeyPartTwo = 0x47
    private const val KotlinQrSeedXorKeyPartThree = 0x6D
    private val kotlinQrSeedFragmentOne = intArrayOf(
        96, 108, 97, 111, 98, 123, 124, 102, 123, 98, 110, 124, 111, 108
    )
    private val kotlinQrSeedFragmentTwo = intArrayOf(
        4, 12, 24, 8, 1, 1, 11, 14, 9, 2, 24, 22, 21, 24
    )
    private val kotlinQrSeedFragmentThree = intArrayOf(
        62, 40, 46, 63, 40, 57, 50, 59, 95, 50, 95, 93, 95, 91
    )

    private val secureRandom = SecureRandom()
    private val kotlinFallbackSecretSeedBytes by lazy {
        decodeKotlinQrSeedFragment(kotlinQrSeedFragmentOne, KotlinQrSeedXorKeyPartOne) +
            decodeKotlinQrSeedFragment(kotlinQrSeedFragmentTwo, KotlinQrSeedXorKeyPartTwo) +
            decodeKotlinQrSeedFragment(kotlinQrSeedFragmentThree, KotlinQrSeedXorKeyPartThree)
    }
    private val kotlinFallbackSecretKey by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(kotlinFallbackSecretSeedBytes)
        SecretKeySpec(digest, "AES")
    }

    fun encrypt(payload: ExamQrPayload): String {
        val plaintext = payload.serialize().toByteArray(StandardCharsets.UTF_8)
        val packed = NativeSecurityBridge.encryptQrPayload(plaintext) {
            if (!NativeLibraryLoader.isAvailable) {
                runCatching {
                    Log.w("ExamQrCodec", "Using Kotlin fallback for QR encryption. Native library unavailable.")
                }
            }
            encryptPayloadKotlin(plaintext)
        }
        return PAYLOAD_PREFIX + encodeBase64Url(packed)
    }

    fun decrypt(rawValue: String): ExamQrPayload {
        require(!rawValue.startsWith(LEGACY_PAYLOAD_PREFIX)) {
            "Format QR lama tidak lagi didukung. Buat ulang QR dari aplikasi terbaru."
        }
        require(rawValue.startsWith(PAYLOAD_PREFIX)) {
            "QR ini bukan format COBLAX EXAM LOCK."
        }

        val packed = decodeBase64Url(rawValue.removePrefix(PAYLOAD_PREFIX))
        require(packed.size > IV_LENGTH) {
            "Payload QR tidak lengkap."
        }

        val decrypted = NativeSecurityBridge.decryptQrPayload(packed) {
            if (!NativeLibraryLoader.isAvailable) {
                runCatching {
                    Log.w("ExamQrCodec", "Using Kotlin fallback for QR decryption. Native library unavailable.")
                }
            }
            decryptPayloadKotlin(packed)
        }

        return deserialize(String(decrypted, StandardCharsets.UTF_8))
    }

    internal object ParityAccess {
        fun encryptWithBackend(
            payload: ExamQrPayload,
            backendMode: NativeBridgeBackendMode
        ): String = NativeBridgeTestControl.withBackendMode(backendMode) {
            encrypt(payload)
        }

        fun decryptWithBackend(
            rawValue: String,
            backendMode: NativeBridgeBackendMode
        ): ExamQrPayload = NativeBridgeTestControl.withBackendMode(backendMode) {
            decrypt(rawValue)
        }

        fun encryptReference(payload: ExamQrPayload): String =
            PAYLOAD_PREFIX + encodeBase64Url(encryptPayloadKotlin(payload.serialize().toByteArray(StandardCharsets.UTF_8)))

        /** The decrypted `|`-separated fields, to check the wire format itself. */
        fun plaintextOf(rawValue: String): String =
            String(
                decryptPayloadKotlin(decodeBase64Url(rawValue.removePrefix(PAYLOAD_PREFIX))),
                StandardCharsets.UTF_8
            )

        /** A QR holding [plaintext] as is, to feed the decoder fields the encoder never writes. */
        fun encryptPlaintext(plaintext: String): String =
            PAYLOAD_PREFIX + encodeBase64Url(encryptPayloadKotlin(plaintext.toByteArray(StandardCharsets.UTF_8)))

        fun decryptReference(rawValue: String): ExamQrPayload {
            require(!rawValue.startsWith(LEGACY_PAYLOAD_PREFIX)) {
                "Format QR lama tidak lagi didukung. Buat ulang QR dari aplikasi terbaru."
            }
            require(rawValue.startsWith(PAYLOAD_PREFIX)) {
                "QR ini bukan format COBLAX EXAM LOCK."
            }
            val packed = decodeBase64Url(rawValue.removePrefix(PAYLOAD_PREFIX))
            require(packed.size > IV_LENGTH) {
                "Payload QR tidak lengkap."
            }
            return deserialize(String(decryptPayloadKotlin(packed), StandardCharsets.UTF_8))
        }
    }

    private fun ExamQrPayload.serialize(): String {
        val qrLocationPolicy = locationPolicy ?: ExamQrLocationPolicy()
        val payloadVersion = when {
            minAppVersionCode > 0 -> MIN_VERSION_PAYLOAD_VERSION
            securityBypasses.isNotEmpty() -> BYPASS_PAYLOAD_VERSION
            else -> PAYLOAD_VERSION
        }
        val fields = listOf(
            payloadVersion,
            encodeBase64Url(examUrl.toByteArray(StandardCharsets.UTF_8)),
            encodeBase64Url(examName.toByteArray(StandardCharsets.UTF_8)),
            encodeBase64Url(startDateTime.toByteArray(StandardCharsets.UTF_8)),
            encodeBase64Url(endDateTime.toByteArray(StandardCharsets.UTF_8)),
            if (saveToDirectLink) "1" else "0",
            issuedAt.toString(),
            qrLocationPolicy.shapeType.name.lowercase(Locale.US),
            encodeBase64Url(qrLocationPolicy.centerLat.toByteArray(StandardCharsets.UTF_8)),
            encodeBase64Url(qrLocationPolicy.centerLng.toByteArray(StandardCharsets.UTF_8)),
            encodeBase64Url(qrLocationPolicy.radiusMeters.toByteArray(StandardCharsets.UTF_8)),
            encodeBase64Url(serializeVertices(qrLocationPolicy.vertices).toByteArray(StandardCharsets.UTF_8)),
            encodeBase64Url(serializeVertices(qrLocationPolicy.effectiveCircleCenters).toByteArray(StandardCharsets.UTF_8)),
            encodeBase64Url(timezoneId.toByteArray(StandardCharsets.UTF_8))
        )
        val bypassFields = if (payloadVersion == PAYLOAD_VERSION) {
            emptyList()
        } else {
            listOf(ExamQrSecurityBypass.toMask(securityBypasses).toString(16))
        }
        val minVersionFields = if (payloadVersion == MIN_VERSION_PAYLOAD_VERSION) {
            listOf(
                minAppVersionCode.toString(),
                encodeBase64Url(minAppVersionName.toByteArray(StandardCharsets.UTF_8)),
                encodeBase64Url(appUpdateUrl.toByteArray(StandardCharsets.UTF_8))
            )
        } else {
            emptyList()
        }
        return (fields + bypassFields + minVersionFields).joinToString("|")
    }

    private fun deserialize(serialized: String): ExamQrPayload {
        val parts = serialized.split("|")
        val version = parts.getOrNull(0).orEmpty()
        require(
            version == "6" || version == "7" || version == BYPASS_PAYLOAD_VERSION ||
                version == MIN_VERSION_PAYLOAD_VERSION
        ) {
            "Versi payload QR tidak didukung."
        }
        require(
            (version == "6" && parts.size == 13) ||
                (version == "7" && parts.size == 14) ||
                (version == BYPASS_PAYLOAD_VERSION && parts.size == 15) ||
                (version == MIN_VERSION_PAYLOAD_VERSION && parts.size == 18)
        ) {
            "Format payload QR tidak dikenal."
        }

        val saveFlag =
            parts[5].equals("1", ignoreCase = true) || parts[5].equals("true", ignoreCase = true)
        val shapeType = parseShapeType(parts[7])
        val centerLat = decodeField(parts[8])
        val centerLng = decodeField(parts[9])
        val decodedCircleCenters = deserializeVertices(decodeField(parts[12]))
        val locationPolicy = ExamQrLocationPolicy(
            shapeType = shapeType,
            centerLat = centerLat,
            centerLng = centerLng,
            radiusMeters = decodeField(parts[10]),
            vertices = deserializeVertices(decodeField(parts[11])),
            circleCenters = if (shapeType == GeofenceShapeType.Circle) {
                when {
                    decodedCircleCenters.isNotEmpty() -> decodedCircleCenters
                    centerLat.isNotBlank() && centerLng.isNotBlank() ->
                        listOf(GeofenceVertex(centerLat, centerLng))
                    else -> emptyList()
                }
            } else {
                emptyList()
            }
        )
        // v7 and later include timezone; v6 falls back to device timezone
        val timezoneId = if (version != "6") {
            decodeField(parts[13]).ifBlank { TimeZone.getDefault().id }
        } else {
            TimeZone.getDefault().id
        }
        val securityBypasses = if (version == BYPASS_PAYLOAD_VERSION || version == MIN_VERSION_PAYLOAD_VERSION) {
            val mask = parts[14].toLongOrNull(16)
            require(mask != null && mask >= 0L) {
                "Format payload QR tidak dikenal."
            }
            ExamQrSecurityBypass.fromMask(mask)
        } else {
            emptySet()
        }
        val minAppVersionCode = if (version == MIN_VERSION_PAYLOAD_VERSION) {
            val code = parts[15].toIntOrNull()
            require(code != null && code >= 0) {
                "Format payload QR tidak dikenal."
            }
            code
        } else {
            0
        }
        val minAppVersionName = if (version == MIN_VERSION_PAYLOAD_VERSION) decodeField(parts[16]) else ""
        val appUpdateUrl = if (version == MIN_VERSION_PAYLOAD_VERSION) decodeField(parts[17]) else ""

        return ExamQrPayload(
            examUrl = decodeField(parts[1]),
            examName = decodeField(parts[2]),
            startDateTime = decodeField(parts[3]),
            endDateTime = decodeField(parts[4]),
            saveToDirectLink = saveFlag,
            issuedAt = parts[6].toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
            locationPolicy = locationPolicy,
            locationPolicySource = LocationPolicySource.CustomQr,
            timezoneId = timezoneId,
            securityBypasses = securityBypasses,
            minAppVersionCode = minAppVersionCode,
            minAppVersionName = minAppVersionName,
            appUpdateUrl = appUpdateUrl
        )
    }

    private fun encodeBase64Url(bytes: ByteArray): String {
        return runCatching { encodeWithJavaBase64(bytes) }
            .getOrElse {
                Base64.encodeToString(
                    bytes,
                    Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
                )
            }
    }

    private fun decodeBase64Url(value: String): ByteArray {
        return runCatching { decodeWithJavaBase64(value) }
            .getOrElse {
                Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE)
            }
    }

    private fun decodeField(value: String): String {
        return String(decodeBase64Url(value), StandardCharsets.UTF_8)
    }

    private fun decodeKotlinQrSeedFragment(fragment: IntArray, xorKey: Int): ByteArray {
        return fragment.map { encoded -> (encoded xor xorKey).toByte() }.toByteArray()
    }

    private fun encryptPayloadKotlin(plaintext: ByteArray): ByteArray {
        val iv = ByteArray(IV_LENGTH).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, kotlinFallbackSecretKey, GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv))
        val encrypted = cipher.doFinal(plaintext)
        val packed = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(encrypted, 0, packed, iv.size, encrypted.size)
        return packed
    }

    private fun decryptPayloadKotlin(packed: ByteArray): ByteArray {
        val iv = packed.copyOfRange(0, IV_LENGTH)
        val encrypted = packed.copyOfRange(IV_LENGTH, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, kotlinFallbackSecretKey, GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private fun parseShapeType(value: String): GeofenceShapeType {
        return when (value.trim().lowercase(Locale.US)) {
            "circle" -> GeofenceShapeType.Circle
            "polygon" -> GeofenceShapeType.Polygon
            else -> GeofenceShapeType.Disabled
        }
    }

    private fun serializeVertices(vertices: List<GeofenceVertex>): String {
        return vertices.joinToString(";") { vertex ->
            "${vertex.latitude.trim()},${vertex.longitude.trim()}"
        }
    }

    private fun deserializeVertices(rawValue: String): List<GeofenceVertex> {
        if (rawValue.isBlank()) {
            return emptyList()
        }
        return rawValue.split(';')
            .mapNotNull { rawPoint ->
                val parts = rawPoint.split(',')
                if (parts.size != 2) {
                    null
                } else {
                    GeofenceVertex(
                        latitude = parts[0].trim(),
                        longitude = parts[1].trim()
                    )
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun encodeWithJavaBase64(bytes: ByteArray): String {
        val base64Class = Class.forName("java.util.Base64")
        val encoder = base64Class.getMethod("getUrlEncoder").invoke(null)
        val encoderWithoutPadding = encoder.javaClass.getMethod("withoutPadding").invoke(encoder)
        return encoderWithoutPadding.javaClass
            .getMethod("encodeToString", ByteArray::class.java)
            .invoke(encoderWithoutPadding, bytes) as String
    }

    private fun decodeWithJavaBase64(value: String): ByteArray {
        val base64Class = Class.forName("java.util.Base64")
        val decoder = base64Class.getMethod("getUrlDecoder").invoke(null)
        return decoder.javaClass
            .getMethod("decode", String::class.java)
            .invoke(decoder, value) as ByteArray
    }
}
