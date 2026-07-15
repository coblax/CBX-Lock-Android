package com.coblax.examlock

import android.content.ClipboardManager
import android.content.Context
import com.coblax.examlock.nativebridge.NativeBridgeBackendMode
import com.coblax.examlock.nativebridge.NativeBridgeTestControl
import com.coblax.examlock.nativebridge.NativeSecurityBridge
import java.security.MessageDigest
import java.util.Locale

internal enum class ClipboardBypassState {
    Active,
    Inactive,
    Tampered
}

internal object ClipboardBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): ClipboardBypassState {
        return when {
            tampered -> ClipboardBypassState.Tampered
            enabled -> ClipboardBypassState.Active
            else -> ClipboardBypassState.Inactive
        }
    }
}

internal enum class ClipboardChangeDecision {
    Idle,
    ObservedPending,
    IgnoredWarmup,
    IgnoredSemanticMatch,
    IgnoredCoveredByAppSwitch,
    IgnoredReturnedToBaseline,
    IgnoredResumeNotStable,
    IgnoredNoSubstantiveChange,
    Confirmed,
    ConfirmedResumeCheck
}

internal fun ClipboardChangeDecision.diagnosticLabel(): String {
    return when (this) {
        ClipboardChangeDecision.Idle -> "idle"
        ClipboardChangeDecision.ObservedPending -> "observed_pending"
        ClipboardChangeDecision.IgnoredWarmup -> "ignored_warmup"
        ClipboardChangeDecision.IgnoredSemanticMatch -> "ignored_semantic_match"
        ClipboardChangeDecision.IgnoredCoveredByAppSwitch -> "ignored_covered_by_app_switch"
        ClipboardChangeDecision.IgnoredReturnedToBaseline -> "ignored_returned_to_baseline"
        ClipboardChangeDecision.IgnoredResumeNotStable -> "ignored_resume_not_stable"
        ClipboardChangeDecision.IgnoredNoSubstantiveChange -> "ignored_no_substantive_change"
        ClipboardChangeDecision.Confirmed -> "confirmed"
        ClipboardChangeDecision.ConfirmedResumeCheck -> "confirmed_resume_check"
    }
}

internal data class ClipboardSnapshot(
    val decisionFingerprint: String,
    val semanticSignature: String,
    val rawSignature: String,
    val itemCount: Int,
    val hasText: Boolean,
    val hasHtml: Boolean,
    val hasUri: Boolean,
    val hasIntent: Boolean,
    val isEmpty: Boolean,
    val readErrorOccurred: Boolean = false
)

internal enum class ClipboardSnapshotMode {
    RuntimeLite,
    DiagnosticFull
}

internal class ClipboardNormalizedItemInput(
    @JvmField val normalizedText: String,
    @JvmField val normalizedHtmlPlainText: String,
    @JvmField val normalizedUri: String,
    @JvmField val normalizedIntentAction: String,
    @JvmField val normalizedIntentData: String,
    @JvmField val normalizedIntentComponent: String,
    @JvmField val rawText: String,
    @JvmField val rawHtml: String,
    @JvmField val rawUri: String,
    @JvmField val rawIntentAction: String,
    @JvmField val rawIntentData: String,
    @JvmField val rawIntentComponent: String,
    @JvmField val hasText: Boolean,
    @JvmField val hasHtml: Boolean,
    @JvmField val hasUri: Boolean,
    @JvmField val hasIntent: Boolean
)

internal data class NativeClipboardSnapshotCore(
    val decisionFingerprint: String,
    val semanticSignature: String,
    val rawSignature: String,
    val itemCount: Int,
    val hasText: Boolean,
    val hasHtml: Boolean,
    val hasUri: Boolean,
    val hasIntent: Boolean,
    val isEmpty: Boolean
)

internal data class ClipboardRuntimeStatus(
    val lastObservedAt: String?,
    val lastConfirmedAt: String?,
    val lastObservedSignature: String?,
    val lastDecision: String,
    val baselineSemanticSignature: String?,
    val detectedSemanticSignature: String?,
    val currentSemanticSignature: String?
)

internal object ClipboardHardeningParityAccess {
    fun buildNormalizedItemInputForTests(
        text: String = "",
        html: String = "",
        uri: String = "",
        intentAction: String = "",
        intentData: String = "",
        intentComponent: String = "",
        includeDiagnosticFields: Boolean = true
    ): ClipboardNormalizedItemInput {
        return buildClipboardNormalizedItemInput(
            text = text,
            html = html,
            uri = uri,
            intentAction = intentAction,
            intentData = intentData,
            intentComponent = intentComponent,
            includeDiagnosticFields = includeDiagnosticFields
        )
    }

    fun buildSnapshotCoreReference(
        mode: ClipboardSnapshotMode,
        items: Array<ClipboardNormalizedItemInput>
    ): NativeClipboardSnapshotCore = buildClipboardSnapshotCoreKotlin(mode, items)

    fun buildSnapshotCoreWithBackend(
        mode: ClipboardSnapshotMode,
        items: Array<ClipboardNormalizedItemInput>,
        backendMode: NativeBridgeBackendMode
    ): NativeClipboardSnapshotCore = NativeBridgeTestControl.withBackendMode(backendMode) {
        buildClipboardSnapshotCore(mode, items)
    }
}

