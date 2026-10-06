package com.coblax.examlock.ui.geofence

import com.coblax.examlock.ExamQrCodec
import com.coblax.examlock.ExamQrLocationPolicy
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.ExamQrSecurityBypass
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import com.coblax.examlock.QrCodeGenerator
import com.coblax.examlock.isSelfIntersectingPolygon
import com.coblax.examlock.GeofencePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceDraftTest {
    @Test
    fun pastedCoordinatesInTheUsualShapesAreRead() {
        val expected = GeofenceCoordinate(-7.025123, 110.417456)
        listOf(
            "-7.025123, 110.417456",
            "-7.025123,110.417456",
            "  -7.025123   110.417456 ",
            "-7.025123; 110.417456",
            "https://www.google.com/maps/place/SMK+Negeri+1/@-7.025123,110.417456,17z/data=!3m1",
            "https://maps.google.com/?q=-7.025123,110.417456"
        ).forEach { text ->
            assertEquals(text, expected, parseCoordinateText(text))
        }
    }

    @Test
    fun degreesMinutesSecondsAreRead() {
        val parsed = parseCoordinateText("7°01'30.4\"S 110°25'01.6\"E")!!
        assertEquals(-7.025111, parsed.latitude, 1e-5)
        assertEquals(110.417111, parsed.longitude, 1e-5)
    }

    @Test
    fun textThatIsNotACoordinateIsRefused() {
        listOf("", "abc", "-7.0251", "95.1, 110.2", "-7.1, 190.5", "https://maps.app.goo.gl/AbC123").forEach { text ->
            assertNull(text, parseCoordinateText(text))
        }
    }

    @Test
    fun circleNeedsAPointAndAUsableRadius() {
        val point = GeofenceVertex("-7.0251", "110.4171")
        assertEquals(GeofenceDraftIssue.NeedPoints, geofenceDraftIssue(GeofenceShapeType.Circle, emptyList(), "100"))
        assertEquals(GeofenceDraftIssue.InvalidRadius, geofenceDraftIssue(GeofenceShapeType.Circle, listOf(point), ""))
        assertEquals(GeofenceDraftIssue.RadiusTooSmall, geofenceDraftIssue(GeofenceShapeType.Circle, listOf(point), "5"))
        assertEquals(GeofenceDraftIssue.RadiusTooLarge, geofenceDraftIssue(GeofenceShapeType.Circle, listOf(point), "60000"))
        assertEquals(
            GeofenceDraftIssue.InvalidPoint,
            geofenceDraftIssue(GeofenceShapeType.Circle, listOf(point, GeofenceVertex("", "")), "100")
        )
        assertEquals(
            GeofenceDraftIssue.TooManyPoints,
            geofenceDraftIssue(GeofenceShapeType.Circle, List(MaxCircleCenters + 1) { point }, "100")
        )
        assertNull(geofenceDraftIssue(GeofenceShapeType.Circle, listOf(point), "100"))
    }

    @Test
    fun polygonIssuesAreNamed() {
        val square = square()
        assertNull(geofenceDraftIssue(GeofenceShapeType.Polygon, square, ""))
        assertEquals(GeofenceDraftIssue.NeedPoints, geofenceDraftIssue(GeofenceShapeType.Polygon, square.take(2), ""))
        val bowTie = listOf(square[0], square[2], square[1], square[3])
        assertEquals(GeofenceDraftIssue.PolygonCrossing, geofenceDraftIssue(GeofenceShapeType.Polygon, bowTie, ""))
        val line = listOf(GeofenceVertex("-7.0", "110.0"), GeofenceVertex("-7.0", "110.001"), GeofenceVertex("-7.0", "110.002"))
        assertEquals(GeofenceDraftIssue.PolygonTooSmall, geofenceDraftIssue(GeofenceShapeType.Polygon, line, ""))
    }

    @Test
    fun fixOrderUntanglesACrossedPolygon() {
        val square = square()
        val bowTie = listOf(square[0], square[2], square[1], square[3])
        val ordered = orderPolygonAroundCenter(bowTie)

        assertFalse(isSelfIntersectingPolygon(ordered.map { GeofencePoint(it.latitude.toDouble(), it.longitude.toDouble()) }))
        assertNull(geofenceDraftIssue(GeofenceShapeType.Polygon, ordered, ""))
        assertEquals(bowTie.toSet(), ordered.toSet())
    }

    @Test
    fun coordinatesAreStoredWithSixDecimals() {
        assertEquals(GeofenceVertex("-7.123457", "110.000000"), GeofenceCoordinate(-7.1234567, 110.0).toVertex())
    }

    /** The most a polygon may hold still fits one QR beside long URLs and every option. */
    @Test
    fun theLargestAllowedPolygonFitsInOneQr() {
        val vertices = (0 until MaxPolygonPoints).map { index ->
            GeofenceCoordinate(-7.123456 - index * 0.000731, 110.123456 + index * 0.000917).toVertex()
        }
        val payload = ExamQrPayload(
            examUrl = "https://cbt.smkn1contoh.sch.id/ujian/semester-ganjil/kelas-12/matematika/token",
            examName = "Penilaian Akhir Semester Matematika Kelas XII",
            startDateTime = "04/10/2026 07:00",
            endDateTime = "04/10/2026 09:00",
            locationPolicy = ExamQrLocationPolicy(shapeType = GeofenceShapeType.Polygon, vertices = vertices),
            securityBypasses = ExamQrSecurityBypass.entries.toSet(),
            minAppVersionCode = 377,
            minAppVersionName = "3.3.5",
            appUpdateUrl = "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz0123456/view?usp=sharing"
        )
        assertTrue(QrCodeGenerator.canEncode(ExamQrCodec.encrypt(payload)))
    }

    @Test
    fun theRadiusSliderGivesTheCommonRadiiRoom() {
        assertEquals(0f, radiusToSliderPosition(MinCircleRadiusMeters), 0.001f)
        assertEquals(1f, radiusToSliderPosition(RadiusSliderMaxMeters), 0.001f)
        // 100 m sits about a third of the way along, not in the first 4%.
        assertTrue(radiusToSliderPosition(100) in 0.3f..0.4f)
        listOf(25, 100, 150, 300, 750, 1500).forEach { radius ->
            assertEquals(radius, sliderPositionToRadius(radiusToSliderPosition(radius)))
        }
    }

    @Test
    fun radiusTextIsReadAsWholeMeters() {
        assertEquals(150, parseRadiusMeters("150"))
        assertEquals(151, parseRadiusMeters(" 150,6 "))
        assertNull(parseRadiusMeters("0"))
        assertNull(parseRadiusMeters("abc"))
    }

    private fun square() = listOf(
        GeofenceVertex("-7.0000", "110.0000"),
        GeofenceVertex("-7.0000", "110.0010"),
        GeofenceVertex("-7.0010", "110.0010"),
        GeofenceVertex("-7.0010", "110.0000")
    )
}
