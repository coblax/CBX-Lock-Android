package com.coblax.examlock.ui.admin

import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import com.coblax.examlock.ui.geofence.GeofenceDraftIssue
import com.coblax.examlock.viewmodel.CustomQrDraftState
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomQrDraftRulesTest {
    private val complete = CustomQrDraftState(
        examUrl = "https://exam.example",
        examName = "Final Exam",
        startTime = "30/07/2026 08:00",
        endTime = "30/07/2026 10:00"
    )

    @Test
    fun anUrlWithoutSchemeIsReadAsHttps() {
        assertEquals("https://cbt.sekolah.sch.id", normalizeAdminUrl(" cbt.sekolah.sch.id "))
        assertEquals("http://cbt.sekolah.sch.id", normalizeAdminUrl("http://cbt.sekolah.sch.id"))
        assertEquals("", normalizeAdminUrl("  "))
        assertTrue(isCustomQrExamStepComplete(complete.copy(examUrl = "cbt.sekolah.sch.id")))
        // A school's own http server is allowed; the exam opens with a warning.
        assertTrue(isCustomQrExamStepComplete(complete.copy(examUrl = "http://cbt.sekolah.sch.id")))
        assertFalse(isCustomQrExamStepComplete(complete.copy(examUrl = "ftp://cbt.sekolah.sch.id")))
    }

    @Test
    fun theEndMustComeAfterTheStart() {
        assertNull(customQrScheduleProblem(complete.startTime, complete.endTime))
        assertEquals(
            CustomQrScheduleProblem.EndNotAfterStart,
            customQrScheduleProblem("30/07/2026 10:00", "30/07/2026 08:00")
        )
        assertEquals(
            CustomQrScheduleProblem.EndNotAfterStart,
            customQrScheduleProblem("30/07/2026 10:00", "30/07/2026 10:00")
        )
        assertFalse(isCustomQrExamStepComplete(complete.copy(endTime = "30/07/2026 07:00")))
    }

    @Test
    fun anEndInThePastIsCaughtOnlyWhenTheClockIsGiven() {
        val after = parseCustomQrDateTimeMillis(complete.endTime)!! + 1
        assertNull(customQrScheduleProblem(complete.startTime, complete.endTime))
        assertEquals(
            CustomQrScheduleProblem.AlreadyEnded,
            customQrScheduleProblem(complete.startTime, complete.endTime, nowMillis = after)
        )
    }

    @Test
    fun bothStoredDateFormatsAreRead() {
        assertEquals(
            parseCustomQrDateTimeMillis("30/07/2026 08:00"),
            parseCustomQrDateTimeMillis("2026-07-30 08:00")
        )
        assertNull(parseCustomQrDateTimeMillis("tomorrow"))
    }

    @Test
    fun aPolygonQrCarriesOnlyItsCorners() {
        val draft = complete.copy(
            geofenceEnabled = true,
            geofenceShapeTypeName = GeofenceShapeType.Polygon.name,
            geofenceCenterLat = "-7.1",
            geofenceCenterLng = "110.1",
            geofenceRadiusMeters = "100",
            polygonVertices = listOf(GeofenceVertex(" -7.0 ", "110.0"))
        )
        val policy = buildCustomQrLocationPolicy(draft)

        assertEquals(GeofenceShapeType.Polygon, policy.shapeType)
        assertEquals("", policy.centerLat)
        assertEquals("", policy.radiusMeters)
        assertEquals(listOf(GeofenceVertex("-7.0", "110.0")), policy.vertices)
    }

    @Test
    fun anOlderSingleCenterDraftStillCounts() {
        val draft = complete.copy(
            geofenceEnabled = true,
            geofenceCenterLat = "-7.1",
            geofenceCenterLng = "110.1",
            geofenceRadiusMeters = "100"
        )
        assertEquals(listOf(GeofenceVertex("-7.1", "110.1")), draft.circlePoints)
        assertNull(draft.locationIssue())
        assertEquals(listOf(GeofenceVertex("-7.1", "110.1")), buildCustomQrLocationPolicy(draft).circleCenters)
    }

    @Test
    fun locationModeMapsOntoTheDraftFlags() {
        val polygon = complete.withLocationMode(CustomQrLocationMode.Polygon)
        assertEquals(CustomQrLocationMode.Polygon, polygon.locationMode)
        assertEquals(GeofenceDraftIssue.NeedPoints, polygon.locationIssue())
        val anywhere = polygon.withLocationMode(CustomQrLocationMode.Anywhere)
        assertEquals(CustomQrLocationMode.Anywhere, anywhere.locationMode)
        assertNull(anywhere.locationIssue())
        // Switching away and back keeps the chosen shape.
        assertEquals(GeofenceShapeType.Polygon.name, anywhere.geofenceShapeTypeName)
    }

    @Test
    fun theDatePickerSeesTheLocalDayEarlyInTheMorning() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jakarta"))
            val earlyMorning = Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.OCTOBER, 4, 6, 30)
            }
            val utcDay = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = earlyMorning.toDatePickerUtcMillis()
            }
            assertEquals(4, utcDay.get(Calendar.DAY_OF_MONTH))

            val picked = earlyMorning.withDatePickerDay(utcDay.apply { set(Calendar.DAY_OF_MONTH, 9) }.timeInMillis)
            assertEquals(9, picked.get(Calendar.DAY_OF_MONTH))
            assertEquals(6, picked.get(Calendar.HOUR_OF_DAY))
            assertEquals(30, picked.get(Calendar.MINUTE))
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
