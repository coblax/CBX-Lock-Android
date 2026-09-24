package com.coblax.examlock.ui.admin

import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.SecretAdminTab
import com.coblax.examlock.model.UiLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretAdminOverridesTest {
    private val base = AdminSettings()

    /**
     * The grouped list replaced 19 hand-written switches. Each one must still appear
     * exactly once and drive its own setting, or a bypass could become unreachable (or
     * one switch could silently flip another).
     */
    @Test
    fun everyBypassAppearsOnceAndDrivesItsOwnSetting() {
        val items = securityOverrideGroups(UiLanguage.English, base).flatMap { it.items }

        assertEquals(19, items.size)
        assertEquals(items.size, items.map { it.key }.toSet().size)
        assertEquals(0, activeSecurityOverrideCount(base))
        items.forEach { item ->
            val on = item.set(base, true)
            assertTrue(item.key, item.isOn(on))
            assertEquals(item.key, 1, activeSecurityOverrideCount(on))
            assertFalse(item.key, item.isOn(item.set(on, false)))
        }
    }

    @Test
    fun turningAllOffClearsEveryBypass() {
        val allOn = securityOverrideGroups(UiLanguage.English, base)
            .flatMap { it.items }
            .fold(base) { settings, item -> item.set(settings, true) }
        assertEquals(19, activeSecurityOverrideCount(allOn))

        val allOff = allOn.withAllSecurityOverridesOff()

        assertEquals(0, activeSecurityOverrideCount(allOff))
        assertFalse(allOff.hasAnyBypass())
    }

    @Test
    fun tamperedBypassNeverShowsAsOn() {
        val tampered = base.copy(
            bypassReverseEngineering = true,
            reverseEngineeringBypassTampered = true
        )
        val item = securityOverrideGroups(UiLanguage.English, tampered)
            .flatMap { it.items }
            .single { it.key == "reverse_engineering" }

        assertFalse(item.isOn(tampered))
        assertTrue(item.description.contains("tampered"))
    }

    /** Saved tab names from older versions (and the retired Location tab) still open. */
    @Test
    fun persistedTabNamesResolveToAVisibleTab() {
        assertEquals(SecretAdminTab.Setup, resolveSecretAdminTab("setup"))
        assertEquals(SecretAdminTab.Setup, resolveSecretAdminTab("Location"))
        assertEquals(SecretAdminTab.Overrides, resolveSecretAdminTab("Overrides"))
        assertEquals(SecretAdminTab.Setup, resolveSecretAdminTab("no_such_tab"))
        assertTrue(SecretAdminVisibleTabs.none { it == SecretAdminTab.Location })
    }
}
