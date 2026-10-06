package com.coblax.examlock.ui.app

import com.coblax.examlock.i18n.localized
import com.coblax.examlock.model.UiLanguage

/**
 * What a student sees when a scanned code is not a usable exam QR. The codec's own
 * messages are Indonesian-only, and a damaged or foreign code surfaced the cipher's
 * text ("Tag mismatch", "Illegal base64 character") as is.
 */
internal fun examQrReadFailureMessage(throwable: Throwable, language: UiLanguage): String {
    val message = throwable.message.orEmpty()
    return when {
        message.startsWith("Format QR lama") -> localized(
            language,
            "This QR was made by an old CBX Lock. Ask the admin to create it again.",
            "QR ini dibuat oleh CBX Lock versi lama. Minta admin membuat ulang QR-nya."
        )
        message.startsWith("QR ini bukan format") -> localized(
            language,
            "This is not a CBX Lock exam QR. Scan the QR the admin gave for this exam.",
            "Ini bukan QR ujian CBX Lock. Pindai QR yang diberikan admin untuk ujian ini."
        )
        message.startsWith("Versi payload QR tidak didukung") -> localized(
            language,
            "This QR was made by a newer CBX Lock. Update CBX Lock, then scan again.",
            "QR ini dibuat oleh CBX Lock yang lebih baru. Perbarui CBX Lock, lalu pindai lagi."
        )
        else -> localized(
            language,
            "This QR could not be read. It may be damaged or not made by CBX Lock; ask the admin for a new one.",
            "QR ini tidak dapat dibaca. Mungkin rusak atau bukan buatan CBX Lock; minta QR baru ke admin."
        )
    }
}