internal fun readClipboardSnapshot(context: Context): ClipboardSnapshot {
    return readClipboardSnapshot(context, ClipboardSnapshotMode.DiagnosticFull)
}

internal fun readClipboardSnapshotLite(context: Context): ClipboardSnapshot {
    return readClipboardSnapshot(context, ClipboardSnapshotMode.RuntimeLite)
}

internal fun readClipboardSnapshotFull(context: Context): ClipboardSnapshot {
    return readClipboardSnapshot(context, ClipboardSnapshotMode.DiagnosticFull)
}

private fun readClipboardSnapshot(
    context: Context,
    mode: ClipboardSnapshotMode
): ClipboardSnapshot {
    return runCatching {
        val clipboardManager = context.getSystemService(ClipboardManager::class.java)
        val clipData = clipboardManager?.primaryClip
        if (clipData == null || clipData.itemCount <= 0) {
            return emptyClipboardSnapshot()
        }

        val includeDiagnosticFields = mode == ClipboardSnapshotMode.DiagnosticFull
        val items = Array(clipData.itemCount) { index ->
            val item = clipData.getItemAt(index)
            buildClipboardNormalizedItemInput(
                text = item.text?.toString().orEmpty(),
                html = item.htmlText.orEmpty(),
                uri = item.uri?.toString().orEmpty(),
                intentAction = item.intent?.action.orEmpty(),
                intentData = item.intent?.dataString.orEmpty(),
                intentComponent = item.intent?.component?.flattenToShortString().orEmpty(),
                includeDiagnosticFields = includeDiagnosticFields
            )
        }

        buildClipboardSnapshotCore(mode, items).toClipboardSnapshot()
    }.getOrElse {
        emptyClipboardSnapshot(readErrorOccurred = true)
    }
}

private fun buildClipboardSnapshotCore(
    mode: ClipboardSnapshotMode,
    items: Array<ClipboardNormalizedItemInput>
): NativeClipboardSnapshotCore {
    return NativeSecurityBridge.buildClipboardSnapshotCore(mode, items) {
        buildClipboardSnapshotCoreKotlin(mode, items)
    }
}

private fun buildClipboardSnapshotCoreKotlin(
    mode: ClipboardSnapshotMode,
    items: Array<ClipboardNormalizedItemInput>
): NativeClipboardSnapshotCore {
    if (items.isEmpty()) {
        return emptyClipboardSnapshotCore()
    }

    var hasText = false
    var hasHtml = false
    var hasUri = false
    var hasIntent = false
    val decisionSignatureBuilder = StringBuilder()
    val rawSignatureBuilder =
        if (mode == ClipboardSnapshotMode.DiagnosticFull) StringBuilder() else null

    items.forEachIndexed { index, item ->
        hasText = hasText || item.hasText
        hasHtml = hasHtml || item.hasHtml
        hasUri = hasUri || item.hasUri
        hasIntent = hasIntent || item.hasIntent

        appendSignatureSeparatorIfNeeded(decisionSignatureBuilder)
        appendDecisionItemSignature(decisionSignatureBuilder, index, item)

        rawSignatureBuilder?.let { builder ->
            appendSignatureSeparatorIfNeeded(builder)
            appendRawItemSignature(builder, index, item)
        }
    }

    val isEmpty = !hasText && !hasHtml && !hasUri && !hasIntent
    val decisionSignature = decisionSignatureBuilder.toString()
    val rawSignature = rawSignatureBuilder?.toString().orEmpty()
    val semanticSignature =
        if (isEmpty) {
            emptyClipboardSemanticSignature()
        } else {
            decisionSignature.clipForClipboardDiagnostics()
        }
    val decisionFingerprint =
        if (isEmpty) {
            emptyClipboardFingerprint()
        } else {
            sha256Hex(decisionSignature)
        }

    return NativeClipboardSnapshotCore(
        decisionFingerprint = decisionFingerprint,
        semanticSignature = semanticSignature,
        rawSignature = rawSignature,
        itemCount = items.size,
        hasText = hasText,
        hasHtml = hasHtml,
        hasUri = hasUri,
        hasIntent = hasIntent,
        isEmpty = isEmpty
    )
}

