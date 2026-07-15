@file:Suppress("DEPRECATION")

package com.coblax.examlock.persistence

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.GateKey
import com.coblax.examlock.GateKeys
import com.coblax.examlock.LegacySwitchMatrixStorage
import com.coblax.examlock.config.AdminBypassSecurePreferencesName
import com.coblax.examlock.config.AdminKeyBypassBindingSchemeVersion
import com.coblax.examlock.config.AdminKeyBypassEnvelopeMigrated
import com.coblax.examlock.config.AdminKeyBypassLastSeenCounter
import com.coblax.examlock.config.AdminKeyBypassMigrationResetNotice
import com.coblax.examlock.config.AdminKeyLocationBypassSplitMigrated
import com.coblax.examlock.config.AdminPreferencesName
import com.coblax.examlock.nativebridge.BypassCryptoBridge

private const val BypassStorageTag = "BypassStorageRepo"
private const val BypassEnvelopeKey = "bypass_envelope_v1"

internal object BypassStorageRepository {
    @Volatile
    private var isInitializing = false
    private val managedKeys = listOf(
        GateKeys.ScreenPinning,
        GateKeys.Bluetooth,
        GateKeys.Accessibility,
        GateKeys.Adb,
        GateKeys.Root,
        GateKeys.VirtualEnv,
        GateKeys.KeyboardPolicy,
        GateKeys.Clipboard,
        GateKeys.Overlay,
        GateKeys.AppSwitch,
        GateKeys.Location,
        GateKeys.Geofence,
        GateKeys.FakeLocation,
        GateKeys.DeviceTime,
        GateKeys.Vpn,
        GateKeys.ScreenRecorder,
        GateKeys.DisplayMirror,
        GateKeys.MultiWindow,
        GateKeys.ReverseEngineering,
        GateKeys.ApkIntegrity
    )

    private val defaultGateStates: Map<Int, Boolean>
        get() = managedKeys.associate { it.id to false }

    fun read(context: Context): BypassStorageReadResult {
        ensureInitialized(context)
        val adminPrefs = adminPrefs(context)
        val securePrefs = securePrefs(context) ?: return tamperedResult(
            adminPrefs = adminPrefs,
            reason = "secure_prefs_unavailable"
        )
        val serializedEnvelope = securePrefs.getString(BypassEnvelopeKey, null).orEmpty()
        val deviceBinding = BypassCryptoBridge.buildDeviceBinding(context)
        val validation = BypassEnvelopeVerifier.validateEncodedEnvelope(
            serializedEnvelope = serializedEnvelope,
            expectedDeviceBinding = deviceBinding,
            lastSeenCounter = adminPrefs.getLong(AdminKeyBypassLastSeenCounter, -1L),
            macComputer = { payload, binding ->
                BypassCryptoBridge.computeEnvelopeMac(payload, binding)
            }
        )
        val migrationNotice = adminPrefs.getBoolean(AdminKeyBypassMigrationResetNotice, false)
        return if (validation.status == BypassEnvelopeValidationStatus.Valid) {
            val payload = checkNotNull(validation.payload)
            adminPrefs.edit {
                if (payload.monotonicCounter > adminPrefs.getLong(AdminKeyBypassLastSeenCounter, -1L)) {
                    putLong(AdminKeyBypassLastSeenCounter, payload.monotonicCounter)
                }
            }
            BypassStorageReadResult(
                gateStates = normalizeGateStates(payload.gateStates),
                tampered = false,
                migrationResetNotice = migrationNotice
            )
        } else {
            BypassStorageReadResult(
                gateStates = defaultGateStates,
                tampered = true,
                migrationResetNotice = migrationNotice,
                reason = validation.reason
            )
        }
    }

    fun writeAllStates(
        context: Context,
        gateStates: Map<Int, Boolean>,
        clearMigrationResetNotice: Boolean = true
    ): Boolean {
        ensureInitialized(context)
        return writeAllStatesInternal(context, gateStates, clearMigrationResetNotice)
    }

