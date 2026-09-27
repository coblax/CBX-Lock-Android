package com.coblax.examlock.installer

import android.content.Context
import android.util.Base64
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal sealed class ReportOutcome {
    object Sent : ReportOutcome()
    /** No connection yet; kept and sent in the background as soon as there is one. */
    object Queued : ReportOutcome()
    /** Telegram answered and refused (bad token, bot removed from the group, …). */
    data class Rejected(val httpCode: Int) : ReportOutcome()
    object Duplicate : ReportOutcome()
    /** Test builds carry no token, so testing never posts into the admins' group. */
    object NotConfigured : ReportOutcome()
}

private sealed class SendResult {
    object Ok : SendResult()
    object TryLater : SendResult()
    data class Refused(val httpCode: Int) : SendResult()
}

/**
 * Sends installer reports to the same admin chat as CBX diagnostics. A report that cannot go
 * out now (often: no internet right when installing) is kept and sent by a background job
 * once the phone is online, even if the student never opens the installer again. The same
 * failure on the same phone is sent at most once a day.
 */
internal object TelegramReporter {
    private const val Tag = "CbxInstallerReport"
    private const val PreferencesName = "installer_reports"
    private const val KeyPending = "pending"
    private const val KeySentKeys = "sent_keys"
    private const val MaxPending = 20
    private const val Separator = "\u001E"
    private const val ObfuscationKey = 115

    val isConfigured: Boolean
        get() = token.isNotBlank() && chatId.isNotBlank()

    private val token: String by lazy { decode(BuildConfig.TELEGRAM_BOT_TOKEN_OBF) }
    private val chatId: String by lazy { decode(BuildConfig.TELEGRAM_CHAT_ID_OBF) }

    /** Blocks on the network; call off the main thread. */
    fun report(context: Context, dedupeKey: String, text: String): ReportOutcome {
        if (!isConfigured) {
            // Test builds: the text still shows up in logcat for checking it.
            Log.i(Tag, text)
            return ReportOutcome.NotConfigured
        }
        val prefs = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val sentKeys = prefs.getStringSet(KeySentKeys, emptySet()).orEmpty()
        if (dedupeKey in sentKeys) return ReportOutcome.Duplicate
        val outcome = when (val result = send(text)) {
            SendResult.Ok -> ReportOutcome.Sent
            SendResult.TryLater -> {
                enqueue(context, text)
                ReportOutcome.Queued
            }
            is SendResult.Refused -> ReportOutcome.Rejected(result.httpCode)
        }
        prefs.edit().putStringSet(KeySentKeys, (sentKeys.toList().takeLast(50) + dedupeKey).toSet()).apply()
        return outcome
    }

    /**
     * Keeps [text] to send later without touching the network: for the crash handler, where
     * the process is about to die. Written synchronously so it survives that.
     */
    fun enqueueNow(context: Context, text: String) {
        if (!isConfigured) {
            Log.i(Tag, text)
            return
        }
        enqueue(context, text, synchronous = true)
    }

    /** Sends what was kept; returns how many still wait for a connection. Blocking. */
    fun flushPending(context: Context): Int {
        if (!isConfigured) return 0
        val prefs = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val pending = readPending(prefs.getString(KeyPending, "").orEmpty())
        if (pending.isEmpty()) return 0
        // A refused report will never go through (the token is fixed in this build), so only
        // the ones that failed for lack of a connection are kept for the next try.
        val remaining = pending.filter { send(it) == SendResult.TryLater }
        prefs.edit().putString(KeyPending, remaining.joinToString(Separator)).commit()
        return remaining.size
    }

    fun pendingCount(context: Context): Int = readPending(
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(KeyPending, "").orEmpty()
    ).size

    private fun enqueue(context: Context, text: String, synchronous: Boolean = false) {
        val prefs = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val pending = readPending(prefs.getString(KeyPending, "").orEmpty()) + text
        val editor = prefs.edit().putString(KeyPending, pending.takeLast(MaxPending).joinToString(Separator))
        if (synchronous) editor.commit() else editor.apply()
        ReportRetryJobService.schedule(context)
    }

    private fun readPending(raw: String): List<String> = raw.split(Separator).filter { it.isNotBlank() }

    private fun send(text: String): SendResult {
        val body = "chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8.name()) +
            "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8.name()) +
            "&disable_web_page_preview=true"
        val connection = try {
            URL("https://api.telegram.org/bot$token/sendMessage").openConnection() as HttpURLConnection
        } catch (_: Exception) {
            return SendResult.TryLater
        }
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val code = connection.responseCode
            when {
                code in 200..299 && connection.inputStream.bufferedReader().use { it.readText() }
                    .contains("\"ok\":true") -> SendResult.Ok
                // Rate limited or Telegram having trouble: worth another try later.
                code == 429 || code >= 500 -> SendResult.TryLater
                else -> SendResult.Refused(code)
            }
        } catch (_: Exception) {
            SendResult.TryLater
        } finally {
            connection.disconnect()
        }
    }

    private fun decode(obfuscated: String): String {
        if (obfuscated.isBlank()) return ""
        return runCatching {
            val bytes = Base64.decode(obfuscated, Base64.DEFAULT)
            for (index in bytes.indices) {
                bytes[index] = (bytes[index].toInt() xor ObfuscationKey).toByte()
            }
            String(bytes, StandardCharsets.UTF_8)
        }.getOrDefault("")
    }
}
