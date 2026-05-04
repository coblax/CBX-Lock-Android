package com.example.coblaxexamlock

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.coblaxexamlock.nativebridge.NativeBridgeBackendMode
import com.example.coblaxexamlock.nativebridge.NativeBridgeTestControl
import com.example.coblaxexamlock.nativebridge.NativeSecurityBridge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeReverseEngineeringParityTest {
    @After
    fun tearDown() {
        NativeBridgeTestControl.resetBackendModeForTests()
    }

    @Test
    fun nativeLibraryIsAvailableForSecurityParity() {
        assertTrue(NativeSecurityBridge.isNativeAvailableForTests())
    }

    @Test
    fun tracerPidMatchesKotlinReference() {
        val nativeTracerPid = ReverseEngineeringGuard.ParityAccess.readTracerPidWithBackend(
            NativeBridgeBackendMode.ForceNative
        )
        val kotlinTracerPid = ReverseEngineeringGuard.ParityAccess.readTracerPidReference()

        assertEquals(kotlinTracerPid, nativeTracerPid)
    }

    @Test
    fun procMapsMarkerHitsMatchKotlinReferenceInInputOrder() {
        val nativeHits = ReverseEngineeringGuard.ParityAccess.scanProcMapsWithBackend(
            NativeBridgeBackendMode.ForceNative
        ).toList()
        val kotlinHits = ReverseEngineeringGuard.ParityAccess.scanProcMapsReference().toList()

        assertEquals(kotlinHits, nativeHits)
    }

    @Test
    fun inspectResultMatchesBetweenNativeAndKotlinFallback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val nativeResult = ReverseEngineeringGuard.ParityAccess.inspectWithBackend(
            context,
            NativeBridgeBackendMode.ForceNative
        )
        val kotlinResult = ReverseEngineeringGuard.ParityAccess.inspectWithBackend(
            context,
            NativeBridgeBackendMode.ForceKotlinFallback
        )

        assertEquals(kotlinResult, nativeResult)
    }
}
