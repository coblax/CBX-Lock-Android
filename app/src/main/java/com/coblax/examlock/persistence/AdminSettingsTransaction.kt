package com.coblax.examlock.persistence

import com.coblax.examlock.AdminAuthSession
import com.coblax.examlock.ExamUrlValidationError
import com.coblax.examlock.LowRamProfileOverride
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.effectiveExamUserAgent
import com.coblax.examlock.validateExamUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class AdminSettingsValidationField {
    DirectLinkUrl,
    OfficialApkUrl
}

internal enum class AdminSettingsValidationReason {
    Required,
    SecureHttpsUrlRequired
}

internal data class AdminSettingsValidationIssue(
    val field: AdminSettingsValidationField,
    val reason: AdminSettingsValidationReason
)

internal enum class AdminSettingsPersistenceStage {
    ReadCurrent,
    WriteBypass,
    VerifyBypass,
    WriteSettings,
    VerifySettings
}

internal enum class AdminSettingsRollbackStatus {
    NotNeeded,
    Succeeded,
    Failed
}

internal sealed interface AdminSettingsApplyResult {
    data class Success(
        val settings: AdminSettings,
        val changed: Boolean
    ) : AdminSettingsApplyResult

    data class ValidationFailed(
        val issues: List<AdminSettingsValidationIssue>
    ) : AdminSettingsApplyResult

    data object ReauthenticationRequired : AdminSettingsApplyResult

    data class Conflict(
        val currentSettings: AdminSettings
    ) : AdminSettingsApplyResult

    data class PersistenceFailed(
        val stage: AdminSettingsPersistenceStage,
        val rollbackStatus: AdminSettingsRollbackStatus
    ) : AdminSettingsApplyResult
}

internal data class AdminOrdinarySettingsSnapshot(
    val fastExamUrl: String,
    val fastExamLabel: String,
    val officialApkUrl: String,
    val examUserAgent: String,
    val lowRamProfileOverride: LowRamProfileOverride,
    val directLinkLocationPolicySaved: Boolean,
    val directLinkLocationPolicySerialized: String,
    val directLinkGeofenceEnabled: Boolean,
    val directLinkGeofenceCenterLat: String,
    val directLinkGeofenceCenterLng: String,
    val directLinkGeofenceRadiusMeters: String,
    val customQrSaveToDirectLinkEnabled: Boolean,
    val showChecklistDetails: Boolean,
    val telegramDiagnosticsEnabled: Boolean
)

internal data class AdminBypassSettingsSnapshot(
    val screenPinning: Boolean,
    val bluetooth: Boolean,
    val accessibility: Boolean,
    val adb: Boolean,
    val root: Boolean,
    val virtualEnvironment: Boolean,
    val vpn: Boolean,
    val keyboardPolicy: Boolean,
    val clipboard: Boolean,
    val overlay: Boolean,
    val geofence: Boolean,
    val fakeLocation: Boolean,
    val deviceTime: Boolean,
    val appSwitch: Boolean,
    val screenRecorder: Boolean,
    val displayMirror: Boolean,
    val multiWindow: Boolean,
    val reverseEngineering: Boolean,
    val apkIntegrity: Boolean
)

internal data class AdminPersistedSettingsSnapshot(
    val ordinary: AdminOrdinarySettingsSnapshot,
    val bypass: AdminBypassSettingsSnapshot
)

internal fun AdminSettings.normalizedForAdminPersistence(): AdminSettings {
    return copy(
        fastExamUrl = fastExamUrl.trim(),
        fastExamLabel = fastExamLabel.trim(),
        officialApkUrl = officialApkUrl.trim(),
        examUserAgent = effectiveExamUserAgent(),
        directLinkGeofenceCenterLat = directLinkGeofenceCenterLat.trim(),
        directLinkGeofenceCenterLng = directLinkGeofenceCenterLng.trim(),
        directLinkGeofenceRadiusMeters = directLinkGeofenceRadiusMeters.trim(),
        bypassLocation = bypassGeofence || bypassFakeLocation
    )
}

