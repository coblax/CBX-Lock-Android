package com.coblax.examlock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamDeviceOwnerCleanupTest {
    private fun status(deviceOwner: Boolean, restrictionActive: Boolean) = defaultDpcRuntimeStatus().copy(
        deviceOwner = deviceOwner,
        createWindowsRestrictionActive = restrictionActive
    )

    /**
     * An exam that crashed or was force-stopped never cleared the restriction it set, and a
     * device owner then blocked every app's overlays and toasts until someone noticed.
     */
    @Test
    fun leftoverRestrictionIsClearedOnlyWhereTheAppIsDeviceOwner() {
        assertTrue(shouldClearStaleCreateWindowsRestriction(status(deviceOwner = true, restrictionActive = true)))
        assertFalse(shouldClearStaleCreateWindowsRestriction(status(deviceOwner = true, restrictionActive = false)))
        assertFalse(shouldClearStaleCreateWindowsRestriction(status(deviceOwner = false, restrictionActive = true)))
    }
}
