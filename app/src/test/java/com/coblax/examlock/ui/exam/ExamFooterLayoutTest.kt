package com.coblax.examlock.ui.exam

import com.coblax.examlock.model.CellularDiagnostics
import com.coblax.examlock.model.ExamNetworkStatus
import com.coblax.examlock.model.NetworkDiagnostics
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkReadinessVerdict
import com.coblax.examlock.model.WifiDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamFooterLayoutTest {
    @Test
    fun compactPhoneKeepsAllActionsAtAccessibleHeight() {
        val spec = calculateExamFooterLayoutSpec(
            maxWidthDp = 320,
            lowRamEnabled = false,
            lowRamSevere = false
        )

        assertEquals(ExamFooterLayoutMode.Compact, spec.layoutMode)
        assertTrue(spec.compact)
        assertFalse(spec.severe)
        assertEquals(48, spec.touchTargetDp)
        assertEquals(52, spec.railHeightDp)
        assertFalse(spec.showFullActionLabels)
    }

    @Test
    fun severeLowRamDoesNotShrinkTouchTargets() {
        val spec = calculateExamFooterLayoutSpec(
            maxWidthDp = 360,
            lowRamEnabled = true,
            lowRamSevere = true
        )

        assertEquals(ExamFooterLayoutMode.Compact, spec.layoutMode)
        assertTrue(spec.compact)
        assertTrue(spec.severe)
        assertEquals(48, spec.touchTargetDp)
        assertEquals(52, spec.railHeightDp)
        assertEquals(17, spec.iconSizeDp)
    }

    @Test
    fun mediumPhoneUsesFullActionLabels() {
        val spec = calculateExamFooterLayoutSpec(
            maxWidthDp = 480,
            lowRamEnabled = false,
            lowRamSevere = false
        )

        assertEquals(ExamFooterLayoutMode.Regular, spec.layoutMode)
        assertFalse(spec.compact)
        assertTrue(spec.showFullActionLabels)
    }

    @Test
    fun wideTabletUsesWideActionRail() {
        val spec = calculateExamFooterLayoutSpec(
            maxWidthDp = 720,
            lowRamEnabled = false,
            lowRamSevere = false
        )

        assertEquals(ExamFooterLayoutMode.TabletWide, spec.layoutMode)
        assertFalse(spec.compact)
        assertEquals(20, spec.iconSizeDp)
        assertTrue(spec.showFullActionLabels)
    }

    @Test
    fun wifiStableAndServerOnlineResolveOnline() {
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = networkStatus(
                transports = listOf("wifi"),
                transportLabel = "Wi-Fi",
                wifi = wifi(signalLevel = 4)
            ),
            serverStatus = ExamServerFooterStatus.Online
        )

        assertEquals(ExamFooterConnectivityTransport.Wifi, visual.transport)
        assertEquals(ExamFooterConnectivitySeverity.Stable, visual.severity)
        assertEquals(ExamRuntimeConnectivityState.Online, visual.state)
        assertEquals(4, visual.signalLevel)
        assertNull(resolveExamRuntimeConnectionNotice(networkStatus(), ExamServerFooterStatus.Online))
    }

    @Test
    fun serverCheckingUsesInformationalStateWithoutBanner() {
        val status = networkStatus()
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = status,
            serverStatus = ExamServerFooterStatus.Checking
        )

        assertEquals(ExamFooterConnectivitySeverity.Info, visual.severity)
        assertEquals(ExamRuntimeConnectivityState.Checking, visual.state)
        assertNull(resolveExamRuntimeConnectionNotice(status, ExamServerFooterStatus.Checking))
    }

    @Test
    fun offlineConnectivityIsWarningRatherThanSecurityDanger() {
        val status = networkStatus(
            verdict = NetworkReadinessVerdict.Offline,
            transports = emptyList(),
            transportLabel = ""
        )
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = status,
            serverStatus = ExamServerFooterStatus.Online
        )

        assertEquals(ExamFooterConnectivitySeverity.Warning, visual.severity)
        assertEquals(ExamRuntimeConnectivityState.Offline, visual.state)
        assertEquals(0, visual.signalLevel)
        assertEquals(
            ExamRuntimeConnectionNoticeKind.Offline,
            resolveExamRuntimeConnectionNotice(status, ExamServerFooterStatus.Online)
        )
    }

    @Test
    fun vpnUsesWarningPresentationWhileShieldOwnsBlockingState() {
        val status = networkStatus(
            verdict = NetworkReadinessVerdict.VpnActive,
            transports = listOf("wifi", "vpn"),
            transportLabel = "Wi-Fi + VPN"
        )
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = status,
            serverStatus = ExamServerFooterStatus.Online
        )

        assertEquals(ExamFooterConnectivitySeverity.Warning, visual.severity)
        assertEquals(ExamRuntimeConnectivityState.Limited, visual.state)
        assertEquals(
            ExamRuntimeConnectionNoticeKind.VpnActive,
            resolveExamRuntimeConnectionNotice(status, ExamServerFooterStatus.Online)
        )
    }

    @Test
    fun serverOfflineProducesNonBlockingWarningNotice() {
        val status = networkStatus()
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = status,
            serverStatus = ExamServerFooterStatus.Offline
        )

        assertEquals(ExamFooterConnectivitySeverity.Warning, visual.severity)
        assertEquals(ExamRuntimeConnectivityState.Limited, visual.state)
        assertEquals(
            ExamRuntimeConnectionNoticeKind.ServerOffline,
            resolveExamRuntimeConnectionNotice(status, ExamServerFooterStatus.Offline)
        )
    }

    @Test
    fun networkIssueTakesPriorityOverServerIssue() {
        val status = networkStatus(verdict = NetworkReadinessVerdict.CaptivePortal)

        assertEquals(
            ExamRuntimeConnectionNoticeKind.CaptivePortal,
            resolveExamRuntimeConnectionNotice(status, ExamServerFooterStatus.Offline)
        )
    }

    @Test
    fun cellularUnstableRetainsTransportLabelAndSignalLevel() {
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = networkStatus(
                verdict = NetworkReadinessVerdict.Unstable,
                transports = listOf("cellular"),
                transportLabel = "Cellular",
                cellular = CellularDiagnostics(
                    providerName = "Carrier",
                    operatorCode = "00101",
                    networkType = "LTE",
                    roaming = false,
                    signalLevel = 2,
                    simState = "READY"
                )
            ),
            serverStatus = ExamServerFooterStatus.Online
        )

        assertEquals(ExamFooterConnectivityTransport.Cellular, visual.transport)
        assertEquals(ExamFooterConnectivitySeverity.Warning, visual.severity)
        assertEquals(2, visual.signalLevel)
        assertEquals("4G", visual.cellularLabel)
    }

    private fun wifi(signalLevel: Int): WifiDiagnostics =
        WifiDiagnostics(
            ssid = "School",
            bssid = "00:00:00:00:00:00",
            rssiDbm = -45,
            signalLevel = signalLevel,
            linkSpeedMbps = 150,
            frequencyMHz = 5200,
            bandLabel = "5 GHz",
            hiddenSsid = false,
            locationPermissionGranted = true,
            locationServicesEnabled = true
        )

    private fun networkStatus(
        verdict: NetworkReadinessVerdict = NetworkReadinessVerdict.ConnectedStable,
        transports: List<String> = listOf("wifi"),
        transportLabel: String = "Wi-Fi",
        wifi: WifiDiagnostics? = null,
        cellular: CellularDiagnostics? = null
    ): NetworkReadinessStatus {
        return NetworkReadinessStatus(
            examStatus = ExamNetworkStatus(
                label = if (verdict == NetworkReadinessVerdict.Offline) "Offline" else "Online",
                detail = transportLabel,
                isConnected = verdict != NetworkReadinessVerdict.Offline
            ),
            diagnostics = NetworkDiagnostics(
                activeNetworkAvailable = verdict != NetworkReadinessVerdict.Offline,
                transports = transports,
                hasInternetCapability = verdict != NetworkReadinessVerdict.Offline,
                isValidated = verdict == NetworkReadinessVerdict.ConnectedStable,
                isCaptivePortal = verdict == NetworkReadinessVerdict.CaptivePortal,
                isMetered = cellular != null,
                isVpnActive = verdict == NetworkReadinessVerdict.VpnActive,
                isAirplaneModeEnabled = verdict == NetworkReadinessVerdict.AirplaneMode,
                notRoaming = true,
                interfaceName = if (cellular != null) "rmnet_data0" else "wlan0",
                wifi = wifi,
                cellular = cellular
            ),
            verdict = verdict,
            transportLabel = transportLabel,
            quickFixReason = null
        )
    }
}
