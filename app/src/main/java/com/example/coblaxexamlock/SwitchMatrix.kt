@file:Suppress("DEPRECATION")

package com.example.coblaxexamlock

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.coblaxexamlock.persistence.BypassStorageRepository
import java.util.Locale

@JvmInline
value class GateKey(val id: Int)

internal data class CriticalGateResolution(
    val enabled: Boolean,
    val tampered: Boolean
)

object GateKeys {
    private const val MASK = 0x5A7F3C1D
    private fun g(value: Int): GateKey = GateKey(value xor MASK)

    val ScreenPinning = g(0x13579BDF)
    val Bluetooth = g(0x2468ACE0)
    val Accessibility = g(0x0F0E0D0C)
    val Adb = g(0x55AA11EE)
    val Root = g(0xCAFEBABE.toInt())
    val VirtualEnv = g(0x0BADF00D)
    val KeyboardPolicy = g(0x33CC77AA)
    val Clipboard = g(0x1A2B3C4D)
    val Overlay = g(0x6789ABCD)
    val AppSwitch = g(0x10203040)
    val Location = g(0x5EEDC0DE)
    val Geofence = g(0x31415926)
    val FakeLocation = g(0x27182818)
    val DeviceTime = g(0x7F4A1C3B)
    val Vpn = g(0x4E56504E)
    val ScreenRecorder = g(0x5C8E3A2F)
    val DisplayMirror = g(0x3B7D4E1A)
    val MultiWindow = g(0x2A6C5F09)
    val ReverseEngineering = g(0x6D2A4B10)
    val ApkIntegrity = g(0x41C0FFEE)

    val DecoyOne = g(0x6A09E667)
    val DecoyTwo = g(0xBB67AE85.toInt())

    internal val all = listOf(
        ScreenPinning,
        Bluetooth,
        Accessibility,
        Adb,
        Root,
        VirtualEnv,
        KeyboardPolicy,
        Clipboard,
        Overlay,
        AppSwitch,
        Location,
        Geofence,
        FakeLocation,
        DecoyOne,
        DecoyTwo,
        DeviceTime,
        Vpn,
        ScreenRecorder,
        DisplayMirror,
        MultiWindow,
        ReverseEngineering,
        ApkIntegrity
    )
}

object GateResolver {
    private const val MATRIX_SIZE = 24
    private const val GateResolverSalt = 137
    private var cachedSeed: Int? = null
    private var cachedPermutation: IntArray? = null

    fun indexFor(context: Context, key: GateKey): Int {
        val slot = GateKeys.all.indexOfFirst { it.id == key.id }
        if (slot < 0) {
            return 0
        }
        val permutation = getPermutation(context)
        return permutation[slot]
    }

    private fun getPermutation(context: Context): IntArray {
        val seed = seedFor(context)
        val cached = cachedPermutation
        if (cached != null && cachedSeed == seed) {
            return cached
        }
        val indices = IntArray(MATRIX_SIZE) { it }
        var x = seed
        for (i in indices.lastIndex downTo 1) {
            x = xorshift(x)
            val j = (x and Int.MAX_VALUE) % (i + 1)
            val tmp = indices[i]
            indices[i] = indices[j]
            indices[j] = tmp
        }
        cachedSeed = seed
        cachedPermutation = indices
        return indices
    }

    @SuppressLint("HardwareIds")
    private fun seedFor(context: Context): Int {
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        var seed = 0x9E3779B9.toInt()
        seed = seed xor GateResolverSalt
        seed = seed xor BuildConfig.VERSION_CODE
        seed = seed xor context.packageName.hashCode()
        seed = seed xor androidId.hashCode()
        return seed
    }

    private fun xorshift(value: Int): Int {
        var x = value
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        return x
    }
}

internal object SwitchMatrix {
    private val criticalGateIds = setOf(
        GateKeys.ScreenPinning.id,
        GateKeys.Accessibility.id,
        GateKeys.Adb.id,
        GateKeys.Root.id,
        GateKeys.Clipboard.id,
        GateKeys.Overlay.id,
        GateKeys.AppSwitch.id,
        GateKeys.Location.id,
        GateKeys.Geofence.id,
        GateKeys.FakeLocation.id,
        GateKeys.DeviceTime.id,
        GateKeys.Vpn.id
    )

