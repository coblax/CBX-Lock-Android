package com.coblax.examlock.runtime

import com.coblax.examlock.model.NetworkDnsProbeVerdict
import com.coblax.examlock.model.NetworkReadinessUserVerdict
import com.coblax.examlock.model.NetworkReadinessVerdict
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NetworkDnsTimeoutTest {
    @Test
    fun resolvedAndFailedDnsHaveExplicitResults() = runBlocking {
        assertEquals(NetworkDnsProbeVerdict.Resolved,
            probeNetworkDnsStatus("exam.test", resolver = {}).verdict)
        val failed = probeNetworkDnsStatus("exam.test", resolver = { throw UnknownHostException() })
        assertEquals(NetworkDnsProbeVerdict.Failed, failed.verdict)
        assertEquals("UnknownHostException", failed.error)
    }

    @Test
    fun slowSuspendingResolverReturnsTimeout() = runBlocking {
        val result = probeNetworkDnsStatus("exam.test", timeoutMillis = 50, resolver = { delay(5_000) })
        assertEquals(NetworkDnsProbeVerdict.Timeout, result.verdict)
    }

    @Test
    fun cancellingProbeDoesNotTurnCancellationIntoDnsFailure() = runBlocking {
        var returned = false
        val started = CompletableDeferred<Unit>()
        val job = launch {
            probeNetworkDnsStatus("exam.test", resolver = {
                started.complete(Unit)
                delay(5_000)
            })
            returned = true
        }
        started.await()
        job.cancelAndJoin()
        assertFalse(returned)
    }

    @Test
    fun resolverCancellationPropagates() = runBlocking {
        try {
            probeNetworkDnsStatus("exam.test", resolver = { throw CancellationException("cancel") })
            fail("Cancellation must propagate")
        } catch (expected: CancellationException) {
            assertEquals("cancel", expected.message)
        }
    }

    @Test(timeout = 5_000)
    fun blockingDnsThatIgnoresInterruptCannotHoldCallerPastTimeout() = runBlocking {
        val release = CountDownLatch(1)
        val workerFinished = CountDownLatch(1)
        try {
            val result = probeNetworkDnsStatus("exam.test", timeoutMillis = 250) { host ->
                resolveNetworkDnsHost(host) {
                    try {
                        while (release.count > 0) {
                            try { release.await() } catch (_: InterruptedException) { }
                        }
                    } finally {
                        workerFinished.countDown()
                    }
                }
            }
            assertEquals(NetworkDnsProbeVerdict.Timeout, result.verdict)
            assertEquals(1L, release.count)
        } finally {
            release.countDown()
            workerFinished.await(2, TimeUnit.SECONDS)
        }
    }

    /**
     * A lookup that ignores interruption keeps its worker long after the caller times
     * out. A probe that cannot get a worker never asks the network anything, so it must
     * stay inconclusive: reporting it as Failed/Timeout told students on a healthy
     * connection that the exam host DNS was down, and two such probes escalated the
     * readiness verdict all the way to Unstable.
     */
    @Test(timeout = 20_000)
    fun probeThatNeverGetsAWorkerIsInconclusiveNotADnsFailure() = runBlocking {
        val release = CountDownLatch(1)
        val occupied = CountDownLatch(2)
        val stuck: (String) -> Unit = {
            occupied.countDown()
            while (release.count > 0) {
                try { release.await() } catch (_: InterruptedException) { }
            }
        }
        val blockers = List(2) { index ->
            launch(Dispatchers.IO) {
                runCatching { resolveNetworkDnsHost("stuck-$index", stuck) }
            }
        }
        try {
            assertTrue("resolver workers never started", occupied.await(10, TimeUnit.SECONDS))
            val busy = probeNetworkDnsStatus("exam.test", timeoutMillis = 3_000)
            assertEquals(NetworkDnsProbeVerdict.Skipped, busy.verdict)
            assertEquals("resolver_busy", busy.error)
            assertEquals(
                NetworkReadinessUserVerdict.Stable,
                resolveNetworkReadinessUserVerdict(
                    NetworkReadinessVerdict.ConnectedStable, busy, busy
                )
            )
        } finally {
            release.countDown()
            blockers.forEach { it.cancel() }
        }
    }
}
