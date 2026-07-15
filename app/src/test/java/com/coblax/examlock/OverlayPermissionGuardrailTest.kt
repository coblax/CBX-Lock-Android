package com.coblax.examlock

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPermissionGuardrailTest {
    @Test
    fun manifestDoesNotRequestSystemAlertWindow() {
        val manifest = projectFile("src/main/AndroidManifest.xml").readText()

        assertFalse(manifest.contains("android.permission.SYSTEM_ALERT_WINDOW"))
        assertTrue(manifest.contains("android.permission.HIDE_OVERLAY_WINDOWS"))
    }

    @Test
    fun overlayShieldUsesWindowHideOverlayApi() {
        val source = projectFile("src/main/java/com/example/coblaxexamlock/MainActivity.kt")
            .readText()
        val shieldSource = Regex("fun setOverlayShieldMode[\\s\\S]*?\\n    }")
            .find(source)?.value.orEmpty()

        assertTrue(shieldSource.contains("window.setHideOverlayWindows(enabled)"))
        assertFalse(shieldSource.contains("javaClass.getMethod"))
    }

    @Test
    fun overlaySettingsLauncherDoesNotOpenCbxSpecificAppearOnTopPermission() {
        val source = projectFile("src/main/java/com/example/coblaxexamlock/HostPlatformHelpers.kt")
            .readText()
        val openOverlaySettingsSource = Regex(
            "internal fun openOverlaySettings[\\s\\S]*?internal fun openWebViewProviderSettings"
        ).find(source)?.value.orEmpty()

        assertTrue(openOverlaySettingsSource.contains("ACTION_MANAGE_OVERLAY_PERMISSION"))
        assertFalse(openOverlaySettingsSource.contains("""package:${'$'}{context.packageName}"""))
        assertFalse(openOverlaySettingsSource.contains("ACTION_APPLICATION_DETAILS_SETTINGS"))
    }

    @Test
    fun preparationQuickFixTextDoesNotInviteCbxAppearOnTopSetup() {
        val source = projectFile(
            "src/main/java/com/example/coblaxexamlock/ui/preparation/PreparationQuickFixActions.kt"
        ).readText()

        assertFalse(source.contains("Open Overlay Settings"))
        assertFalse(source.contains("Buka Izin Overlay"))
        assertFalse(source.contains("Review Floating App Permission"))
        assertFalse(source.contains("Review Floating App Permissions"))
        assertFalse(source.contains("Periksa izin floating app"))
        assertFalse(source.contains("Periksa Izin Floating App"))
        assertFalse(source.contains("overlay_app_permission"))
    }

    private fun projectFile(path: String): File {
        return listOf(
            File(path),
            File("app/$path")
        ).firstOrNull { it.isFile }
            ?: error("Missing test fixture file: $path")
    }
}
