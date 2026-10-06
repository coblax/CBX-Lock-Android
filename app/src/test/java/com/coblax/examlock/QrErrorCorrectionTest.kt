package com.coblax.examlock

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrErrorCorrectionTest {
    private val everydayExam = ExamQrPayload(
        examUrl = "https://cbt.sekolah.sch.id/",
        examName = "PAS Matematika",
        startDateTime = "04/10/2026 07:00",
        endDateTime = "04/10/2026 09:00",
        timezoneId = "Asia/Jakarta"
    )

    /** The densest code Custom QR can make: 15 corners, every bypass, a minimum version. */
    private val densestExam = everydayExam.copy(
        examUrl = "https://cbt.smkn1contoh.sch.id/ujian/semester-ganjil/kelas-12/matematika/token",
        examName = "Penilaian Akhir Semester Matematika Kelas XII",
        locationPolicy = ExamQrLocationPolicy(
            shapeType = GeofenceShapeType.Polygon,
            vertices = (0 until 15).map { index ->
                GeofenceVertex("-7.${123456 + index * 731}", "110.${123456 + index * 917}")
            }
        ),
        securityBypasses = ExamQrSecurityBypass.entries.toSet(),
        minAppVersionCode = 378,
        minAppVersionName = "3.3.6",
        appUpdateUrl = "https://github.com/coblax/CBX-Lock-Android/releases/latest"
    )

    @Test
    fun everydayExamsKeepTheStrongestErrorCorrection() {
        val qr = ExamQrCodec.encrypt(everydayExam)
        assertEquals(ErrorCorrectionLevel.H, QrMaskSelector.errorCorrectionFor(qr))
    }

    /**
     * At H this code was version 37, 165 modules a side, and photos of its poster often
     * failed. It now steps down until it is no denser than version 27.
     */
    @Test
    fun theDensestExamStepsDownToStayPhotographable() {
        val qr = ExamQrCodec.encrypt(densestExam)
        assertEquals(ErrorCorrectionLevel.M, QrMaskSelector.errorCorrectionFor(qr))
        val modulesWithQuietZone = QrCodeGenerator.moduleCount(qr)
        assertTrue("modules=$modulesWithQuietZone", modulesWithQuietZone in 1..(125 + 4))
        assertTrue(QrMaskSelector.isReadableByScanner(qr, QrMaskSelector.readableMaskFor(qr)))
    }

    @Test
    fun theLevelNeverDropsBelowM() {
        val levels = listOf(everydayExam, densestExam).map { exam ->
            QrMaskSelector.errorCorrectionFor(ExamQrCodec.encrypt(exam))
        }
        assertTrue(levels.none { it == ErrorCorrectionLevel.L })
    }
}
