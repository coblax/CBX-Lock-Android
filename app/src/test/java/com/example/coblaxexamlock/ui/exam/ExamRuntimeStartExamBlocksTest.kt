package com.example.coblaxexamlock.ui.exam

import com.example.coblaxexamlock.model.ExamNetworkStatus
import com.example.coblaxexamlock.model.NetworkDiagnostics
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.UiLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamRuntimeStartExamBlocksTest {
    @Test
    fun vpnActiveProducesDedicatedStartBlock() {
        val block = resolveStartExamVpnBlockMessage(
            uiLanguage = UiLanguage.English,
            status = networkStatus(vpnActive = true)
        )

        assertEquals(ExamRuntimeHardeningDiagnostics.StartExamBlockedVpn, block?.code)
        assertTrue(block?.details?.contains("vpn=yes") == true)
    }

    @Test
    fun vpnInactiveDoesNotProduceStartBlock() {
        val block = resolveStartExamVpnBlockMessage(
            uiLanguage = UiLanguage.English,
            status = networkStatus(vpnActive = false)
        )

        assertNull(block)
    }

    private fun networkStatus(vpnActive: Boolean): NetworkReadinessStatus {
        return NetworkReadinessStatus(
            examStatus = ExamNetworkStatus(
                label = "Online",
                detail = "Connected",
                isConnected = true
            ),
            diagnostics = NetworkDiagnostics(
                activeNetworkAvailable = true,
                transports = if (vpnActive) listOf("wifi", "vpn") else listOf("wifi"),
                hasInternetCapability = true,
                isValidated = true,
                isCaptivePortal = false,
                isMetered = false,
                isVpnActive = vpnActive,
                isAirplaneModeEnabled = false,
                notRoaming = true,
                interfaceName = if (vpnActive) "tun0" else "wlan0",
                wifi = null,
                cellular = null
            ),
            verdict = if (vpnActive) NetworkReadinessVerdict.VpnActive else NetworkReadinessVerdict.ConnectedStable,
            transportLabel = if (vpnActive) "Wi-Fi + VPN" else "Wi-Fi",
            quickFixReason = if (vpnActive) "vpn_active" else null,
            userFacingVerdict = if (vpnActive) {
                NetworkReadinessUserVerdict.VpnActive
            } else {
                NetworkReadinessUserVerdict.Stable
            },
            userFacingQuickFixText = null
        )
    }
}
