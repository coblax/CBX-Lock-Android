package com.coblax.examlock

import com.coblax.examlock.model.UiLanguage

internal fun accessibilityServiceFriendlyLabel(serviceComponent: String): String {
    val normalized = serviceComponent.trim()
    if (normalized.isBlank()) {
        return "-"
    }
    val packageName = normalized.substringBefore('/').trim()
    val serviceName = normalized.substringAfter('/', missingDelimiterValue = "").trim()
    val combined = "$packageName/$serviceName"

    return when {
        combined.contains("selecttospeak", ignoreCase = true) ->
            "Select to Speak / Pilih untuk Diucapkan"
        combined.contains("talkback", ignoreCase = true) ->
            "TalkBack / Pembaca Layar"
        combined.contains("switchaccess", ignoreCase = true) ->
            "Switch Access / Akses Tombol"
        combined.contains("voiceaccess", ignoreCase = true) ->
            "Voice Access / Kontrol Suara"
        combined.contains("autoclick", ignoreCase = true) ||
            combined.contains("auto.click", ignoreCase = true) ||
            combined.contains("clicker", ignoreCase = true) ->
            "Auto Clicker / Klik Otomatis"
        combined.contains("assistivetouch", ignoreCase = true) ||
            combined.contains("floating", ignoreCase = true) ->
            "Assistive Touch / Floating Menu"
        packageName == "com.coblax.examlock" ->
            "CBX Lock Exam Guard"
        packageName == "com.eset.ems2.gp" ->
            "ESET Mobile Security"
        else -> serviceName
            .substringAfterLast('.')
            .removeSuffix("Service")
            .splitCamelCase()
            .ifBlank { packageName.ifBlank { normalized } }
    }
}

internal fun accessibilityServiceFriendlySummary(
    serviceComponents: List<String>,
    maxItems: Int = 2,
    includePackage: Boolean = false
): String {
    val labels = serviceComponents
        .filter { it.isNotBlank() }
        .distinct()
        .take(maxItems)
        .map { component ->
            val label = accessibilityServiceFriendlyLabel(component)
            val packageName = component.substringBefore('/').trim()
            if (includePackage && packageName.isNotBlank()) {
                "$label ($packageName)"
            } else {
                label
            }
        }
    if (labels.isEmpty()) {
        return "-"
    }
    val remaining = serviceComponents.distinct().size - labels.size
    return if (remaining > 0) {
        labels.joinToString() + " +$remaining"
    } else {
        labels.joinToString()
    }
}

internal fun accessibilityBlockingCauseText(
    inspection: AccessibilityInspectionResult,
    uiLanguage: UiLanguage
): String {
    val summary = accessibilityServiceFriendlySummary(
        serviceComponents = inspection.effectiveServiceComponents,
        maxItems = 2,
        includePackage = true
    )
    return if (uiLanguage == UiLanguage.English) {
        if (summary == "-") {
            "Accessibility is enabled, but the active service name could not be read yet."
        } else {
            "Detected active service: $summary."
        }
    } else {
        if (summary == "-") {
            "Aksesibilitas aktif, tetapi nama service aktif belum bisa dibaca."
        } else {
            "Service aktif terdeteksi: $summary."
        }
    }
}

internal fun accessibilityBlockingFixText(
    inspection: AccessibilityInspectionResult,
    uiLanguage: UiLanguage
): String {
    val summary = accessibilityServiceFriendlySummary(
        serviceComponents = inspection.effectiveServiceComponents,
        maxItems = 1
    )
    return if (uiLanguage == UiLanguage.English) {
        val target = if (summary == "-") {
            "the active accessibility service"
        } else {
            summary
        }
        "Turn off $target in Settings > Accessibility, then return to CBX Lock and refresh. Also check Select to Speak, TalkBack, Switch Access, Voice Access, auto clicker, app lock, antivirus/cleaner, or floating menu services."
    } else {
        val target = if (summary == "-") {
            "layanan aksesibilitas yang aktif"
        } else {
            summary
        }
        "Matikan $target di Pengaturan > Aksesibilitas, lalu kembali ke CBX Lock dan refresh. Cek juga Select to Speak/Pilih untuk Diucapkan, TalkBack, Akses Tombol/Switch Access, Voice Access, auto clicker, app lock, antivirus/cleaner, atau floating menu."
    }
}

internal fun accessibilityQuickFixButtonText(
    inspection: AccessibilityInspectionResult,
    uiLanguage: UiLanguage
): String {
    val summary = accessibilityServiceFriendlySummary(
        serviceComponents = inspection.effectiveServiceComponents,
        maxItems = 1
    )
    return if (uiLanguage == UiLanguage.English) {
        if (summary == "-") {
            "Open Accessibility Settings"
        } else {
            "Turn Off $summary"
        }
    } else {
        if (summary == "-") {
            "Buka Pengaturan Aksesibilitas"
        } else {
            "Matikan $summary"
        }
    }
}

private fun String.splitCamelCase(): String =
    replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .replace('-', ' ')
        .trim()
