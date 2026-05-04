package com.example.coblaxexamlock.ui.preparation

internal fun preparationListSummary(values: Iterable<String>): String =
    values.joinToString().ifBlank { "-" }

internal fun preparationDetailOrNull(english: String, indonesian: String): String? =
    buildString {
        appendLine(english.trim())
        appendLine()
        append(indonesian.trim())
    }.trim().ifBlank { null }
