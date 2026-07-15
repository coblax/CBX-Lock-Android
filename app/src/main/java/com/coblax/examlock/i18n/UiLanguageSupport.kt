package com.coblax.examlock.i18n
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.model.UiLanguage
internal val LocalUiLanguage = staticCompositionLocalOf { UiLanguage.English }

internal fun localized(language: UiLanguage, english: String, indonesian: String): String {
    return if (language == UiLanguage.English) english else indonesian
}

@Composable
internal fun tr(english: String, indonesian: String): String {
    return localized(LocalUiLanguage.current, english, indonesian)
}

internal fun diagnosticSectionLabel(section: DiagnosticSection, uiLanguage: UiLanguage): String {
    return when (section) {
        DiagnosticSection.Keyboard -> localized(uiLanguage, "Keyboard", "Keyboard")
        DiagnosticSection.Bluetooth -> localized(uiLanguage, "Bluetooth", "Bluetooth")
        DiagnosticSection.Network -> localized(uiLanguage, "Network / Connectivity", "Network / Konektivitas")
        DiagnosticSection.Accessibility -> localized(uiLanguage, "Accessibility Service", "Accessibility Service")
        DiagnosticSection.Overlay -> localized(uiLanguage, "Overlay / Floating App", "Overlay / Floating App")
        DiagnosticSection.Geofence -> localized(uiLanguage, "Geofence", "Geofence")
        DiagnosticSection.FakeLocation -> localized(uiLanguage, "Anti-Fake-Location", "Anti-Fake-Location")
        DiagnosticSection.AppSwitch -> localized(uiLanguage, "App Switch", "App Switch")
        DiagnosticSection.DeveloperAdb -> localized(uiLanguage, "Developer Mode / ADB", "Developer Mode / ADB")
        DiagnosticSection.Root -> localized(uiLanguage, "Root Device", "Root Device")
        DiagnosticSection.Signature -> localized(uiLanguage, "Official APK Signature", "Signature APK Resmi")
        DiagnosticSection.VirtualEnvironment ->
            localized(uiLanguage, "Virtual Environment", "Virtual Environment")
        DiagnosticSection.Clipboard -> localized(uiLanguage, "Clipboard", "Clipboard")
        DiagnosticSection.ScreenPinning -> localized(uiLanguage, "Screen Pinning", "Screen Pinning")
        DiagnosticSection.DeviceTime -> localized(uiLanguage, "Device Time", "Waktu Perangkat")
        DiagnosticSection.SecurityHealth -> localized(uiLanguage, "Security Health", "Kesehatan Keamanan")
        DiagnosticSection.ScreenRecorder -> localized(uiLanguage, "Screen Recorder", "Screen Recorder")
        DiagnosticSection.DisplayMirror -> localized(uiLanguage, "Display Mirror", "Display Mirror")
        DiagnosticSection.MultiWindow -> localized(uiLanguage, "Multi-Window", "Multi-Window")
    }
}
