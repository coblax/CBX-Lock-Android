package com.coblax.examlock.runtime

import java.net.InetAddress
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine

// InetAddress DNS may ignore interruption. Bound both workers and queued requests,
// and detach a cancelled caller so a resolver stall cannot freeze preparation.
private val dnsExecutor = ThreadPoolExecutor(
    2, 2, 30L, TimeUnit.SECONDS, ArrayBlockingQueue(2),
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
    } catch (error: java.util.concurrent.RejectedExecutionException) {
        continuation.resumeWith(Result.failure(error))
    }
}
