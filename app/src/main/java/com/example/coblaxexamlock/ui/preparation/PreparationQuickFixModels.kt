package com.example.coblaxexamlock.ui.preparation

internal enum class QuickFixTarget {
    All,
    Network,
    Location,
    DeviceTime,
    ScreenPinning,
    WebView,
    Battery,
    ScreenRecorder,
    DisplayMirror,
    MultiWindow
}

internal enum class QuickFixSeverity {
    Blocking,
    Warning
}

internal const val QuickFixStartScreenPinningCode = "start_screen_pinning"
internal const val QuickFixScreenPinningDeferredCode = "screen_pinning_deferred_until_blockers_clear"

internal data class PreparationQuickFixAction(
    val code: String,
    val text: String,
    val severity: QuickFixSeverity,
    val target: QuickFixTarget?,
    val priority: Int,
    val filled: Boolean = false,
    val loading: Boolean = false,
    val enabled: Boolean = true,
    val opensExternalSettings: Boolean = false,
    val isNotice: Boolean = false,
    val diagnosticDetails: String? = null,
    val onClick: () -> Unit
)

