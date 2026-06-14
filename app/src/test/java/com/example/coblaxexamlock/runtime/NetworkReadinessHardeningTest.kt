package com.example.coblaxexamlock.runtime

import com.example.coblaxexamlock.model.NetworkDnsProbeStatus
import com.example.coblaxexamlock.model.NetworkDnsProbeVerdict
import com.example.coblaxexamlock.model.NetworkLatencyBucket
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkReadinessHardeningTest {
    @Test
    fun examUrlProbeUsesExamHostInsteadOfGenericHost() {
        assertEquals(
            "skansatp.web.id",
            networkProbeHostForExamUrl("https://skansatp.web.id/?examkey=223611")
        )
    }

    @Test
    fun invalidExamUrlProbeFallsBackToGenericHost() {
        assertEquals("example.com", networkProbeHostForExamUrl("not a url"))
    }

    @Test
    fun connectedWithDnsFailureBecomesDnsFailed() {
        val verdict = resolveNetworkReadinessUserVerdict(
            verdict = NetworkReadinessVerdict.ConnectedStable,
            dnsProbeStatus = NetworkDnsProbeStatus(
                verdict = NetworkDnsProbeVerdict.Failed,
                host = "example.com"
            )
        )

        assertEquals(NetworkReadinessUserVerdict.DnsFailed, verdict)
    }

    @Test
    fun connectedWithSlowProbeBecomesSlow() {
        val verdict = resolveNetworkReadinessUserVerdict(
            verdict = NetworkReadinessVerdict.ConnectedStable,
            dnsProbeStatus = NetworkDnsProbeStatus(
                verdict = NetworkDnsProbeVerdict.Resolved,
                host = "example.com",
                latencyMillis = 1_200L,
                latencyBucket = NetworkLatencyBucket.Slow
            )
        )

        assertEquals(NetworkReadinessUserVerdict.Slow, verdict)
    }

    @Test
    fun connectedWithGlobalDnsFailureBecomesDnsFailedWhenExamProbeDidNotRun() {
        val verdict = resolveNetworkReadinessUserVerdict(
            verdict = NetworkReadinessVerdict.ConnectedStable,
            dnsProbeStatus = NetworkDnsProbeStatus(),
            globalDnsProbeStatus = NetworkDnsProbeStatus(
                verdict = NetworkDnsProbeVerdict.Failed,
                host = GlobalDnsProbeHost,
                error = "UnknownHostException"
            )
        )

        assertEquals(NetworkReadinessUserVerdict.DnsFailed, verdict)
    }

    @Test
    fun resolvedExamDnsKeepsStartSignalStableEvenIfGlobalProbeFails() {
        val verdict = resolveNetworkReadinessUserVerdict(
            verdict = NetworkReadinessVerdict.ConnectedStable,
            dnsProbeStatus = NetworkDnsProbeStatus(
                verdict = NetworkDnsProbeVerdict.Resolved,
                host = "skansatp.web.id",
                latencyBucket = NetworkLatencyBucket.Fast
            ),
            globalDnsProbeStatus = NetworkDnsProbeStatus(
                verdict = NetworkDnsProbeVerdict.Failed,
                host = GlobalDnsProbeHost,
                error = "UnknownHostException"
            )
        )

        assertEquals(NetworkReadinessUserVerdict.Stable, verdict)
    }

    @Test
    fun captivePortalAndOfflineStayExplicit() {
        assertEquals(
            NetworkReadinessUserVerdict.CaptivePortal,
            resolveNetworkReadinessUserVerdict(
                NetworkReadinessVerdict.CaptivePortal,
                NetworkDnsProbeStatus()
            )
        )
        assertEquals(
            NetworkReadinessUserVerdict.Offline,
            resolveNetworkReadinessUserVerdict(
                NetworkReadinessVerdict.Offline,
                NetworkDnsProbeStatus()
            )
        )
    }

    @Test
    fun vpnActiveBecomesVpnActiveUserVerdict() {
        assertEquals(
            NetworkReadinessUserVerdict.VpnActive,
            resolveNetworkReadinessUserVerdict(
                NetworkReadinessVerdict.VpnActive,
                NetworkDnsProbeStatus()
            )
        )
    }
}
