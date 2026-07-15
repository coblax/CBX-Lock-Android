package com.coblax.examlock.ui.preparation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coblax.examlock.GeofenceRuntimeStatus
import com.coblax.examlock.ui.geofence.GeofenceMapViewerScreen

@Composable
internal fun ExamPreparationScene(
    showGeofenceMapViewer: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    isRefreshingGeofence: Boolean,
    onDismissGeofenceMapViewer: () -> Unit,
    onRefreshGeofenceLocation: () -> Unit,
    preparationState: PreparationScreenState,
    preparationActions: PreparationScreenActions,
    modifier: Modifier = Modifier
) {
    if (showGeofenceMapViewer) {
        GeofenceMapViewerScreen(
            runtimeStatus = geofenceRuntimeStatus,
            isRefreshingLocation = isRefreshingGeofence,
            onDismiss = onDismissGeofenceMapViewer,
            onRefreshLocation = onRefreshGeofenceLocation,
            modifier = modifier
        )
        return
    }

    ExamPreparationContent(
        state = preparationState,
        actions = preparationActions,
        modifier = modifier
    )
}

@Composable
internal fun ExamPreparationContent(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    modifier: Modifier = Modifier
) {
    ExamSecurityPreparationScreen(
        state = state,
        actions = actions,
        modifier = modifier
    )
}
