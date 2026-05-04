package com.example.coblaxexamlock

import android.content.Context
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.edit
import com.example.coblaxexamlock.config.AdminKeyBypassBindingSchemeVersion
import com.example.coblaxexamlock.config.AdminPreferencesName
import com.example.coblaxexamlock.persistence.BypassStorageRepository
import com.example.coblaxexamlock.persistence.readAdminSettings
import com.example.coblaxexamlock.persistence.saveAdminSettings
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminBypassSecurityTest {
    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    @Before
    fun setUp() {
        AdminAuthSession.clear()
        AdminAuth.resetRateLimitForTests()
        BypassStorageRepository.resetForTests(context)
    }

    @After
    fun tearDown() {
        AdminAuthSession.clear()
        AdminAuth.resetRateLimitForTests()
        BypassStorageRepository.resetForTests(context)
    }

    @Test
    fun adminPasswordAcceptsKnownSecretInDebug() {
        assertTrue(AdminAuth.verify(context, AdminAuthDebugAccess.knownPasswordForTests()))
    }

    @Test
    fun adminPasswordRejectsWrongSecret() {
        assertFalse(AdminAuth.verify(context, "definitely-wrong"))
    }

    @Test
    fun bypassWriteFailsWithoutAuthToken() {
        val initial = context.readAdminSettings()
        context.saveAdminSettings(initial.copy(bypassAdb = true, bypassClipboard = true))

        val reloaded = context.readAdminSettings()
        assertFalse(reloaded.bypassAdb)
        assertFalse(reloaded.bypassClipboard)
    }

    @Test
    fun bypassWriteSucceedsAfterAdminAuth() {
        assertTrue(AdminAuth.verify(context, AdminAuthDebugAccess.knownPasswordForTests()))
        val initial = context.readAdminSettings()
        context.saveAdminSettings(initial.copy(bypassAdb = true, bypassClipboard = true))

        val reloaded = context.readAdminSettings()
        assertTrue(reloaded.bypassAdb)
        assertTrue(reloaded.bypassClipboard)
    }

    @Test
    fun expiredAdminTokenBlocksBypassWrite() {
        val expiredIssuedAt =
            SystemClock.elapsedRealtime() - AdminAuthTokenValidDurationMillis - 1L
        AdminAuthSession.issueForTests(expiredIssuedAt)
        val initial = context.readAdminSettings()
        context.saveAdminSettings(initial.copy(bypassAdb = true))

        val reloaded = context.readAdminSettings()
        assertFalse(reloaded.bypassAdb)
        assertFalse(AdminAuthSession.hasActiveToken())
    }

    @Test
    fun bypassStateSurvivesFreshReadAfterAuthWrite() {
        assertTrue(AdminAuth.verify(context, AdminAuthDebugAccess.knownPasswordForTests()))
        val initial = context.readAdminSettings()
        context.saveAdminSettings(initial.copy(bypassRoot = true))
        AdminAuthSession.clear()

        val reloaded = context.readAdminSettings()
        assertTrue(reloaded.bypassRoot)
        assertFalse(reloaded.rootBypassTampered)
    }

    @Test
    fun legacyStorageIsResetToSafeOffAndNoticeIsShown() {
        BypassStorageRepository.setLegacyBypassStateForTests(context, GateKeys.Adb, true)

        val reloaded = context.readAdminSettings()
        assertFalse(reloaded.bypassAdb)
        assertTrue(reloaded.bypassMigrationResetNotice)
    }

    @Test
    fun bindingSchemeUpgradeResetsStoredBypassesToSafeOff() {
        assertTrue(AdminAuth.verify(context, AdminAuthDebugAccess.knownPasswordForTests()))
        val initial = context.readAdminSettings()
        context.saveAdminSettings(initial.copy(bypassGeofence = true, bypassFakeLocation = true))

        context.getSharedPreferences(AdminPreferencesName, Context.MODE_PRIVATE).edit {
            putInt(AdminKeyBypassBindingSchemeVersion, 1)
        }

        val reloaded = context.readAdminSettings()
        assertFalse(reloaded.bypassGeofence)
        assertFalse(reloaded.bypassFakeLocation)
        assertTrue(reloaded.bypassMigrationResetNotice)
    }
}
