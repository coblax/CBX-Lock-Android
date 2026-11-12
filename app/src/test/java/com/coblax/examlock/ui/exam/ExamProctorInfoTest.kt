package com.coblax.examlock.ui.exam

import com.coblax.examlock.ExamParticipantContext
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.ExamNetworkStatus
import com.coblax.examlock.model.NetworkDiagnostics
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkReadinessVerdict
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.theme.UiStatusTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ExamProctorInfoTest {
    @Test
    fun participantNameCombinesNameAndAccount() {
        assertEquals(
            "Siti Aminah (12345)",
            proctorParticipantName(participant(displayName = " Siti Aminah ", username = "12345"))
        )
        assertEquals("Siti", proctorParticipantName(participant(displayName = "Siti", username = "siti")))
        assertEquals("u-77", proctorParticipantName(participant(userId = "u-77")))
        assertNull(proctorParticipantName(participant(displayName = " ")))
    }

    @Test
    fun participantGroupJoinsClassAndRoom() {
        assertEquals("XII IPA 1 · R07", proctorParticipantGroup(participant(kodeKelas = "XII IPA 1", kodeRuang = "R07")))
        assertEquals("R07", proctorParticipantGroup(participant(kodeRuang = "R07")))
        assertNull(proctorParticipantGroup(participant()))
    }

    @Test
    fun rowsInIndonesianCarryTimeStatusAndVersion() {
        val rows = buildExamProctorInfoRows(
            state = state(participantName = "Siti Aminah (12345)", participantGroup = "XII IPA 1 · R07"),
            language = UiLanguage.Indonesian,
            clockText = "09:42:10",
            deviceText = "Samsung SM-A105F · Android 11",
            appVersionText = "3.3.8 (380)"
        ).associateBy { it.label }

        assertEquals("09:42:10", rows.getValue("Jam").value)
        assertEquals("Siti Aminah (12345)", rows.getValue("Peserta").value)
        assertEquals("XII IPA 1 · R07", rows.getValue("Kelas · Ruang").value)
        assertEquals("Ujian Akhir Semester", rows.getValue("Ujian").value)
        assertEquals("Wi-Fi · Terhubung", rows.getValue("Jaringan").value)
        assertEquals(UiStatusTone.Success, rows.getValue("Jaringan").tone)
        assertEquals("Online", rows.getValue("Server ujian").value)
        assertEquals("Aman", rows.getValue("Keamanan").value)
        assertEquals("15%", rows.getValue("Baterai").value)
        assertEquals(UiStatusTone.Danger, rows.getValue("Baterai").tone)
        assertEquals("Samsung SM-A105F · Android 11", rows.getValue("HP").value)
        assertEquals("3.3.8 (380)", rows.getValue("CBX Lock").value)
    }

    @Test
    fun unknownParticipantAndProblemsAreSaidPlainly() {
        val rows = buildExamProctorInfoRows(
            state = state(
                verdict = NetworkReadinessVerdict.Offline,
                serverStatus = ExamServerFooterStatus.Offline,
                shieldStatus = ExamFooterShieldStatus.Danger,
                battery = ExamBatteryStatus(levelPercent = 64, isCharging = true)
            ),
            language = UiLanguage.English,
            clockText = "-",
            deviceText = "-",
            appVersionText = "-"
        ).associateBy { it.label }

        assertEquals("Not read from the exam site yet", rows.getValue("Participant").value)
        assertFalse("no class row without a class", rows.containsKey("Class · Room"))
        assertEquals("Wi-Fi · Offline", rows.getValue("Network").value)
        assertEquals(UiStatusTone.Warning, rows.getValue("Network").tone)
        assertEquals("Not responding", rows.getValue("Exam server").value)
        assertEquals(UiStatusTone.Danger, rows.getValue("Security").tone)
        assertEquals("64%, charging", rows.getValue("Battery").value)
    }

    @Test
    fun examServerAddressIsNeverShown() {
        val rows = buildExamProctorInfoRows(
            state = state(),
            language = UiLanguage.Indonesian,
            clockText = "-",
            deviceText = "-",
            appVersionText = "-"
        )
        rows.forEach { row -> assertFalse(row.value, row.value.contains("sekolah.sch.id")) }
    }

    @Test
    fun deviceTextDoesNotRepeatTheMaker() {
        assertEquals("Samsung SM-A105F · Android 11", proctorDeviceText("samsung", "SM-A105F", "11"))
        assertEquals("Google Pixel 7 · Android 16", proctorDeviceText("Google", "Pixel 7", "16"))
        assertEquals("Xiaomi Redmi 9A · Android 10", proctorDeviceText("Xiaomi", "Xiaomi Redmi 9A", "10"))
        assertEquals("- · Android -", proctorDeviceText("", "", ""))
    }

    private fun participant(
        displayName: String? = null,
        username: String? = null,
        userId: String? = null,
        kodeKelas: String? = null,
        kodeRuang: String? = null
    ) = ExamParticipantContext(
        displayName = displayName,
        username = username,
        userId = userId,
        kodeKelas = kodeKelas,
        kodeRuang = kodeRuang,
        sourceKey = "test"
    )

    private fun state(
        verdict: NetworkReadinessVerdict = NetworkReadinessVerdict.ConnectedStable,
        serverStatus: ExamServerFooterStatus = ExamServerFooterStatus.Online,
        shieldStatus: ExamFooterShieldStatus = ExamFooterShieldStatus.Safe,
        battery: ExamBatteryStatus = ExamBatteryStatus(levelPercent = 15, isCharging = false),
        participantName: String? = null,
        participantGroup: String? = null
    ) = ExamRuntimeChromeState(
        examSessionStarted = true,
        examDisplayName = "Ujian Akhir Semester",
        loadingProgress = 1f,
        webViewErrorMessage = null,
        hasFullscreenCustomView = false,
        useBuiltInExamKeyboard = false,
        showBuiltInExamKeyboard = false,
        hasEditableFocus = false,
        builtInKeyboardShiftEnabled = false,
        networkStatus = NetworkReadinessStatus(
            examStatus = ExamNetworkStatus(
                label = "Online",
                detail = "Wi-Fi https://cbt.sekolah.sch.id",
                isConnected = verdict != NetworkReadinessVerdict.Offline
            ),
            diagnostics = NetworkDiagnostics(
                activeNetworkAvailable = verdict != NetworkReadinessVerdict.Offline,
                transports = listOf("wifi"),
                hasInternetCapability = true,
                isValidated = verdict == NetworkReadinessVerdict.ConnectedStable,
                isCaptivePortal = false,
                isMetered = false,
                isVpnActive = false,
                isAirplaneModeEnabled = false,
                notRoaming = true,
                interfaceName = "wlan0",
                wifi = null,
                cellular = null
            ),
            verdict = verdict,
            transportLabel = "Wi-Fi",
            quickFixReason = null
        ),
        serverStatus = serverStatus,
        batteryStatus = battery,
        shieldStatus = shieldStatus,
        participantName = participantName,
        participantGroup = participantGroup
    )
}
