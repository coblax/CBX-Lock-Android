package com.example.coblaxexamlock.ui.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceVendorChecklistTest {
    @Test
    fun resolvesXiaomiFamily() {
        val checklist = resolveDeviceVendorChecklist(
            manufacturer = "Xiaomi",
            brand = "POCO"
        )

        assertEquals(DeviceVendorFamily.Xiaomi, checklist.family)
        assertTrue(checklist.items.any { it.title.contains("Autostart") })
    }

    @Test
    fun resolvesSamsungFamily() {
        val checklist = resolveDeviceVendorChecklist(
            manufacturer = "samsung",
            brand = "Samsung"
        )

        assertEquals(DeviceVendorFamily.Samsung, checklist.family)
        assertTrue(checklist.items.any { it.title.contains("Sleeping") })
    }

    @Test
    fun genericStillIncludesLowRamSafeBasics() {
        val checklist = resolveDeviceVendorChecklist(
            manufacturer = "unknown",
            brand = "unknown"
        )

        assertEquals(DeviceVendorFamily.Generic, checklist.family)
        assertTrue(checklist.items.any { it.title == "Battery saver" })
        assertTrue(checklist.items.any { it.title == "Location precision" })
    }
}
