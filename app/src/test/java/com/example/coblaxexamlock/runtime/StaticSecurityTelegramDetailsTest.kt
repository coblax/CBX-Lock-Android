package com.example.coblaxexamlock.runtime

import android.view.Display
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticSecurityTelegramDetailsTest {
    @Test
    fun screenRecorderDetailsIncludePackageMetadataAndRuntimeState() {
        val report = buildScreenRecorderTelegramDetails(
            detectedApps = listOf(
                ScreenRecorderAppReport(
                    label = "AZ Screen Recorder",
                    packageName = "com.kimcy929.screenrecorder",
                    versionName = "6.0",
                    systemApp = false,
                    enabled = true,
                    source = ScreenRecorderDetectionSource.KnownPackage
                ),
                ScreenRecorderAppReport(
                    label = "Capture Tool",
                    packageName = "com.example.screen_capture",
                    versionName = "1.2.3",
                    systemApp = true,
                    enabled = false,
                    source = ScreenRecorderDetectionSource.KeywordScan
                )
            ),
            runtimePackageCount = 2,
            bypassActive = true,
            bypassTampered = true,
            violationCount = 3,
            dialogActive = true
        ).joinToString("\n")

        assertTrue(report.contains("Screen recorder packages detected: 2"))
        assertTrue(report.contains("Package visibility note: best-effort"))
        assertTrue(report.contains("label=AZ Screen Recorder"))
        assertTrue(report.contains("package=com.kimcy929.screenrecorder"))
        assertTrue(report.contains("version=6.0"))
        assertTrue(report.contains("appType=user"))
        assertTrue(report.contains("enabled=Ya"))
        assertTrue(report.contains("source=known_package"))
        assertTrue(report.contains("appType=system"))
        assertTrue(report.contains("enabled=Tidak"))
        assertTrue(report.contains("source=keyword_scan"))
        assertTrue(report.contains("Bypass active: Ya"))
        assertTrue(report.contains("Bypass tampered: Ya"))
        assertTrue(report.contains("Runtime violation count: 3"))
        assertTrue(report.contains("Runtime dialog active: Ya"))
    }

    @Test
    fun screenRecorderEmptyDetailsStillExplainVisibilityLimitedScan() {
        val report = buildScreenRecorderTelegramDetails(
            detectedApps = emptyList(),
            runtimePackageCount = 0,
            bypassActive = false,
            bypassTampered = false,
            violationCount = 0,
            dialogActive = false
        ).joinToString("\n")

        assertTrue(report.contains("Detection method: known_package lookup + keyword_scan package scan"))
        assertTrue(report.contains("No visible recorder package detected: Ya"))
    }

    @Test
    fun displayMirrorDetailsIncludeExternalDisplayMetadata() {
        val report = buildDisplayMirrorTelegramDetails(
            displayInfos = listOf(
                ExternalDisplayInfo(
                    displayId = 7,
                    name = "HDMI Display",
                    state = 2,
                    flags = Display.FLAG_SECURE or Display.FLAG_PRESENTATION
                )
            ),
            externalDisplayDetected = true,
            externalDisplayCount = 1,
            bypassActive = false,
            bypassTampered = true,
            violationCount = 4,
            dialogActive = true
        ).joinToString("\n")

        assertTrue(report.contains("Detection method: DisplayManager.getDisplays"))
        assertTrue(report.contains("Blocking definition: external display count > 0"))
        assertTrue(report.contains("External display count: 1"))
        assertTrue(report.contains("id=7"))
        assertTrue(report.contains("name=HDMI Display"))
        assertTrue(report.contains("state=ON"))
        assertTrue(report.contains("flags=SECURE|PRESENTATION"))
        assertTrue(report.contains("Bypass tampered: Ya"))
        assertTrue(report.contains("Runtime violation count: 4"))
        assertTrue(report.contains("Runtime dialog active: Ya"))
    }

    @Test
    fun displayMirrorEmptyDetailsStillSendSection() {
        val report = buildDisplayMirrorTelegramDetails(
            displayInfos = emptyList(),
            externalDisplayDetected = false,
            externalDisplayCount = 0,
            bypassActive = false,
            bypassTampered = false,
            violationCount = 0,
            dialogActive = false
        ).joinToString("\n")

        assertTrue(report.contains("[DISPLAY MIRROR]"))
        assertTrue(report.contains("External display list: -"))
    }

    @Test
    fun multiWindowDetailsSplitMultiWindowAndPictureInPicture() {
        val report = buildMultiWindowTelegramDetails(
            modeInfo = MultiWindowModeInfo(
                multiWindowApiSupported = true,
                pictureInPictureApiSupported = true,
                inMultiWindowMode = true,
                inPictureInPictureMode = false
            ),
            runtimeDetected = true,
            bypassActive = false,
            bypassTampered = true,
            violationCount = 2,
            dialogActive = true
        ).joinToString("\n")

        assertTrue(report.contains("Detection method: Activity.isInMultiWindowMode + Activity.isInPictureInPictureMode"))
        assertTrue(report.contains("MULTI_WINDOW_MODE_CHANGED"))
        assertTrue(report.contains("START_EXAM_BLOCKED_MULTI_WINDOW"))
        assertTrue(report.contains("Multi-window API >= 24 supported: Ya"))
        assertTrue(report.contains("PiP API >= 26 supported: Ya"))
        assertTrue(report.contains("isInMultiWindowMode: Ya"))
        assertTrue(report.contains("isInPictureInPictureMode: Tidak"))
        assertTrue(report.contains("isInAnySplitMode: Ya"))
        assertTrue(report.contains("Runtime combined state: Ya"))
        assertTrue(report.contains("Bypass tampered: Ya"))
        assertTrue(report.contains("Runtime violation count: 2"))
        assertTrue(report.contains("Runtime dialog active: Ya"))
    }
}
