package com.example.coblaxexamlock.ui.exam

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.buildRootSecurityStatus
import com.example.coblaxexamlock.evaluateFakeLocationSecurity
import com.example.coblaxexamlock.evaluateGeofence
import com.example.coblaxexamlock.evaluateGeofenceSecurity
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceConfigParseResult
import com.example.coblaxexamlock.GeofenceEvaluation
import com.example.coblaxexamlock.GeofenceSecurityStatus
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.inspectAdb
import com.example.coblaxexamlock.LocationSpoofSecurityStatus
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.runtime.ExternalDisplayInfo
import com.example.coblaxexamlock.runtime.MultiWindowModeInfo
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
import com.example.coblaxexamlock.runtime.detectSuspiciousFakeLocationPackages
import com.example.coblaxexamlock.runtime.getCachedVirtualEnvironmentDiagnostics
import com.example.coblaxexamlock.runtime.hasBluetoothExamPermission
import com.example.coblaxexamlock.runtime.hasFineLocationPermission
import com.example.coblaxexamlock.runtime.hasLocationPermissionForWifi
import com.example.coblaxexamlock.runtime.isBluetoothEnabledForExam
import com.example.coblaxexamlock.runtime.isLocationServicesEnabled
import com.example.coblaxexamlock.runtime.readMultiWindowModeInfo

internal class ExamRuntimeSecurityUiState(
    val forcedExitViolationCount: MutableIntState,
    val pendingForcedExitViolation: MutableState<Boolean>,
    val showForcedExitAlarm: MutableState<Boolean>,
    val keyboardViolationCount: MutableIntState,
    val showKeyboardViolationDialog: MutableState<Boolean>,
    val overlayViolationCount: MutableIntState,
    val showOverlayViolationDialog: MutableState<Boolean>,
    val overlayShieldRequested: MutableState<Boolean>,
    val overlayShieldLastApplySucceeded: MutableState<Boolean?>,
    val overlayShieldLastAppliedAt: MutableState<String?>,
    val lastOverlayTrigger: MutableState<String?>,
    val lastOverlayAt: MutableState<String?>,
    val lastOverlayContext: MutableState<String?>,
    val lastExamRefreshDecision: MutableState<String?>,
    val overlayWindowHasFocus: MutableState<Boolean>,
    val overlayWindowFocusLossPending: MutableState<Boolean>,
    val overlayFocusLossConfirmRunnable: MutableState<Runnable?>,
    val bluetoothPermissionGranted: MutableState<Boolean>,
    val bluetoothEnabled: MutableState<Boolean>,
    val accessibilityInspection: MutableState<AccessibilityInspectionResult>,
    val accessibilityServiceEnabled: MutableState<Boolean>,
    val adbInspection: MutableState<AdbInspection>,
    val developerOptionsEnabled: MutableState<Boolean>,
    val adbEnabled: MutableState<Boolean>,
    val rootSecurityStatus: MutableState<RootSecurityStatus>,
    val rootDetected: MutableState<Boolean>,
    val selinuxPermissiveWarning: MutableState<Boolean>,
    val signatureMismatchDetected: MutableState<Boolean>,
    val virtualEnvironmentDetected: MutableState<Boolean>,
    val tamperDetected: MutableState<Boolean>,
    val tamperSummary: MutableState<String>,
    val tamperLastLoggedSummary: MutableState<String?>,
    val integrityTamperDetected: MutableState<Boolean>,
    val integritySummary: MutableState<String>,
    val integrityPublicSummary: MutableState<String>,
    val integrityLastLoggedSummary: MutableState<String?>,
    val integrityBaselineFingerprint: MutableState<String?>,
    val bluetoothViolationCount: MutableIntState,
    val showBluetoothViolationDialog: MutableState<Boolean>,
    val screenRecorderPackages: MutableState<List<String>>,
    val screenRecorderViolationCount: MutableIntState,
    val showScreenRecorderViolationDialog: MutableState<Boolean>,
    val externalDisplayDetected: MutableState<Boolean>,
    val externalDisplayCount: MutableIntState,
    val externalDisplayInfoList: MutableState<List<ExternalDisplayInfo>>,
    val displayMirrorViolationCount: MutableIntState,
    val showDisplayMirrorViolationDialog: MutableState<Boolean>,
    val multiWindowDetected: MutableState<Boolean>,
    val multiWindowModeInfo: MutableState<MultiWindowModeInfo>,
    val multiWindowViolationCount: MutableIntState,
    val showMultiWindowViolationDialog: MutableState<Boolean>,
    val geofenceEvaluation: MutableState<GeofenceEvaluation>,
    val geofenceSecurityStatus: MutableState<GeofenceSecurityStatus>,
    val fakeLocationSecurityStatus: MutableState<LocationSpoofSecurityStatus>
)

