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

internal fun validateExamUrl(rawUrl: String): ExamUrlValidationResult {
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
    if (scheme != "https" || host.isBlank()) {
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
