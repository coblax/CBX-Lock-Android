package com.example.coblaxexamlock.runtime

import android.content.Context
import com.example.coblaxexamlock.config.TelegramOfflineQueueMaxSize
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

    fun enqueue(entry: QueuedTelegramMessage) {
        synchronized(lock) {
            val entries = loadEntries().toMutableList()
            while (entries.size >= maxSize) {
                entries.removeAt(0)
            }
            entries.add(entry)
            saveEntries(entries)
        }
    }

    fun peek(count: Int): List<QueuedTelegramMessage> {
        synchronized(lock) {
            return loadEntries().take(count)
        }
    }

    fun remove(id: String) {
        synchronized(lock) {
            val entries = loadEntries().toMutableList()
            entries.removeAll { it.id == id }
            saveEntries(entries)
        }
    }

    fun removeAll(ids: List<String>) {
        if (ids.isEmpty()) return
        synchronized(lock) {
            val idSet = ids.toHashSet()
            val entries = loadEntries().filterNot { it.id in idSet }
            saveEntries(entries)
        }
    }

    fun size(): Int {
        synchronized(lock) {
            return loadEntries().size
        }
    }

    fun clear() {
        synchronized(lock) {
            prefs.edit().remove(KEY_QUEUE).apply()
        }
    }

    private fun loadEntries(): List<QueuedTelegramMessage> {
        val json = prefs.getString(KEY_QUEUE, null) ?: return emptyList()
        return runCatching { deserializeQueue(json) }.getOrDefault(emptyList())
    }

    private fun saveEntries(entries: List<QueuedTelegramMessage>) {
        val json = serializeQueue(entries)
        prefs.edit().putString(KEY_QUEUE, json).apply()
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
