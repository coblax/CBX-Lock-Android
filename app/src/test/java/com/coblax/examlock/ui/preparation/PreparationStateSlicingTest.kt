package com.coblax.examlock.ui.preparation

import com.coblax.examlock.AdbBypassState
import com.coblax.examlock.AdbInspection
import com.coblax.examlock.AccessibilityInspectionResult
import com.coblax.examlock.AppSwitchBypassState
import com.coblax.examlock.AppSwitchProtectionMode
import com.coblax.examlock.AppSwitchStatus
import com.coblax.examlock.ClipboardBypassState
import com.coblax.examlock.ClipboardRuntimeStatus
import com.coblax.examlock.CompatibilityScore
import com.coblax.examlock.DeviceCompatibilityFamily
import com.coblax.examlock.DeviceSurvivalPolicy
import com.coblax.examlock.DeviceSurvivalRuntimeTier
import com.coblax.examlock.DeviceSurvivalUiTier
import com.coblax.examlock.DeviceTimeBaseline
import com.coblax.examlock.DeviceTimeBypassState
import com.coblax.examlock.FakeLocationBypassState
import com.coblax.examlock.FakeLocationRuntimeStatus
import com.coblax.examlock.GeofenceBypassState
import com.coblax.examlock.GeofenceEvaluation
import com.coblax.examlock.GeofenceRuntimeStatus
import com.coblax.examlock.GeofenceSecurityStatus
import com.coblax.examlock.GeofenceSecurityVerdict
import com.coblax.examlock.GeofenceVerdict
import com.coblax.examlock.LocationFixQualityStatus
import com.coblax.examlock.LocationFixQualityVerdict
import com.coblax.examlock.LocationPolicySource
import com.coblax.examlock.LocationSpoofConfidenceTier
import com.coblax.examlock.LocationSpoofSecurityStatus
import com.coblax.examlock.LocationSpoofSecurityVerdict
import com.coblax.examlock.OverlayRiskResult
import com.coblax.examlock.OverlayShieldStatus
import com.coblax.examlock.PinningActivationState
import com.coblax.examlock.PreviousExamSessionBreadcrumb
import com.coblax.examlock.RootBypassState
import com.coblax.examlock.VpnBypassState
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.buildRootSecurityStatus
import com.coblax.examlock.defaultDpcRuntimeStatus
import com.coblax.examlock.evaluateDeviceTimeSecurityStatus
import com.coblax.examlock.resolveWebViewCompatibilityStatus
import com.coblax.examlock.model.ExamNetworkStatus
import com.coblax.examlock.model.NetworkDiagnostics
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkReadinessUserVerdict
import com.coblax.examlock.model.NetworkReadinessVerdict
import com.coblax.examlock.model.NetworkUnstableRuntimeStatus
import com.coblax.examlock.model.RootDetectionDetails
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.runtime.MultiWindowModeInfo
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
    fun locationCategoryIncludesAllLocationQuickFixActions() {
        val permissionActions = filterQuickFixActionsForCategory(
            PreparationCategory.Location,
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

        val outsideActions = filterQuickFixActionsForCategory(
            PreparationCategory.Location,
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
    fun connectivityCategoryIncludesNetworkRefreshActions() {
        val actions = filterQuickFixActionsForCategory(
            PreparationCategory.Connectivity,
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
    fun runtimeSecurityCategoryIncludesAppSwitchViolationFix() {
        val actions = filterQuickFixActionsForCategory(
            PreparationCategory.RuntimeSecurity,
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
    fun slicedReadinessHonorsReverseEngineeringBypass() {
        val blocked = readinessFor(
            runtimeSecurity = runtimeSecurityState().copy(
                tamperDetected = true,
                reverseEngineeringDetected = true,
                reverseEngineeringSummary = "debugger=true"
            )
        )
        val bypassed = readinessFor(
            runtimeSecurity = runtimeSecurityState().copy(
                tamperDetected = true,
                reverseEngineeringDetected = true,
                reverseEngineeringSummary = "debugger=true",
                reverseEngineeringBypassActive = true
            ),
            bypass = bypassState().copy(bypassReverseEngineering = true)
        )

        assertFalse(blocked.reverseEngineeringReady)
        assertFalse(blocked.canStartExam)
        assertTrue(bypassed.reverseEngineeringReady)
        assertTrue(bypassed.canStartExam)
    }

    @Test
    fun slicedReadinessHonorsApkIntegrityBypass() {
        val blocked = readinessFor(
            runtimeSecurity = runtimeSecurityState().copy(
                tamperDetected = true,
                integrityDetected = true,
                integritySummary = "signature_changed"
            )
        )
        val bypassed = readinessFor(
            runtimeSecurity = runtimeSecurityState().copy(
                tamperDetected = true,
                integrityDetected = true,
                integritySummary = "signature_changed",
                integrityBypassActive = true
            ),
            bypass = bypassState().copy(bypassApkIntegrity = true)
        )

        assertFalse(blocked.integrityReady)
        assertFalse(blocked.canStartExam)
        assertTrue(bypassed.integrityReady)
        assertTrue(bypassed.canStartExam)
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

    @Test
    fun overviewForHealthyDeviceIsReadyWithNothingToFix() {
        val (readiness, overview) = overviewFor(preparationState())

        assertTrue(readiness.canStartExam)
        assertEquals(PreparationOverallState.Ready, overview.overallState)
        assertEquals(0, overview.blockingCount)
        assertTrue(overview.attention.isEmpty())
        assertEquals(PreparationCategory.entries.size, overview.clearCategoryCount)
    }

    @Test
    fun everyStartBlockerShowsARequiredIssueInItsCategory() {
        val cases: List<Pair<PreparationCategory, PreparationScreenState>> = listOf(
            PreparationCategory.DeviceSetup to preparationState(
                device = deviceState().copy(bluetoothEnabled = true)
            ),
            PreparationCategory.Connectivity to preparationState(
                network = networkState(
                    readinessStatus = networkReadinessStatus(
                        verdict = NetworkReadinessVerdict.VpnActive,
                        userVerdict = NetworkReadinessUserVerdict.VpnActive,
                        vpnActive = true
                    )
                )
            ),
            PreparationCategory.DeviceHealth to preparationState(
                device = deviceState().copy(
                    deviceTimeSecurityStatus = evaluateDeviceTimeSecurityStatus(
                        autoTimeEnabled = false,
                        autoTimeZoneEnabled = true,
                        baseline = DeviceTimeBaseline(1_000L, 500L),
                        bypassState = DeviceTimeBypassState.Inactive,
                        timezoneSummary = "Asia/Jakarta",
                        nowWallClockMillis = 2_000L,
                        nowElapsedRealtimeMillis = 1_500L
                    )
                )
            ),
            PreparationCategory.RuntimeInteraction to preparationState(
                runtimeSecurity = runtimeSecurityState().copy(accessibilityServiceEnabled = true)
            ),
            PreparationCategory.RuntimeInteraction to preparationState(
                runtimeSecurity = runtimeSecurityState().copy(
                    overlayRiskResult = runtimeSecurityState().overlayRiskResult.copy(
                        confirmedInteractionDetected = true
                    )
                )
            ),
            PreparationCategory.DeviceIntegrity to preparationState(
                device = deviceState().copy(
                    adbInspection = deviceState().adbInspection.copy(adbEnabled = true)
                )
            ),
            PreparationCategory.DeviceIntegrity to preparationState(
                device = deviceState().copy(
                    adbInspection = deviceState().adbInspection.copy(adbSecureProperty = "0")
                )
            ),
            PreparationCategory.DeviceIntegrity to preparationState(
                device = deviceState().copy(virtualEnvironmentDetected = true)
            ),
            PreparationCategory.DeviceIntegrity to preparationState(
                device = deviceState().copy(signatureMismatchDetected = true)
            ),
            PreparationCategory.DeviceIntegrity to preparationState(
                runtimeSecurity = runtimeSecurityState().copy(
                    reverseEngineeringDetected = true,
                    reverseEngineeringSummary = "debugger"
                )
            ),
            PreparationCategory.DeviceIntegrity to preparationState(
                runtimeSecurity = runtimeSecurityState().copy(
                    integrityDetected = true,
                    integritySummary = "dex_hash_mismatch"
                )
            ),
            PreparationCategory.Location to preparationState(
                location = locationState(
                    geofenceRuntimeStatus = geofenceRuntimeStatus(
                        finalVerdict = GeofenceSecurityVerdict.Outside
                    )
                )
            ),
            PreparationCategory.Location to preparationState(
                location = locationState(
                    geofenceRuntimeStatus = geofenceRuntimeStatus(
                        finalVerdict = GeofenceSecurityVerdict.PermissionMissing
                    )
                )
            ),
            PreparationCategory.DeviceLock to preparationState(
                device = deviceState().copy(isScreenPinningActive = false)
            ),
            PreparationCategory.RuntimeSecurity to preparationState(
                runtimeSecurity = runtimeSecurityState().copy(screenRecorderPackages = listOf("recorder.app"))
            ),
            PreparationCategory.RuntimeSecurity to preparationState(
                runtimeSecurity = runtimeSecurityState().copy(
                    externalDisplayDetected = true,
                    externalDisplayCount = 1
                )
            ),
            PreparationCategory.RuntimeSecurity to preparationState(
                runtimeSecurity = runtimeSecurityState().copy(multiWindowDetected = true)
            )
        )

        cases.forEachIndexed { index, (category, state) ->
            val (readiness, overview) = overviewFor(state)
            val label = "case #$index ($category)"
            assertFalse("$label should block start", readiness.canStartExam)
            assertEquals("$label overall state", PreparationOverallState.Blocked, overview.overallState)
            assertTrue("$label needs a required issue", overview.status(category).blockingCount > 0)
            assertEquals(
                "$label blocking issues belong to one category",
                overview.blockingCount,
                overview.status(category).blockingCount
            )
            assertEquals("$label is listed first", category, overview.attention.first().category)
        }
    }

    @Test
    fun rootedDeviceBlocksStartWithIntegrityIssue() {
        val state = preparationState(
            device = deviceState().copy(
                rootSecurityStatus = buildRootSecurityStatus(
                    rootDetectionDetails().copy(hasSuBinary = true, rootBinaryPaths = listOf("/system/xbin/su"))
                )
            )
        )
        val (readiness, overview) = overviewFor(state)

        if (!readiness.rootReady) {
            assertTrue(
                overview.status(PreparationCategory.DeviceIntegrity).issues.any {
                    it.key == "root_detected" && it.blocking
                }
            )
        }
        assertEquals(!readiness.canStartExam, overview.blockingCount > 0)
    }

    // Only soft network states are optional: Start Exam refuses offline and airplane mode
    // (see offlineAndAirplaneModeAreRequiredFixes).
    @Test
    fun softNetworkWarningsAreOptionalAndKeepStartEnabled() {
        val (readiness, overview) = overviewFor(
            preparationState(
                network = networkState(
                    readinessStatus = networkReadinessStatus(
                        verdict = NetworkReadinessVerdict.Unvalidated,
                        userVerdict = NetworkReadinessUserVerdict.Unvalidated
                    )
                )
            )
        )

        assertTrue(readiness.canStartExam)
        assertEquals(PreparationOverallState.ReadyWithWarnings, overview.overallState)
        assertEquals(PreparationTone.Warning, overview.status(PreparationCategory.Connectivity).tone)
        assertEquals(0, overview.blockingCount)
    }

    @Test
    fun attentionListsRequiredFixesFirstAndScreenPinningLast() {
        val (_, overview) = overviewFor(
            preparationState(
                device = deviceState().copy(
                    bluetoothEnabled = true,
                    isScreenPinningActive = false
                ),
                network = networkState(
                    readinessStatus = networkReadinessStatus(
                        verdict = NetworkReadinessVerdict.Unstable,
                        userVerdict = NetworkReadinessUserVerdict.Unstable
                    )
                )
            )
        )

        assertEquals(
            listOf(
                PreparationCategory.DeviceSetup,
                PreparationCategory.DeviceLock,
                PreparationCategory.Connectivity
            ),
            overview.attention.map { it.category }
        )
        val pinningActions = overview.status(PreparationCategory.DeviceLock).actions
        assertTrue(pinningActions.any { it.code == QuickFixScreenPinningDeferredCode && it.isNotice })
    }

    @Test
    fun initialScanShowsScanningInsteadOfUnexplainedBlock() {
        val (readiness, overview) = overviewFor(
            preparationState(
                runtimeSecurity = runtimeSecurityState().copy(staticSecurityInitialScanComplete = false)
            )
        )

        assertFalse(readiness.canStartExam)
        assertEquals(PreparationOverallState.Scanning, overview.overallState)
    }

    @Test
    fun examGuardIssueAndFixShareTheScreenLockCategory() {
        val state = preparationState(
            device = deviceState().copy(
                screenPinningAvailable = false,
                isScreenPinningActive = false
            )
        )
        val (readiness, overview) = overviewFor(
            state,
            accessibilityGuardAvailable = true,
            accessibilityGuardEnabled = false
        )
        val lock = overview.status(PreparationCategory.DeviceLock)

        assertFalse(readiness.canStartExam)
        assertTrue(lock.issues.any { it.key == "exam_guard" && it.blocking })
        assertTrue(lock.actions.any { it.priority == 36 })
        assertFalse(
            overview.status(PreparationCategory.RuntimeInteraction).actions.any { it.priority == 36 }
        )
    }

    @Test
    fun unknownAccessibilityStateDoesNotClaimScreenLockIsUnsupported() {
        val state = preparationState(
            device = deviceState().copy(
                screenPinningAvailable = false,
                isScreenPinningActive = false
            )
        )
        val (_, loading) = overviewFor(
            state,
            accessibilityGuardAvailable = false,
            accessibilityStateLoaded = false
        )
        val (_, loaded) = overviewFor(
            state,
            accessibilityGuardAvailable = false,
            accessibilityStateLoaded = true
        )

        assertEquals(PreparationOverallState.Scanning, loading.overallState)
        assertFalse(
            loading.status(PreparationCategory.DeviceLock).issues.any { it.key == "screen_lock_unavailable" }
        )
        assertTrue(
            loaded.status(PreparationCategory.DeviceLock).issues.any { it.key == "screen_lock_unavailable" }
        )
    }

    @Test
    fun adminBypassedIntegrityShowsWarningWithoutBlocking() {
        val (readiness, overview) = overviewFor(
            preparationState(
                runtimeSecurity = runtimeSecurityState().copy(
                    integrityDetected = true,
                    integritySummary = "dex_hash_mismatch",
                    integrityBypassActive = true
                )
            )
        )

        assertTrue(readiness.canStartExam)
        assertEquals(PreparationTone.Warning, overview.status(PreparationCategory.DeviceIntegrity).tone)
    }

    @Test
    fun categoryFilterNeverListsATaggedActionTwice() {
        val actionsBySection = PreparationCategory.entries.map { category ->
            PreparationQuickFixAction(
                code = "section_${category.name}",
                text = category.name,
                severity = QuickFixSeverity.Blocking,
                target = null,
                priority = 1,
                section = category.preparationSection(),
                onClick = {}
            )
        } + PreparationQuickFixAction(
            // Code matches the RuntimeInteraction prefix fallback, but the section tag wins.
            code = "quick_fix_36",
            text = "Enable Exam Guard",
            severity = QuickFixSeverity.Blocking,
            target = QuickFixTarget.All,
            priority = 36,
            section = PreparationSection.DeviceLock,
            onClick = {}
        ) + PreparationQuickFixAction(
            code = QuickFixRefreshAllSecurityChecksCode,
            text = "Refresh",
            severity = QuickFixSeverity.Warning,
            target = null,
            priority = 900,
            onClick = {}
        )

        val placements = PreparationCategory.entries.flatMap { category ->
            filterQuickFixActionsForCategory(category, actionsBySection).map { it.code to category }
        }

        PreparationCategory.entries.forEach { category ->
            assertTrue(placements.contains("section_${category.name}" to category))
        }
        assertEquals(listOf(PreparationCategory.DeviceLock), placements.filter { it.first == "quick_fix_36" }.map { it.second })
        assertFalse(placements.any { it.first == QuickFixRefreshAllSecurityChecksCode })
        assertEquals(placements.size, placements.map { it.first }.distinct().size)
    }

    @Test
    fun forwardingActionsAlwaysReachTheLatestSessionActions() {
        var firstStarts = 0
        var secondStarts = 0
        var secondReports = 0
        val first = preparationActions().let {
            it.copy(session = it.session.copy(onStartExam = { firstStarts += 1 }))
        }
        val second = preparationActions().let {
            it.copy(
                session = it.session.copy(
                    onStartExam = { secondStarts += 1 },
                    onRequestSectionReport = { secondReports += 1 }
                )
            )
        }
        var current = first
        val forwarding = forwardingPreparationActions { current }

        forwarding.onStartExam()
        current = second
        forwarding.onStartExam()
        forwarding.onRequestSectionReport(com.coblax.examlock.model.DiagnosticSection.Network)

        assertEquals(1, firstStarts)
        assertEquals(1, secondStarts)
        assertEquals(1, secondReports)
    }

    @Test
    fun adviceOnlyFixesAreNotOfferedAsButtons() {
        val (_, overview) = overviewFor(
            preparationState(device = deviceState().copy(virtualEnvironmentDetected = true))
        )
        val integrity = overview.status(PreparationCategory.DeviceIntegrity)

        assertTrue(integrity.issues.any { it.key == "virtual_environment" && it.blocking })
        assertFalse(integrity.actions.any { it.code in AdviceOnlyQuickFixCodes })
    }

    /**
     * SELinux permissive only earns a warning (rootReady ignores it), but its quick fix
     * was Blocking, and Screen Pinning is withheld while any Blocking fix is listed. On
     * such a phone the pinning button never appeared and the exam could never start.
     */
    @Test
    fun selinuxPermissiveNeverWithholdsScreenPinning() {
        val device = deviceState().let { base ->
            base.copy(
                rootSecurityStatus = base.rootSecurityStatus.copy(
                    detected = true,
                    selinuxPermissive = true,
                    blocking = false
                ),
                isScreenPinningActive = false
            )
        }
        val (_, overview) = overviewFor(preparationState(device = device))

        val lockActions = overview.status(PreparationCategory.DeviceLock).actions
        assertTrue(lockActions.any { it.code == QuickFixStartScreenPinningCode })
        assertFalse(lockActions.any { it.code == QuickFixScreenPinningDeferredCode })
        assertFalse(overview.status(PreparationCategory.DeviceIntegrity).issues.any { it.blocking })
    }

    /** Start Exam refuses without a WebView provider, so the screen must say so first. */
    @Test
    fun missingWebViewIsARequiredFixNotASuggestion() {
        val unavailable = resolveWebViewCompatibilityStatus(packageName = null, versionName = null)
        val state = preparationState(
            device = deviceState().copy(webViewCompatibilityStatus = unavailable),
            diagnostics = diagnosticsWithHealth(
                PreExamHealthItem(
                    category = PreExamHealthCategory.WebView,
                    verdict = PreExamHealthVerdict.Blocking,
                    title = "WebView Provider",
                    detail = unavailable.studentSummary
                )
            )
        )
        val (readiness, overview) = overviewFor(state)

        assertFalse(readiness.startHealthReady)
        assertFalse(overview.canStartExam)
        val issue = overview.status(PreparationCategory.DeviceHealth).issues
            .single { it.key == "webview_unavailable" }
        assertTrue(issue.blocking)
        assertEquals(webViewProviderNote(unavailable, UiLanguage.English), issue.message)
    }

    /**
     * Policy: personal Android 7-11 phones may sit exams. Floating apps cannot be hidden
     * there, so the student is told to close them, and Start Exam stays available.
     */
    @Test
    fun legacyAndroidFloatingAppLimitIsASuggestionNotABlock() {
        val runtime = runtimeSecurityState().let { base ->
            base.copy(
                overlayRiskResult = base.overlayRiskResult.copy(
                    shieldStatus = OverlayShieldStatus(
                        supported = false,
                        requested = false,
                        lastApplySucceeded = null,
                        lastApplyAt = null
                    )
                ),
                dpcRuntimeStatus = defaultDpcRuntimeStatus(sdkInt = 30, overlayShieldSupported = false)
            )
        }
        val state = preparationState(
            runtimeSecurity = runtime,
            diagnostics = diagnosticsWithHealth(
                PreExamHealthItem(
                    category = PreExamHealthCategory.FloatingAppOverlay,
                    verdict = PreExamHealthVerdict.Warning,
                    title = "Floating App / Overlay",
                    detail = "Legacy Android normal APK cannot hide floating apps before Android 12."
                )
            )
        )
        val (readiness, overview) = overviewFor(state)

        assertTrue(readiness.startHealthReady)
        assertTrue(overview.canStartExam)
        val issue = overview.status(PreparationCategory.RuntimeInteraction).issues
            .single { it.key == "overlay_protection_limited" }
        assertFalse(issue.blocking)
    }

    /** A failed overlay shield on Android 12+ still refuses Start Exam, and says so here. */
    @Test
    fun failedOverlayShieldIsARequiredFix() {
        val runtime = runtimeSecurityState().let { base ->
            base.copy(
                overlayRiskResult = base.overlayRiskResult.copy(
                    shieldStatus = OverlayShieldStatus(
                        supported = true,
                        requested = true,
                        lastApplySucceeded = false,
                        lastApplyAt = null
                    )
                )
            )
        }
        val state = preparationState(
            runtimeSecurity = runtime,
            diagnostics = diagnosticsWithHealth(
                PreExamHealthItem(
                    category = PreExamHealthCategory.FloatingAppOverlay,
                    verdict = PreExamHealthVerdict.Blocking,
                    title = "Floating App / Overlay",
                    detail = "Android overlay shield is supported but failed to apply."
                )
            )
        )
        val (readiness, overview) = overviewFor(state)

        assertFalse(readiness.startHealthReady)
        assertFalse(overview.canStartExam)
        assertTrue(
            overview.status(PreparationCategory.RuntimeInteraction).issues
                .any { it.key == "overlay_shield_failed" && it.blocking }
        )
    }

    /** Health categories with their own readiness flag are not double-gated. */
    @Test
    fun screenPinningHealthBlockerDoesNotAddASecondGate() {
        val state = preparationState(
            diagnostics = diagnosticsWithHealth(
                PreExamHealthItem(
                    category = PreExamHealthCategory.ScreenPinning,
                    verdict = PreExamHealthVerdict.Blocking,
                    title = "Managed Kiosk Mode",
                    detail = "Lock task not started yet."
                )
            )
        )
        val (readiness, overview) = overviewFor(state)

        assertTrue(readiness.startHealthReady)
        assertTrue(overview.canStartExam)
    }

    /**
     * The fake-location app only blocks while Developer options are on, so "turn off
     * mock location" alone could not clear it. The message names the real fixes.
     */
    @Test
    fun fakeLocationAppMessageNamesTheAppAndTheRealFix() {
        val fake = fakeLocationRuntimeStatus().let { base ->
            base.copy(
                securityStatus = base.securityStatus.copy(
                    monitoringEnabled = true,
                    suspiciousFakeLocationPackages = listOf("com.lexa.fakegps"),
                    developerOptionsEnabled = true,
                    finalVerdict = LocationSpoofSecurityVerdict.PackageWarning
                )
            )
        }
        val (readiness, overview) = overviewFor(
            preparationState(location = locationState(fakeLocationRuntimeStatus = fake))
        )

        assertFalse(readiness.fakeLocationReady)
        val issue = overview.status(PreparationCategory.Location).issues
            .single { it.key == "fake_location_app" }
        assertTrue(issue.message!!.contains("com.lexa.fakegps"))
        assertTrue(issue.message!!.contains("Developer options"))
    }

    /**
     * Start Exam refuses offline and in airplane mode. They used to be optional
     * suggestions, so "Ready to start" was followed by a refusal.
     */
    @Test
    fun offlineAndAirplaneModeAreRequiredFixes() {
        listOf(
            NetworkReadinessVerdict.Offline to "network_offline",
            NetworkReadinessVerdict.AirplaneMode to "network_airplane"
        ).forEach { (verdict, key) ->
            val state = preparationState(
                network = networkState(readinessStatus = networkReadinessStatus(verdict = verdict)),
                device = deviceState().copy(isScreenPinningActive = false)
            )
            val (readiness, overview) = overviewFor(state)

            assertFalse(verdict.name, readiness.networkReachableReady)
            assertFalse(verdict.name, overview.canStartExam)
            assertTrue(
                verdict.name,
                overview.status(PreparationCategory.Connectivity).issues.any { it.key == key && it.blocking }
            )
            // Settings cannot be opened once pinned, so pinning waits for the network fix.
            val lockActions = overview.status(PreparationCategory.DeviceLock).actions
            assertTrue(verdict.name, lockActions.any { it.code == QuickFixScreenPinningDeferredCode })
            assertFalse(verdict.name, lockActions.any { it.code == QuickFixStartScreenPinningCode })
        }
    }

    @Test
    fun unstableNetworkStaysASuggestion() {
        val state = preparationState(
            network = networkState(
                readinessStatus = networkReadinessStatus(verdict = NetworkReadinessVerdict.Unstable)
            )
        )
        val (readiness, overview) = overviewFor(state)

        assertTrue(readiness.networkReachableReady)
        assertTrue(overview.canStartExam)
        assertTrue(overview.status(PreparationCategory.Connectivity).issues.none { it.blocking })
    }

    /**
     * Start Exam refuses on Developer options alone. The issue used to say "USB debugging
     * is on", so a student who turned off only USB debugging stayed blocked.
     */
    @Test
    fun developerOptionsAloneIsNamedAndTheFixSaysToTurnThemOff() {
        val device = deviceState().let { base ->
            base.copy(
                isScreenPinningActive = false,
                adbInspection = base.adbInspection.copy(
                    developerOptionsEnabled = true,
                    developerOptionsRawValue = "1"
                )
            )
        }
        val (_, overview) = overviewFor(preparationState(device = device))
        val integrity = overview.status(PreparationCategory.DeviceIntegrity)
        val issue = integrity.issues.single { it.key == "adb_enabled" }

        assertEquals("Developer options are on", issue.title)
        assertTrue(issue.message!!.contains("Developer options"))
        assertTrue(integrity.actions.any { it.buttonLabel() == "Turn Off Developer Options" })
    }

    /**
     * A touch through a floating app (or a failed shield on a start that never began)
     * used to block preparation for good, with no button and no way to clear it.
     */
    @Test
    fun recordedFloatingAppTouchOffersAWayForward() {
        val runtime = runtimeSecurityState().let { base ->
            base.copy(
                overlayRiskResult = base.overlayRiskResult.copy(confirmedInteractionDetected = true)
            )
        }
        val (readiness, overview) = overviewFor(preparationState(runtimeSecurity = runtime))
        val interaction = overview.status(PreparationCategory.RuntimeInteraction)

        assertFalse(readiness.overlayReady)
        assertTrue(interaction.issues.any { it.key == "overlay_risk" && it.blocking })
        assertTrue(interaction.actions.any { it.code == "overlay_permission_settings" && it.opensExternalSettings })
        assertTrue(interaction.actions.any { it.code == "overlay_violation_acknowledged" && !it.opensExternalSettings })
    }

    @Test
    fun endedExamScheduleIsARequiredIssueNamingTheEnd() {
        val state = preparationState(
            session = sessionState().copy(examEndDateTime = "21/04/2026 09:30")
        )
        val readiness = buildPreparationChecklistReadiness(
            state = state,
            needsBluetoothPermission = false,
            accessibilityGuardRequired = false,
            accessibilityGuardAvailable = true,
            accessibilityGuardEnabled = false,
            examScheduleEnded = true
        )
        val overview = buildPreparationOverview(
            state = state,
            readiness = readiness,
            quickFixActions = quickFixActionsFor(state),
            needsBluetoothPermission = false,
            accessibilityGuardAvailable = true,
            uiLanguage = UiLanguage.English
        )

        assertFalse(readiness.scheduleReady)
        assertFalse(overview.canStartExam)
        val issue = overview.status(PreparationCategory.DeviceHealth).issues.single { it.key == "exam_schedule_ended" }
        assertTrue(issue.blocking)
        assertTrue(issue.message!!.contains("09:30"))
    }

    private fun diagnosticsWithHealth(vararg items: PreExamHealthItem): PreparationDiagnosticsState {
        val base = diagnosticsState()
        return base.copy(
            preExamHealthCheckSnapshot = base.preExamHealthCheckSnapshot.copy(items = items.toList())
        )
    }

    private fun overviewFor(
        state: PreparationScreenState,
        accessibilityGuardAvailable: Boolean = true,
        accessibilityGuardEnabled: Boolean = false,
        accessibilityStateLoaded: Boolean = true
    ): Pair<PreparationChecklistReadiness, PreparationOverview> {
        val guardRequired =
            !state.screenPinningAvailable && !state.bypassScreenPinning && accessibilityGuardAvailable
        val readiness = buildPreparationChecklistReadiness(
            state = state,
            needsBluetoothPermission = false,
            accessibilityGuardRequired = guardRequired,
            accessibilityGuardAvailable = accessibilityGuardAvailable,
            accessibilityGuardEnabled = accessibilityGuardEnabled
        )
        val quickFixActions = buildPreparationQuickFixActions(
            state = state,
            actions = preparationActions(),
            uiLanguage = UiLanguage.English,
            accessibilityGuardRequired = guardRequired,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            geofenceReady = readiness.geofenceReady,
            fakeLocationReady = readiness.fakeLocationReady,
            needsBluetoothPermission = false,
            accessibilityInspection = accessibilityInspection(),
            runQuickFix = { _, _, _, action -> action() }
        )
        return readiness to buildPreparationOverview(
            state = state,
            readiness = readiness,
            quickFixActions = quickFixActions,
            needsBluetoothPermission = false,
            accessibilityGuardAvailable = accessibilityGuardAvailable,
            uiLanguage = UiLanguage.English,
            accessibilityStateLoaded = accessibilityStateLoaded
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
                onOpenOverlayGuardSettings = noOp,
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
                wirelessAdbEnabled = false,
                developerOptionsRawValue = "0",
                adbRawValue = "0",
                wirelessAdbRawValue = "-",
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
            tamperDetected = false,
            reverseEngineeringDetected = false,
            reverseEngineeringSummary = "-",
            reverseEngineeringBypassActive = false,
            integrityDetected = false,
            integritySummary = "-",
            integrityBypassActive = false
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
            bypassMultiWindow = false,
            bypassReverseEngineering = false,
            bypassApkIntegrity = false
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
