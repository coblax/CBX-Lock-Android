package com.example.coblaxexamlock.ui.preparation

import android.content.Context
import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.isExamGuardAccessibilityAvailable
import com.example.coblaxexamlock.isExamGuardAccessibilityEnabled
import com.example.coblaxexamlock.runtime.LowRamDispatchers
import kotlinx.coroutines.withContext

internal data class PreparationAccessibilityState(
    val inspection: AccessibilityInspectionResult,
    val guardEnabled: Boolean,
    val guardAvailable: Boolean
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
        guardAvailable = false
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
