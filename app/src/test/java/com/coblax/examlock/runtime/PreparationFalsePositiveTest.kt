package com.coblax.examlock.runtime

import android.content.pm.ApplicationInfo
import android.view.Display
import com.coblax.examlock.model.RootDetectionDetails
import org.junit.Assert.*
import org.junit.Test

/**
 * Every check here decides whether a student may start an exam at all, so a false
 * positive is not cosmetic: it locks a pupil out of a test they are entitled to sit.
 * These cases pin the real-hardware profiles that used to be refused, alongside the
 * emulator/root profiles that must still be caught.
 */
class PreparationFalsePositiveTest {

    // --- Emulator scoring -------------------------------------------------------

    @Test
    fun oneUnambiguousEmulatorSignalIsStillEnough() {
        // goldfish hardware, a qemu file, a BlueStacks package: each names an emulator.
        assertTrue(resolveVirtualEnvironmentDetected(strongCount = 1, weakCount = 0))
        assertTrue(resolveVirtualEnvironmentDetected(strongCount = 3, weakCount = 2))
    }

    @Test
    fun twoWeakSignalsNoLongerRefuseRealBudgetHardware() {
        // The old rule was score >= 2 with weak signals worth 1 each, so an x86
        // Chromebook (x86 ABI + short sensor list) or a cheap tablet (BOARD "unknown"
        // + short sensor list) scored exactly 2 and was refused as an emulator.
        assertFalse(resolveVirtualEnvironmentDetected(strongCount = 0, weakCount = 1))
        assertFalse(resolveVirtualEnvironmentDetected(strongCount = 0, weakCount = 2))
    }

    @Test
    fun threeCorroboratingWeakSignalsStillCount() {
        assertTrue(resolveVirtualEnvironmentDetected(strongCount = 0, weakCount = 3))
        assertTrue(resolveVirtualEnvironmentDetected(strongCount = 0, weakCount = 4))
    }

    @Test
    fun aCleanDeviceIsNotAnEmulator() {
        assertFalse(resolveVirtualEnvironmentDetected(strongCount = 0, weakCount = 0))
    }

    // --- Root detection ---------------------------------------------------------

    private fun details(
        hasTestKeys: Boolean = false,
        hasSuBinary: Boolean = false,
        foundRootPackages: List<String> = emptyList(),
        rootBinaryPaths: List<String> = emptyList(),
        magiskPaths: List<String> = emptyList(),
        zygiskDetected: Boolean = false,
        xposedBridgeDetected: Boolean = false,
        bootloaderUnlocked: Boolean = false,
        selinuxEnabled: Boolean? = true,
        selinuxEnforced: Boolean? = true,
        dangerousSystemProperties: List<String> = emptyList()
    ) = RootDetectionDetails(
        hasTestKeys = hasTestKeys,
        hasSuBinary = hasSuBinary,
        foundRootPackages = foundRootPackages,
        rootBinaryPaths = rootBinaryPaths,
        magiskPaths = magiskPaths,
        zygiskDetected = zygiskDetected,
        xposedBridgeDetected = xposedBridgeDetected,
        verifiedBootState = "green",
        vbmetaDeviceState = "locked",
        flashLocked = "1",
        bootloaderUnlocked = bootloaderUnlocked,
        selinuxEnabled = selinuxEnabled,
        selinuxEnforced = selinuxEnforced,
        dangerousSystemProperties = dangerousSystemProperties,
        roDebuggable = "0",
        roSecure = "1",
        roAdbSecure = "1",
        roBuildType = "user"
    )

    @Test
    fun stockBudgetRomWithTestKeysIsNotRooted() {
        // ro.build.tags=test-keys ships on genuine retail ROMs from smaller OEMs,
        // which is most of this app's install base.
        assertFalse(isDeviceRooted(details(hasTestKeys = true)))
    }

    @Test
    fun aStrayBusyboxIsNotRooted() {
        // busybox ships on Android TV boxes and several MediaTek vendor images.
        for (path in listOf(
            "/system/bin/busybox", "/system/xbin/busybox", "/vendor/bin/busybox",
            "/system/sbin/busybox", "/sbin/busybox", "/data/adb/busybox"
        )) {
            assertFalse(path, isDeviceRooted(details(rootBinaryPaths = listOf(path))))
        }
    }

    @Test
    fun testKeysTogetherWithBusyboxIsRooted() {
        assertTrue(
            isDeviceRooted(
                details(hasTestKeys = true, rootBinaryPaths = listOf("/system/xbin/busybox"))
            )
        )
    }

    @Test
    fun testKeysOnAPermissiveDeviceIsRooted() {
        assertTrue(isDeviceRooted(details(hasTestKeys = true, selinuxEnforced = false)))
    }