private fun buildClipboardNormalizedItemInput(
    text: String,
    html: String,
    uri: String,
    intentAction: String,
    intentData: String,
    intentComponent: String,
    includeDiagnosticFields: Boolean
): ClipboardNormalizedItemInput {
    return ClipboardNormalizedItemInput(
        normalizedText = text.normalizedClipboardValue(),
        normalizedHtmlPlainText = html.htmlToClipboardPlainText().normalizedClipboardValue(),
        normalizedUri = uri.normalizedClipboardValue(),
        normalizedIntentAction = intentAction.normalizedClipboardValue(),
        normalizedIntentData = intentData.normalizedClipboardValue(),
        normalizedIntentComponent = intentComponent.normalizedClipboardValue(),
        rawText = text.takeIf { includeDiagnosticFields }.orEmpty(),
        rawHtml = html.takeIf { includeDiagnosticFields }.orEmpty(),
        rawUri = uri.takeIf { includeDiagnosticFields }.orEmpty(),
        rawIntentAction = intentAction.takeIf { includeDiagnosticFields }.orEmpty(),
        rawIntentData = intentData.takeIf { includeDiagnosticFields }.orEmpty(),
        rawIntentComponent = intentComponent.takeIf { includeDiagnosticFields }.orEmpty(),
        hasText = text.isNotBlank(),
        hasHtml = html.isNotBlank(),
        hasUri = uri.isNotBlank(),
        hasIntent =
            intentAction.isNotBlank() ||
                intentData.isNotBlank() ||
                intentComponent.isNotBlank()
    )
}

private fun NativeClipboardSnapshotCore.toClipboardSnapshot(): ClipboardSnapshot {
    return ClipboardSnapshot(
        decisionFingerprint = decisionFingerprint,
        semanticSignature = semanticSignature,
        rawSignature = rawSignature,
        itemCount = itemCount,
        hasText = hasText,
        hasHtml = hasHtml,
        hasUri = hasUri,
        hasIntent = hasIntent,
        isEmpty = isEmpty
    )
}

private fun emptyClipboardSnapshot(readErrorOccurred: Boolean = false): ClipboardSnapshot =
    emptyClipboardSnapshotCore().toClipboardSnapshot().copy(readErrorOccurred = readErrorOccurred)

private fun emptyClipboardSnapshotCore(): NativeClipboardSnapshotCore {
    return NativeClipboardSnapshotCore(
        decisionFingerprint = emptyClipboardFingerprint(),
        semanticSignature = emptyClipboardSemanticSignature(),
        rawSignature = "",
        itemCount = 0,
        hasText = false,
        hasHtml = false,
        hasUri = false,
        hasIntent = false,
        isEmpty = true
    )
}

private fun emptyClipboardFingerprint(): String = "clipboard_empty"

private fun emptyClipboardSemanticSignature(): String = "clipboard_empty"

private fun appendSignatureSeparatorIfNeeded(builder: StringBuilder) {
    if (builder.isNotEmpty()) {
        builder.append(" || ")
    }
}

private fun appendRawItemSignature(
    builder: StringBuilder,
    index: Int,
    item: ClipboardNormalizedItemInput
) {
    builder.append("item")
    builder.append(index)
    builder.append('=')
    builder.append(item.rawText)
    builder.append('|')
    builder.append(item.rawHtml)
    builder.append('|')
    builder.append(item.rawUri)
    builder.append('|')
    builder.append(item.rawIntentAction)
    builder.append('|')
    builder.append(item.rawIntentData)
    builder.append('|')
    builder.append(item.rawIntentComponent)
}

private fun appendDecisionItemSignature(
    builder: StringBuilder,
    index: Int,
    item: ClipboardNormalizedItemInput
) {
    val semanticKind: String
    val semanticValue: String
    when {
        item.normalizedText.isNotEmpty() -> {
            semanticKind = "text"
            semanticValue = item.normalizedText
        }
        item.normalizedHtmlPlainText.isNotEmpty() -> {
            semanticKind = "html"
            semanticValue = item.normalizedHtmlPlainText
        }
        item.normalizedUri.isNotEmpty() -> {
            semanticKind = "uri"
            semanticValue = item.normalizedUri
        }
        item.normalizedIntentAction.isNotEmpty() ||
            item.normalizedIntentData.isNotEmpty() ||
            item.normalizedIntentComponent.isNotEmpty() -> {
            semanticKind = "intent"
            semanticValue = buildString {
                append(item.normalizedIntentAction)
                append('|')
                append(item.normalizedIntentData)
                append('|')
                append(item.normalizedIntentComponent)
            }
        }
        else -> {
            semanticKind = "empty"
            semanticValue = "-"
        }
    }

    builder.append("item")
    builder.append(index)
    builder.append('=')
    builder.append(semanticKind)
    builder.append('|')
    builder.append(semanticValue)
}

private fun String.normalizedClipboardValue(): String {
    return trim()
        .replace(Regex("\\s+"), " ")
}

private fun String.htmlToClipboardPlainText(): String {
    if (isBlank()) {
        return ""
    }

    return replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#39;", "'")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#x27;", "'")
        .replace("&#x2F;", "/")
        .replace(Regex("&#(\\d+);")) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null && code in 1..0x10FFFF) {
                runCatching { String(Character.toChars(code)) }.getOrDefault(match.value)
            } else match.value
        }
        .replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null && code in 1..0x10FFFF) {
                runCatching { String(Character.toChars(code)) }.getOrDefault(match.value)
            } else match.value
        }
}

private fun String.clipForClipboardDiagnostics(maxLength: Int = 320): String {
    val compact = replace(Regex("\\s+"), " ").trim()
    if (compact.length <= maxLength) {
        return compact
    }
    return compact.take(maxLength - 1) + "…"
}

private fun sha256Hex(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { byte ->
        String.format(Locale.US, "%02x", byte)
    }
}
