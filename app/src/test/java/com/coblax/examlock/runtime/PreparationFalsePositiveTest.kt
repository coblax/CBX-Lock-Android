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
    fun anUnreadableEnforceFlagMeansEnforcing() {
        // Android 9+ refuses apps a read of /sys/fs/selinux/enforce; only an enforcing
        // policy can do that, a permissive one just logs it.
        assertEquals(true, resolveSelinuxEnforced(nodeValue = null, accessDenied = true))
        assertEquals(true, resolveSelinuxEnforced(nodeValue = "1\n", accessDenied = false))
        assertEquals(false, resolveSelinuxEnforced(nodeValue = "0", accessDenied = false))
        assertNull(resolveSelinuxEnforced(nodeValue = null, accessDenied = false))
        assertNull(resolveSelinuxEnforced(nodeValue = "", accessDenied = false))
    }

    @Test
    fun testKeysOnAnAndroid9PhoneThatHidesTheEnforceFlagIsNotRooted() {
        val stock = details(
            hasTestKeys = true,
            selinuxEnforced = resolveSelinuxEnforced(nodeValue = null, accessDenied = true)
        )
        assertFalse(isDeviceRooted(stock))
        assertFalse(isSelinuxPermissive(stock))
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

    // --- Airplane mode -----------------------------------------------------------

    private fun network(airplane: Boolean) = com.coblax.examlock.model.NetworkDiagnostics(
        activeNetworkAvailable = true,
        transports = listOf("WIFI"),
        hasInternetCapability = true,
        isValidated = true,
        isCaptivePortal = false,
        isMetered = false,
        isVpnActive = false,
        isAirplaneModeEnabled = airplane,
        notRoaming = null,
        interfaceName = "wlan0",
        wifi = null,
        cellular = null
    )

    @Test
    fun airplaneModeWithWifiBackOnIsOnline() {
        // Students switch airplane mode on to silence calls, then turn Wi-Fi back on.
        assertEquals(
            com.coblax.examlock.model.NetworkReadinessVerdict.ConnectedStable,
            resolveNetworkReadinessVerdict(connected = true, diagnostics = network(airplane = true))
        )
    }

    @Test
    fun airplaneModeStillExplainsANoConnection() {
        assertEquals(
            com.coblax.examlock.model.NetworkReadinessVerdict.AirplaneMode,
            resolveNetworkReadinessVerdict(connected = false, diagnostics = network(airplane = true))
        )
        assertEquals(
            com.coblax.examlock.model.NetworkReadinessVerdict.Offline,
            resolveNetworkReadinessVerdict(connected = false, diagnostics = network(airplane = false))
        )
    }

    // --- Clone apps and virtual containers ---------------------------------------

    private val pkg = "com.coblax.examlock"

    @Test
    fun installedCloneAppsAreNotEmulatorEvidence() {
        val records = inventory(
            userApp("com.lbe.parallel.intl"),
            userApp("com.excelliance.dualaid"),
            userApp("com.ludashi.benchmark")
        )
        assertTrue(findEmulatorPackagesFromInventory(records).isEmpty())
        assertEquals(3, findVirtualSpaceHostPackagesFromInventory(records).size)
    }

    @Test
    fun emulatorGuestPackagesAreStillCaught() {
        val records = inventory(userApp("com.bluestacks.home"))
        assertEquals(listOf("com.bluestacks.home"), findEmulatorPackagesFromInventory(records))
    }

    @Test
    fun normalInstallLayoutsAreNotAContainer() {
        for (dataDir in listOf(
            "/data/user/0/$pkg",
            "/data/data/$pkg",
            "/data/user/10/$pkg",   // work profile
            "/data/user/999/$pkg",  // OEM dual apps
            "/data/user_de/0/$pkg",
            "/mnt/expand/1234-abcd/user/0/$pkg"  // adopted storage
        )) {
            assertTrue(
                dataDir,
                resolveVirtualContainerIndicators(
                    expectedPackageName = pkg,
                    dataDir = dataDir,
                    sourceDir = "/data/app/~~abc==/$pkg-xyz==/base.apk",
                    processName = pkg
                ).isEmpty()
            )
        }
        assertTrue(
            resolveVirtualContainerIndicators(pkg, null, null, "$pkg:remote").isEmpty()
        )
    }

    @Test
    fun androidSevenToNineInstallLayoutIsNotAContainer() {
        // Before Android 8.1 the APK sits in /data/app/<pkg>-1 and, below Android 9, the
        // process name comes from /proc/self/cmdline rather than Application.getProcessName.
        for (sourceDir in listOf(
            "/data/app/$pkg-1/base.apk",
            "/data/app/$pkg-2/base.apk",
            "/mnt/expand/1234-abcd/app/$pkg-1/base.apk"
        )) {
            assertTrue(
                sourceDir,
                resolveVirtualContainerIndicators(
                    expectedPackageName = pkg,
                    dataDir = "/data/user/0/$pkg",
                    sourceDir = sourceDir,
                    processName = pkg
                ).isEmpty()
            )
        }
    }

    @Test
    fun missingReadingsNeverCountAsAContainer() {
        // A field Android would not give us is unknown, not suspicious.
        assertTrue(resolveVirtualContainerIndicators(pkg, null, null, null).isEmpty())
        assertTrue(resolveVirtualContainerIndicators(pkg, "", " ", "").isEmpty())
    }

    @Test
    fun runningInsideACloneAppIsCaught() {
        assertFalse(
            resolveVirtualContainerIndicators(
                expectedPackageName = pkg,
                dataDir = "/data/user/0/com.lbe.parallel.intl/parallel_intl/0/$pkg",
                sourceDir = "/data/app/~~abc==/$pkg-xyz==/base.apk",
                processName = pkg
            ).isEmpty()
        )
        assertFalse(
            resolveVirtualContainerIndicators(
                expectedPackageName = pkg,
                dataDir = "/data/user/0/$pkg",
                sourceDir = "/data/user/0/io.va.exposed/virtual/data/app/$pkg/base.apk",
                processName = pkg
            ).isEmpty()
        )
        assertFalse(
            resolveVirtualContainerIndicators(
                expectedPackageName = pkg,
                dataDir = "/data/user/0/$pkg",
                sourceDir = "/data/app/$pkg-1/base.apk",
                processName = "com.lbe.parallel.intl:p0"
            ).isEmpty()
        )
    }

    // --- Location without a geofence ---------------------------------------------

    private fun fakeLocation(
        monitoringEnabled: Boolean,
        permissionGranted: Boolean = true,
        servicesEnabled: Boolean = true
    ) = com.coblax.examlock.evaluateFakeLocationSecurity(
        monitoringEnabled = monitoringEnabled,
        permissionGranted = permissionGranted,
        locationServicesEnabled = servicesEnabled,
        locationSnapshot = null,
        fixQualityStatus = com.coblax.examlock.evaluateLocationFixQuality(null),
        developerOptionsEnabled = false,
        suspiciousFakeLocationPackages = emptyList(),
        bypassState = com.coblax.examlock.LocationBypassState.Inactive
    )

    private fun geofence(enabled: Boolean) = com.coblax.examlock.GeofenceConfigParseResult(
        enabled = enabled,
        config = null,
        error = null
    )

    @Test
    fun anExamWithoutGeofenceNeverAsksForLocation() {
        // With no geofence the position is never used, so anti fake-location is off and
        // permission, location services and a fix are all not needed.
        assertFalse(
            com.coblax.examlock.isGeofenceEnforced(
                geofence(enabled = false),
                com.coblax.examlock.LocationBypassState.Inactive
            )
        )
        assertFalse(
            com.coblax.examlock.isGeofenceEnforced(
                geofence(enabled = true),
                com.coblax.examlock.LocationBypassState.Active
            )
        )
        for (status in listOf(
            fakeLocation(monitoringEnabled = false, permissionGranted = false),
            fakeLocation(monitoringEnabled = false, servicesEnabled = false),
            fakeLocation(monitoringEnabled = false)
        )) {
            assertEquals(com.coblax.examlock.LocationSpoofSecurityVerdict.Disabled, status.finalVerdict)
            assertFalse(status.blocking)
        }
    }

    @Test
    fun aGeofenceExamStillRequiresAUsableLocation() {
        assertTrue(
            com.coblax.examlock.isGeofenceEnforced(
                geofence(enabled = true),
                com.coblax.examlock.LocationBypassState.Inactive
            )
        )
        val status = fakeLocation(monitoringEnabled = true)
        assertEquals(
            com.coblax.examlock.LocationSpoofSecurityVerdict.LocationUnavailable,
            status.finalVerdict
        )
        assertTrue(status.blocking)
        assertTrue(fakeLocation(monitoringEnabled = true, permissionGranted = false).blocking)
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
