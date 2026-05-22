package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.AppSwitchBypassState
import com.example.coblaxexamlock.AppSwitchProtectionMode
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.CompatibilityScore
import com.example.coblaxexamlock.DeviceCompatibilityFamily
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.DeviceSurvivalRuntimeTier
import com.example.coblaxexamlock.DeviceSurvivalUiTier
import com.example.coblaxexamlock.DeviceTimeBaseline
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceEvaluation
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.GeofenceSecurityStatus
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.GeofenceVerdict
import com.example.coblaxexamlock.LocationFixQualityStatus
import com.example.coblaxexamlock.LocationFixQualityVerdict
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityStatus
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.OverlayShieldStatus
import com.example.coblaxexamlock.PinningActivationState
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumb
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.VpnBypassState
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.buildRootSecurityStatus
import com.example.coblaxexamlock.evaluateDeviceTimeSecurityStatus
import com.example.coblaxexamlock.model.ExamNetworkStatus
import com.example.coblaxexamlock.model.NetworkDiagnostics
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.model.RootDetectionDetails
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.runtime.MultiWindowModeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparationStateSlicingTest {
    @Test
    fun wrapperAliasesExposeGroupedSlices() {
        val state = preparationState(
            session = sessionState().copy(examName = "Slice Exam"),
            network = networkState().copy(lastConnectedNetworkLabel = "Wi-Fi Lab"),
            device = deviceState().copy(keyboardPackage = "com.example.keyboard"),
            runtimeSecurity = runtimeSecurityState().copy(screenRecorderPackages = listOf("recorder.app"))
        )

        assertEquals("Slice Exam", state.examName)
        assertEquals("Wi-Fi Lab", state.lastConnectedNetworkLabel)
        assertEquals("com.example.keyboard", state.keyboardPackage)
        assertEquals(listOf("recorder.app"), state.screenRecorderPackages)
        assertSame(state.network.networkReadinessStatus, state.networkReadinessStatus)
        assertSame(state.device.webViewCompatibilityStatus, state.webViewCompatibilityStatus)
    }

    @Test
    fun slicedReadinessBlocksSupportedInactiveScreenPinning() {
        val readiness = readinessFor(
            device = deviceState().copy(
                screenPinningAvailable = true,
                isScreenPinningActive = false
            )
        )

        assertFalse(readiness.screenPinningReady)
        assertFalse(readiness.canStartExam)
    }

    @Test
    fun slicedReadinessAllowsActiveOrBypassedScreenPinning() {
        val activeReadiness = readinessFor(
            device = deviceState().copy(
                screenPinningAvailable = true,
                isScreenPinningActive = true
            )
        )
        val bypassReadiness = readinessFor(
            bypass = bypassState().copy(bypassScreenPinning = true)
        )

        assertTrue(activeReadiness.screenPinningReady)
        assertTrue(activeReadiness.canStartExam)
        assertTrue(bypassReadiness.screenPinningReady)
        assertTrue(bypassReadiness.canStartExam)
    }

    @Test
    fun quickFixDefersScreenPinningUntilOtherBlockersAreClear() {
        val state = preparationState(
            device = deviceState().copy(
                screenPinningAvailable = true,
                isScreenPinningActive = false
            ),
            runtimeSecurity = runtimeSecurityState().copy(accessibilityServiceEnabled = true)
        )

        val actions = quickFixActionsFor(state)
        val deferredNotice = actions.first { it.code == QuickFixScreenPinningDeferredCode }

        assertFalse(actions.any { it.code == QuickFixStartScreenPinningCode && it.enabled })
        assertTrue(deferredNotice.isNotice)
        assertFalse(deferredNotice.enabled)
        assertEquals(QuickFixTarget.ScreenPinning, deferredNotice.target)
        assertEquals(PreparationSection.DeviceLock, deferredNotice.section)
        assertTrue(deferredNotice.diagnosticDetails.orEmpty().contains("blockers=1"))
    }

    @Test
    fun quickFixDefersScreenPinningForBlockingWebViewFix() {
        val state = preparationState(
            device = deviceState().copy(
                screenPinningAvailable = true,
                isScreenPinningActive = false
            ),
            diagnostics = diagnosticsState().copy(
                preExamHealthCheckSnapshot = PreExamHealthSnapshot(
                    compatibilityFamily = DeviceCompatibilityFamily.Generic,
                    compatibilityLabel = "Android",
                    generatedAtElapsedMs = 0L,
                    items = listOf(
                        PreExamHealthItem(
                            category = PreExamHealthCategory.WebView,
                            verdict = PreExamHealthVerdict.Blocking,
                            title = "WebView",
                            detail = "Provider unavailable",
                            quickFix = "Open WebView Settings"
                        )
                    )
                )
            )
        )

        val actions = quickFixActionsFor(state)

        assertTrue(actions.any { it.code == QuickFixScreenPinningDeferredCode })
        assertFalse(actions.any { it.code == QuickFixStartScreenPinningCode && it.enabled })
        val webViewFix = actions.first { it.code == "webview_provider_settings" }
        assertTrue(webViewFix.opensExternalSettings)
        assertEquals(PreparationSection.DeviceHealth, webViewFix.section)
    }

    @Test
    fun quickFixShowsScreenPinningWhenItIsTheOnlyBlocker() {
        val state = preparationState(
            device = deviceState().copy(
                screenPinningAvailable = true,
                isScreenPinningActive = false
            )
        )

        val actions = quickFixActionsFor(state)
        val startPinning = actions.first { it.code == QuickFixStartScreenPinningCode }

        assertTrue(startPinning.enabled)
        assertFalse(startPinning.isNotice)
        assertEquals(QuickFixSeverity.Blocking, startPinning.severity)
        assertEquals(QuickFixTarget.ScreenPinning, startPinning.target)
        assertEquals(PreparationSection.DeviceLock, startPinning.section)
        assertFalse(actions.any { it.code == QuickFixScreenPinningDeferredCode })
    }

    @Test
    fun quickFixDisablesExternalSettingsActionsWhenScreenPinningAlreadyActive() {
        val state = preparationState(
            device = deviceState().copy(
                screenPinningAvailable = true,
                isScreenPinningActive = true
            ),
            runtimeSecurity = runtimeSecurityState().copy(accessibilityServiceEnabled = true)
        )

        val actions = quickFixActionsFor(state)
        val accessibilityFix = actions.first { it.priority == 35 }

        assertTrue(accessibilityFix.opensExternalSettings)
        assertFalse(accessibilityFix.enabled)
        assertTrue(accessibilityFix.text.contains("Turn off Screen Pinning first"))
        assertEquals(PreparationSection.RuntimeInteraction, accessibilityFix.section)
    }

    @Test
    fun quickFixKeepsInternalRefreshActionsEnabledWhenScreenPinningAlreadyActive() {
        val state = preparationState(
            device = deviceState().copy(
                screenPinningAvailable = true,
                isScreenPinningActive = true
            ),
            network = networkState(
                readinessStatus = networkReadinessStatus(
                    verdict = NetworkReadinessVerdict.Unstable,
                    userVerdict = NetworkReadinessUserVerdict.Unstable
                )
            )
        )

        val actions = quickFixActionsFor(state)
        val refreshNetwork = actions.first { it.priority == 70 }

        assertFalse(refreshNetwork.opensExternalSettings)
        assertTrue(refreshNetwork.enabled)
        assertFalse(refreshNetwork.isNotice)
    }

    @Test
    fun wizardLocationStepIncludesAllLocationQuickFixActions() {
        val permissionActions = filterQuickFixActionsForStep(
            WizardStep.Location,
            quickFixActionsFor(
                preparationState(
                    location = locationState(
                        geofenceRuntimeStatus = geofenceRuntimeStatus(
                            finalVerdict = GeofenceSecurityVerdict.PermissionMissing
                        )
                    )
                )
            )
        )

        assertTrue(
            permissionActions.any {
                it.priority == 40 &&
                    it.target == QuickFixTarget.Location &&
                    it.section == PreparationSection.Location
            }
        )

        val outsideActions = filterQuickFixActionsForStep(
            WizardStep.Location,
            quickFixActionsFor(
                preparationState(
                    location = locationState(
                        geofenceRuntimeStatus = geofenceRuntimeStatus(
                            finalVerdict = GeofenceSecurityVerdict.Outside
                        )
                    )
                )
            )
        )

        assertTrue(
            outsideActions.any {
                it.priority == 50 &&
                    it.target == QuickFixTarget.Location &&
                    it.section == PreparationSection.Location
            }
        )
        assertTrue(
            outsideActions.any {
                it.priority == 55 &&
                    it.target == QuickFixTarget.Location &&
                    it.section == PreparationSection.Location
            }
        )
    }

    @Test
    fun wizardConnectivityStepIncludesNetworkRefreshActions() {
        val actions = filterQuickFixActionsForStep(
            WizardStep.Connectivity,
            quickFixActionsFor(
                preparationState(
                    network = networkState(
                        readinessStatus = networkReadinessStatus(
                            verdict = NetworkReadinessVerdict.Unstable,
                            userVerdict = NetworkReadinessUserVerdict.Unstable
                        )
                    )
                )
            )
        )

        assertTrue(
            actions.any {
                it.priority == 70 &&
                    it.target == QuickFixTarget.Network &&
                    it.section == PreparationSection.Connectivity
            }
        )
    }

    @Test
    fun wizardRuntimeSecurityStepIncludesAppSwitchViolationFix() {
        val actions = filterQuickFixActionsForStep(
            WizardStep.RuntimeSecurity,
            quickFixActionsFor(
                preparationState(
                    runtimeSecurity = runtimeSecurityState().copy(
                        appSwitchStatus = runtimeSecurityState().appSwitchStatus.copy(
                            violationCount = 1
                        )
                    )
                )
            )
        )

        assertTrue(
            actions.any {
                it.code == "app_switch_violations" &&
                    it.section == PreparationSection.RuntimeSecurity
            }
        )
    }

    @Test
    fun quickFixBuilderTagsDeviceSetupAndIntegrityActions() {
        val actions = quickFixActionsFor(
            preparationState(
                device = deviceState().copy(
                    usingBuiltInExamKeyboard = true,
                    reinstallApkFixNeeded = true
                )
            )
        )

        assertTrue(
            actions.any {
                it.priority == 15 &&
                    it.section == PreparationSection.DeviceIntegrity
            }
        )
        assertTrue(
            actions.any {
                it.priority == 200 &&
                    it.section == PreparationSection.DeviceSetup
            }
        )
        assertTrue(
            actions.any {
                it.priority == 205 &&
                    it.section == PreparationSection.DeviceSetup
            }
        )
    }

    @Test
    fun quickFixBuilderTagsRuntimeSecurityActions() {
        val actions = quickFixActionsFor(
            preparationState(
                runtimeSecurity = runtimeSecurityState().copy(
                    screenRecorderPackages = listOf("recorder.app"),
                    externalDisplayDetected = true,
                    externalDisplayCount = 1,
                    multiWindowDetected = true,
                    multiWindowModeInfo = runtimeSecurityState().multiWindowModeInfo.copy(
                        inMultiWindowMode = true
                    )
                )
            )
        )

        assertTrue(
            actions.any {
                it.target == QuickFixTarget.ScreenRecorder &&
                    it.section == PreparationSection.RuntimeSecurity
            }
        )
        assertTrue(
            actions.any {
                it.target == QuickFixTarget.DisplayMirror &&
                    it.section == PreparationSection.RuntimeSecurity
            }
        )
        assertTrue(
            actions.any {
                it.target == QuickFixTarget.MultiWindow &&
                    it.section == PreparationSection.RuntimeSecurity
            }
        )
    }

    @Test
    fun slicedReadinessPreservesVpnGate() {
        val vpnNetwork = networkState(
            readinessStatus = networkReadinessStatus(
                verdict = NetworkReadinessVerdict.VpnActive,
                userVerdict = NetworkReadinessUserVerdict.VpnActive,
                vpnActive = true
            ),
            bypassVpn = false
        )
        val blocked = readinessFor(network = vpnNetwork)
        val bypassed = readinessFor(
            network = vpnNetwork.copy(bypassVpn = true),
            bypass = bypassState().copy(bypassVpn = true)
        )

        assertFalse(blocked.vpnReady)
        assertFalse(blocked.canStartExam)
        assertTrue(bypassed.vpnReady)
        assertTrue(bypassed.canStartExam)
    }

    @Test
    fun slicedReadinessBlocksStartWhileInitialStaticSecurityScanPending() {
        val readiness = readinessFor(
            runtimeSecurity = runtimeSecurityState().copy(
                staticSecurityInitialScanComplete = false
            )
        )

        assertFalse(readiness.staticSecurityInitialScanComplete)
        assertFalse(readiness.canStartExam)
    }

    @Test
    fun wrapperAndSliceReadinessStayEquivalent() {
        val state = preparationState(
            network = networkState(
                readinessStatus = networkReadinessStatus(
                    verdict = NetworkReadinessVerdict.VpnActive,
                    userVerdict = NetworkReadinessUserVerdict.VpnActive,
                    vpnActive = true
                ),
                bypassVpn = true
            ),
            bypass = bypassState().copy(bypassVpn = true)
        )

        assertEquals(
            buildPreparationChecklistReadiness(
                state = state,
                needsBluetoothPermission = false,
                accessibilityGuardRequired = false,
                accessibilityGuardAvailable = true,
                accessibilityGuardEnabled = false
            ),
            buildPreparationChecklistReadiness(
                network = state.network,
                device = state.device,
                location = state.location,
                runtimeSecurity = state.runtimeSecurity,
                bypass = state.bypass,
                needsBluetoothPermission = false,
                accessibilityGuardRequired = false,
                accessibilityGuardAvailable = true,
                accessibilityGuardEnabled = false
            )
        )
    }

    private fun readinessFor(
        network: PreparationNetworkState = networkState(),
        device: PreparationDeviceState = deviceState(),
        location: PreparationLocationState = locationState(),
        runtimeSecurity: PreparationRuntimeSecurityState = runtimeSecurityState(),
        bypass: PreparationBypassState = bypassState()
    ): PreparationChecklistReadiness {
        return buildPreparationChecklistReadiness(
            network = network,
            device = device,
            location = location,
            runtimeSecurity = runtimeSecurity,
            bypass = bypass,
            needsBluetoothPermission = false,
            accessibilityGuardRequired = false,
            accessibilityGuardAvailable = true,
            accessibilityGuardEnabled = false
        )
    }

    private fun quickFixActionsFor(state: PreparationScreenState): List<PreparationQuickFixAction> {
        return buildPreparationQuickFixActions(
            state = state,
            actions = preparationActions(),
            uiLanguage = UiLanguage.English,
            accessibilityGuardRequired = false,
            accessibilityGuardEnabled = false,
            geofenceReady = true,
            fakeLocationReady = true,
            needsBluetoothPermission = false,
            accessibilityInspection = accessibilityInspection(),
            runQuickFix = { _, _, _, action -> action() }
        )
    }

    private fun preparationActions(): PreparationScreenActions {
        val noOp = {}
        return PreparationScreenActions(
            session = PreparationSessionActions(
                onRefreshStatus = noOp,
                onRefreshAllSecurityChecks = noOp,
                onRefreshHealthCheck = noOp,
                onRequestSectionReport = {},
                onExportDiagnostics = noOp,
                onAutoFixShown = {},
                onPreviousSessionRecoveryHintShown = {},
                onAutoFixActionOpened = {},
                onScreenPinningDeferred = {},
                onStartExam = noOp,
                onBackHome = noOp
            ),
            network = PreparationNetworkActions(
                onOpenInternetSettings = noOp,
                onOpenVpnSettings = noOp,
                onOpenWifiSettings = noOp,
                onOpenCellularSettings = noOp,
                onOpenAirplaneModeSettings = noOp,
                onRefreshNetworkStatus = noOp
            ),
            device = PreparationDeviceActions(
                onChooseKeyboard = noOp,
                onOpenKeyboardSettings = noOp,
                onGrantBluetoothPermission = noOp,
                onOpenBluetoothSettings = noOp,
                onOpenAccessibilitySettings = noOp,
                onOpenOverlayAccessibilitySettings = noOp,
                onOpenDeveloperOptionsSettings = noOp,
                onOpenDateTimeSettings = noOp,
                onOpenScreenPinningSettings = noOp,
                onStartScreenPinning = noOp,
                onOpenOverlaySettings = noOp,
                onOpenAppSettings = noOp,
                onOpenCastSettings = noOp,
                onOpenWebViewProviderSettings = noOp,
                onReinstallOfficialApk = noOp
            ),
            location = PreparationLocationActions(
                onRequestLocationPermission = noOp,
                onOpenLocationServicesSettings = noOp,
                onRefreshGeofenceLocation = noOp,
                onOpenGeofenceMapViewer = noOp,
                onOpenFakeLocationDeveloperOptionsSettings = noOp
            ),
            runtimeSecurity = PreparationRuntimeSecurityActions(
                onOpenAccessibilitySettings = noOp,
                onOpenOverlayAccessibilitySettings = noOp,
                onOpenOverlaySettings = noOp,
                onOpenAppSettings = noOp,
                onOpenCastSettings = noOp
            )
        )
    }

    private fun accessibilityInspection(): AccessibilityInspectionResult {
        return AccessibilityInspectionResult(
            managerEnabled = false,
            touchExplorationEnabled = false,
            rawEnabledServices = "",
            activeServiceComponents = emptyList(),
            activePackages = emptyList(),
            allowedServiceComponents = emptyList(),
            allowedPackages = emptyList(),
            effectiveServiceComponents = emptyList(),
            effectivePackages = emptyList(),
            riskyPackages = emptyList()
        )
    }

    private fun preparationState(
        session: PreparationSessionState = sessionState(),
        network: PreparationNetworkState = networkState(),
        device: PreparationDeviceState = deviceState(),
        location: PreparationLocationState = locationState(),
        runtimeSecurity: PreparationRuntimeSecurityState = runtimeSecurityState(),
        bypass: PreparationBypassState = bypassState(),
        diagnostics: PreparationDiagnosticsState = diagnosticsState()
    ): PreparationScreenState {
        return PreparationScreenState(
            session = session,
            network = network,
            device = device,
            location = location,
            runtimeSecurity = runtimeSecurity,
            bypass = bypass,
            diagnostics = diagnostics
        )
    }

    private fun sessionState(): PreparationSessionState {
        return PreparationSessionState(
            examName = "Exam",
            sendingSection = null,
            isStartingExam = false,
            pinningActivationState = PinningActivationState.Idle,
            screenPinningMessage = null,
            webViewSessionResetInFlight = false,
            webViewSessionResetError = null,
            showChecklistDetails = false
        )
    }

    private fun networkState(
        readinessStatus: NetworkReadinessStatus = networkReadinessStatus(),
        bypassVpn: Boolean = false
    ): PreparationNetworkState {
        return PreparationNetworkState(
            networkReadinessStatus = readinessStatus,
            networkUnstableRuntimeStatus = NetworkUnstableRuntimeStatus(
                unstableActive = false,
                episodeStartedAt = null,
                flapCount = 0,
                lastFlapAt = null,
                warningShown = false,
                lastWarningAt = null,
                lastTransportLabel = null
            ),
            networkTimelinePreview = emptyList(),
            lastNetworkChangeAt = null,
            lastNetworkChangeSource = null,
            lastConnectedNetworkLabel = null,
            isRefreshingNetwork = false,
            bypassVpn = bypassVpn,
            vpnBypassState = if (bypassVpn) VpnBypassState.Active else VpnBypassState.Inactive
        )
    }

    private fun networkReadinessStatus(
        verdict: NetworkReadinessVerdict = NetworkReadinessVerdict.ConnectedStable,
        userVerdict: NetworkReadinessUserVerdict = NetworkReadinessUserVerdict.Stable,
        vpnActive: Boolean = false
    ): NetworkReadinessStatus {
        return NetworkReadinessStatus(
            examStatus = ExamNetworkStatus(
                label = "Wi-Fi",
                detail = "connected",
                isConnected = true
            ),
            diagnostics = NetworkDiagnostics(
                activeNetworkAvailable = true,
                transports = if (vpnActive) listOf("VPN", "WIFI") else listOf("WIFI"),
                hasInternetCapability = true,
                isValidated = true,
                isCaptivePortal = false,
                isMetered = false,
                isVpnActive = vpnActive,
                isAirplaneModeEnabled = false,
                notRoaming = true,
                interfaceName = if (vpnActive) "tun0" else "wlan0",
                wifi = null,
                cellular = null
            ),
            verdict = verdict,
            transportLabel = if (vpnActive) "VPN, Wi-Fi" else "Wi-Fi",
            quickFixReason = null,
            userFacingVerdict = userVerdict,
            userFacingQuickFixText = null
        )
    }

    private fun deviceState(): PreparationDeviceState {
        return PreparationDeviceState(
            keyboardPackage = "com.android.inputmethod.latin",
            keyboardAllowed = true,
            usingBuiltInExamKeyboard = false,
            bluetoothPermissionGranted = true,
            bluetoothEnabled = false,
            adbInspection = AdbInspection(
                developerOptionsEnabled = false,
                adbEnabled = false,
                developerOptionsRawValue = "0",
                adbRawValue = "0",
                adbSecureProperty = "1"
            ),
            adbBypassState = AdbBypassState.Inactive,
            rootSecurityStatus = buildRootSecurityStatus(rootDetectionDetails()),
            rootBypassState = RootBypassState.Inactive,
            signatureMismatchDetected = false,
            virtualEnvironmentDetected = false,
            screenPinningAvailable = true,
            isScreenPinningActive = true,
            screenPinningFixNeeded = false,
            webViewCompatibilityStatus = WebViewCompatibilityStatus(
                available = true,
                packageName = "com.android.webview",
                versionName = "120.0.0",
                majorVersion = 120,
                outdatedLikely = false,
                providerSource = "test",
                quickFix = null
            ),
            deviceTimeSecurityStatus = evaluateDeviceTimeSecurityStatus(
                autoTimeEnabled = true,
                autoTimeZoneEnabled = true,
                baseline = DeviceTimeBaseline(1_000L, 500L),
                bypassState = DeviceTimeBypassState.Inactive,
                timezoneSummary = "Asia/Jakarta",
                nowWallClockMillis = 2_000L,
                nowElapsedRealtimeMillis = 1_500L
            ),
            deviceTimeBypassState = DeviceTimeBypassState.Inactive,
            reinstallApkFixNeeded = false
        )
    }

    private fun locationState(
        geofenceRuntimeStatus: GeofenceRuntimeStatus = geofenceRuntimeStatus(),
        fakeLocationRuntimeStatus: FakeLocationRuntimeStatus = fakeLocationRuntimeStatus()
    ): PreparationLocationState {
        return PreparationLocationState(
            geofenceRuntimeStatus = geofenceRuntimeStatus,
            fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
            isRefreshingGeofence = false,
            isWarmingLocation = false,
            lastGeofenceRefreshAt = null,
            geofenceBypassState = GeofenceBypassState.Inactive,
            fakeLocationBypassState = FakeLocationBypassState.Inactive
        )
    }

    private fun runtimeSecurityState(): PreparationRuntimeSecurityState {
        return PreparationRuntimeSecurityState(
            accessibilityServiceEnabled = false,
            overlayRiskResult = OverlayRiskResult(
                bypassed = false,
                confirmedInteractionDetected = false,
                heuristicRisk = false,
                accessibilityEnabled = false,
                riskyAccessibilityPackages = emptyList(),
                violationCount = 0,
                signals = emptySet(),
                quickFixTargets = emptySet(),
                shieldStatus = OverlayShieldStatus(
                    supported = true,
                    requested = true,
                    lastApplySucceeded = true,
                    lastApplyAt = null
                ),
                lastTrigger = null,
                lastDetectedAt = null,
                lastContext = null
            ),
            appSwitchStatus = AppSwitchStatus(
                bypassState = AppSwitchBypassState.Inactive,
                monitoringEnabled = true,
                runtimeMonitoringActive = false,
                protectionMode = AppSwitchProtectionMode.ProtectedByPinning,
                lockTaskActive = true,
                violationCount = 0,
                pendingViolation = false,
                lastTrigger = null,
                lastDetectedAt = null,
                lastContext = null,
                accessibilityGuardEnabled = false,
                accessibilityFallbackActive = false,
                accessibilityViolationCount = 0,
                accessibilityLastReason = null,
                accessibilityLastForeignPackage = null,
                accessibilityLastEventType = null,
                accessibilityLastDetectedAt = null,
                accessibilityAlarmSeverity = null
            ),
            clipboardViolationCount = 0,
            clipboardRuntimeStatus = ClipboardRuntimeStatus(
                lastObservedAt = null,
                lastConfirmedAt = null,
                lastObservedSignature = null,
                lastDecision = "idle",
                baselineSemanticSignature = null,
                detectedSemanticSignature = null,
                currentSemanticSignature = null
            ),
            clipboardBypassState = ClipboardBypassState.Inactive,
            screenRecorderPackages = emptyList(),
            externalDisplayDetected = false,
            externalDisplayCount = 0,
            externalDisplayInfoList = emptyList(),
            multiWindowDetected = false,
            multiWindowModeInfo = MultiWindowModeInfo(
                multiWindowApiSupported = true,
                pictureInPictureApiSupported = true,
                inMultiWindowMode = false,
                inPictureInPictureMode = false
            ),
            staticSecurityInitialScanComplete = true,
            tamperDetected = false
        )
    }

    private fun bypassState(): PreparationBypassState {
        return PreparationBypassState(
            bypassScreenPinning = false,
            bypassBluetooth = false,
            bypassAccessibility = false,
            bypassAdb = false,
            bypassRoot = false,
            bypassVirtualEnvironment = false,
            bypassVpn = false,
            vpnBypassState = VpnBypassState.Inactive,
            bypassKeyboardPolicy = false,
            bypassClipboard = false,
            bypassOverlay = false,
            bypassGeofence = false,
            geofenceBypassState = GeofenceBypassState.Inactive,
            bypassFakeLocation = false,
            fakeLocationBypassState = FakeLocationBypassState.Inactive,
            bypassDeviceTime = false,
            bypassAppSwitch = false,
            bypassScreenRecorder = false,
            bypassDisplayMirror = false,
            bypassMultiWindow = false
        )
    }

    private fun diagnosticsState(): PreparationDiagnosticsState {
        return PreparationDiagnosticsState(
            preExamHealthCheckSnapshot = PreExamHealthSnapshot(
                compatibilityFamily = DeviceCompatibilityFamily.Generic,
                compatibilityLabel = "Android",
                generatedAtElapsedMs = 0L,
                items = emptyList()
            ),
            deviceSurvivalPolicy = DeviceSurvivalPolicy(
                score = CompatibilityScore.Excellent,
                runtimeTier = DeviceSurvivalRuntimeTier.Standard,
                uiTier = DeviceSurvivalUiTier.Rich,
                vendorRiskLabel = "safe",
                webViewRiskLabel = "safe",
                startExamAllowedByHealth = true,
                healthBlockingCount = 0,
                healthWarningCount = 0,
                fieldBlockedCount = 0,
                fieldWarningCount = 0,
                recommendedActions = emptyList()
            ),
            previousExamSessionBreadcrumb = PreviousExamSessionBreadcrumb(emptyList())
        )
    }

    private fun geofenceRuntimeStatus(
        finalVerdict: GeofenceSecurityVerdict = GeofenceSecurityVerdict.Disabled
    ): GeofenceRuntimeStatus {
        val enabled = finalVerdict != GeofenceSecurityVerdict.Disabled
        val permissionGranted = finalVerdict != GeofenceSecurityVerdict.PermissionMissing
        val locationServicesEnabled = finalVerdict != GeofenceSecurityVerdict.LocationDisabled
        val geofenceVerdict = when (finalVerdict) {
            GeofenceSecurityVerdict.Disabled,
            GeofenceSecurityVerdict.Bypassed -> GeofenceVerdict.Disabled
            GeofenceSecurityVerdict.Inside -> GeofenceVerdict.Inside
            GeofenceSecurityVerdict.Outside,
            GeofenceSecurityVerdict.StaleFix,
            GeofenceSecurityVerdict.LowAccuracy,
            GeofenceSecurityVerdict.MissingAccuracy -> GeofenceVerdict.Outside
            GeofenceSecurityVerdict.PermissionMissing,
            GeofenceSecurityVerdict.PreciseRequired -> GeofenceVerdict.PermissionMissing
            GeofenceSecurityVerdict.LocationDisabled -> GeofenceVerdict.LocationDisabled
            GeofenceSecurityVerdict.NoFix -> GeofenceVerdict.NoFix
            GeofenceSecurityVerdict.ConfigInvalid -> GeofenceVerdict.ConfigInvalid
        }
        val evaluation = GeofenceEvaluation(
            enabled = enabled,
            config = null,
            configError = null,
            permissionGranted = permissionGranted,
            locationServicesEnabled = locationServicesEnabled,
            locationSnapshot = null,
            closestCircleCenter = null,
            distanceMeters = null,
            verdict = geofenceVerdict
        )
        val fixQuality = LocationFixQualityStatus(
            snapshot = null,
            ageMs = null,
            accuracyMeters = null,
            accuracyThresholdMeters = 100f,
            verdict = LocationFixQualityVerdict.NoFix
        )
        return GeofenceRuntimeStatus(
            evaluation = evaluation,
            securityStatus = GeofenceSecurityStatus(
                geofenceEvaluation = evaluation,
                bypassState = GeofenceBypassState.Inactive,
                preciseLocationGranted = finalVerdict != GeofenceSecurityVerdict.PreciseRequired,
                fixQualityStatus = fixQuality,
                finalVerdict = finalVerdict
            ),
            policySource = LocationPolicySource.DisabledNoPolicy,
            violationCount = 0,
            lastTrigger = null,
            lastDetectedAt = null,
            lastContext = null
        )
    }

    private fun fakeLocationRuntimeStatus(): FakeLocationRuntimeStatus {
        return FakeLocationRuntimeStatus(
            securityStatus = LocationSpoofSecurityStatus(
                monitoringEnabled = false,
                bypassState = FakeLocationBypassState.Inactive,
                permissionGranted = true,
                locationServicesEnabled = true,
                snapshotAvailable = false,
                suspiciousFakeLocationPackages = emptyList(),
                developerOptionsEnabled = false,
                mockLocationDetected = false,
                supportingSignals = emptySet(),
                confidenceTier = LocationSpoofConfidenceTier.Safe,
                fixQualityStatus = LocationFixQualityStatus(
                    snapshot = null,
                    ageMs = null,
                    accuracyMeters = null,
                    accuracyThresholdMeters = 100f,
                    verdict = LocationFixQualityVerdict.NoFix
                ),
                fixQualityEligible = false,
                finalVerdict = LocationSpoofSecurityVerdict.Disabled
            ),
            violationCount = 0,
            lastTrigger = null,
            lastDetectedAt = null,
            lastContext = null
        )
    }

    private fun rootDetectionDetails(): RootDetectionDetails {
        return RootDetectionDetails(
            hasTestKeys = false,
            hasSuBinary = false,
            foundRootPackages = emptyList(),
            rootBinaryPaths = emptyList(),
            magiskPaths = emptyList(),
            zygiskDetected = false,
            xposedBridgeDetected = false,
            verifiedBootState = "green",
            vbmetaDeviceState = "locked",
            flashLocked = "1",
            bootloaderUnlocked = false,
            selinuxEnabled = true,
            selinuxEnforced = true,
            dangerousSystemProperties = emptyList(),
            roDebuggable = "0",
            roSecure = "1",
            roAdbSecure = "1",
            roBuildType = "user"
        )
    }
}
