package com.coblax.examlock.save

import androidx.compose.runtime.saveable.SaverScope
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.ExamQrSecurityBypass
import com.coblax.examlock.LocationPolicySource
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamQrPayloadSaverTest {
    private val scope = SaverScope { true }

    /**
     * After Android kills the app mid-exam, the restored session must still run with the
     * QR's bypasses and exam time zone, not the device defaults.
     */
    @Test
    fun restoredPayloadKeepsBypassesAndTimeZone() {
        val payload = ExamQrPayload(
            examUrl = "https://example.com/ujian",
            examName = "Fisika",
            startDateTime = "12/03/2026 07:00",
            endDateTime = "12/03/2026 09:00",
            issuedAt = 42L,
            locationPolicySource = LocationPolicySource.DisabledNoPolicy,
            timezoneId = "Asia/Makassar",
            securityBypasses = setOf(ExamQrSecurityBypass.Bluetooth, ExamQrSecurityBypass.DeviceTime)
        )

        val saved = with(ExamQrPayloadSaver) { scope.save(payload) }
        val restored = ExamQrPayloadSaver.restore(saved!!)

        assertEquals(payload, restored)
    }

    @Test
    fun stateSavedByAnOlderBuildRestoresWithoutBypasses() {
        val olderState = listOf(
            "https://example.com/ujian", "Fisika", "12/03/2026 07:00", "12/03/2026 09:00",
            42L, false, null, LocationPolicySource.DisabledNoPolicy.name
        )

        val restored = ExamQrPayloadSaver.restore(olderState)

        assertEquals(emptySet<ExamQrSecurityBypass>(), restored?.securityBypasses)
        assertEquals("https://example.com/ujian", restored?.examUrl)
    }
}
