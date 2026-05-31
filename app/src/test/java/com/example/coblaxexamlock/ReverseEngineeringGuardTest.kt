package com.example.coblaxexamlock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReverseEngineeringGuardTest {
    @Test
    fun procMapsMarkerIgnoresRandomAppPathContainingKsu() {
        val randomAndroidPath =
            "7a7a000000-7a7b000000 r--p 00000000 103:31 12345 /data/app/~~abCKsuQQ==/com.example.coblaxexamlock/base.apk"

        val hits = ReverseEngineeringGuard.ParityAccess.scanProcMapsLineReference(randomAndroidPath)

        assertFalse(hits.contains("ksu"))
        assertTrue(hits.isEmpty())
    }

    @Test
    fun procMapsMarkerStillDetectsSpecificKernelSuPaths() {
        val kernelSuPath =
            "7a7a000000-7a7b000000 r-xp 00000000 103:31 12345 /data/adb/ksu/bin/ksud"

        val hits = ReverseEngineeringGuard.ParityAccess.scanProcMapsLineReference(kernelSuPath)

        assertTrue(hits.contains("/data/adb/ksu"))
    }
}
