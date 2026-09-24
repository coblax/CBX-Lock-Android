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
    fun phoneChromeShowsEveryWordInSlimBars() {
        val spec = calculateExamChromeLayoutSpec(
            maxWidthDp = 360,
            fontScale = 1f,
            lowRamSevere = false
        )

        assertEquals(ExamFooterLayoutMode.Regular, spec.layoutMode)
        assertTrue(spec.showExamName)
        assertTrue(spec.showStatusLabels)
        assertTrue(spec.showActionLabels)
        assertFalse(spec.stackActionLabels)
        assertEquals(32, spec.headerHeightDp)
        assertEquals(48, spec.footerHeightDp)
        assertEquals(48, spec.touchTargetDp)
        // Both bars together stay well under the old 48dp card rail plus 40dp status strip.
        assertTrue(spec.headerHeightDp + spec.footerHeightDp <= 80)
    }

    @Test
    fun largeFontDropsStatusWordsBeforeActionLabels() {
        val spec = calculateExamChromeLayoutSpec(
            maxWidthDp = 360,
            fontScale = 1.1f,
            lowRamSevere = false
        )

        assertFalse(spec.showStatusLabels)
        assertTrue(spec.showActionLabels)
        assertFalse(spec.stackActionLabels)
        assertTrue(spec.showExamName)
    }

    @Test
    fun largerFontMovesActionLabelsUnderIconsInsteadOfDroppingThem() {
        val spec = calculateExamChromeLayoutSpec(
            maxWidthDp = 360,
            fontScale = 1.3f,
            lowRamSevere = false
        )

        assertTrue(spec.showActionLabels)
        assertTrue(spec.stackActionLabels)
        assertEquals(48, spec.touchTargetDp)
    }

    @Test
    fun hugeFontIsCappedSoTheBarsKeepTheirWords() {
        val spec = calculateExamChromeLayoutSpec(
            maxWidthDp = 320,
            fontScale = 2f,
            lowRamSevere = false
        )

        // 2x is treated as the 1.3x cap: the bars stay slim and every button keeps a word.
        assertEquals(ExamFooterLayoutMode.Compact, spec.layoutMode)
        assertFalse(spec.showStatusLabels)
        assertTrue(spec.showActionLabels)
        assertTrue(spec.stackActionLabels)
        assertTrue(spec.showExamName)
        assertEquals(48, spec.footerHeightDp)
        assertEquals(48, spec.touchTargetDp)
    }

    @Test
    fun severeLowRamKeepsLabelsAndTouchTargets() {
        val spec = calculateExamChromeLayoutSpec(
            maxWidthDp = 720,
            fontScale = 1f,
            lowRamSevere = true
        )

        assertEquals(ExamFooterLayoutMode.Compact, spec.layoutMode)
        assertTrue(spec.showActionLabels)
        assertEquals(48, spec.touchTargetDp)
    }

    @Test
    fun wideTabletShowsEverything() {
        val spec = calculateExamChromeLayoutSpec(
            maxWidthDp = 720,
            fontScale = 1.3f,
            lowRamSevere = false
        )

        assertEquals(ExamFooterLayoutMode.TabletWide, spec.layoutMode)
        assertTrue(spec.showActionLabels)
        assertTrue(spec.showStatusLabels)
        assertTrue(spec.showExamName)
        assertFalse(spec.stackActionLabels)
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
