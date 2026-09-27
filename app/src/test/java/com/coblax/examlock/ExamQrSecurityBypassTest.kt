package com.coblax.examlock

import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.admin.examQrBypassTitles
import com.coblax.examlock.ui.admin.securityOverrideGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamQrSecurityBypassTest {
    private val base = AdminSettings()
    private val secretAdminItems = securityOverrideGroups(UiLanguage.English, base).flatMap { it.items }

    /**
     * QRs already handed out keep their meaning only while every bit stays where it is.
     * Moving one would silently relax a different check on the next scan.
     */
    @Test
    fun bitPositionsArePinned() {
        val expected = listOf(
            "screen_pinning", "app_switch", "multi_window", "overlay", "bluetooth",
            "keyboard", "accessibility", "clipboard", "device_time", "adb", "root",
            "reverse_engineering", "apk_integrity", "virtual_environment", "vpn",
            "geofence", "fake_location", "screen_recorder", "display_mirror"
        )
        assertEquals(expected, ExamQrSecurityBypass.entries.sortedBy { it.bit }.map { it.key })
        assertEquals(expected.indices.toList(), ExamQrSecurityBypass.entries.map { it.bit }.sorted())
    }

    @Test
    fun everySecretAdminSwitchCanBeCarriedByTheQr() {
        assertEquals(
            secretAdminItems.map { it.key }.toSet(),
            ExamQrSecurityBypass.entries.map { it.key }.toSet()
        )
    }

    @Test
    fun eachQrBypassTurnsOnExactlyItsOwnSecretAdminSwitch() {
        ExamQrSecurityBypass.entries.forEach { bypass ->
            val merged = base.withExamQrBypasses(setOf(bypass))
            secretAdminItems.forEach { item ->
                assertEquals("${bypass.key} -> ${item.key}", item.key == bypass.key, item.isOn(merged))
            }
            assertEquals(setOf(bypass), merged.activeSecurityBypasses())
        }
    }

    @Test
    fun maskRoundTripsAndDropsBitsThisVersionDoesNotKnow() {
        val all = ExamQrSecurityBypass.entries.toSet()
        assertEquals(all, ExamQrSecurityBypass.fromMask(ExamQrSecurityBypass.toMask(all)))
        assertEquals(0L, ExamQrSecurityBypass.toMask(emptySet()))

        val futureBit = 1L shl 40
        val mask = ExamQrSecurityBypass.toMask(setOf(ExamQrSecurityBypass.Bluetooth)) or futureBit
        assertEquals(setOf(ExamQrSecurityBypass.Bluetooth), ExamQrSecurityBypass.fromMask(mask))
    }

    @Test
    fun noQrBypassLeavesTheDeviceSettingsAsSaved() {
        val device = base.copy(bypassBluetooth = true, geofenceBypassTampered = true)
        assertSame(device, device.withExamQrBypasses(emptySet()))
    }

    @Test
    fun qrBypassAddsToTheDeviceBypassesWithoutDroppingThem() {
        val device = base.copy(bypassBluetooth = true)
        val merged = device.withExamQrBypasses(setOf(ExamQrSecurityBypass.Geofence))
        assertEquals(
            setOf(ExamQrSecurityBypass.Bluetooth, ExamQrSecurityBypass.Geofence),
            merged.activeSecurityBypasses()
        )
        assertTrue(merged.bypassLocation)
    }

    /** A tampered local store keeps enforcement on, but the signed QR is a separate source. */
    @Test
    fun qrBypassWinsOverATamperedLocalStore() {
        val device = base.copy(geofenceBypassTampered = true, deviceTimeBypassTampered = true)
        val merged = device.withExamQrBypasses(setOf(ExamQrSecurityBypass.Geofence))

        assertEquals(
            GeofenceBypassState.Active,
            GeofenceBypassResolver.stateOf(merged.bypassGeofence, merged.geofenceBypassTampered)
        )
        assertFalse(merged.locationBypassTampered)
        // A check the QR does not relax keeps its tampered state.
        assertEquals(
            DeviceTimeBypassState.Tampered,
            DeviceTimeBypassResolver.stateOf(merged.bypassDeviceTime, merged.deviceTimeBypassTampered)
        )
    }

    @Test
    fun reportSummaryNamesWhatTheQrRelaxed() {
        val device = base.copy(bypassBluetooth = true)
        val qr = setOf(ExamQrSecurityBypass.Geofence, ExamQrSecurityBypass.Vpn)
        val summary = examOverridesSummary(device.withExamQrBypasses(qr), qr)

        assertTrue(summary, summary.startsWith("bluetooth, vpn, geofence"))
        assertTrue(summary, summary.endsWith("| from exam QR: vpn, geofence"))
        assertEquals("-", examOverridesSummary(base, emptySet()))
    }

    @Test
    fun titlesUseTheSecretAdminNamesInListOrder() {
        val titles = examQrBypassTitles(
            UiLanguage.Indonesian,
            setOf(ExamQrSecurityBypass.Geofence, ExamQrSecurityBypass.ScreenPinning)
        )
        assertEquals(listOf("Screen pinning", "Geofence"), titles)
        assertEquals(emptyList<String>(), examQrBypassTitles(UiLanguage.English, emptySet()))
    }
}
