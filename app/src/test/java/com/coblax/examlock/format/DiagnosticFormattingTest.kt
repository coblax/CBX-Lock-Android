package com.coblax.examlock.format

import com.coblax.examlock.model.DiagnosticSection
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticFormattingTest {
    @Test
    fun networkSectionIncludesVpnEvents() {
        val codes = diagnosticSectionEventCodes(DiagnosticSection.Network)

        assertTrue(codes.contains("NETWORK_VPN_DETECTED"))
        assertTrue(codes.contains("NETWORK_VPN_CLEARED"))
        assertTrue(codes.contains("START_EXAM_BLOCKED_VPN"))
        assertTrue(codes.contains("VPN_BYPASS_TAMPER_DETECTED"))
        assertTrue(codes.contains("VPN_SETTINGS_OPENED"))
    }

    @Test
    fun staticSecuritySectionsIncludeExistingRuntimeEvents() {
        val screenRecorderCodes = diagnosticSectionEventCodes(DiagnosticSection.ScreenRecorder)
        val displayMirrorCodes = diagnosticSectionEventCodes(DiagnosticSection.DisplayMirror)
        val multiWindowCodes = diagnosticSectionEventCodes(DiagnosticSection.MultiWindow)

        assertTrue(screenRecorderCodes.contains("SCREEN_RECORDER_DETECTED"))
        assertTrue(screenRecorderCodes.contains("SCREEN_RECORDER_CLEARED"))
        assertTrue(screenRecorderCodes.contains("START_EXAM_BLOCKED_SCREEN_RECORDER"))
        assertTrue(displayMirrorCodes.contains("DISPLAY_MIRROR_DETECTED"))
        assertTrue(displayMirrorCodes.contains("DISPLAY_MIRROR_CLEARED"))
        assertTrue(displayMirrorCodes.contains("START_EXAM_BLOCKED_DISPLAY_MIRROR"))
        assertTrue(multiWindowCodes.contains("MULTI_WINDOW_MODE_CHANGED"))
        assertTrue(multiWindowCodes.contains("MULTI_WINDOW_DETECTED"))
        assertTrue(multiWindowCodes.contains("MULTI_WINDOW_CLEARED"))
        assertTrue(multiWindowCodes.contains("START_EXAM_BLOCKED_MULTI_WINDOW"))
    }
}
