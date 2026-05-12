package com.example.coblaxexamlock.runtime

import android.view.Display
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeStaticSecurityEvidenceTest {
    @Test
    fun vpnRuntimeEvidenceIncludesTransportInterfaceAndBypassState() {
        val detail = buildVpnRuntimeEvidence(
            transportLabel = "VPN, Wi-Fi",
            interfaceName = "tun0",
            bypassActive = false,
            bypassTampered = true
        )

        assertTrue(detail.contains("Transport: VPN, Wi-Fi"))
        assertTrue(detail.contains("Interface: tun0"))
        assertTrue(detail.contains("VPN bypass active: Tidak"))
        assertTrue(detail.contains("VPN bypass tampered: Ya"))
    }

    @Test
    fun screenRecorderRuntimeEvidenceIncludesCountPackagesAndViolationCount() {
        val detail = buildScreenRecorderRuntimeEvidence(
            packages = listOf("AZ Screen Recorder (com.kimcy929.screenrecorder)"),
            violationCount = 3
        )

        assertTrue(detail.contains("Detection method: known_package lookup + keyword_scan package scan"))
        assertTrue(detail.contains("Detected package count: 1"))
        assertTrue(detail.contains("Runtime violation count: 3"))
        assertTrue(detail.contains("AZ Screen Recorder"))
    }

    @Test
    fun displayMirrorRuntimeEvidenceIncludesDisplayMetadata() {
        val detail = buildDisplayMirrorRuntimeEvidence(
            externalDisplayCount = 1,
            externalDisplayInfoList = listOf(
                ExternalDisplayInfo(
                    displayId = 7,
                    name = "HDMI Display",
                    state = 2,
                    flags = Display.FLAG_SECURE or Display.FLAG_PRESENTATION
                )
            ),
            violationCount = 4
        )

        assertTrue(detail.contains("Detection method: DisplayManager.getDisplays"))
        assertTrue(detail.contains("External display count: 1"))
        assertTrue(detail.contains("id=7"))
        assertTrue(detail.contains("name=HDMI Display"))
        assertTrue(detail.contains("state=ON"))
        assertTrue(detail.contains("flags=SECURE|PRESENTATION"))
        assertTrue(detail.contains("Runtime violation count: 4"))
    }

    @Test
    fun multiWindowRuntimeEvidenceSplitsMultiWindowAndPictureInPicture() {
        val detail = buildMultiWindowRuntimeEvidence(
            modeInfo = MultiWindowModeInfo(
                multiWindowApiSupported = true,
                pictureInPictureApiSupported = true,
                inMultiWindowMode = true,
                inPictureInPictureMode = false
            ),
            runtimeDetected = true,
            violationCount = 2
        )

        assertTrue(detail.contains("Detection method: Activity.isInMultiWindowMode + Activity.isInPictureInPictureMode"))
        assertTrue(detail.contains("Multi-window API >= 24 supported: Ya"))
        assertTrue(detail.contains("PiP API >= 26 supported: Ya"))
        assertTrue(detail.contains("isInMultiWindowMode: Ya"))
        assertTrue(detail.contains("isInPictureInPictureMode: Tidak"))
        assertTrue(detail.contains("isInAnySplitMode: Ya"))
        assertTrue(detail.contains("Runtime combined state: Ya"))
        assertTrue(detail.contains("Runtime violation count: 2"))
    }
}
