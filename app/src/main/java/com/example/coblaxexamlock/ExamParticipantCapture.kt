package com.example.coblaxexamlock

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import org.json.JSONObject


internal data class ExamParticipantContext(
    val displayName: String? = null,
    val username: String? = null,
    val userId: String? = null,
    val role: String? = null,
    val kodeKelas: String? = null,
    val kodeRuang: String? = null,
    val sourceKey: String
) {
    fun hasIdentity(): Boolean {
        return !displayName.isNullOrBlank() ||
            !username.isNullOrBlank() ||
            !userId.isNullOrBlank()
    }

    fun diagnosticSummary(): String {
        return buildString {
            append("source_key=").append(sourceKey.ifBlank { "-" })
            append(" | user_id=").append(userId.orEmpty().ifBlank { "-" })
            append(" | username=").append(username.orEmpty().ifBlank { "-" })
            append(" | role=").append(role.orEmpty().ifBlank { "-" })
            append(" | kelas=").append(kodeKelas.orEmpty().ifBlank { "-" })
            append(" | ruang=").append(kodeRuang.orEmpty().ifBlank { "-" })
            append(" | peserta=").append(displayName.orEmpty().ifBlank { "-" })
        }
    }
}

internal sealed interface ExamParticipantCaptureResult {
    data class Captured(val context: ExamParticipantContext) : ExamParticipantCaptureResult

    data class Ignored(val reason: String, val sourceKey: String) : ExamParticipantCaptureResult

    data class Failed(val reason: String, val sourceKey: String) : ExamParticipantCaptureResult
}

internal fun parseExamParticipantContext(
    rawPayload: String,
    sourceKey: String
): ExamParticipantCaptureResult {
    val normalizedKey = sourceKey.trim().ifBlank { "-" }
    val payload = rawPayload.trim()
    if (payload.isBlank()) {
        return ExamParticipantCaptureResult.Ignored(
            reason = "empty_payload",
            sourceKey = normalizedKey
        )
    }

    return runCatching {
        val root = JSONObject(payload)
        val user = root.optJSONObject("user")
            ?: root.optJSONObject("data")?.optJSONObject("user")
            ?: return@runCatching ExamParticipantCaptureResult.Ignored(
                reason = "missing_user",
                sourceKey = normalizedKey
            )

        val context = ExamParticipantContext(
            displayName = user.optSanitizedString("display_name"),
            username = user.optSanitizedString("username"),
            userId = user.optFlexibleString("user_id"),
            role = user.optSanitizedString("role"),
            kodeKelas = user.optSanitizedString("kode_kelas"),
            kodeRuang = user.optSanitizedString("kode_ruang"),
            sourceKey = normalizedKey
        )

        if (!context.hasIdentity()) {
            ExamParticipantCaptureResult.Ignored(
                reason = "missing_identity",
                sourceKey = normalizedKey
            )
        } else {
            ExamParticipantCaptureResult.Captured(context)
        }
    }.getOrElse {
        ExamParticipantCaptureResult.Failed(
            reason = it.message?.take(80) ?: "invalid_json",
            sourceKey = normalizedKey
        )
    }
}

internal class ExamParticipantCaptureBridge(
    private val onParticipantPayloadCaptured: (rawPayload: String, sourceKey: String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Suppress("unused")
    @JavascriptInterface
    fun onParticipantContextCaptured(rawPayload: String?, sourceKey: String?) {
        val safePayload = rawPayload?.trim().orEmpty()
        val safeSourceKey = sourceKey?.trim().orEmpty()
        mainHandler.post {
            onParticipantPayloadCaptured(safePayload, safeSourceKey)
        }
    }
}

internal val ExamParticipantCaptureProbeScript = """
    (function() {
        var bridge = window.ExamParticipantCaptureBridge;
        if (!bridge || !bridge.onParticipantContextCaptured) return;

        var preferredKey = 'cbt_exam_frontend_auth_v1';
        var keyPrefix = 'cbt_exam_frontend_auth_';
        var retryDelays = [0, 180, 600, 1400, 2600];
        var pollIntervalMillis = 3000;

        function scheduleCapture(delay) {
            window.setTimeout(tryCapture, delay || 0);
        }

        function inspectStorage(storage) {
            if (!storage) return null;
            try {
                var preferredValue = storage.getItem(preferredKey);
                if (preferredValue) {
                    return { key: preferredKey, value: preferredValue };
                }

                for (var index = 0; index < storage.length; index += 1) {
                    var key = storage.key(index);
                    if (!key || key.indexOf(keyPrefix) !== 0) continue;
                    var value = storage.getItem(key);
                    if (value) {
                        return { key: key, value: value };
                    }
                }
            } catch (error) {
                return { key: 'storage_error', value: '' };
            }
            return null;
        }

        function isCandidateKey(key) {
            return !!key && (key === preferredKey || key.indexOf(keyPrefix) === 0);
        }

        function reportFound(found) {
            if (!found || !found.value) return false;
            var reportKey = (found.key || preferredKey) + '|' + found.value;
            if (window.__coblaxParticipantCaptureLastReportKey === reportKey) {
                return false;
            }
            window.__coblaxParticipantCaptureLastReportKey = reportKey;
            try {
                bridge.onParticipantContextCaptured(found.value, found.key || preferredKey);
                return true;
            } catch (error) {
                return false;
            }
        }

        function tryCapture() {
            var found = inspectStorage(window.localStorage);
            if (!found || !found.value) {
                found = inspectStorage(window.sessionStorage);
            }
            return reportFound(found);
        }

        if (!window.__coblaxParticipantCaptureInstalled) {
            window.__coblaxParticipantCaptureInstalled = true;

            var originalSetItem = Storage.prototype.setItem;
            Storage.prototype.setItem = function(key, value) {
                var result = originalSetItem.apply(this, arguments);
                if (isCandidateKey(String(key || ''))) {
                    scheduleCapture(0);
                    scheduleCapture(150);
                }
                return result;
            };

            var originalRemoveItem = Storage.prototype.removeItem;
            Storage.prototype.removeItem = function(key) {
                var result = originalRemoveItem.apply(this, arguments);
                if (isCandidateKey(String(key || ''))) {
                    window.__coblaxParticipantCaptureLastReportKey = '';
                    scheduleCapture(0);
                }
                return result;
            };

            var originalClear = Storage.prototype.clear;
            Storage.prototype.clear = function() {
                var result = originalClear.apply(this, arguments);
                window.__coblaxParticipantCaptureLastReportKey = '';
                scheduleCapture(0);
                return result;
            };

            document.addEventListener('visibilitychange', function() {
                if (!document.hidden) {
                    scheduleCapture(0);
                }
            }, true);

            window.addEventListener('focus', function() {
                scheduleCapture(0);
            }, true);

            window.addEventListener('pageshow', function() {
                scheduleCapture(0);
            }, true);

            window.__coblaxParticipantCapturePollHandle =
                window.setInterval(tryCapture, pollIntervalMillis);
        }

        retryDelays.forEach(scheduleCapture);
    })();
""".trimIndent()

private fun JSONObject.optSanitizedString(key: String): String? {
    return optString(key)
        .trim()
        .ifBlank { null }
}

private fun JSONObject.optFlexibleString(key: String): String? {
    val rawValue = opt(key)
    return when (rawValue) {
        null -> null
        is Number -> rawValue.toLong().toString()
        is String -> rawValue.trim().ifBlank { null }
        else -> rawValue.toString().trim().ifBlank { null }
    }
}
