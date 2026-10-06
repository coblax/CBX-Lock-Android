package com.coblax.examlock

import java.net.URI
import java.util.Locale

internal enum class ExamUrlValidationError {
    Blank,
    Invalid
}

internal data class ExamUrlValidationResult(
    val normalizedUrl: String?,
    val error: ExamUrlValidationError?
) {
    val isValid: Boolean
        get() = error == null
}

/**
 * An exam may be served over plain http (a school server on its own network) and still
 * opens, with a warning to the student. [allowCleartext] is off for links that download
 * an APK, where a swapped file on the way would be installed as CBX Lock.
 */
internal fun validateExamUrl(rawUrl: String, allowCleartext: Boolean = true): ExamUrlValidationResult {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) {
        return ExamUrlValidationResult(
            normalizedUrl = null,
            error = ExamUrlValidationError.Blank
        )
    }

    val uri = runCatching { URI(trimmed) }.getOrNull()
        ?: return ExamUrlValidationResult(
            normalizedUrl = null,
            error = ExamUrlValidationError.Invalid
        )

    val scheme = uri.scheme.orEmpty().lowercase(Locale.US)
    val host = uri.host.orEmpty()
    val schemeAllowed = scheme == "https" || (allowCleartext && scheme == "http")
    if (!schemeAllowed || host.isBlank()) {
        return ExamUrlValidationResult(
            normalizedUrl = null,
            error = ExamUrlValidationError.Invalid
        )
    }

    return ExamUrlValidationResult(
        normalizedUrl = trimmed,
        error = null
    )
}
