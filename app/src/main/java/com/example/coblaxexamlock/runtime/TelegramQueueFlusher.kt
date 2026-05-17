package com.example.coblaxexamlock.runtime

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val QueueFlusherTag = "TelegramQueueFlusher"

internal class TelegramQueueFlusher(
    private val context: Context,
    private val queue: TelegramPersistentQueue,
    private val retryExecutor: TelegramRetryExecutor,
    private val rateLimiter: TelegramRateLimiter,
    private val scope: CoroutineScope
) {
    private val flushing = AtomicBoolean(false)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun register() {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                triggerFlush()
            }
        }
        networkCallback = callback
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
        }.onFailure {
            Log.w(QueueFlusherTag, "Failed to register network callback", it)
        }
    }

    fun unregister() {
        val callback = networkCallback ?: return
        networkCallback = null
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return
        runCatching {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    fun triggerFlush() {
        if (queue.size() == 0) return
        if (!flushing.compareAndSet(false, true)) return

        scope.launch {
            try {
                flushQueue()
            } finally {
                flushing.set(false)
            }
        }
    }

    private suspend fun flushQueue() {
        val pending = queue.peek(queue.size())
        if (pending.isEmpty()) return

        Log.i(QueueFlusherTag, "Flushing ${pending.size} queued messages")

        for (entry in pending) {
            rateLimiter.acquire()

            val result = retryExecutor.execute(entry.token, entry.chatId, entry.message)
            if (result.isSuccess) {
                queue.remove(entry.id)
            } else {
                Log.w(QueueFlusherTag, "Flush stopped: send failed for ${entry.id}")
                break
            }
        }
    }
}