    private fun writeAllStatesInternal(
        context: Context,
        gateStates: Map<Int, Boolean>,
        clearMigrationResetNotice: Boolean
    ): Boolean {
        val adminPrefs = adminPrefs(context)
        val securePrefs = securePrefs(context) ?: return false
        val currentPayload = readCurrentValidPayload(context)
        val now = System.currentTimeMillis()
        val deviceBinding = BypassCryptoBridge.buildDeviceBinding(context)
        if (deviceBinding.isBlank()) {
            Log.w(BypassStorageTag, "Cannot write bypass envelope because device binding is unavailable.")
            return false
        }
        val normalizedStates = normalizeGateStates(gateStates)
        val nextCounter = maxOf(
            currentPayload?.monotonicCounter ?: 0L,
            adminPrefs.getLong(AdminKeyBypassLastSeenCounter, 0L)
        ) + 1L
        val payload = BypassEnvelopePayload(
            monotonicCounter = nextCounter,
            createdAtEpochMillis = currentPayload?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
            deviceBinding = deviceBinding,
            gateStates = normalizedStates
        )
        val serializedPayload = BypassEnvelopeCodec.encodePayload(payload)
        val mac = BypassCryptoBridge.computeEnvelopeMac(serializedPayload, deviceBinding)
        if (mac.isBlank()) {
            Log.w(BypassStorageTag, "Cannot write bypass envelope because MAC computation failed.")
            return false
        }
        val serializedEnvelope = BypassEnvelopeCodec.encodeEnvelope(
            BypassEnvelope(payload = serializedPayload, mac = mac)
        )
        securePrefs.edit {
            putString(BypassEnvelopeKey, serializedEnvelope)
        }
        adminPrefs.edit {
            putLong(AdminKeyBypassLastSeenCounter, nextCounter)
            putInt(AdminKeyBypassBindingSchemeVersion, BypassDeviceBindingSchemeVersion)
            if (clearMigrationResetNotice) {
                putBoolean(AdminKeyBypassMigrationResetNotice, false)
            }
        }
        return true
    }

    fun clearMigrationNotice(context: Context) {
        adminPrefs(context).edit {
            putBoolean(AdminKeyBypassMigrationResetNotice, false)
        }
    }

    fun setLegacyBypassStateForTests(context: Context, key: GateKey, enabled: Boolean) {
        LegacySwitchMatrixStorage.setEnabled(context, key, enabled)
        adminPrefs(context).edit {
            putBoolean(AdminKeyBypassEnvelopeMigrated, false)
        }
        securePrefs(context)?.edit { remove(BypassEnvelopeKey) }
    }

    fun resetForTests(context: Context) {
        adminPrefs(context).edit {
            remove(AdminKeyBypassBindingSchemeVersion)
            remove(AdminKeyBypassEnvelopeMigrated)
            remove(AdminKeyBypassMigrationResetNotice)
            remove(AdminKeyBypassLastSeenCounter)
            remove(AdminKeyLocationBypassSplitMigrated)
        }
        securePrefs(context)?.edit {
            clear()
        }
        LegacySwitchMatrixStorage.clear(context)
    }

    private fun ensureInitialized(context: Context) {
        if (isInitializing) return
        isInitializing = true
        try {
            val adminPrefs = adminPrefs(context)
            if (!adminPrefs.getBoolean(AdminKeyBypassEnvelopeMigrated, false)) {
                migrateLegacyStorage(context, adminPrefs)
            }
            if (adminPrefs.getInt(AdminKeyBypassBindingSchemeVersion, 0) != BypassDeviceBindingSchemeVersion) {
                migrateBindingScheme(context, adminPrefs)
            }
            val securePrefs = securePrefs(context) ?: return
            if (securePrefs.getString(BypassEnvelopeKey, null).isNullOrBlank()) {
                writeAllStatesInternal(context, defaultGateStates, clearMigrationResetNotice = false)
            }
        } finally {
            isInitializing = false
        }
    }