internal fun AdminSettings.persistedSnapshot(): AdminPersistedSettingsSnapshot {
    val normalized = normalizedForAdminPersistence()
    return AdminPersistedSettingsSnapshot(
        ordinary = AdminOrdinarySettingsSnapshot(
            fastExamUrl = normalized.fastExamUrl,
            fastExamLabel = normalized.fastExamLabel,
            officialApkUrl = normalized.officialApkUrl,
            examUserAgent = normalized.examUserAgent,
            lowRamProfileOverride = normalized.lowRamProfileOverride,
            directLinkLocationPolicySaved = normalized.directLinkLocationPolicySaved,
            directLinkLocationPolicySerialized = normalized.directLinkLocationPolicySerialized,
            directLinkGeofenceEnabled = normalized.directLinkGeofenceEnabled,
            directLinkGeofenceCenterLat = normalized.directLinkGeofenceCenterLat,
            directLinkGeofenceCenterLng = normalized.directLinkGeofenceCenterLng,
            directLinkGeofenceRadiusMeters = normalized.directLinkGeofenceRadiusMeters,
            customQrSaveToDirectLinkEnabled = normalized.customQrSaveToDirectLinkEnabled,
            showChecklistDetails = normalized.showChecklistDetails,
            telegramDiagnosticsEnabled = normalized.telegramDiagnosticsEnabled
        ),
        bypass = AdminBypassSettingsSnapshot(
            screenPinning = normalized.bypassScreenPinning,
            bluetooth = normalized.bypassBluetooth,
            accessibility = normalized.bypassAccessibility,
            adb = normalized.bypassAdb,
            root = normalized.bypassRoot,
            virtualEnvironment = normalized.bypassVirtualEnvironment,
            vpn = normalized.bypassVpn,
            keyboardPolicy = normalized.bypassKeyboardPolicy,
            clipboard = normalized.bypassClipboard,
            overlay = normalized.bypassOverlay,
            geofence = normalized.bypassGeofence,
            fakeLocation = normalized.bypassFakeLocation,
            deviceTime = normalized.bypassDeviceTime,
            appSwitch = normalized.bypassAppSwitch,
            screenRecorder = normalized.bypassScreenRecorder,
            displayMirror = normalized.bypassDisplayMirror,
            multiWindow = normalized.bypassMultiWindow,
            reverseEngineering = normalized.bypassReverseEngineering,
            apkIntegrity = normalized.bypassApkIntegrity
        )
    )
}

internal fun adminSettingsArePersistenceEquivalent(
    first: AdminSettings?,
    second: AdminSettings?
): Boolean {
    if (first == null || second == null) {
        return first == second
    }
    return first.persistedSnapshot() == second.persistedSnapshot()
}

internal fun validateAdminSettingsForApply(
    settings: AdminSettings
): List<AdminSettingsValidationIssue> {
    val normalized = settings.normalizedForAdminPersistence()
    val issues = mutableListOf<AdminSettingsValidationIssue>()
    val directLinkValidation = validateExamUrl(normalized.fastExamUrl)
    if (!directLinkValidation.isValid) {
        issues += AdminSettingsValidationIssue(
            field = AdminSettingsValidationField.DirectLinkUrl,
            reason = when (directLinkValidation.error) {
                ExamUrlValidationError.Blank -> AdminSettingsValidationReason.Required
                ExamUrlValidationError.Invalid,
                null -> AdminSettingsValidationReason.SecureHttpsUrlRequired
            }
        )
    }
    if (normalized.officialApkUrl.isNotBlank()) {
        val officialApkValidation = validateExamUrl(normalized.officialApkUrl)
        if (!officialApkValidation.isValid) {
            issues += AdminSettingsValidationIssue(
                field = AdminSettingsValidationField.OfficialApkUrl,
                reason = AdminSettingsValidationReason.SecureHttpsUrlRequired
            )
        }
    }
    return issues
}

internal interface AdminSettingsTransactionStore {
    fun read(): AdminSettings
    fun hasActiveAdminCapability(): Boolean
    fun writeOrdinary(settings: AdminSettings): Boolean
    fun writeBypass(settings: AdminSettings): Boolean
}

