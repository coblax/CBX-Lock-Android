package com.coblax.examlock

object SecureStrings {
    /** Default saved exam link (Direct Link) for a fresh install. */
    internal const val OBF_FAST_EXAM_URL =
        "GwcHAwBJXFwWCxIeXQAeGB1CBxIdGQYdFAMSHRcSHV0AEBtdGhc="

    /** The default before 3.3.10; a phone that saved it unchanged moves to the new one. */
    internal const val OBF_LEGACY_FAST_EXAM_URL =
        "GwcHAwBJXFwAGBIdABIHA10EFhFdGhdcTBYLEh4YFgpOQUFARUJC"

    val fastExamUrl: String by lazy {
        RuntimeStringDecoder.decodeBase64Xor(OBF_FAST_EXAM_URL)
    }

    private val legacyFastExamUrl: String by lazy {
        RuntimeStringDecoder.decodeBase64Xor(OBF_LEGACY_FAST_EXAM_URL)
    }

    /**
     * The saved exam link, or the default when none is saved. Pressing Apply in Secret
     * Admin saves every setting, the untouched default link included, so a phone that
     * did that kept the old default forever; a link still equal to it means the current
     * default. A link an admin typed in is kept as it is.
     */
    fun resolveSavedFastExamUrl(saved: String?): String {
        val trimmed = saved?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> fastExamUrl
            trimmed == legacyFastExamUrl -> fastExamUrl
            else -> saved!!
        }
    }

    /** Default X-CBX-App-Key value; admins change it in Secret Admin. */
    internal const val OBF_DEFAULT_EXAM_APP_KEY = "QUFARUJC"

    val defaultExamAppKey: String by lazy {
        RuntimeStringDecoder.decodeBase64Xor(OBF_DEFAULT_EXAM_APP_KEY)
    }

    val telegramBotToken: String by lazy {
        RuntimeStringDecoder.decodeBase64Xor(BuildConfig.TELEGRAM_BOT_TOKEN_OBF)
    }

    val telegramBugChatId: String by lazy {
        RuntimeStringDecoder.decodeBase64Xor(BuildConfig.TELEGRAM_BUG_CHAT_ID_OBF)
    }

    val signingFingerprintRelease: String by lazy {
        RuntimeStringDecoder.decodeBase64Xor(BuildConfig.SIGNING_SHA256_RELEASE_OBF)
    }

    val signingFingerprintDebug: String by lazy {
        RuntimeStringDecoder.decodeBase64Xor(BuildConfig.SIGNING_SHA256_DEBUG_OBF)
    }

    val mapsApiKey: String by lazy {
        RuntimeStringDecoder.decodeBase64Xor(BuildConfig.MAPS_API_KEY_OBF)
    }
}
