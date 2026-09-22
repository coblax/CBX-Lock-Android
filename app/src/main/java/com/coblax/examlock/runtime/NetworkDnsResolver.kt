package com.coblax.examlock.runtime

import java.net.InetAddress
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine

/** No worker was free, so the host was never looked up. This is not a DNS verdict. */
internal class DnsResolverBusyException(cause: Throwable? = null) :
    IllegalStateException("DNS resolver workers are busy", cause)

// InetAddress DNS may ignore interruption, so a stalled lookup keeps its worker long
// after the caller's timeout. Bound the workers and hand off directly instead of
// queueing: a queued probe can only report back after the caller has already given
// up, which used to surface as a DNS timeout for a lookup that never ran.
private val dnsExecutor = ThreadPoolExecutor(
    2, 2, 30L, TimeUnit.SECONDS, SynchronousQueue(),
    { runnable -> Thread(runnable, "exam-dns").apply { isDaemon = true } }
).apply { allowCoreThreadTimeOut(true) }

internal suspend fun resolveNetworkDnsHost(
    host: String,
    lookup: (String) -> Unit = { InetAddress.getByName(it) }
): Unit = suspendCancellableCoroutine { continuation ->
    val task = FutureTask<Unit> {
        val result = runCatching { lookup(host) }
        continuation.resumeWith(result)
    }
    continuation.invokeOnCancellation {
        task.cancel(true)
        dnsExecutor.remove(task)
    }
    try {
        dnsExecutor.execute(task)
        if (task.isCancelled) dnsExecutor.remove(task)
    } catch (error: RejectedExecutionException) {
        continuation.resumeWith(Result.failure(DnsResolverBusyException(error)))
    }
}
