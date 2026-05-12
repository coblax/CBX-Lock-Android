package com.example.coblaxexamlock.runtime

import android.content.pm.ApplicationInfo
import com.example.coblaxexamlock.config.AllowedSystemKeyboardPackagePrefixes
import com.example.coblaxexamlock.config.EmulatorPackagePrefixes
import com.example.coblaxexamlock.config.RootPackageNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageInventoryDetectorTest {
    @Test
    fun screenRecorderKnownPackageIsDetectedFromInventory() {
        val inventory = inventoryOf("com.kimcy929.screenrecorder")

        val matches = findScreenRecorderMatchesFromInventory(inventory)

        assertEquals(1, matches.size)
        assertEquals("com.kimcy929.screenrecorder", matches.single().packageName)
        assertEquals(ScreenRecorderDetectionSource.KnownPackage, matches.single().source)
    }

    @Test
    fun screenRecorderKnownPackageCanUseFallbackRecord() {
        val matches = findScreenRecorderMatchesFromInventory(
            inventory = inventoryOfRecords(),
            fallbackRecordProvider = { packageName ->
                if (packageName == "com.kimcy929.screenrecorder") {
                    InstalledPackageRecord(packageName = packageName, flags = 0, enabled = true)
                } else {
                    null
                }
            }
        )

        assertEquals(1, matches.size)
        assertEquals(ScreenRecorderDetectionSource.KnownPackage, matches.single().source)
    }

    @Test
    fun screenRecorderKeywordUserAppIsDetectedAndSystemAppIsSkipped() {
        val inventory = inventoryOfRecords(
            InstalledPackageRecord("com.example.screen_capture", flags = 0, enabled = true),
            InstalledPackageRecord(
                packageName = "com.oem.screenrecorder",
                flags = ApplicationInfo.FLAG_SYSTEM,
                enabled = true
            )
        )

        val matches = findScreenRecorderMatchesFromInventory(inventory)

        assertEquals(listOf("com.example.screen_capture"), matches.map { it.packageName })
        assertEquals(ScreenRecorderDetectionSource.KeywordScan, matches.single().source)
    }

    @Test
    fun fakeLocationKnownAndKeywordUserAppsAreDetected() {
        val inventory = inventoryOf(
            "com.lexa.fakegps",
            "com.example.mock.location"
        )

        val matches = findSuspiciousFakeLocationPackageRecordsFromInventory(inventory)

        assertEquals(
            listOf("com.lexa.fakegps", "com.example.mock.location"),
            matches.map { it.packageName }
        )
    }

    @Test
    fun fakeLocationKeywordSystemAppIsSkipped() {
        val inventory = inventoryOf(
            "com.oem.fakegps" to ApplicationInfo.FLAG_SYSTEM
        )

        val matches = findSuspiciousFakeLocationPackageRecordsFromInventory(inventory)

        assertTrue(matches.isEmpty())
    }

    @Test
    fun virtualEnvironmentEmulatorPackagePrefixIsDetected() {
        val packageName = EmulatorPackagePrefixes.first() + "launcher"
        val inventory = inventoryOf(packageName)

        val emulatorPackages = findEmulatorPackagesFromInventory(inventory)

        assertEquals(listOf(packageName), emulatorPackages)
    }

    @Test
    fun inventoryBuildsLookupByPackageName() {
        val inventory = inventoryOf("com.example.one", "com.example.two")

        assertTrue(inventory.byPackageName.containsKey("com.example.one"))
        assertFalse(inventory.byPackageName.containsKey("com.example.missing"))
    }

    @Test
    fun inventoryHelpersFindPackagesAndSystemFlags() {
        val updatedSystemPackage = "com.example.updatedsystem"
        val inventory = inventoryOfRecords(
            InstalledPackageRecord("com.example.user", flags = 0, enabled = true),
            InstalledPackageRecord(
                packageName = "com.example.system",
                flags = ApplicationInfo.FLAG_SYSTEM,
                enabled = true
            ),
            InstalledPackageRecord(
                packageName = updatedSystemPackage,
                flags = ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
                enabled = true
            )
        )

        assertTrue(inventory.hasPackage("com.example.user"))
        assertFalse(inventory.hasPackage("com.example.missing"))
        assertTrue(inventory.isSystemOrUpdatedSystemPackage("com.example.system"))
        assertTrue(inventory.isSystemOrUpdatedSystemPackage(updatedSystemPackage))
        assertFalse(inventory.isSystemOrUpdatedSystemPackage("com.example.user"))
        assertEquals(
            listOf("com.example.system", updatedSystemPackage),
            inventory.findPackages(
                listOf("com.example.system", "com.example.missing", updatedSystemPackage)
            ).map { record -> record.packageName }
        )
    }

    @Test
    fun rootPackageIsDetectedFromInventoryAndFallback() {
        val rootPackage = RootPackageNames.first()

        assertEquals(
            listOf(rootPackage),
            findRootPackagesFromInventory(inventoryOf(rootPackage))
        )
        assertEquals(
            listOf(rootPackage),
            findRootPackagesFromInventory(inventoryOfRecords()) { packageName ->
                packageName == rootPackage
            }
        )
    }

    @Test
    fun keyboardSystemPackageUsesInventoryAndFallbackMetadata() {
        val systemKeyboard = AllowedSystemKeyboardPackagePrefixes.first() + "unit"
        val updatedSystemKeyboard = AllowedSystemKeyboardPackagePrefixes.first() + "updated"
        val inventory = inventoryOfRecords(
            InstalledPackageRecord(
                packageName = systemKeyboard,
                flags = ApplicationInfo.FLAG_SYSTEM,
                enabled = true
            ),
            InstalledPackageRecord(
                packageName = updatedSystemKeyboard,
                flags = ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
                enabled = true
            )
        )

        assertTrue(isSystemAppPackage(systemKeyboard, inventory))
        assertTrue(isSystemAppPackage(updatedSystemKeyboard, inventory))
        assertTrue(
            isSystemAppPackage(
                packageName = "com.example.fallbackkeyboard",
                packageInventory = inventoryOfRecords(),
                fallbackMetadataProvider = { packageName ->
                    InstalledPackageMetadata(
                        packageName = packageName,
                        enabled = true,
                        systemApp = false,
                        updatedSystemApp = true
                    )
                }
            )
        )
    }

    @Test
    fun keyboardPolicyStillAllowsSystemPrefixAndBlocksUserOrSuspiciousKeyboard() {
        val systemKeyboard = AllowedSystemKeyboardPackagePrefixes.first() + "unit"

        assertTrue(
            isAllowedExamKeyboardPackage(
                packageName = systemKeyboard,
                isSystemKeyboard = true,
                trustedOemDevice = false
            )
        )
        assertFalse(
            isAllowedExamKeyboardPackage(
                packageName = "com.example.inputmethod.custom",
                isSystemKeyboard = false,
                trustedOemDevice = true
            )
        )
        assertFalse(
            isAllowedExamKeyboardPackage(
                packageName = "com.example.chatgpt.keyboard",
                isSystemKeyboard = true,
                trustedOemDevice = true
            )
        )
    }

    private fun inventoryOf(vararg packages: String): InstalledPackageInventory {
        return InstalledPackageInventory(
            records = packages.map { packageName ->
                InstalledPackageRecord(packageName = packageName, flags = 0, enabled = true)
            }
        )
    }

    private fun inventoryOf(vararg packages: Pair<String, Int>): InstalledPackageInventory {
        return InstalledPackageInventory(
            records = packages.map { (packageName, flags) ->
                InstalledPackageRecord(packageName = packageName, flags = flags, enabled = true)
            }
        )
    }

    private fun inventoryOfRecords(vararg records: InstalledPackageRecord): InstalledPackageInventory {
        return InstalledPackageInventory(records = records.toList())
    }
}
