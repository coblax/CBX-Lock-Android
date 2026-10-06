package com.coblax.examlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrExportTuningTest {
    @Test
    fun normalExportKeepsFullResolution() {
        val spec = calculateQrExportBitmapSpec(LowRamProfile())

        assertEquals(1440, spec.widthPx)
        assertEquals(2120, spec.heightPx)
        assertEquals(760, spec.qrSizePx)
        assertEquals(1440 * 2120 * 2, spec.estimatedBitmapBytes)
    }

    @Test
    fun lowRamExportReducesBitmapMemory() {
        val normal = calculateQrExportBitmapSpec(LowRamProfile())
        val lowRam = calculateQrExportBitmapSpec(
            LowRamProfile(enabled = true, severe = false)
        )

        assertEquals(1037, lowRam.widthPx)
        assertEquals(1526, lowRam.heightPx)
        assertEquals(547, lowRam.qrSizePx)
        assertTrue(lowRam.estimatedBitmapBytes < normal.estimatedBitmapBytes)
    }

    @Test
    fun severeExportStaysReadableButMuchSmaller() {
        val normal = calculateQrExportBitmapSpec(LowRamProfile())
        val severe = calculateQrExportBitmapSpec(
            LowRamProfile(enabled = true, severe = true)
        )

        assertEquals(500, severe.qrSizePx)
        assertTrue(severe.estimatedBitmapBytes < normal.estimatedBitmapBytes / 2)
    }

    /** Width, height and QR keep the poster's proportions, so its layout stays centered. */
    @Test
    fun everyExportIsTheSamePosterScaled() {
        listOf(
            LowRamProfile(),
            LowRamProfile(enabled = true),
            LowRamProfile(enabled = true, severe = true)
        ).forEach { profile ->
            val spec = calculateQrExportBitmapSpec(profile)
            assertEquals(1440f, spec.widthPx / spec.scale, 1.5f)
            assertEquals(2120f, spec.heightPx / spec.scale, 1.5f)
            assertEquals(760f, spec.qrSizePx / spec.scale, 1.5f)
        }
    }

    /** A dense QR keeps five pixels a module even on the smallest export, up to full size. */
    @Test
    fun aDenseQrRaisesTheExportScale() {
        val severe = calculateQrExportBitmapSpec(LowRamProfile(enabled = true, severe = true), qrModules = 181)

        assertEquals(760, severe.qrSizePx)
        assertTrue(calculateQrExportBitmapSpec(LowRamProfile(enabled = true, severe = true), qrModules = 109).qrSizePx >= 109 * 5)
        assertEquals(760, calculateQrExportBitmapSpec(LowRamProfile(), qrModules = 400).qrSizePx)
    }
}
