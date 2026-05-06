package com.example.coblaxexamlock.ui.exam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamDiagnosticSnapshotTest {
    @Test
    fun redactsFullUrlToHostOnly() {
        val redacted = redactDiagnosticDetail(
            "load=https://exam.example.sch.id/path/to/test?token=abc123&student=42"
        )

        assertTrue(redacted.contains("exam.example.sch.id"))
        assertFalse(redacted.contains("/path/to/test"))
        assertFalse(redacted.contains("abc123"))
    }

    @Test
    fun masksWifiAndCoordinates() {
        val redacted = redactDiagnosticDetail(
            "ssid=School Lab WiFi | bssid=12:34:56:78:90:ab | lat=-6.123456 | lng=106.123456"
        )

        assertFalse(redacted.contains("School Lab WiFi"))
        assertFalse(redacted.contains("12:34:56:78:90:ab"))
        assertFalse(redacted.contains("-6.123456"))
        assertFalse(redacted.contains("106.123456"))
        assertTrue(redacted.contains("[coord]"))
    }

    @Test
    fun extractsHostWithoutPath() {
        assertEquals("cbx.example.id", redactUrlToHost("https://cbx.example.id/exam/start?id=secret"))
        assertEquals("cbx.example.id", redactUrlToHost("cbx.example.id/exam/start?id=secret"))
    }

    @Test
    fun redactsQrPayloadTokenAndPasswordLabels() {
        val redacted = redactDiagnosticDetail(
            "qr_payload=abcdef token=secret-token password=123456 payload=raw-secret"
        )

        assertFalse(redacted.contains("abcdef"))
        assertFalse(redacted.contains("secret-token"))
        assertFalse(redacted.contains("123456"))
        assertFalse(redacted.contains("raw-secret"))
        assertTrue(redacted.contains("[redacted]"))
    }

    @Test
    fun deviceFieldReportKeepsOnlyRedactedHostAndSummary() {
        val report = ExamDeviceFieldReport(
            generatedAt = "2026-05-05T00:00:00Z",
            source = "unit_test",
            appVersionName = "1.0",
            versionCode = 1,
            buildType = "debug",
            manufacturer = "Samsung",
            brand = "samsung",
            model = "SM-T295",
            sdkInt = 29,
            lowRamEnabled = true,
            lowRamSevere = true,
            qrMaxEdgePx = 960,
            slowPollingMultiplier = 2,
            compatibilityFamily = "SamsungLegacyTablet",
            compatibilityLabel = "Samsung legacy tablet",
            compatibilitySummary = "family=SamsungLegacyTablet",
            survivalScore = "Good",
            survivalPolicySummary = "score=Good | runtime=Constrained",
            survivalRecommendedActions = listOf("field_samsunglegacy:warning:Samsung Legacy Mode"),
            previousSessionBreadcrumbSummary = "preparation_opened,start_pressed",
            previousSessionRecoveryHint = null,
            screenPinningSystemSetting = "Aktif",
            lockTaskState = "PINNED",
            overlayPolicy = "partial_policy=WarnAndAllow",
            webViewAvailable = true,
            webViewPackage = "com.android.webview",
            webViewVersion = "120.0.0.0",
            webViewMajor = 120,
            webViewOutdatedLikely = false,
            webViewProviderSource = "current_provider",
            webViewHealthVerdict = "Ready",
            webViewHealthSeverity = "Stable",
            webViewRiskLabel = "WebView provider ready",
            webViewQuickFix = null,
            networkVerdict = "ConnectedStable",
            networkUserVerdict = "Stable",
            networkTransport = "Wi-Fi",
            networkValidated = true,
            networkCaptivePortal = false,
            networkDnsProbe = "Resolved",
            batteryLevelPercent = 82,
            batteryCharging = true,
            directLinkHost = redactUrlToHost("https://exam.example.sch.id/secret?token=abc"),
            directLinkGeofenceEnabled = true,
            preExamHealthSummary = "family=SamsungLegacyTablet | blocking=0",
            fieldReadinessVerdict = "Warning",
            fieldReadinessSummary = "verdict=Warning | family=SamsungLegacyTablet",
            fieldReadinessItems = listOf("SamsungLegacy:Warning:Samsung Legacy Mode")
        )

        val json = report.toJsonString()

        assertTrue(json.contains("exam.example.sch.id"))
        assertFalse(json.contains("/secret"))
        assertFalse(json.contains("abc"))
        assertTrue(json.contains("SamsungLegacyTablet"))
        assertTrue(json.contains("webView"))
        assertTrue(json.contains("Ready"))
    }
}
