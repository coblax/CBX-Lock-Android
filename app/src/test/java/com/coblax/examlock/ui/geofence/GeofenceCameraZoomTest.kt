package com.coblax.examlock.ui.geofence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceCameraZoomTest {
    @Test
    fun aSchoolYardFramesAtStreetLevel() {
        // About 220 m a side, in a phone-sized map.
        val zoom = approximateBoundsZoom(0.002, 0.002, viewWidthDp = 360f, viewHeightDp = 300f)
        assertTrue("zoom=$zoom", zoom in 16f..19f)
    }

    @Test
    fun theWholeCountryFramesFarOut() {
        val zoom = approximateBoundsZoom(17.0, 46.0, viewWidthDp = 360f, viewHeightDp = 600f)
        assertTrue("zoom=$zoom", zoom in 2f..5f)
    }

    /**
     * From the country overview (zoom 4.5) to a school is a jump of a dozen levels: far past
     * the few that are still animated, so the camera moves there directly.
     */
    @Test
    fun framingASchoolFromTheCountryViewIsALongJump() {
        val school = approximateBoundsZoom(0.002, 0.002, viewWidthDp = 360f, viewHeightDp = 300f)
        assertTrue(school - 4.5f > 4f)
    }

    @Test
    fun aDegenerateBoxFallsBackToPointZoom() {
        assertEquals(PointZoom, approximateBoundsZoom(0.0, 0.0, viewWidthDp = 360f, viewHeightDp = 300f))
    }
}
