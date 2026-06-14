package com.example.coblaxexamlock.runtime

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayAppScannerTest {
    @Test
    fun samsungSystemOverlayPackagesAreSkipped() {
        val skipped = shouldSkipOverlayAppCandidate(
            packageName = "com.samsung.android.knox.attestation",
            ownPackageName = "com.example.coblaxexamlock",
            flags = ApplicationInfo.FLAG_SYSTEM,
            enabled = true,
            requestedPermissions = arrayOf("android.permission.SYSTEM_ALERT_WINDOW")
        )

        assertTrue(skipped)
    }

    @Test
    fun updatedSystemOverlayPackagesAreSkipped() {
        val skipped = shouldSkipOverlayAppCandidate(
            packageName = "com.google.android.gms",
            ownPackageName = "com.example.coblaxexamlock",
            flags = ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
            enabled = true,
            requestedPermissions = arrayOf("android.permission.SYSTEM_ALERT_WINDOW")
        )

        assertTrue(skipped)
    }

    @Test
    fun packagesWithoutManifestOverlayPermissionAreSkipped() {
        val skipped = shouldSkipOverlayAppCandidate(
            packageName = "com.example.normal",
            ownPackageName = "com.example.coblaxexamlock",
            flags = 0,
            enabled = true,
            requestedPermissions = arrayOf("android.permission.INTERNET")
        )

        assertTrue(skipped)
    }

    @Test
    fun userInstalledOverlayAppsRemainCandidates() {
        val skipped = shouldSkipOverlayAppCandidate(
            packageName = "com.example.floatingtool",
            ownPackageName = "com.example.coblaxexamlock",
            flags = 0,
            enabled = true,
            requestedPermissions = arrayOf("android.permission.SYSTEM_ALERT_WINDOW")
        )

        assertFalse(skipped)
    }
}
