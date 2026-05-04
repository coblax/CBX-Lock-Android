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
class NativeIntegrityGuardParityTest {
    @After
    fun tearDown() {
        NativeBridgeTestControl.resetBackendModeForTests()
    }

    @Test
    fun nativeLibraryIsAvailableForIntegrityParity() {
        assertTrue(NativeSecurityBridge.isNativeAvailableForTests())
    }

    @Test
    fun dexHashMatchesKotlinReference() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val nativeHash = IntegrityGuard.ParityAccess.readDexHashWithBackend(
            context,
            NativeBridgeBackendMode.ForceNative
        )
        val kotlinHash = IntegrityGuard.ParityAccess.readDexHashReference(context)

        assertEquals(kotlinHash, nativeHash)
    }

    @Test
    fun systemPropertiesMatchKotlinReference() {
        val keys = listOf("ro.debuggable", "ro.secure", "ro.adb.secure")

        keys.forEach { key ->
            val nativeValue = IntegrityGuard.ParityAccess.getSystemPropertyWithBackend(
                key,
                NativeBridgeBackendMode.ForceNative
            )
            val kotlinValue = IntegrityGuard.ParityAccess.getSystemPropertyReference(key)
            assertEquals("Mismatch for $key", kotlinValue, nativeValue)
        }
    }

    @Test
    fun fullIntegrityCheckMatchesBetweenNativeAndKotlinFallback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val nativeResult = IntegrityGuard.ParityAccess.checkWithBackend(
            context,
            baselineFingerprint = null,
            backendMode = NativeBridgeBackendMode.ForceNative
        )
        val kotlinResult = IntegrityGuard.ParityAccess.checkWithBackend(
            context,
            baselineFingerprint = null,
            backendMode = NativeBridgeBackendMode.ForceKotlinFallback
        )

        assertEquals(kotlinResult, nativeResult)
    }
}
