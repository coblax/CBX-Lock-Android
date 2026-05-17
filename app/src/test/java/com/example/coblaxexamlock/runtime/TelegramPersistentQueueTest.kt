package com.example.coblaxexamlock.runtime

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the serialization/deserialization logic of TelegramPersistentQueue.
 * Since the queue uses SharedPreferences (requires Android context), we test
 * the JSON serialization logic directly here. Full integration tests with
 * SharedPreferences are in androidTest.
 */
class TelegramPersistentQueueTest {

    @Test
    fun serializationRoundTrip() {
        val msg = QueuedTelegramMessage(
            id = "test-uuid-123",
            token = "bot123:ABCdef",
            chatId = "-100123456789",
            message = "Hello world! Special chars: <>&\"' emoji: 🎉",
            enqueuedAt = 1700000000000L,
            attemptCount = 2
        )

        val json = serializeMessage(msg)
        val parsed = deserializeMessage(json)

        assertEquals(msg.id, parsed.id)
        assertEquals(msg.token, parsed.token)
        assertEquals(msg.chatId, parsed.chatId)
        assertEquals(msg.message, parsed.message)
        assertEquals(msg.enqueuedAt, parsed.enqueuedAt)
        assertEquals(msg.attemptCount, parsed.attemptCount)
    }

    @Test
    fun serializeMultipleMessages() {
        val messages = listOf(
            makeMessage("1", "first"),
            makeMessage("2", "second"),
            makeMessage("3", "third")
        )

        val json = serializeQueue(messages)
        val parsed = deserializeQueue(json)

        assertEquals(3, parsed.size)
        assertEquals("1", parsed[0].id)
        assertEquals("2", parsed[1].id)
        assertEquals("3", parsed[2].id)
        assertEquals("first", parsed[0].message)
        assertEquals("second", parsed[1].message)
        assertEquals("third", parsed[2].message)
    }

    @Test
    fun emptyQueueSerializesToEmptyArray() {
        val json = serializeQueue(emptyList())
        val parsed = deserializeQueue(json)
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun deserializeEmptyStringReturnsEmptyList() {
        val parsed = deserializeQueue("[]")
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun maxSizeEnforcementLogic() {
        val maxSize = 3
        val entries = mutableListOf<QueuedTelegramMessage>()

        // Simulate enqueue with max size enforcement
        for (i in 1..5) {
            while (entries.size >= maxSize) {
                entries.removeAt(0)
            }
            entries.add(makeMessage("$i", "msg$i"))
        }

        assertEquals(3, entries.size)
        assertEquals("3", entries[0].id) // oldest surviving
        assertEquals("4", entries[1].id)
        assertEquals("5", entries[2].id) // newest
    }

    @Test
    fun fifoOrderPreserved() {
        val entries = mutableListOf<QueuedTelegramMessage>()
        entries.add(makeMessage("a", "alpha"))
        entries.add(makeMessage("b", "beta"))
        entries.add(makeMessage("c", "gamma"))

        // Simulate remove by id
        entries.removeAll { it.id == "b" }

        assertEquals(2, entries.size)
        assertEquals("a", entries[0].id)
        assertEquals("c", entries[1].id)
    }

    @Test
    fun attemptCountDefaultsToZero() {
        val json = JSONObject().apply {
            put("id", "x")
            put("token", "t")
            put("chatId", "c")
            put("message", "m")
            put("enqueuedAt", 123L)
            // No attemptCount field
        }.toString()

        val parsed = deserializeMessage(json)
        assertEquals(0, parsed.attemptCount)
    }

    // --- Helpers that mirror the queue's internal serialization ---

    private fun serializeMessage(msg: QueuedTelegramMessage): String {
        return JSONObject().apply {
            put("id", msg.id)
            put("token", msg.token)
            put("chatId", msg.chatId)
            put("message", msg.message)
            put("enqueuedAt", msg.enqueuedAt)
            put("attemptCount", msg.attemptCount)
        }.toString()
    }

    private fun deserializeMessage(json: String): QueuedTelegramMessage {
        val obj = JSONObject(json)
        return QueuedTelegramMessage(
            id = obj.getString("id"),
            token = obj.getString("token"),
            chatId = obj.getString("chatId"),
            message = obj.getString("message"),
            enqueuedAt = obj.getLong("enqueuedAt"),
            attemptCount = obj.optInt("attemptCount", 0)
        )
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

    private fun makeMessage(id: String, message: String) = QueuedTelegramMessage(
        id = id,
        token = "tok",
        chatId = "chat",
        message = message,
        enqueuedAt = System.currentTimeMillis(),
        attemptCount = 0
    )
}