internal class AdminSettingsTransactionCoordinator(
    private val store: AdminSettingsTransactionStore
) {
    fun apply(
        proposedSettings: AdminSettings,
        expectedCurrentSettings: AdminSettings? = null
    ): AdminSettingsApplyResult {
        val normalizedProposed = proposedSettings.normalizedForAdminPersistence()
        val validationIssues = validateAdminSettingsForApply(normalizedProposed)
        if (validationIssues.isNotEmpty()) {
            return AdminSettingsApplyResult.ValidationFailed(validationIssues)
        }

        val current = runCatching(store::read).getOrElse {
            return AdminSettingsApplyResult.PersistenceFailed(
                stage = AdminSettingsPersistenceStage.ReadCurrent,
                rollbackStatus = AdminSettingsRollbackStatus.NotNeeded
            )
        }
        if (
            expectedCurrentSettings != null &&
            !adminSettingsArePersistenceEquivalent(expectedCurrentSettings, current)
        ) {
            return AdminSettingsApplyResult.Conflict(current)
        }

        val currentSnapshot = current.persistedSnapshot()
        val proposedSnapshot = normalizedProposed.persistedSnapshot()
        if (currentSnapshot == proposedSnapshot) {
            return AdminSettingsApplyResult.Success(settings = current, changed = false)
        }

        val bypassChanged = currentSnapshot.bypass != proposedSnapshot.bypass
        val ordinaryChanged = currentSnapshot.ordinary != proposedSnapshot.ordinary
        if (bypassChanged && !runCatching(store::hasActiveAdminCapability).getOrDefault(false)) {
            return AdminSettingsApplyResult.ReauthenticationRequired
        }

        if (bypassChanged) {
            val bypassWritten = runCatching {
                store.writeBypass(normalizedProposed)
            }.getOrDefault(false)
            if (!bypassWritten) {
                return failedWithRollback(
                    stage = AdminSettingsPersistenceStage.WriteBypass,
                    previous = current,
                    restoreBypass = true
                )
            }
            val bypassVerified = runCatching {
                store.read().persistedSnapshot().bypass == proposedSnapshot.bypass
            }.getOrDefault(false)
            if (!bypassVerified) {
                return failedWithRollback(
                    stage = AdminSettingsPersistenceStage.VerifyBypass,
                    previous = current,
                    restoreBypass = true
                )
            }
        }

        if (ordinaryChanged) {
            val ordinaryWritten = runCatching {
                store.writeOrdinary(normalizedProposed)
            }.getOrDefault(false)
            if (!ordinaryWritten) {
                return failedWithRollback(
                    stage = AdminSettingsPersistenceStage.WriteSettings,
                    previous = current,
                    restoreBypass = bypassChanged
                )
            }
        }

        val refreshed = runCatching(store::read).getOrNull()
        if (refreshed?.persistedSnapshot() != proposedSnapshot) {
            return failedWithRollback(
                stage = AdminSettingsPersistenceStage.VerifySettings,
                previous = current,
                restoreBypass = bypassChanged
            )
        }
        return AdminSettingsApplyResult.Success(settings = refreshed, changed = true)
    }

    private fun failedWithRollback(
        stage: AdminSettingsPersistenceStage,
        previous: AdminSettings,
        restoreBypass: Boolean
    ): AdminSettingsApplyResult.PersistenceFailed {
        val bypassRestored = !restoreBypass || runCatching {
            store.writeBypass(previous)
        }.getOrDefault(false)
        val ordinaryRestored = runCatching {
            store.writeOrdinary(previous)
        }.getOrDefault(false)
        val previousSnapshot = previous.persistedSnapshot()
        val rollbackVerified = bypassRestored && ordinaryRestored && runCatching {
            store.read().persistedSnapshot() == previousSnapshot
        }.getOrDefault(false)
        return AdminSettingsApplyResult.PersistenceFailed(
            stage = stage,
            rollbackStatus = if (rollbackVerified) {
                AdminSettingsRollbackStatus.Succeeded
            } else {
                AdminSettingsRollbackStatus.Failed
            }
        )
    }
}

private class ContextAdminSettingsTransactionStore(
    context: android.content.Context
) : AdminSettingsTransactionStore {
    private val appContext = context.applicationContext

    override fun read(): AdminSettings = appContext.readAdminSettings()

    override fun hasActiveAdminCapability(): Boolean = AdminAuthSession.hasActiveToken()

    override fun writeOrdinary(settings: AdminSettings): Boolean {
        return appContext.commitOrdinaryAdminSettings(settings)
    }

    override fun writeBypass(settings: AdminSettings): Boolean {
        return AdminBypassController.persistBypassSettings(appContext, settings)
    }
}

internal suspend fun android.content.Context.applyAdminSettingsExplicitly(
    proposedSettings: AdminSettings,
    expectedCurrentSettings: AdminSettings? = null
): AdminSettingsApplyResult {
    val appContext = applicationContext
    return withContext(Dispatchers.IO) {
        synchronized(AdminSettingsPersistenceLock) {
            AdminSettingsTransactionCoordinator(
                ContextAdminSettingsTransactionStore(appContext)
            ).apply(
                proposedSettings = proposedSettings,
                expectedCurrentSettings = expectedCurrentSettings
            )
        }
    }
}