    @Test
    fun genuineRootEvidenceIsStillCaught() {
        assertTrue("su binary", isDeviceRooted(details(hasSuBinary = true)))
        assertTrue(
            "su path",
            isDeviceRooted(details(rootBinaryPaths = listOf("/system/xbin/su")))
        )
        assertTrue(
            "superuser apk",
            isDeviceRooted(details(rootBinaryPaths = listOf("/system/app/Superuser.apk")))
        )
        assertTrue(
            "root manager",
            isDeviceRooted(details(foundRootPackages = listOf("com.topjohnwu.magisk")))
        )
        assertTrue("magisk", isDeviceRooted(details(magiskPaths = listOf("/data/adb/magisk"))))
        assertTrue("zygisk", isDeviceRooted(details(zygiskDetected = true)))
        assertTrue("xposed", isDeviceRooted(details(xposedBridgeDetected = true)))
        assertTrue("bootloader", isDeviceRooted(details(bootloaderUnlocked = true)))
        assertTrue("selinux off", isDeviceRooted(details(selinuxEnabled = false)))
        assertTrue(
            "dangerous props",
            isDeviceRooted(details(dangerousSystemProperties = listOf("ro.debuggable=1")))
        )
    }

    @Test
    fun aCleanStockDeviceIsNotRooted() {
        assertFalse(isDeviceRooted(details()))
    }

    @Test
    fun onlyBusyboxCountsAsCorroboratingRatherThanConclusive() {
        assertTrue(isCorroboratingRootBinaryPath("/system/xbin/busybox"))
        assertTrue(isCorroboratingRootBinaryPath("/sbin/BusyBox"))
        assertFalse(isCorroboratingRootBinaryPath("/system/xbin/su"))
        assertFalse(isCorroboratingRootBinaryPath("/system/bin/daemonsu"))
        assertFalse(isCorroboratingRootBinaryPath("/system/app/SuperSU/SuperSU.apk"))
    }

    // --- Screen recorder --------------------------------------------------------

    private fun inventory(vararg records: InstalledPackageRecord) =
        InstalledPackageInventory(records.toList())

    private fun userApp(name: String, enabled: Boolean = true) =
        InstalledPackageRecord(packageName = name, flags = 0, enabled = enabled)

    private fun systemApp(name: String, enabled: Boolean = true) =
        InstalledPackageRecord(
            packageName = name,
            flags = ApplicationInfo.FLAG_SYSTEM,
            enabled = enabled
        )

    @Test
    fun aDisabledRecorderNoLongerRefusesTheExam() {
        // A disabled package cannot record, and a student inside lock task cannot
        // reach Settings to switch it back on.
        val known = inventory(userApp("com.kimcy929.screenrecorder", enabled = false))
        assertTrue(findScreenRecorderMatchesFromInventory(known).isEmpty())

        val keyword = inventory(userApp("com.example.screenrecord", enabled = false))
        assertTrue(findScreenRecorderMatchesFromInventory(keyword).isEmpty())
    }

    @Test
    fun anEnabledRecorderIsStillCaught() {
        val known = inventory(userApp("com.kimcy929.screenrecorder"))
        assertEquals(
            listOf("com.kimcy929.screenrecorder"),
            findScreenRecorderMatchesFromInventory(known).map { it.packageName }
        )
        val keyword = inventory(userApp("com.example.screen_capture"))
        assertEquals(
            listOf("com.example.screen_capture"),
            findScreenRecorderMatchesFromInventory(keyword).map { it.packageName }
        )
    }

    @Test
    fun bundledOemAndSystemRecordersStayIgnored() {
        val records = inventory(
            userApp("com.miui.screenrecorder"),
            systemApp("com.android.systemui.screenrecord"),
            userApp("com.example.notes")
        )
        assertTrue(findScreenRecorderMatchesFromInventory(records).isEmpty())
    }

    // --- External display -------------------------------------------------------

    @Test
    fun onlyDisplaysThatAreOnCountAsAMirror() {
        assertTrue(isActiveExternalDisplayState(Display.STATE_ON))
        assertTrue(isActiveExternalDisplayState(Display.STATE_VR))
        assertTrue(isActiveExternalDisplayState(Display.STATE_ON_SUSPEND))
        // Virtual displays left behind by other apps, idle cast targets and overlay
        // displays from developer options report these and cannot show the exam.
        assertFalse(isActiveExternalDisplayState(Display.STATE_OFF))
        assertFalse(isActiveExternalDisplayState(Display.STATE_UNKNOWN))
        assertFalse(isActiveExternalDisplayState(Display.STATE_DOZE))
        assertFalse(isActiveExternalDisplayState(Display.STATE_DOZE_SUSPEND))
    }
}