@Composable
internal fun rememberExamRuntimeSecurityUiState(
    context: Context,
    geofenceConfigParseResult: GeofenceConfigParseResult,
    geofenceBypassState: GeofenceBypassState,
    fakeLocationBypassState: FakeLocationBypassState
): ExamRuntimeSecurityUiState {
    val forcedExitViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val pendingForcedExitViolation = rememberSaveable { mutableStateOf(false) }
    val showForcedExitAlarm = rememberSaveable { mutableStateOf(false) }
    val keyboardViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showKeyboardViolationDialog = rememberSaveable { mutableStateOf(false) }
    val overlayViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showOverlayViolationDialog = rememberSaveable { mutableStateOf(false) }
    val overlayShieldRequested = rememberSaveable { mutableStateOf(false) }
    val overlayShieldLastApplySucceeded = rememberSaveable { mutableStateOf<Boolean?>(null) }
    val overlayShieldLastAppliedAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastOverlayTrigger = rememberSaveable { mutableStateOf<String?>(null) }
    val lastOverlayAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastOverlayContext = rememberSaveable { mutableStateOf<String?>(null) }
    val lastExamRefreshDecision = rememberSaveable { mutableStateOf<String?>(null) }
    val overlayWindowHasFocus = rememberSaveable { mutableStateOf(true) }
    val overlayWindowFocusLossPending = rememberSaveable { mutableStateOf(false) }
    val overlayFocusLossConfirmRunnable = remember { mutableStateOf<Runnable?>(null) }
    val bluetoothPermissionGranted = rememberSaveable {
        mutableStateOf(hasBluetoothExamPermission(context))
    }
    val bluetoothEnabled = rememberSaveable {
        mutableStateOf(isBluetoothEnabledForExam(context))
    }
    val initialAccessibilityInspection = remember(context) { inspectAccessibility(context) }
    val accessibilityInspection = remember { mutableStateOf(initialAccessibilityInspection) }
    val accessibilityServiceEnabled = rememberSaveable {
        mutableStateOf(initialAccessibilityInspection.blockingServiceActive)
    }
    val initialAdbInspection = remember(context) { inspectAdb(context) }
    val adbInspection = remember { mutableStateOf(initialAdbInspection) }
    val developerOptionsEnabled = rememberSaveable {
        mutableStateOf(initialAdbInspection.developerOptionsEnabled)
    }
    val adbEnabled = rememberSaveable {
        mutableStateOf(initialAdbInspection.adbEnabled)
    }
    val initialRootStatus = remember(context) {
        buildRootSecurityStatus(SecurityDetectorCache.readRootDetectionDetails(context))
    }
    val rootSecurityStatus = remember { mutableStateOf(initialRootStatus) }
    val rootDetected = rememberSaveable {
        mutableStateOf(initialRootStatus.detected)
    }
    val selinuxPermissiveWarning = rememberSaveable {
        mutableStateOf(initialRootStatus.selinuxPermissive)
    }
    val signatureMismatchDetected = rememberSaveable { mutableStateOf(false) }
    val virtualEnvironmentDetected = rememberSaveable {
        mutableStateOf(getCachedVirtualEnvironmentDiagnostics()?.detected == true)
    }
    val tamperDetected = rememberSaveable { mutableStateOf(false) }
    val tamperSummary = rememberSaveable { mutableStateOf("-") }
    val tamperLastLoggedSummary = rememberSaveable { mutableStateOf<String?>(null) }
    val integrityTamperDetected = rememberSaveable { mutableStateOf(false) }
    val integritySummary = rememberSaveable { mutableStateOf("-") }
    val integrityPublicSummary = rememberSaveable { mutableStateOf("OK") }
    val integrityLastLoggedSummary = rememberSaveable { mutableStateOf<String?>(null) }
    val integrityBaselineFingerprint = rememberSaveable { mutableStateOf<String?>(null) }
    val bluetoothViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showBluetoothViolationDialog = rememberSaveable { mutableStateOf(false) }
    val initialScreenRecorderPackages = remember(context) {
        SecurityDetectorCache.readScreenRecorderPackages(context)
    }
    val screenRecorderPackages = remember {
        mutableStateOf(initialScreenRecorderPackages)
    }
    val screenRecorderViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showScreenRecorderViolationDialog = rememberSaveable { mutableStateOf(false) }
    val initialExternalDisplaySnapshot = remember(context) {
        SecurityDetectorCache.readExternalDisplaySnapshot(context)
    }
    val externalDisplayDetected = rememberSaveable {
        mutableStateOf(initialExternalDisplaySnapshot.detected)
    }
    val externalDisplayCount = rememberSaveable {
        mutableIntStateOf(initialExternalDisplaySnapshot.count)
    }
    val externalDisplayInfoList = remember {
        mutableStateOf(initialExternalDisplaySnapshot.infoList)
    }
    val displayMirrorViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showDisplayMirrorViolationDialog = rememberSaveable { mutableStateOf(false) }
    val initialMultiWindowModeInfo = remember(context) {
        readMultiWindowModeInfo(context)
    }
    val multiWindowDetected = rememberSaveable {
        mutableStateOf(initialMultiWindowModeInfo.inAnySplitMode)
    }
    val multiWindowModeInfo = remember {
        mutableStateOf(initialMultiWindowModeInfo)
    }
    val multiWindowViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showMultiWindowViolationDialog = rememberSaveable { mutableStateOf(false) }
    val initialGeofenceEvaluation = remember(
        geofenceConfigParseResult,
        context
    ) {
        evaluateGeofence(
            configResult = geofenceConfigParseResult,
            permissionGranted = hasLocationPermissionForWifi(context),
            locationServicesEnabled = isLocationServicesEnabled(context),
            locationSnapshot = null
        )
    }
    val geofenceEvaluation = remember(geofenceConfigParseResult) {
        mutableStateOf(initialGeofenceEvaluation)
    }
    val initialGeofenceSecurityStatus = remember(
        geofenceConfigParseResult,
        context,
        geofenceBypassState
    ) {
        evaluateGeofenceSecurity(
            configResult = geofenceConfigParseResult,
            permissionGranted = hasLocationPermissionForWifi(context),
            preciseLocationGranted = hasFineLocationPermission(context),
            locationServicesEnabled = isLocationServicesEnabled(context),
            locationSnapshot = null,
            bypassState = geofenceBypassState
        )
    }
    val geofenceSecurityStatus = remember(
        geofenceConfigParseResult,
        geofenceBypassState
    ) {
        mutableStateOf(initialGeofenceSecurityStatus)
    }
    val initialFakeLocationSecurityStatus = remember(
        geofenceConfigParseResult,
        context,
        fakeLocationBypassState
    ) {
        evaluateFakeLocationSecurity(
            monitoringEnabled = true,
            permissionGranted = hasLocationPermissionForWifi(context),
            locationServicesEnabled = isLocationServicesEnabled(context),
            locationSnapshot = null,
            fixQualityStatus = initialGeofenceSecurityStatus.fixQualityStatus,
            developerOptionsEnabled = inspectAdb(context).developerOptionsEnabled,
            suspiciousFakeLocationPackages = detectSuspiciousFakeLocationPackages(context),
            bypassState = fakeLocationBypassState
        )
    }
    val fakeLocationSecurityStatus = remember(
        geofenceConfigParseResult,
        fakeLocationBypassState
    ) {
        mutableStateOf(initialFakeLocationSecurityStatus)
    }
    return ExamRuntimeSecurityUiState(
        forcedExitViolationCount = forcedExitViolationCount,
        pendingForcedExitViolation = pendingForcedExitViolation,
        showForcedExitAlarm = showForcedExitAlarm,
        keyboardViolationCount = keyboardViolationCount,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        overlayViolationCount = overlayViolationCount,
        showOverlayViolationDialog = showOverlayViolationDialog,
        overlayShieldRequested = overlayShieldRequested,
        overlayShieldLastApplySucceeded = overlayShieldLastApplySucceeded,
        overlayShieldLastAppliedAt = overlayShieldLastAppliedAt,
        lastOverlayTrigger = lastOverlayTrigger,
        lastOverlayAt = lastOverlayAt,
        lastOverlayContext = lastOverlayContext,
        lastExamRefreshDecision = lastExamRefreshDecision,
        overlayWindowHasFocus = overlayWindowHasFocus,
        overlayWindowFocusLossPending = overlayWindowFocusLossPending,
        overlayFocusLossConfirmRunnable = overlayFocusLossConfirmRunnable,
        bluetoothPermissionGranted = bluetoothPermissionGranted,
        bluetoothEnabled = bluetoothEnabled,
        accessibilityInspection = accessibilityInspection,
        accessibilityServiceEnabled = accessibilityServiceEnabled,
        adbInspection = adbInspection,
        developerOptionsEnabled = developerOptionsEnabled,
        adbEnabled = adbEnabled,
        rootSecurityStatus = rootSecurityStatus,
        rootDetected = rootDetected,
        selinuxPermissiveWarning = selinuxPermissiveWarning,
        signatureMismatchDetected = signatureMismatchDetected,
        virtualEnvironmentDetected = virtualEnvironmentDetected,
        tamperDetected = tamperDetected,
        tamperSummary = tamperSummary,
        tamperLastLoggedSummary = tamperLastLoggedSummary,
        integrityTamperDetected = integrityTamperDetected,
        integritySummary = integritySummary,
        integrityPublicSummary = integrityPublicSummary,
        integrityLastLoggedSummary = integrityLastLoggedSummary,
        integrityBaselineFingerprint = integrityBaselineFingerprint,
        bluetoothViolationCount = bluetoothViolationCount,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        screenRecorderPackages = screenRecorderPackages,
        screenRecorderViolationCount = screenRecorderViolationCount,
        showScreenRecorderViolationDialog = showScreenRecorderViolationDialog,
        externalDisplayDetected = externalDisplayDetected,
        externalDisplayCount = externalDisplayCount,
        externalDisplayInfoList = externalDisplayInfoList,
        displayMirrorViolationCount = displayMirrorViolationCount,
        showDisplayMirrorViolationDialog = showDisplayMirrorViolationDialog,
        multiWindowDetected = multiWindowDetected,
        multiWindowModeInfo = multiWindowModeInfo,
        multiWindowViolationCount = multiWindowViolationCount,
        showMultiWindowViolationDialog = showMultiWindowViolationDialog,
        geofenceEvaluation = geofenceEvaluation,
        geofenceSecurityStatus = geofenceSecurityStatus,
        fakeLocationSecurityStatus = fakeLocationSecurityStatus
    )
}
