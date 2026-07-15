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
        assertEquals(1440 * 2120 * 4, spec.estimatedBitmapBytes)
    }

    @Test
    fun lowRamExportReducesBitmapMemory() {
        val normal = calculateQrExportBitmapSpec(LowRamProfile())
        val lowRam = calculateQrExportBitmapSpec(
            LowRamProfile(enabled = true, severe = false)
        )

        assertEquals(1036, lowRam.widthPx)
        assertEquals(1526, lowRam.heightPx)
        assertEquals(547, lowRam.qrSizePx)
        assertTrue(lowRam.widthPx < normal.widthPx)
        assertTrue(lowRam.heightPx < normal.heightPx)
        assertTrue(lowRam.qrSizePx >= 500)
        assertTrue(lowRam.estimatedBitmapBytes < normal.estimatedBitmapBytes)
    }

    @Test
    fun severeExportStaysReadableButMuchSmaller() {
        val normal = calculateQrExportBitmapSpec(LowRamProfile())
        val severe = calculateQrExportBitmapSpec(
            LowRamProfile(enabled = true, severe = true)
        )

        assertEquals(900, severe.widthPx)
        assertEquals(1320, severe.heightPx)
        assertEquals(500, severe.qrSizePx)
        assertTrue(severe.estimatedBitmapBytes < normal.estimatedBitmapBytes / 2)
    }
}