    private fun migrateLegacyStorage(context: Context, adminPrefs: SharedPreferences) {
        val legacyDetected = LegacySwitchMatrixStorage.containsLegacyState(context) ||
            adminPrefs.contains(AdminKeyLocationBypassSplitMigrated)
        LegacySwitchMatrixStorage.clear(context)
        adminPrefs.edit {
            putBoolean(AdminKeyBypassEnvelopeMigrated, true)
            putInt(AdminKeyBypassBindingSchemeVersion, BypassDeviceBindingSchemeVersion)
            remove(AdminKeyLocationBypassSplitMigrated)
            if (legacyDetected) {
                putBoolean(AdminKeyBypassMigrationResetNotice, true)
            }
        }
        writeAllStatesInternal(
            context,
            defaultGateStates,
            clearMigrationResetNotice = !legacyDetected
        )
    }

    private fun migrateBindingScheme(context: Context, adminPrefs: SharedPreferences) {
        val securePrefs = securePrefs(context)
        val hadEnvelope = !securePrefs?.getString(BypassEnvelopeKey, null).isNullOrBlank()
        val hadPersistentBypassState = hadEnvelope || adminPrefs.contains(AdminKeyBypassLastSeenCounter)

        securePrefs?.edit {
            remove(BypassEnvelopeKey)
        }
        adminPrefs.edit {
            putInt(AdminKeyBypassBindingSchemeVersion, BypassDeviceBindingSchemeVersion)
            if (hadPersistentBypassState) {
                putBoolean(AdminKeyBypassMigrationResetNotice, true)
            }
        }
        if (hadPersistentBypassState) {
            Log.i(
                BypassStorageTag,
                "Bypass binding scheme changed; resetting stored bypass envelope to safe OFF."
            )
            writeAllStatesInternal(
                context,
                defaultGateStates,
                clearMigrationResetNotice = false
            )
        }
    }

    private fun readCurrentValidPayload(context: Context): BypassEnvelopePayload? {
        val securePrefs = securePrefs(context) ?: return null
        val serializedEnvelope = securePrefs.getString(BypassEnvelopeKey, null).orEmpty()
        val deviceBinding = BypassCryptoBridge.buildDeviceBinding(context)
        val validation = BypassEnvelopeVerifier.validateEncodedEnvelope(
            serializedEnvelope = serializedEnvelope,
            expectedDeviceBinding = deviceBinding,
            lastSeenCounter = -1L,
            macComputer = { payload, binding ->
                BypassCryptoBridge.computeEnvelopeMac(payload, binding)
            }
        )
        return validation.payload
    }

    private fun normalizeGateStates(states: Map<Int, Boolean>): Map<Int, Boolean> {
        val normalized = defaultGateStates.toMutableMap()
        states.forEach { (gateId, enabled) ->
            if (normalized.containsKey(gateId)) {
                normalized[gateId] = enabled
            }
        }
        normalized[GateKeys.Location.id] = false
        return normalized.toMap()
    }

    private fun adminPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(AdminPreferencesName, Context.MODE_PRIVATE)
    }

    private fun securePrefs(context: Context): SharedPreferences? {
        return runCatching {
            EncryptedSharedPreferences.create(
                context,
                AdminBypassSecurePreferencesName,
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.onFailure { throwable ->
            Log.e(BypassStorageTag, "CRITICAL: EncryptedSharedPreferences unavailable for bypass storage. " +
                "All bypass gates will be treated as tampered (OFF).", throwable)
        }.getOrNull()
    }

    private fun tamperedResult(adminPrefs: SharedPreferences, reason: String): BypassStorageReadResult {
        return BypassStorageReadResult(
            gateStates = defaultGateStates,
            tampered = true,
            migrationResetNotice = adminPrefs.getBoolean(AdminKeyBypassMigrationResetNotice, false),
            reason = reason
        )
    }
}