    fun isEnabled(context: Context, key: GateKey): Boolean {
        return BypassStorageRepository.read(context).isEnabled(key.id)
    }

    fun setEnabled(context: Context, key: GateKey, enabled: Boolean) {
        val current = BypassStorageRepository.read(context).gateStates.toMutableMap()
        current[key.id] = enabled
        BypassStorageRepository.writeAllStates(context, current)
    }

    internal fun resolveCritical(context: Context, key: GateKey): CriticalGateResolution {
        val snapshot = BypassStorageRepository.read(context)
        if (key.id !in criticalGateIds) {
            return CriticalGateResolution(enabled = snapshot.isEnabled(key.id), tampered = false)
        }
        return CriticalGateResolution(
            enabled = if (snapshot.tampered) false else snapshot.isEnabled(key.id),
            tampered = snapshot.tampered
        )
    }
}

internal object LegacySwitchMatrixStorage {
    private fun prefs(context: Context): SharedPreferences {
        val prefName = storageName(context)
        return runCatching {
            EncryptedSharedPreferences.create(
                context,
                prefName,
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        }
    }

    fun containsLegacyState(context: Context): Boolean {
        return prefs(context).all.isNotEmpty()
    }

    fun clear(context: Context) {
        prefs(context).edit { clear() }
    }

    fun setEnabled(context: Context, key: GateKey, enabled: Boolean) {
        val idx = GateResolver.indexFor(context, key)
        val mask = 1L shl idx
        val bits = prefs(context).getLong(matrixKey(context), 0L)
        val updated = if (enabled) bits or mask else bits and mask.inv()
        prefs(context).edit {
            putLong(matrixKey(context), updated)
            if (isCritical(key)) {
                putString(sealKey(context, key), legacySeal(context, key, enabled))
            }
        }
    }

    private fun storageName(context: Context): String {
        return safePreferenceName(obfuscateKey(context, "switch_matrix"))
    }

    private fun matrixKey(context: Context): String = obfuscateKey(context, "matrix_bits")

    private fun sealKey(context: Context, key: GateKey): String {
        return obfuscateKey(context, "matrix_seal_${key.id}")
    }

    @SuppressLint("HardwareIds")
    private fun legacySeal(context: Context, key: GateKey, enabled: Boolean): String {
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val payload = buildString {
            append(context.packageName)
            append('|')
            append(context.applicationInfo.dataDir)
            append('|')
            append(BuildConfig.VERSION_CODE)
            append('|')
            append(key.id)
            append('|')
            append(if (enabled) '1' else '0')
            append('|')
            append(androidId)
        }
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> String.format(Locale.US, "%02x", byte) }
    }

    private fun isCritical(key: GateKey): Boolean {
        return key.id in setOf(
            GateKeys.ScreenPinning.id,
            GateKeys.Accessibility.id,
            GateKeys.Adb.id,
            GateKeys.Root.id,
            GateKeys.Clipboard.id,
            GateKeys.Overlay.id,
            GateKeys.AppSwitch.id,
            GateKeys.Location.id,
            GateKeys.Geofence.id,
            GateKeys.FakeLocation.id,
            GateKeys.DeviceTime.id,
            GateKeys.Vpn.id
        )
    }

    private fun obfuscateKey(context: Context, value: String): String {
        val seed = GateResolver.indexFor(context, GateKeys.DecoyOne) +
            GateResolver.indexFor(context, GateKeys.DecoyTwo)
        val bytes = value.toByteArray(Charsets.UTF_8)
        for (index in bytes.indices) {
            val shift = ((seed + index * 31) and 0xFF)
            bytes[index] = (bytes[index].toInt() xor shift).toByte()
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP).lowercase(Locale.US)
    }

    private fun safePreferenceName(value: String): String {
        return value.replace('/', '_').replace('\\', '_')
    }
}
