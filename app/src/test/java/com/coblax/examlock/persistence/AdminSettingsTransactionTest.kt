package com.coblax.examlock.persistence

import com.coblax.examlock.model.AdminSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminSettingsTransactionTest {
    private val initial = AdminSettings(
        fastExamUrl = "https://exam.example",
        fastExamLabel = "Exam"
    )

    @Test
    fun invalidDirectLinkIsRejectedBeforeStorageIsRead() {
        val store = FakeAdminSettingsStore(initial)
        val result = AdminSettingsTransactionCoordinator(store).apply(
            proposedSettings = initial.copy(fastExamUrl = "http://insecure.example")
        )

        assertTrue(result is AdminSettingsApplyResult.ValidationFailed)
        assertEquals(0, store.readCount)
        assertEquals(0, store.ordinaryWriteCount)
        assertEquals(0, store.bypassWriteCount)
    }

    @Test
    fun ordinaryChangeDoesNotRequireAdminCapability() {
        val store = FakeAdminSettingsStore(initial, hasCapability = false)
        val result = AdminSettingsTransactionCoordinator(store).apply(
            proposedSettings = initial.copy(fastExamLabel = "Updated")
        )

        assertTrue(result is AdminSettingsApplyResult.Success)
        assertEquals("Updated", store.read().fastExamLabel)
        assertEquals(1, store.ordinaryWriteCount)
        assertEquals(0, store.bypassWriteCount)
    }

    @Test
    fun bypassChangeRequiresAdminCapabilityBeforeAnyWrite() {
        val store = FakeAdminSettingsStore(initial, hasCapability = false)
        val result = AdminSettingsTransactionCoordinator(store).apply(
            proposedSettings = initial.copy(bypassAdb = true)
        )

        assertEquals(AdminSettingsApplyResult.ReauthenticationRequired, result)
        assertEquals(0, store.ordinaryWriteCount)
        assertEquals(0, store.bypassWriteCount)
        assertFalse(store.read().bypassAdb)
    }

    @Test
    fun successfulApplyWritesBypassThenOrdinaryAndVerifies() {
        val store = FakeAdminSettingsStore(initial, hasCapability = true)
        val result = AdminSettingsTransactionCoordinator(store).apply(
            proposedSettings = initial.copy(
                fastExamLabel = "Updated",
                bypassAdb = true
            ),
            expectedCurrentSettings = initial
        )

        assertTrue(result is AdminSettingsApplyResult.Success)
        assertEquals(listOf("bypass", "ordinary"), store.writeOrder)
        assertTrue(store.read().bypassAdb)
        assertEquals("Updated", store.read().fastExamLabel)
    }

    @Test
    fun ordinaryWriteFailureRollsBypassBackToSnapshot() {
        val store = FakeAdminSettingsStore(initial, hasCapability = true).apply {
            failNextOrdinaryWrite = true
        }
        val result = AdminSettingsTransactionCoordinator(store).apply(
            proposedSettings = initial.copy(
                fastExamLabel = "Updated",
                bypassRoot = true
            )
        )

        assertEquals(
            AdminSettingsApplyResult.PersistenceFailed(
                stage = AdminSettingsPersistenceStage.WriteSettings,
                rollbackStatus = AdminSettingsRollbackStatus.Succeeded
            ),
            result
        )
        assertEquals(initial.persistedSnapshot(), store.read().persistedSnapshot())
    }

    @Test
    fun failedRollbackIsReportedAndNeverLooksSuccessful() {
        val store = FakeAdminSettingsStore(initial, hasCapability = true).apply {
            failAllOrdinaryWrites = true
        }
        val result = AdminSettingsTransactionCoordinator(store).apply(
            proposedSettings = initial.copy(
                fastExamLabel = "Updated",
                bypassRoot = true
            )
        )

        assertEquals(
            AdminSettingsApplyResult.PersistenceFailed(
                stage = AdminSettingsPersistenceStage.WriteSettings,
                rollbackStatus = AdminSettingsRollbackStatus.Failed
            ),
            result
        )
        assertFalse(result is AdminSettingsApplyResult.Success)
    }

    @Test
    fun staleExpectedSnapshotReturnsConflictWithoutWriting() {
        val current = initial.copy(fastExamLabel = "External")
        val store = FakeAdminSettingsStore(current, hasCapability = true)
        val result = AdminSettingsTransactionCoordinator(store).apply(
            proposedSettings = initial.copy(fastExamLabel = "Draft"),
            expectedCurrentSettings = initial
        )

        assertTrue(result is AdminSettingsApplyResult.Conflict)
        assertEquals(0, store.ordinaryWriteCount)
        assertEquals(0, store.bypassWriteCount)
        assertEquals("External", (result as AdminSettingsApplyResult.Conflict).currentSettings.fastExamLabel)
    }
}

private class FakeAdminSettingsStore(
    initial: AdminSettings,
    private val hasCapability: Boolean = true
) : AdminSettingsTransactionStore {
    private var ordinarySettings = initial
    private var bypassSettings = initial

    var failNextOrdinaryWrite: Boolean = false
    var failAllOrdinaryWrites: Boolean = false
    var readCount: Int = 0
    var ordinaryWriteCount: Int = 0
    var bypassWriteCount: Int = 0
    val writeOrder = mutableListOf<String>()

    override fun read(): AdminSettings {
        readCount += 1
        return ordinarySettings.withBypassesFrom(bypassSettings)
    }

    override fun hasActiveAdminCapability(): Boolean = hasCapability

    override fun writeOrdinary(settings: AdminSettings): Boolean {
        ordinaryWriteCount += 1
        writeOrder += "ordinary"
        if (failAllOrdinaryWrites) {
            return false
        }
        if (failNextOrdinaryWrite) {
            failNextOrdinaryWrite = false
            return false
        }
        ordinarySettings = settings
        return true
    }

    override fun writeBypass(settings: AdminSettings): Boolean {
        bypassWriteCount += 1
        writeOrder += "bypass"
        bypassSettings = settings
        return true
    }
}

private fun AdminSettings.withBypassesFrom(source: AdminSettings): AdminSettings {
    return copy(
        bypassScreenPinning = source.bypassScreenPinning,
        bypassBluetooth = source.bypassBluetooth,
        bypassAccessibility = source.bypassAccessibility,
        bypassAdb = source.bypassAdb,
        bypassRoot = source.bypassRoot,
        bypassVirtualEnvironment = source.bypassVirtualEnvironment,
        bypassVpn = source.bypassVpn,
        bypassKeyboardPolicy = source.bypassKeyboardPolicy,
        bypassClipboard = source.bypassClipboard,
        bypassOverlay = source.bypassOverlay,
        bypassGeofence = source.bypassGeofence,
        bypassFakeLocation = source.bypassFakeLocation,
        bypassDeviceTime = source.bypassDeviceTime,
        bypassLocation = source.bypassGeofence || source.bypassFakeLocation,
        bypassAppSwitch = source.bypassAppSwitch,
        bypassScreenRecorder = source.bypassScreenRecorder,
        bypassDisplayMirror = source.bypassDisplayMirror,
        bypassMultiWindow = source.bypassMultiWindow,
        bypassReverseEngineering = source.bypassReverseEngineering,
        bypassApkIntegrity = source.bypassApkIntegrity
    )
}
