package com.example.coblaxexamlock.ui.exam

import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessVerdict

internal enum class ExamFooterLayoutMode {
    SingleRow,
    TwoRowCompact,
    TabletWide
}

internal data class ExamFooterLayoutSpec(
    val layoutMode: ExamFooterLayoutMode,
    val compact: Boolean,
    val severe: Boolean,
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val itemSpacingDp: Int,
    val actionSpacingDp: Int,
    val buttonSizeDp: Int,
    val arrowPillWidthDp: Int,
    val connectivityPillWidthDp: Int,
    val shieldPillWidthDp: Int,
    val touchTargetDp: Int,
    val iconSizeDp: Int,
    val arrowIconSizeDp: Int,
    val minHeightDp: Int,
    val maxHeightDp: Int,
    val rowSpacingDp: Int,
    val cornerRadiusDp: Int,
    val tonalElevationDp: Int,
    val shadowElevationDp: Int,
    val showBatteryPercent: Boolean,
    val showConnectivityDot: Boolean
)

internal enum class ExamFooterConnectivityTransport {
    Wifi,
    Cellular,
    Unknown
}

internal enum class ExamFooterConnectivitySeverity {
    Stable,
    Warning,
    Danger
}

internal data class ExamFooterConnectivityVisual(
    val transport: ExamFooterConnectivityTransport,
    val severity: ExamFooterConnectivitySeverity,
    val signalLevel: Int,
    val badgeText: String?,
    val cellularLabel: String?
)

internal fun calculateExamFooterLayoutSpec(
    maxWidthDp: Int,
    lowRamEnabled: Boolean,
    lowRamSevere: Boolean
): ExamFooterLayoutSpec {
    val tiny = maxWidthDp < 300
    val twoRow = maxWidthDp < 300
    val severe = tiny || lowRamSevere
    val compact = severe || maxWidthDp <= 420 || lowRamEnabled
    val layoutMode = when {
        twoRow -> ExamFooterLayoutMode.TwoRowCompact
        maxWidthDp >= 600 && !lowRamEnabled -> ExamFooterLayoutMode.TabletWide
        else -> ExamFooterLayoutMode.SingleRow
    }

    return ExamFooterLayoutSpec(
        layoutMode = layoutMode,
        compact = compact,
        severe = severe,
        horizontalPaddingDp = when {
            severe -> 4
            compact -> 6
            else -> 8
        },
        verticalPaddingDp = when {
            severe -> 4
            compact -> 5
            else -> 6
        },
        itemSpacingDp = when {
            severe -> 3
            compact -> 4
            else -> 6
        },
        actionSpacingDp = when {
            severe -> 3
            compact -> 4
            else -> 6
        },
        buttonSizeDp = when {
            severe -> 34
            compact -> 36
            layoutMode == ExamFooterLayoutMode.TabletWide -> 40
            else -> 38
        },
        arrowPillWidthDp = when {
            severe -> 56
            compact -> 60
            else -> 64
        },
        connectivityPillWidthDp = when {
            severe -> 46
            compact -> 48
            else -> 50
        },
        shieldPillWidthDp = when {
            severe -> 52
            compact -> 56
            else -> 60
        },
        touchTargetDp = 48,
        iconSizeDp = when {
            tiny -> 15
            severe -> 16
            compact -> 17
            layoutMode == ExamFooterLayoutMode.TabletWide -> 20
            else -> 19
        },
        arrowIconSizeDp = when {
            tiny -> 15
            severe -> 16
            compact -> 17
            layoutMode == ExamFooterLayoutMode.TabletWide -> 19
            else -> 18
        },
        minHeightDp = when {
            layoutMode == ExamFooterLayoutMode.TwoRowCompact && severe -> 78
            layoutMode == ExamFooterLayoutMode.TwoRowCompact -> 82
            layoutMode == ExamFooterLayoutMode.TabletWide -> 52
            else -> 52
        },
        maxHeightDp = when (layoutMode) {
            ExamFooterLayoutMode.TwoRowCompact -> 88
            ExamFooterLayoutMode.TabletWide -> 58
            ExamFooterLayoutMode.SingleRow -> 58
        },
        rowSpacingDp = if (severe) 2 else 4,
        cornerRadiusDp = if (compact) 14 else 16,
        tonalElevationDp = if (lowRamEnabled) 1 else 4,
        shadowElevationDp = if (lowRamEnabled) 0 else 6,
        showBatteryPercent = !tiny && maxWidthDp >= 340,
        showConnectivityDot = !severe || layoutMode == ExamFooterLayoutMode.TwoRowCompact
    )
}

internal fun resolveExamFooterConnectivityVisual(
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus
): ExamFooterConnectivityVisual {
    val transportSummary =
        (networkStatus.diagnostics.transports + networkStatus.transportLabel)
            .joinToString(" ")
            .lowercase()
    val transport = when {
        networkStatus.diagnostics.wifi != null || transportSummary.contains("wifi") ->
            ExamFooterConnectivityTransport.Wifi
        networkStatus.diagnostics.cellular != null ||
            transportSummary.contains("cellular") ||
            transportSummary.contains("mobile") ->
            ExamFooterConnectivityTransport.Cellular
        else -> ExamFooterConnectivityTransport.Unknown
    }
    val severity = when {
        networkStatus.verdict == NetworkReadinessVerdict.Offline ||
            networkStatus.verdict == NetworkReadinessVerdict.VpnActive ||
            networkStatus.verdict == NetworkReadinessVerdict.AirplaneMode -> ExamFooterConnectivitySeverity.Danger
        networkStatus.verdict == NetworkReadinessVerdict.Unvalidated ||
            networkStatus.verdict == NetworkReadinessVerdict.CaptivePortal ||
            networkStatus.verdict == NetworkReadinessVerdict.Unstable -> ExamFooterConnectivitySeverity.Warning
        else -> ExamFooterConnectivitySeverity.Stable
    }
    val rawSignalLevel = when (transport) {
        ExamFooterConnectivityTransport.Wifi -> networkStatus.diagnostics.wifi?.signalLevel
        ExamFooterConnectivityTransport.Cellular -> networkStatus.diagnostics.cellular?.signalLevel
        ExamFooterConnectivityTransport.Unknown -> null
    }
    val signalLevel = when (severity) {
        ExamFooterConnectivitySeverity.Danger -> 0
        else -> rawSignalLevel?.coerceIn(0, 4) ?: if (severity == ExamFooterConnectivitySeverity.Stable) 3 else 2
    }
    val cellularLabel = networkStatus.diagnostics.cellular
        ?.networkType
        ?.uppercase()
        ?.takeIf { transport == ExamFooterConnectivityTransport.Cellular }
        ?.let { type ->
            when {
                "5G" in type || "NR" in type -> "5G"
                "4G" in type || "LTE" in type -> "4G"
                "3G" in type || "UMTS" in type || "HSPA" in type -> "3G"
                else -> null
            }
        }
    return ExamFooterConnectivityVisual(
        transport = transport,
        severity = severity,
        signalLevel = signalLevel,
        badgeText = if (severity == ExamFooterConnectivitySeverity.Warning) "!" else null,
        cellularLabel = cellularLabel
    )
}
