package com.coblax.examlock.runtime

import android.content.Context
import com.coblax.examlock.config.TelegramOfflineQueueMaxSize
import org.json.JSONArray
import org.json.JSONObject

internal data class QueuedTelegramMessage(
    val id: String,
    val token: String,
    val chatId: String,
    val message: String,
    val enqueuedAt: Long,
    val attemptCount: Int = 0
)

internal class TelegramPersistentQueue(
    context: Context,
    private val maxSize: Int = TelegramOfflineQueueMaxSize,
    prefsName: String = "telegram_offline_queue"
) {
    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val lock = Any()
    @Volatile private var cachedEntries: MutableList<QueuedTelegramMessage>? = null

    fun enqueue(entry: QueuedTelegramMessage) {
        synchronized(lock) {
            val entries = ensureLoaded()
            while (entries.size >= maxSize) {
                entries.removeAt(0)
            }
            entries.add(entry)
            persistEntries(entries)
        }
    }

    fun peek(count: Int): List<QueuedTelegramMessage> {
        synchronized(lock) {
            return ensureLoaded().take(count)
        }
    }

    fun remove(id: String) {
        synchronized(lock) {
            val entries = ensureLoaded()
            entries.removeAll { it.id == id }
            persistEntries(entries)
        }
    }

    fun removeAll(ids: List<String>) {
        if (ids.isEmpty()) return
        synchronized(lock) {
            val idSet = ids.toHashSet()
            val entries = ensureLoaded()
            entries.removeAll { it.id in idSet }
            persistEntries(entries)
        }
    }

    fun size(): Int {
        synchronized(lock) {
            return ensureLoaded().size
        }
    }

    fun clear() {
        synchronized(lock) {
            cachedEntries = mutableListOf()
            prefs.edit().remove(KEY_QUEUE).apply()
        }
    }

    private fun ensureLoaded(): MutableList<QueuedTelegramMessage> {
        return cachedEntries ?: loadEntriesFromDisk().toMutableList().also { cachedEntries = it }
    }

    private fun persistEntries(entries: MutableList<QueuedTelegramMessage>) {
        cachedEntries = entries
        val json = serializeQueue(entries)
        prefs.edit().putString(KEY_QUEUE, json).apply()
    }

    private fun loadEntriesFromDisk(): List<QueuedTelegramMessage> {
        val json = prefs.getString(KEY_QUEUE, null) ?: return emptyList()
        return runCatching { deserializeQueue(json) }.getOrDefault(emptyList())
    }

    private fun serializeQueue(entries: List<QueuedTelegramMessage>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().apply {
                put("id", entry.id)
                put("token", entry.token)
                put("chatId", entry.chatId)
                put("message", entry.message)
                put("enqueuedAt", entry.enqueuedAt)
                put("attemptCount", entry.attemptCount)
            })
        }
        return array.toString()
    }

    private fun deserializeQueue(json: String): List<QueuedTelegramMessage> {
        val array = JSONArray(json)
        val entries = mutableListOf<QueuedTelegramMessage>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            entries.add(
                QueuedTelegramMessage(
                    id = obj.getString("id"),
                    token = obj.getString("token"),
                    chatId = obj.getString("chatId"),
                    message = obj.getString("message"),
                    enqueuedAt = obj.getLong("enqueuedAt"),
                    attemptCount = obj.optInt("attemptCount", 0)
                )
            )
        }
        return entries
    }

    private companion object {
        const val KEY_QUEUE = "queued_messages"
    }
}
