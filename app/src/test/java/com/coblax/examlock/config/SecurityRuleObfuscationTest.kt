package com.coblax.examlock.config

import org.junit.Assert.*
import org.junit.Test

/**
 * The root/hook detection inputs are stored obfuscated so that a release DEX does not
 * answer `strings classes.dex | grep magisk` with the detector's own shopping list.
 *
 * A wrong encoding would not throw — it would quietly fill these lists with garbage and
 * disable root detection altogether, which is worse than the plaintext it replaced. So
 * the decoded values are pinned here.
 */
class SecurityRuleObfuscationTest {
    @Test
    fun rootPackageNamesDecodeToTheRealManagers() {
        for (expected in listOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "me.weishu.kernelsu",
            "me.bmax.apatch",
            "io.github.huskydg.magisk",
            "org.lsposed.manager",
            "de.robv.android.xposed.installer",
            "com.elderdrivers.riru.edxp",
            "com.saurik.substrate"
        )) {
            assertTrue(expected, expected in RootPackageNames)
        }
        assertEquals(16, RootPackageNames.size)
    }

    @Test
    fun magiskIndicatorPathsDecodeToTheRealPaths() {
        for (expected in listOf(
            "/sbin/.magisk",
            "/data/adb/magisk",
            "/data/adb/zygisk",
            "/data/adb/ksu",
            "/data/adb/ap"
        )) {
            assertTrue(expected, expected in MagiskIndicatorPaths)
        }
        assertEquals(9, MagiskIndicatorPaths.size)
    }

    @Test
    fun rootBinaryIndicatorPathsDecodeToTheRealBinaries() {
        for (expected in listOf(
            "/system/bin/su",
            "/sbin/su",
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/system/app/Superuser.apk"
        )) {
            assertTrue(expected, expected in RootBinaryIndicatorPaths)
        }
        assertEquals(25, RootBinaryIndicatorPaths.size)
    }

    @Test
    fun noDetectionInputSurvivesAsAnEmptyOrStillEncodedString() {
        val all = RootPackageNames + MagiskIndicatorPaths + RootBinaryIndicatorPaths
        for (value in all) {
            assertTrue("blank entry", value.isNotBlank())
            assertFalse("still base64: $value", value.endsWith("="))
        }
        assertTrue(all.none { it.contains(" ") })
    }
}
