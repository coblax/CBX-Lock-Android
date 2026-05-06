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
}
