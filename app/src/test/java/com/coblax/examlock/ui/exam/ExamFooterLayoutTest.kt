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
    fun normalWidthUsesRegularFooter() {
        val spec = calculateExamFooterLayoutSpec(
            maxWidthDp = 430,
            lowRamEnabled = false,
            lowRamSevere = false
        )

        assertFalse(spec.compact)
        assertFalse(spec.severe)
        assertEquals(ExamFooterLayoutMode.SingleRow, spec.layoutMode)
        assertEquals(38, spec.buttonSizeDp)
        assertEquals(64, spec.arrowPillWidthDp)
        assertEquals(50, spec.connectivityPillWidthDp)
        assertEquals(60, spec.shieldPillWidthDp)
        assertEquals(48, spec.touchTargetDp)
        assertEquals(52, spec.minHeightDp)
        assertTrue(spec.showBatteryPercent)
        assertTrue(spec.showConnectivityDot)
        assertEquals(6, spec.shadowElevationDp)
    }

    @Test
    fun narrowPhoneWidthKeepsSingleRowWithActionsVisible() {
        val spec = calculateExamFooterLayoutSpec(
            maxWidthDp = 360,
            lowRamEnabled = false,
            lowRamSevere = false
        )

        assertTrue(spec.compact)
        assertFalse(spec.severe)
        assertEquals(ExamFooterLayoutMode.SingleRow, spec.layoutMode)
        assertEquals(36, spec.buttonSizeDp)
        assertEquals(52, spec.minHeightDp)
        assertEquals(58, spec.maxHeightDp)
        assertTrue(spec.showBatteryPercent)
        assertTrue(spec.showConnectivityDot)
    }

    @Test
    fun severeLowRamStillUsesSingleRowOnPhoneWidth() {
        val spec = calculateExamFooterLayoutSpec(
            maxWidthDp = 720,
            lowRamEnabled = true,
            lowRamSevere = true
        )

        assertTrue(spec.compact)
        assertTrue(spec.severe)
        assertEquals(ExamFooterLayoutMode.SingleRow, spec.layoutMode)
        assertEquals(34, spec.buttonSizeDp)
        assertEquals(56, spec.arrowPillWidthDp)
        assertEquals(52, spec.minHeightDp)
        assertTrue(spec.showBatteryPercent)
        assertFalse(spec.showConnectivityDot)
        assertEquals(0, spec.shadowElevationDp)
    }

    @Test
    fun smallPhoneWidthStillKeepsRefreshAndHomeOnSingleRow() {
        val spec = calculateExamFooterLayoutSpec(
            maxWidthDp = 320,
            lowRamEnabled = true,
            lowRamSevere = true
        )

        assertTrue(spec.compact)
        assertTrue(spec.severe)
        assertEquals(ExamFooterLayoutMode.SingleRow, spec.layoutMode)
        assertEquals(34, spec.buttonSizeDp)
        assertEquals(56, spec.arrowPillWidthDp)
        assertFalse(spec.showConnectivityDot)
        assertFalse(spec.showBatteryPercent)
    }

    @Test
    fun tinyScreenDoesNotShrinkBelowReadableIconSize() {
        val spec = calculateExamFooterLayoutSpec(
            maxWidthDp = 280,
            lowRamEnabled = false,
            lowRamSevere = false
        )

        assertTrue(spec.severe)
        assertEquals(ExamFooterLayoutMode.TwoRowCompact, spec.layoutMode)
        assertEquals(34, spec.buttonSizeDp)
        assertEquals(15, spec.iconSizeDp)
        assertEquals(15, spec.arrowIconSizeDp)
    }

    @Test
    fun wideTabletUsesTabletWideFooter() {
        val spec = calculateExamFooterLayoutSpec(
            maxWidthDp = 720,
            lowRamEnabled = false,
            lowRamSevere = false
        )

        assertEquals(ExamFooterLayoutMode.TabletWide, spec.layoutMode)
        assertFalse(spec.compact)
        assertEquals(40, spec.buttonSizeDp)
        assertEquals(52, spec.minHeightDp)
        assertTrue(spec.showBatteryPercent)
    }

    @Test
    fun wifiStableUsesWifiSignalLevel() {
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = networkStatus(
                transports = listOf("wifi"),
                transportLabel = "Wi-Fi",
                wifi = WifiDiagnostics(
                    ssid = "School",
                    bssid = "00:00:00:00:00:00",
                    rssiDbm = -45,
                    signalLevel = 4,
                    linkSpeedMbps = 150,
                    frequencyMHz = 5200,
                    bandLabel = "5 GHz",
                    hiddenSsid = false,
                    locationPermissionGranted = true,
                    locationServicesEnabled = true
                )
            ),
            serverStatus = ExamServerFooterStatus.Online
        )

        assertEquals(ExamFooterConnectivityTransport.Wifi, visual.transport)
        assertEquals(ExamFooterConnectivitySeverity.Stable, visual.severity)
        assertEquals(4, visual.signalLevel)
        assertNull(visual.badgeText)
    }

    @Test
    fun cellularUnstableUsesCellularLabelAndWarningBadge() {
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
        assertEquals("!", visual.badgeText)
        assertEquals("4G", visual.cellularLabel)
    }

    @Test
    fun offlineIsDangerEvenWithUnknownTransport() {
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = networkStatus(
                verdict = NetworkReadinessVerdict.Offline,
                transports = emptyList(),
                transportLabel = ""
            ),
            serverStatus = ExamServerFooterStatus.Online
        )

        assertEquals(ExamFooterConnectivityTransport.Unknown, visual.transport)
        assertEquals(ExamFooterConnectivitySeverity.Danger, visual.severity)
        assertEquals(0, visual.signalLevel)
        assertNull(visual.badgeText)
    }

    @Test
    fun captivePortalIsWarningForWifi() {
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = networkStatus(
                verdict = NetworkReadinessVerdict.CaptivePortal,
                transports = listOf("wifi"),
                transportLabel = "Wi-Fi"
            ),
            serverStatus = ExamServerFooterStatus.Online
        )

        assertEquals(ExamFooterConnectivityTransport.Wifi, visual.transport)
        assertEquals(ExamFooterConnectivitySeverity.Warning, visual.severity)
        assertEquals("!", visual.badgeText)
    }

    @Test
    fun vpnActiveIsDangerForFooterConnectivity() {
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = networkStatus(
                verdict = NetworkReadinessVerdict.VpnActive,
                transports = listOf("wifi", "vpn"),
                transportLabel = "Wi-Fi + VPN"
            ),
            serverStatus = ExamServerFooterStatus.Online
        )

        assertEquals(ExamFooterConnectivitySeverity.Danger, visual.severity)
        assertNull(visual.badgeText)
    }

    @Test
    fun unknownStableTransportFallsBackWithoutCrash() {
        val visual = resolveExamFooterConnectivityVisual(
            networkStatus = networkStatus(
                transports = emptyList(),
                transportLabel = ""
            ),
            serverStatus = ExamServerFooterStatus.Online
        )

        assertEquals(ExamFooterConnectivityTransport.Unknown, visual.transport)
        assertEquals(ExamFooterConnectivitySeverity.Stable, visual.severity)
        assertEquals(3, visual.signalLevel)
    }

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
