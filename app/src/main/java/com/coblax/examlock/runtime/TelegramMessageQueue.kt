package com.coblax.examlock.runtime

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val MessageQueueTag = "TelegramMessageQueue"

internal class TelegramMessageQueue(
    context: Context,
    private val retryExecutor: TelegramRetryExecutor = TelegramRetryExecutor(),
    private val rateLimiter: TelegramRateLimiter = TelegramRateLimiter(),
    private val persistentQueue: TelegramPersistentQueue = TelegramPersistentQueue(context.applicationContext),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val appContext: Context = context.applicationContext
    private val flusher = TelegramQueueFlusher(
        context = appContext,
        queue = persistentQueue,
        retryExecutor = retryExecutor,
        rateLimiter = rateLimiter,
        scope = scope
    )

    suspend fun send(token: String, chatId: String, message: String): Result<Unit> {
        rateLimiter.acquire()

        val result = retryExecutor.execute(token, chatId, message)

        if (result.isFailure) {
            val entry = QueuedTelegramMessage(
                id = UUID.randomUUID().toString(),
                token = token,
                chatId = chatId,
                message = message,
                enqueuedAt = System.currentTimeMillis()
            )
            persistentQueue.enqueue(entry)
            Log.i(MessageQueueTag, "Message queued for later delivery: ${entry.id}")
        }

        return result
    }

    fun sendAsync(token: String, chatId: String, message: String): Job {
        return scope.launch {
            send(token, chatId, message)
        }
    }

    fun startNetworkListener() {
        flusher.register()
        // Flush on startup if queue has pending messages and network is available
        if (persistentQueue.size() > 0 && isNetworkAvailable()) {
            flusher.triggerFlush()
        }
    }

    fun shutdown() {
        flusher.unregister()
        scope.cancel()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/**
 * Singleton holder for the app-wide TelegramMessageQueue instance.
 * Initialize with application context during app startup.
 */
internal object TelegramMessageQueueHolder {
    @Volatile
    private var _instance: TelegramMessageQueue? = null

    val instance: TelegramMessageQueue
        get() = _instance ?: error("TelegramMessageQueue not initialized. Call initialize() first.")

    fun initialize(context: Context) {
        if (_instance == null) {
            synchronized(this) {
                if (_instance == null) {
                    _instance = TelegramMessageQueue(context.applicationContext).also {
                        it.startNetworkListener()
                    }
                }
            }
        }
    }

    fun shutdown() {
        _instance?.shutdown()
        _instance = null
    }
}
