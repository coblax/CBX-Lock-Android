package com.coblax.examlock.ui.exam

import android.Manifest
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.isPermissionPromptBlocked
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.runtime.getBluetoothConnectPermission
import com.coblax.examlock.runtime.hasFineLocationPermission
import com.coblax.examlock.runtime.hasLocationPermissionForWifi
import com.coblax.examlock.runtime.isBluetoothEnabledForExam
import com.coblax.examlock.runtime.requiresBluetoothExamPermission

/** The location and Bluetooth permission prompts the exam session can raise. */
internal class ExamRuntimePermissionLaunchers(
    private val location: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    private val bluetooth: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launchLocation(permissions: Array<String>) = location.launch(permissions)

    fun launchLocation() = location.launch(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    fun launchBluetooth() = bluetooth.launch(getBluetoothConnectPermission())
}

@Composable
internal fun rememberExamRuntimePermissionLaunchers(
    core: ExamRuntimeSessionCore
): ExamRuntimePermissionLaunchers {
    val context = core.context
    val activity = core.activity
    val uiLanguage = core.uiLanguage
    val geofenceEnabled = core.bypass.geofenceEnabled
    val bypassGeofence = core.bypass.bypassGeofence
    var geofencePermissionRequestInFlight by core.flowUiState.geofencePermissionRequestInFlight
    var pendingStartExamAfterLocationPermission by core.flowUiState.pendingStartExamAfterLocationPermission
    var retryStartExamAfterLocationPermissionGrant by core.flowUiState.retryStartExamAfterLocationPermissionGrant
    var securityIssueDialogTitle by core.adminUiState.securityIssueDialogTitle
    var securityIssueDialogMessage by core.adminUiState.securityIssueDialogMessage
    var securityIssueDialogCode by core.adminUiState.securityIssueDialogCode
    var bluetoothPermissionGranted by core.securityUiState.bluetoothPermissionGranted
    var bluetoothEnabled by core.securityUiState.bluetoothEnabled
    fun recordAction(
        code: String,
        details: String = "-",
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO
    ) = core.recordAction(code, details, level)
    fun invalidateWarmLocationValidationCache() = core.invalidateWarmLocationValidationCache()
    fun launchLocationSecurityManualRefresh(trigger: String) = core.launchLocationSecurityManualRefresh(trigger)
    fun showBlockedPermissionDialog(kind: BlockedPermissionKind) = core.showBlockedPermissionDialog(kind)

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            geofencePermissionRequestInFlight = false
            invalidateWarmLocationValidationCache()
            val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                hasFineLocationPermission(context)
            val anyGranted = result.values.any { it } || hasLocationPermissionForWifi(context)
            val preciseRequiredForStart = geofenceEnabled && !bypassGeofence
            val permissionReadyForStart = if (preciseRequiredForStart) fineGranted else anyGranted
            // Denied twice, Android answers at once without a prompt and "Allow" looked dead.
            val blockedPermissionKind = when {
                permissionReadyForStart -> null
                preciseRequiredForStart &&
                    isPermissionPromptBlocked(activity, Manifest.permission.ACCESS_FINE_LOCATION) ->
                    BlockedPermissionKind.PreciseLocation
                !preciseRequiredForStart &&
                    isPermissionPromptBlocked(activity, Manifest.permission.ACCESS_COARSE_LOCATION) ->
                    BlockedPermissionKind.Location
                else -> null
            }
            if (blockedPermissionKind != null) {
                val wasPendingStart = pendingStartExamAfterLocationPermission
                pendingStartExamAfterLocationPermission = false
                recordAction(
                    code = "LOCATION_PERMISSION_PROMPT_BLOCKED",
                    details = "kind=${blockedPermissionKind.name} | start_pending=$wasPendingStart",
                    level = DiagnosticEventLevel.WARNING
                )
                showBlockedPermissionDialog(blockedPermissionKind)
                if (!wasPendingStart) {
                    launchLocationSecurityManualRefresh(trigger = "location_permission_quick_fix")
                }
            } else if (permissionReadyForStart && pendingStartExamAfterLocationPermission) {
                pendingStartExamAfterLocationPermission = false
                retryStartExamAfterLocationPermissionGrant = true
            } else if (pendingStartExamAfterLocationPermission) {
                pendingStartExamAfterLocationPermission = false
                val blockedByGeofencePrecision = preciseRequiredForStart && anyGranted
                recordAction(
                    code = when {
                        blockedByGeofencePrecision -> "START_EXAM_BLOCKED_GEOFENCE_PRECISE_REQUIRED"
                        preciseRequiredForStart -> "START_EXAM_BLOCKED_GEOFENCE_PERMISSION"
                        else -> "START_EXAM_BLOCKED_FAKE_LOCATION_PERMISSION"
                    },
                    details = when {
                        blockedByGeofencePrecision -> "reason=approximate_only"
                        anyGranted -> "reason=permission_not_ready"
                        else -> "reason=permission_denied"
                    },
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogCode = null
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    if (blockedByGeofencePrecision) "Precise Location Required" else "Location Permission Required",
                    if (blockedByGeofencePrecision) "Lokasi Presisi Diperlukan" else "Izin Lokasi Diperlukan"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    when {
                        blockedByGeofencePrecision ->
                            "Precise location must be granted before the exam can start."
                        preciseRequiredForStart ->
                            "Location permission must be granted before the exam can start."
                        else ->
                            "Location access is required so anti-fake-location can validate the exam before it starts."
                    },
                    when {
                        blockedByGeofencePrecision ->
                            "Lokasi presisi harus diberikan sebelum ujian bisa dimulai."
                        preciseRequiredForStart ->
                            "Izin lokasi harus diberikan sebelum ujian bisa dimulai."
                        else ->
                            "Akses lokasi wajib tersedia agar anti-fake-location bisa memvalidasi ujian sebelum dimulai."
                    }
                )
            } else {
                launchLocationSecurityManualRefresh(trigger = "location_permission_quick_fix")
            }
        }
    val bluetoothPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            bluetoothPermissionGranted = granted || !requiresBluetoothExamPermission()
            bluetoothEnabled = if (bluetoothPermissionGranted) {
                isBluetoothEnabledForExam(context)
            } else {
                false
            }
            // Denied twice, Android answers at once without a prompt: "Allow Bluetooth"
            // looked dead and Start Exam stayed locked with nothing else to try.
            if (
                !bluetoothPermissionGranted &&
                isPermissionPromptBlocked(activity, getBluetoothConnectPermission())
            ) {
                showBlockedPermissionDialog(BlockedPermissionKind.Bluetooth)
            }
        }
    return ExamRuntimePermissionLaunchers(
        location = locationPermissionLauncher,
        bluetooth = bluetoothPermissionLauncher
    )
}
