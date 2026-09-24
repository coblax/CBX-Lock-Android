package com.coblax.examlock.ui.preparation

import android.content.Context
import com.coblax.examlock.AccessibilityInspectionResult
import com.coblax.examlock.inspectAccessibility
import com.coblax.examlock.isExamGuardAccessibilityAvailable
import com.coblax.examlock.isExamGuardAccessibilityEnabled
import com.coblax.examlock.runtime.LowRamDispatchers
import kotlinx.coroutines.withContext

internal data class PreparationAccessibilityState(
    val inspection: AccessibilityInspectionResult,
    val guardEnabled: Boolean,
    val guardAvailable: Boolean,
    /** False until the first inspection finishes; the placeholder values are not facts. */
    val loaded: Boolean = true
)

internal fun initialPreparationAccessibilityState(): PreparationAccessibilityState =
    PreparationAccessibilityState(
        inspection = AccessibilityInspectionResult(
            managerEnabled = false,
            touchExplorationEnabled = false,
            rawEnabledServices = "-",
            activeServiceComponents = emptyList(),
            activePackages = emptyList(),
            allowedServiceComponents = emptyList(),
            allowedPackages = emptyList(),
            effectiveServiceComponents = emptyList(),
            effectivePackages = emptyList(),
            riskyPackages = emptyList()
        ),
        guardEnabled = false,
        guardAvailable = false,
        loaded = false
    )

internal suspend fun loadPreparationAccessibilityState(
    context: Context
): PreparationAccessibilityState = withContext(LowRamDispatchers.detectorIo) {
    val appContext = context.applicationContext
    PreparationAccessibilityState(
        inspection = inspectAccessibility(appContext),
        guardEnabled = isExamGuardAccessibilityEnabled(appContext),
        guardAvailable = isExamGuardAccessibilityAvailable(appContext)
    )
}
