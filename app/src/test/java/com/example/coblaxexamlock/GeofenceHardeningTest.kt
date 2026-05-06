package com.example.coblaxexamlock

import com.example.coblaxexamlock.nativebridge.NativeBridgeBackendMode
import com.example.coblaxexamlock.nativebridge.NativeBridgeTestControl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GeofenceHardeningTest {
    @Before
    fun forceKotlinBackend() {
        NativeBridgeTestControl.setBackendModeForTests(NativeBridgeBackendMode.ForceKotlinFallback)
    }

    @After
    fun resetBackend() {
        NativeBridgeTestControl.resetBackendModeForTests()
    }

    @Test
    fun circleRejectsNanCoordinatesWithoutThrowing() {
        val result = parseGeofenceConfig(
            enabled = true,
            centerLatRaw = "NaN",
            centerLngRaw = "106.8",
            radiusMetersRaw = "100"
        )

        assertNull(result.config)
        assertEquals("invalid_latitude", result.error)
    }

    @Test
    fun polygonRejectsNanVerticesWithoutThrowing() {
        val result = parseGeofenceConfig(
            enabled = true,
            centerLatRaw = "",
            centerLngRaw = "",
            radiusMetersRaw = "",
            shapeType = GeofenceShapeType.Polygon,
            polygonVertices = listOf(
                GeofenceVertex("NaN", "106.8"),
                GeofenceVertex("-6.2", "106.9"),
                GeofenceVertex("-6.3", "106.8")
            )
        )

        assertNull(result.config)
        assertEquals("invalid_polygon_latitude", result.error)
    }

    @Test
    fun polygonRejectsDegenerateZeroAreaShape() {
        val result = parseGeofenceConfig(
            enabled = true,
            centerLatRaw = "",
            centerLngRaw = "",
            radiusMetersRaw = "",
            shapeType = GeofenceShapeType.Polygon,
            polygonVertices = listOf(
                GeofenceVertex("-6.2", "106.8"),
                GeofenceVertex("-6.2", "106.8"),
                GeofenceVertex("-6.2", "106.8")
            )
        )

        assertNull(result.config)
        assertEquals("polygon_degenerate", result.error)
    }

    @Test
    fun polygonValidShapeStillParses() {
        val result = parseGeofenceConfig(
            enabled = true,
            centerLatRaw = "",
            centerLngRaw = "",
            radiusMetersRaw = "",
            shapeType = GeofenceShapeType.Polygon,
            polygonVertices = listOf(
                GeofenceVertex("-6.2000", "106.8000"),
                GeofenceVertex("-6.2000", "106.8010"),
                GeofenceVertex("-6.2010", "106.8000")
            )
        )

        assertNull(result.error)
        assertNotNull(result.config)
        assertEquals(GeofenceShapeType.Polygon, result.config?.shapeType)
    }
}
