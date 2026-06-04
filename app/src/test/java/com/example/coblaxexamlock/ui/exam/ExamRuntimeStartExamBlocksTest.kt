package com.example.coblaxexamlock.ui.exam

import com.example.coblaxexamlock.model.ExamNetworkStatus
import com.example.coblaxexamlock.model.NetworkDiagnostics
import com.example.coblaxexamlock.model.NetworkDnsProbeStatus
import com.example.coblaxexamlock.model.NetworkDnsProbeVerdict
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
    fun reverseEngineeringDetectedBlocksWhenBypassOff() {
        val block = resolveStartExamTamperBlockMessage(
            uiLanguage = UiLanguage.English,
            reverseEngineeringDetected = true,
            reverseEngineeringSummary = "debugger=true | pkg:org.lsposed.manager",
            reverseEngineeringBypassActive = false,
            apkIntegrityDetected = false,
            apkIntegritySummary = "-",
            apkIntegrityBypassActive = false
        )

        assertEquals("START_EXAM_BLOCKED_REVERSE_ENGINEERING", block?.code)
        assertTrue(block?.message?.contains("Debugger") == true)
    }

    @Test
    fun reverseEngineeringDetectedContinuesWhenBypassOn() {
        val block = resolveStartExamTamperBlockMessage(
            uiLanguage = UiLanguage.English,
            reverseEngineeringDetected = true,
            reverseEngineeringSummary = "debugger=true",
            reverseEngineeringBypassActive = true,
            apkIntegrityDetected = false,
            apkIntegritySummary = "-",
            apkIntegrityBypassActive = false
        )

        assertNull(block)
    }

    @Test
    fun apkIntegrityDetectedBlocksWhenBypassOff() {
        val block = resolveStartExamTamperBlockMessage(
            uiLanguage = UiLanguage.English,
            reverseEngineeringDetected = false,
            reverseEngineeringSummary = "-",
            reverseEngineeringBypassActive = false,
            apkIntegrityDetected = true,
            apkIntegritySummary = "signature_changed",
            apkIntegrityBypassActive = false
        )

        assertEquals("START_EXAM_BLOCKED_APK_INTEGRITY", block?.code)
        assertTrue(block?.message?.contains("signature") == true)
    }

    @Test
    fun apkIntegrityDetectedContinuesWhenBypassOn() {
        val block = resolveStartExamTamperBlockMessage(
            uiLanguage = UiLanguage.English,
            reverseEngineeringDetected = false,
            reverseEngineeringSummary = "-",
            reverseEngineeringBypassActive = false,
            apkIntegrityDetected = true,
            apkIntegritySummary = "signature_changed",
            apkIntegrityBypassActive = true
        )

        assertNull(block)
    }

    @Test
    fun singleBypassDoesNotBypassOtherTamperGate() {
        val block = resolveStartExamTamperBlockMessage(
            uiLanguage = UiLanguage.English,
            reverseEngineeringDetected = true,
            reverseEngineeringSummary = "debugger=true",
            reverseEngineeringBypassActive = true,
            apkIntegrityDetected = true,
            apkIntegritySummary = "dex_hash_mismatch",
            apkIntegrityBypassActive = false
        )

        assertEquals("START_EXAM_BLOCKED_APK_INTEGRITY", block?.code)
    }

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

    @Test
    fun examHostDnsFailureDoesNotBlockStartExamWhenAndroidNetworkIsConnected() {
        val block = resolveStartExamNetworkReachabilityBlockMessage(
            uiLanguage = UiLanguage.English,
            status = networkStatus(
                userFacingVerdict = NetworkReadinessUserVerdict.DnsFailed,
                dnsProbeStatus = NetworkDnsProbeStatus(
                    verdict = NetworkDnsProbeVerdict.Failed,
                    host = "skansatp.web.id",
                    error = "UnknownHostException"
                )
            )
        )

        assertNull(block)
    }

    @Test
    fun offlineNetworkStillBlocksStartExam() {
        val block = resolveStartExamNetworkReachabilityBlockMessage(
            uiLanguage = UiLanguage.English,
            status = networkStatus(userFacingVerdict = NetworkReadinessUserVerdict.Offline)
        )

        assertEquals("START_EXAM_BLOCKED_NETWORK_REACHABILITY", block?.code)
    }

    @Test
    fun stableExamHostProbeDoesNotBlockStartExam() {
        val block = resolveStartExamNetworkReachabilityBlockMessage(
            uiLanguage = UiLanguage.English,
            status = networkStatus(
                userFacingVerdict = NetworkReadinessUserVerdict.Stable,
                dnsProbeStatus = NetworkDnsProbeStatus(
                    verdict = NetworkDnsProbeVerdict.Resolved,
                    host = "skansatp.web.id"
                )
            )
        )

        assertNull(block)
    }

    @Test
    fun offlineExamServerProbeIsAdvisoryAndDoesNotBlockStartExam() {
        val block = resolveStartExamServerProbeBlockMessage(
            uiLanguage = UiLanguage.English,
            result = ExamServerProbeResult(
                status = ExamServerFooterStatus.Offline,
                host = "skansatp.web.id",
                method = "GET",
                code = null,
                latencyMs = 6_000L,
                reason = "SocketTimeoutException"
            )
        )

        assertNull(block)
    }

    private fun networkStatus(
        vpnActive: Boolean = false,
        userFacingVerdict: NetworkReadinessUserVerdict = if (vpnActive) {
            NetworkReadinessUserVerdict.VpnActive
        } else {
            NetworkReadinessUserVerdict.Stable
        },
        dnsProbeStatus: NetworkDnsProbeStatus = NetworkDnsProbeStatus()
    ): NetworkReadinessStatus {
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
            dnsProbeStatus = dnsProbeStatus,
            userFacingVerdict = userFacingVerdict,
            userFacingQuickFixText = null
        )
    }
}
