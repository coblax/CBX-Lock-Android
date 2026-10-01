package com.coblax.examlock.ui.exam

import com.coblax.examlock.MemoryPressureCoordinator
import com.coblax.examlock.model.DiagnosticEventLevel

/** Logs, once per pause window, that [check] skipped its round because memory is critical. */
internal fun recordSideCheckMemoryPause(
    check: String,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit
) {
    if (!MemoryPressureCoordinator.claimSideCheckPauseLog(check)) {
        return
    }
    recordAction(
        ExamRuntimeHardeningDiagnostics.SideCheckPausedForMemory,
        "check=$check | until_elapsed_ms=${MemoryPressureCoordinator.sideChecksPausedUntilElapsedMs()}",
        DiagnosticEventLevel.WARNING
    )
}
