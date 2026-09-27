package com.coblax.examlock.ui.preparation

import com.coblax.examlock.model.UiLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreparationBypassNoteTest {
    @Test
    fun nothingRelaxedSaysNothing() {
        assertNull(preparationBypassNote(UiLanguage.Indonesian, false, emptyList(), false))
    }

    @Test
    fun deviceBypassAloneKeepsTheAdminNote() {
        assertEquals(
            "Bypass admin aktif. Semua tetap dicatat.",
            preparationBypassNote(UiLanguage.Indonesian, true, emptyList(), false)
        )
    }

    /** A proctor has to be able to tell a relaxed exam QR from a tampered or bypassed phone. */
    @Test
    fun qrBypassNamesTheChecksItTurnsOff() {
        assertEquals(
            "QR ujian melonggarkan: Cek Bluetooth, Geofence. Semua tetap dicatat.",
            preparationBypassNote(UiLanguage.Indonesian, true, listOf("Cek Bluetooth", "Geofence"), false)
        )
    }

    @Test
    fun qrAndDeviceBypassesAreBothNamed() {
        assertEquals(
            "Admin bypass is active, and the exam QR turns off: Geofence. Everything is still logged.",
            preparationBypassNote(UiLanguage.English, true, listOf("Geofence"), true)
        )
    }
}
