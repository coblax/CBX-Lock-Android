package com.example.coblaxexamlock.format

import com.example.coblaxexamlock.model.DiagnosticSection
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
}
