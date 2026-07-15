package com.coblax.examlock

object SecureStrings {
    internal const val OBF_FAST_EXAM_URL =
        "GwcHAwBJXFwEBARdAB4YHUIHEh0ZBh0UAxIdFxIdXQAQG10aF1w="

    val fastExamUrl: String by lazy {
        RuntimeStringDecoder.decodeBase64Xor(OBF_FAST_EXAM_URL)
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
