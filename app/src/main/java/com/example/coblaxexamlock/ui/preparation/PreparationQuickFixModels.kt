package com.example.coblaxexamlock.ui.preparation

internal enum class QuickFixTarget {
    All,
    Network,
    Location,
    DeviceTime,
    ScreenPinning,
    WebView,
    Battery
}

internal enum class QuickFixSeverity {
    Blocking,
    Warning
}

internal data class PreparationQuickFixAction(
    val code: String,
    val text: String,
    val severity: QuickFixSeverity,
    val target: QuickFixTarget?,
    val priority: Int,
    val filled: Boolean = false,
    val loading: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

