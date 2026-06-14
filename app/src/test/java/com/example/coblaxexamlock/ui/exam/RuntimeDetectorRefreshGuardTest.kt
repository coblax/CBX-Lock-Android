package com.example.coblaxexamlock.ui.exam

import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeDetectorRefreshGuardTest {
    @Test
    fun detectorRefreshGuardBlocksOverlapUntilReleased() {
        val inFlight = AtomicBoolean(false)

        assertTrue(tryEnterRuntimeDetectorRefresh(inFlight))
        assertFalse(tryEnterRuntimeDetectorRefresh(inFlight))

        exitRuntimeDetectorRefresh(inFlight)

        assertTrue(tryEnterRuntimeDetectorRefresh(inFlight))
    }

    @Test
    fun detectorRefreshGuardAllowsUnguardedCallers() {
        assertTrue(tryEnterRuntimeDetectorRefresh(null))
    }
}
