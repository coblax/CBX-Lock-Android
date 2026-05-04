package com.example.coblaxexamlock

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coblaxexamlock.nativebridge.NativeBridgeBackendMode
import com.example.coblaxexamlock.nativebridge.NativeBridgeTestControl
import com.example.coblaxexamlock.nativebridge.NativeGeofenceBridge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeGeofenceParityTest {
    @After
    fun tearDown() {
        NativeBridgeTestControl.resetBackendModeForTests()
    }

    @Test
    fun nativeLibraryIsAvailableForGeofenceParity() {
        assertTrue(NativeGeofenceBridge.isNativeAvailableForTests())
    }

    @Test
    fun pointInPolygonMatchesReferenceForSquareCases() {
        val cases = listOf(
            GeofencePoint(latitude = 5.0, longitude = 5.0) to true,
            GeofencePoint(latitude = 15.0, longitude = 5.0) to false,
            GeofencePoint(latitude = 0.0, longitude = 5.0) to true,
            GeofencePoint(latitude = 0.0, longitude = 0.0) to true
        )

        cases.forEach { (point, expected) ->
            val nativeResult = GeofenceParityAccess.isPointInPolygonWithBackend(
                point,
                NativeGeofenceFixtures.squarePolygon,
                NativeBridgeBackendMode.ForceNative
            )
            val kotlinResult = GeofenceParityAccess.isPointInPolygonReference(
                point,
                NativeGeofenceFixtures.squarePolygon
            )

            assertEquals(expected, kotlinResult)
            assertEquals(kotlinResult, nativeResult)
        }
    }

    @Test
    fun pointInPolygonMatchesReferenceForConcaveCases() {
        val insidePoint = GeofencePoint(latitude = 2.0, longitude = 6.0)
        val outsidePoint = GeofencePoint(latitude = 6.0, longitude = 6.0)

        val nativeInside = GeofenceParityAccess.isPointInPolygonWithBackend(
            insidePoint,
            NativeGeofenceFixtures.concavePolygon,
            NativeBridgeBackendMode.ForceNative
        )
        val kotlinInside = GeofenceParityAccess.isPointInPolygonReference(
            insidePoint,
            NativeGeofenceFixtures.concavePolygon
        )
        val nativeOutside = GeofenceParityAccess.isPointInPolygonWithBackend(
            outsidePoint,
            NativeGeofenceFixtures.concavePolygon,
            NativeBridgeBackendMode.ForceNative
        )
        val kotlinOutside = GeofenceParityAccess.isPointInPolygonReference(
            outsidePoint,
            NativeGeofenceFixtures.concavePolygon
        )

        assertTrue(kotlinInside)
        assertFalse(kotlinOutside)
        assertEquals(kotlinInside, nativeInside)
        assertEquals(kotlinOutside, nativeOutside)
    }

    @Test
    fun selfIntersectionMatchesReference() {
        val nativeValid = GeofenceParityAccess.isSelfIntersectingWithBackend(
            NativeGeofenceFixtures.squarePolygon,
            NativeBridgeBackendMode.ForceNative
        )
        val kotlinValid = GeofenceParityAccess.isSelfIntersectingReference(
            NativeGeofenceFixtures.squarePolygon
        )
        val nativeBowTie = GeofenceParityAccess.isSelfIntersectingWithBackend(
            NativeGeofenceFixtures.bowTiePolygon,
            NativeBridgeBackendMode.ForceNative
        )
        val kotlinBowTie = GeofenceParityAccess.isSelfIntersectingReference(
            NativeGeofenceFixtures.bowTiePolygon
        )

        assertFalse(kotlinValid)
        assertTrue(kotlinBowTie)
        assertEquals(kotlinValid, nativeValid)
        assertEquals(kotlinBowTie, nativeBowTie)
    }

    @Test
    fun pointToSegmentDistanceMatchesReferenceWithinTolerance() {
        val point = GeofencePoint(latitude = 5.0, longitude = 5.0)
        val segmentStart = GeofencePoint(latitude = 0.0, longitude = 0.0)
        val segmentEnd = GeofencePoint(latitude = 10.0, longitude = 0.0)

        val nativeDistance = GeofenceParityAccess.distancePointToSegmentMetersWithBackend(
            point,
            segmentStart,
            segmentEnd,
            NativeBridgeBackendMode.ForceNative
        )
        val kotlinDistance = GeofenceParityAccess.distancePointToSegmentMetersReference(
            point,
            segmentStart,
            segmentEnd
        )

        assertEquals(kotlinDistance, nativeDistance, 1e-4)
    }

    @Test
    fun closestCircleDistanceMatchesReferenceWithinTolerance() {
        val nativeDistance = GeofenceParityAccess.closestCircleDistanceMetersWithBackend(
            NativeGeofenceFixtures.locationSnapshot,
            NativeGeofenceFixtures.circleCenters,
            NativeBridgeBackendMode.ForceNative
        )
        val kotlinDistance = GeofenceParityAccess.closestCircleDistanceMetersReference(
            NativeGeofenceFixtures.locationSnapshot,
            NativeGeofenceFixtures.circleCenters
        )

        assertEquals(kotlinDistance, nativeDistance, 1.5)
    }
}
