package com.coblax.examlock.runtime

import com.coblax.examlock.model.NetworkDnsProbeVerdict
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
}
