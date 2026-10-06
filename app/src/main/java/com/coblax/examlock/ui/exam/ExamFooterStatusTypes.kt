package com.coblax.examlock.ui.exam

internal enum class ExamFooterShieldStatus {
    Safe,
    Warning,
    Danger
}

internal enum class ExamServerFooterStatus {
    Checking,
    Online,
    Warning,
    Offline,
    Unstable,

    /** Online, but not on a protected connection: a certificate problem or plain http. */
    Insecure
}
