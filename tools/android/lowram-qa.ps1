param(
    [Parameter(Position = 0)]
    [string]$ParseExistingPath = "",
    [string]$Serial = "",
    [string]$ApkPath = "",
    [string]$Package = "com.example.coblaxexamlock",
    [string]$OutDir = "",
    [ValidateSet("debug", "lowRamQa", "release")]
    [string]$Variant = "debug",
    [switch]$Build,
    [switch]$Install,
    [switch]$UninstallFirst,
    [switch]$SetViewport720x1280,
    [int]$PostInstallSettleSeconds = 0,
    [int]$IdleSeconds = 30,
    [switch]$TraceStartup,
    [int]$StartupTraceSeconds = 45,
    [ValidateSet("None", "AppHealth", "StrictDevice")]
    [string]$GateMode = "None",
    [switch]$ParseExisting
)

$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    $scriptDir = Split-Path -Parent $PSCommandPath
    return (Resolve-Path (Join-Path $scriptDir "..\..")).Path
}

function Convert-LocalPropertiesPath([string]$rawPath) {
    $value = $rawPath.Trim()
    $value = $value -replace "\\:", ":"
    $value = $value -replace "\\\\", "\"
    return $value
}

function ConvertTo-VariantTaskName([string]$variant) {
    return $variant.Substring(0, 1).ToUpperInvariant() + $variant.Substring(1)
}

function Find-Adb([string]$repoRoot) {
    $candidates = New-Object System.Collections.Generic.List[string]
    $localProperties = Join-Path $repoRoot "local.properties"
    if (Test-Path $localProperties) {
        $sdkLine = Get-Content $localProperties |
            Where-Object { $_ -match "^\s*sdk\.dir\s*=" } |
            Select-Object -First 1
        if ($sdkLine) {
            $sdkDir = Convert-LocalPropertiesPath(($sdkLine -split "=", 2)[1])
            $candidates.Add((Join-Path $sdkDir "platform-tools\adb.exe"))
        }
    }

    foreach ($envName in @("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
        $sdkDir = [Environment]::GetEnvironmentVariable($envName)
        if ($sdkDir) {
            $candidates.Add((Join-Path $sdkDir "platform-tools\adb.exe"))
        }
    }

    $pathAdb = Get-Command "adb.exe" -ErrorAction SilentlyContinue
    if ($pathAdb) {
        $candidates.Add($pathAdb.Source)
    }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return (Resolve-Path $candidate).Path
        }
    }

    throw "adb.exe not found. Configure local.properties sdk.dir, ANDROID_HOME, ANDROID_SDK_ROOT, or PATH."
}

function Invoke-Adb([string]$adb, [string]$serial, [string[]]$arguments) {
    $fullArgs = New-Object System.Collections.Generic.List[string]
    if ($serial.Trim()) {
        $fullArgs.Add("-s")
        $fullArgs.Add($serial.Trim())
    }
    foreach ($argument in $arguments) {
        $fullArgs.Add($argument)
    }
    & $adb @fullArgs
}

function Resolve-LaunchComponent([string]$adb, [string]$serial, [string]$packageName) {
    $lines = Invoke-Adb $adb $serial @("shell", "cmd", "package", "resolve-activity", "--brief", $packageName)
    $component = $lines |
        ForEach-Object { "$_".Trim() } |
        Where-Object { $_ -match "/" } |
        Select-Object -Last 1
    if (-not $component) {
        throw "Could not resolve launcher activity for package $packageName."
    }
    return $component
}

function Resolve-TargetSerial([string]$adb, [string]$requestedSerial) {
    $deviceLines = Invoke-Adb $adb "" @("devices", "-l")
    $connectedDevices = @($deviceLines |
        ForEach-Object { "$_".Trim() } |
        Where-Object { $_ -match "^(\S+)\s+device\b" } |
        ForEach-Object {
            [ordered]@{
                serial = ($_.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries)[0])
                line = $_
            }
        })

    if ($requestedSerial.Trim()) {
        $match = $connectedDevices | Where-Object { $_.serial -eq $requestedSerial.Trim() } | Select-Object -First 1
        if (-not $match) {
            throw "Requested adb serial '$requestedSerial' is not connected as a ready device."
        }
        return $requestedSerial.Trim()
    }

    if (-not $connectedDevices -or $connectedDevices.Count -eq 0) {
        throw "No ready adb device found. Start an emulator/device first, then rerun the script."
    }
    if ($connectedDevices.Count -gt 1) {
        $serials = ($connectedDevices | ForEach-Object { $_.serial }) -join ", "
        throw "Multiple adb devices found ($serials). Pass -Serial to choose one."
    }
    return $connectedDevices[0].serial
}

function Write-TextFile([string]$path, $content) {
    if ($null -eq $content) {
        "" | Out-File -FilePath $path -Encoding utf8
    } else {
        $content | Out-File -FilePath $path -Encoding utf8
    }
}

function Resolve-DefaultApkPath([string]$repoRoot, [string]$variant) {
    $variantDir = Join-Path $repoRoot "app\build\outputs\apk\$variant"
    $candidateNames = @(
        "app-$variant.apk",
        "app-$($variant.ToLowerInvariant()).apk"
    )
    foreach ($candidateName in $candidateNames) {
        $candidatePath = Join-Path $variantDir $candidateName
        if (Test-Path $candidatePath) {
            return (Resolve-Path $candidatePath).Path
        }
    }
    if (Test-Path $variantDir) {
        $apk = Get-ChildItem -Path $variantDir -Filter "*.apk" -File |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($apk) {
            return $apk.FullName
        }
    }
    return Join-Path $variantDir "app-$variant.apk"
}

function Invoke-GradleBuild([string]$repoRoot, [string]$variant, [string]$outDir) {
    $gradlew = Join-Path $repoRoot "gradlew.bat"
    if (-not (Test-Path $gradlew)) {
        throw "gradlew.bat not found at $gradlew."
    }
    $taskName = ":app:assemble$(ConvertTo-VariantTaskName $variant)"
    $output = & $gradlew $taskName "--console=plain" 2>&1
    $exitCode = $LASTEXITCODE
    Write-TextFile (Join-Path $outDir "gradle-build.txt") $output
    if ($exitCode -ne 0) {
        throw "Gradle build failed for $taskName. See gradle-build.txt."
    }
}

function Parse-IntField($lines, [string]$fieldName) {
    foreach ($line in $lines) {
        if ("$line" -match "$([regex]::Escape($fieldName)):\s*(\d+)") {
            return [int]$matches[1]
        }
    }
    return $null
}

function Parse-MeminfoSummary($lines) {
    foreach ($line in $lines) {
        $text = "$line"
        if ($text -match "^\s*TOTAL\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)") {
            return [ordered]@{
                totalPssKb = [int]$matches[1]
                privateDirtyKb = [int]$matches[2]
                privateCleanKb = [int]$matches[3]
                swapPssDirtyKb = [int]$matches[4]
            }
        }
    }
    return [ordered]@{
        totalPssKb = $null
        privateDirtyKb = $null
        privateCleanKb = $null
        swapPssDirtyKb = $null
    }
}

function Parse-LaunchComponent($lines) {
    foreach ($line in $lines) {
        if ("$line" -match "Activity:\s*(\S+/\S+)") {
            return $matches[1]
        }
        if ("$line" -match "cmp=(\S+/\S+)\s*}") {
            return $matches[1]
        }
    }
    return ""
}

function Parse-SerialFromAdbDevices($lines) {
    foreach ($line in $lines) {
        $text = "$line".Trim()
        if ($text -match "^(\S+)\s+device\b") {
            return $matches[1]
        }
    }
    return ""
}

function Parse-GfxSummary($lines) {
    $summary = [ordered]@{
        totalFrames = $null
        jankyFrames = $null
        jankyPercent = $null
        percentile90ms = $null
        percentile95ms = $null
        percentile99ms = $null
    }
    foreach ($line in $lines) {
        $text = "$line"
        if ($text -match "Total frames rendered:\s*(\d+)") {
            $summary.totalFrames = [int]$matches[1]
        } elseif ($text -match "Janky frames:\s*(\d+)\s*\(([^%]+)%\)") {
            $summary.jankyFrames = [int]$matches[1]
            $summary.jankyPercent = [double]$matches[2]
        } elseif ($text -match "90th percentile:\s*(\d+)ms") {
            $summary.percentile90ms = [int]$matches[1]
        } elseif ($text -match "95th percentile:\s*(\d+)ms") {
            $summary.percentile95ms = [int]$matches[1]
        } elseif ($text -match "99th percentile:\s*(\d+)ms") {
            $summary.percentile99ms = [int]$matches[1]
        }
    }
    return $summary
}

function Test-LinesHavePattern($lines, [string]$pattern) {
    foreach ($line in $lines) {
        if ("$line" -match $pattern) {
            return $true
        }
    }
    return $false
}

function Count-LinesWithPattern($lines, [string]$pattern) {
    $count = 0
    foreach ($line in $lines) {
        if ("$line" -match $pattern) {
            $count += 1
        }
    }
    return $count
}

function ConvertTo-DurationMs([string]$durationText) {
    $text = $durationText.Trim().TrimStart("+")
    if ($text -match "^(\d+)m(\d+)s(\d+)ms$") {
        return ([int]$matches[1] * 60000) + ([int]$matches[2] * 1000) + [int]$matches[3]
    }
    if ($text -match "^(\d+)s(\d+)ms$") {
        return ([int]$matches[1] * 1000) + [int]$matches[2]
    }
    if ($text -match "^(\d+)ms$") {
        return [int]$matches[1]
    }
    return $null
}

function Parse-DisplayedMs($lines, [string]$packageName) {
    $escapedPackage = [regex]::Escape($packageName)
    foreach ($line in $lines) {
        $text = "$line"
        if ($text -match "Displayed\s+$escapedPackage[^:]*:\s+\+?([0-9]+m[0-9]+s[0-9]+ms|[0-9]+s[0-9]+ms|[0-9]+ms)") {
            return ConvertTo-DurationMs $matches[1]
        }
    }
    return $null
}

function Parse-StartupTimeline($lines) {
    $items = @()
    $lineIndex = 0
    foreach ($line in $lines) {
        $text = "$line"
        if ($text -match "StartupTimeline\(\s*(\d+)\):\s*event=([^|]+)\|\s*elapsed_ms=(\d+)\s*\|\s*uptime_ms=(\d+)(?:\s*\|\s*(.*))?$") {
            $items += [ordered]@{
                pid = [int]$matches[1]
                event = $matches[2].Trim()
                elapsedMs = [int]$matches[3]
                uptimeMs = [int64]$matches[4]
                extra = if ($matches.Count -ge 6 -and $matches[5]) { $matches[5].Trim() } else { "" }
                lineIndex = $lineIndex
                raw = $text
            }
        }
        $lineIndex += 1
    }
    return $items
}

function Select-StartupTimelineRun($timeline) {
    $selected = $null
    foreach ($group in ($timeline | Group-Object -Property pid)) {
        $events = @($group.Group)
        $activity = Get-TimelineElapsedMs $events "activity_on_create_start"
        $firstFrame = Get-TimelineElapsedMs $events "home_first_frame"
        if ($activity -ne $null -and $firstFrame -ne $null) {
            $lastIndex = ($events | ForEach-Object { [int]$_["lineIndex"] } | Measure-Object -Maximum).Maximum
            if ($selected -eq $null -or $lastIndex -gt $selected.lastLineIndex) {
                $selected = [ordered]@{
                    pid = [int]$group.Name
                    events = $events
                    lastLineIndex = $lastIndex
                }
            }
        }
    }
    if ($selected -ne $null) {
        return $selected
    }
    if ($timeline -and $timeline.Count -gt 0) {
        $lastPid = ($timeline | Select-Object -Last 1).pid
        return [ordered]@{
            pid = $lastPid
            events = @($timeline | Where-Object { $_.pid -eq $lastPid })
            lastLineIndex = ($timeline | ForEach-Object { [int]$_["lineIndex"] } | Measure-Object -Maximum).Maximum
        }
    }
    return $null
}

function Get-TimelineElapsedMs($timeline, [string]$eventName) {
    foreach ($item in $timeline) {
        if ($item.event -eq $eventName) {
            return $item.elapsedMs
        }
    }
    return $null
}

function Test-SystemAnrBeforeDisplayed($lines, [string]$packageName) {
    $escapedPackage = [regex]::Escape($packageName)
    $displayedIndex = -1
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ("$($lines[$index])" -match "Displayed\s+$escapedPackage[^:]*:") {
            $displayedIndex = $index
            break
        }
    }
    if ($displayedIndex -lt 0) {
        return $null
    }
    for ($index = 0; $index -lt $displayedIndex; $index++) {
        if ("$($lines[$index])" -match "ANR in\s+(?!$escapedPackage)") {
            return $true
        }
    }
    return $false
}

function Count-SystemLmkKills($lines, [string]$packageName) {
    $escapedPackage = [regex]::Escape($packageName)
    $count = 0
    foreach ($line in $lines) {
        $text = "$line"
        if ($text -match "lowmemorykiller.*(Kill|Killing)" -and $text -notmatch $escapedPackage) {
            $count += 1
        }
    }
    return $count
}

function Count-AppLmkKills($lines, [string]$packageName) {
    $escapedPackage = [regex]::Escape($packageName)
    return Count-LinesWithPattern $lines "lowmemorykiller.*(Kill|Killing).*?$escapedPackage"
}

function Get-FocusedAnrOwner($windowLines) {
    foreach ($line in $windowLines) {
        $text = "$line"
        if ($text -match "mCurrentFocus=.*Application Not Responding:\s*([^}\s]+)") {
            return $matches[1]
        }
        if ($text -match "mCurrentFocus=.*Process system isn't responding") {
            return "system"
        }
    }
    return $null
}

function Get-StartupDiagnosis($mainActivityToHomeFirstFrameMs, $displayedMs, $systemAnrBeforeDisplayed, $launchMetricMs) {
    if ($mainActivityToHomeFirstFrameMs -ne $null) {
        if ($mainActivityToHomeFirstFrameMs -lt 5000) {
            if (($displayedMs -ne $null -and $displayedMs -ge 8000) -or ($launchMetricMs -ne $null -and $launchMetricMs -ge 8000)) {
                if ($systemAnrBeforeDisplayed -eq $true) {
                    return "app_first_frame_fast_system_anr_before_displayed"
                }
                return "app_first_frame_fast_displayed_or_wait_slow"
            }
            return "app_first_frame_within_target"
        }
        return "app_first_frame_slow"
    }
    if ($displayedMs -ne $null -and $displayedMs -ge 8000 -and $systemAnrBeforeDisplayed -eq $true) {
        return "displayed_slow_with_system_anr_before_displayed"
    }
    return "insufficient_startup_timeline"
}

function Read-ArtifactLines([string]$outDir, [string]$name) {
    $path = Join-Path $outDir $name
    if (Test-Path $path) {
        return @(Get-Content -Path $path)
    }
    return @()
}

function Write-Summary(
    [string]$outDir,
    [string]$packageName,
    [string]$serial,
    [string]$adb,
    [string]$variant,
    [string]$apkPath,
    [string]$component,
    [int]$idleSeconds
) {
    $escapedPackage = [regex]::Escape($packageName)
    $startLines = Read-ArtifactLines $outDir "am-start-W.txt"
    $meminfoLines = Read-ArtifactLines $outDir "meminfo-home.txt"
    $gfxinfoLines = Read-ArtifactLines $outDir "gfxinfo-home.txt"
    $logcatLines = Read-ArtifactLines $outDir "logcat-full.txt"
    $filteredLogcat = Read-ArtifactLines $outDir "logcat-filtered.txt"
    $windowLines = Read-ArtifactLines $outDir "dumpsys-window.txt"
    $pidLines = Read-ArtifactLines $outDir "pidof-after-idle.txt"
    $adbDeviceLines = Read-ArtifactLines $outDir "adb-devices.txt"
    $perfettoTrace = Get-ChildItem -Path $outDir -Filter "startup-*.pftrace" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    $resolvedComponent = if ($component.Trim()) { $component.Trim() } else { Parse-LaunchComponent $startLines }
    $resolvedSerial = if ($serial.Trim()) { $serial.Trim() } else { Parse-SerialFromAdbDevices $adbDeviceLines }

    if (-not $filteredLogcat -or $filteredLogcat.Count -eq 0) {
        $filteredLogcat = $logcatLines | Where-Object {
            $_ -match $escapedPackage -or
            $_ -match "FATAL EXCEPTION|ANR in|AndroidRuntime|WebView|crash|Exception|lowmemorykiller|Application Not Responding" -or
            $_ -match "ExamRuntimeHardening|WEBVIEW_RENDERER_GONE|WEBVIEW_RECOVERY_READY|WEBVIEW_EXIT_CLEANUP_|MEMORY_TRIM_HANDLED"
        }
        Write-TextFile (Join-Path $outDir "logcat-filtered.txt") $filteredLogcat
    }

    $totalTime = Parse-IntField $startLines "TotalTime"
    $waitTime = Parse-IntField $startLines "WaitTime"
    $launchMetric = if ($totalTime -ne $null) { $totalTime } else { $waitTime }
    $launchMetricSource = if ($totalTime -ne $null) { "TotalTime" } elseif ($waitTime -ne $null) { "WaitTime" } else { $null }
    $displayedMs = Parse-DisplayedMs $logcatLines $packageName
    $startupTimeline = Parse-StartupTimeline $logcatLines
    $selectedStartupRun = Select-StartupTimelineRun $startupTimeline
    $selectedStartupTimeline = if ($selectedStartupRun) { @($selectedStartupRun.events) } else { @() }
    $activityOnCreateMs = Get-TimelineElapsedMs $selectedStartupTimeline "activity_on_create_start"
    $homeFirstFrameMs = Get-TimelineElapsedMs $selectedStartupTimeline "home_first_frame"
    $composeSetContentStartMs = Get-TimelineElapsedMs $selectedStartupTimeline "compose_set_content_start"
    $nativeSurvivalIdleReadyMs = Get-TimelineElapsedMs $selectedStartupTimeline "native_survival_idle_ready"
    $nativeHomeDirectLinkLabelLoadedMs = Get-TimelineElapsedMs $selectedStartupTimeline "native_home_direct_link_label_loaded"
    $nativeHomeViewReadyMs = Get-TimelineElapsedMs $selectedStartupTimeline "native_home_view_ready"
    $nativeHomeMainIdleMs = Get-TimelineElapsedMs $selectedStartupTimeline "native_home_main_idle"
    $homeFirstFrameMarker = @($selectedStartupTimeline | Where-Object { $_.event -eq "home_first_frame" } | Select-Object -First 1)
    $homeFirstFrameShell = if ($homeFirstFrameMarker) { "$($homeFirstFrameMarker.extra)" } else { "" }
    $nativeSurvivalHome = $homeFirstFrameShell -match "native_survival"
    $composeStartedBeforeHomeFirstFrame = if ($composeSetContentStartMs -ne $null -and $homeFirstFrameMs -ne $null) {
        $composeSetContentStartMs -le $homeFirstFrameMs
    } else {
        $null
    }
    $adminSettingsSyncFallbackBeforeHomeFirstFrame = if ($homeFirstFrameMs -ne $null) {
        @($selectedStartupTimeline | Where-Object {
            $_.event -eq "admin_settings_sync_fallback" -and [int]$_.elapsedMs -le $homeFirstFrameMs
        }).Count -gt 0
    } else {
        $null
    }
    $adminSettingsLoadBeforeHomeFirstFrame = if ($homeFirstFrameMs -ne $null) {
        @($selectedStartupTimeline | Where-Object {
            $_.event -eq "admin_settings_load_start" -and [int]$_.elapsedMs -le $homeFirstFrameMs
        }).Count -gt 0
    } else {
        $null
    }
    $appStartupMs = $homeFirstFrameMs
    $mainActivityToHomeFirstFrameMs = if ($activityOnCreateMs -ne $null -and $homeFirstFrameMs -ne $null) {
        $homeFirstFrameMs - $activityOnCreateMs
    } else {
        $null
    }
    $mainActivityToNativeHomeViewReadyMs = if ($activityOnCreateMs -ne $null -and $nativeHomeViewReadyMs -ne $null) {
        $nativeHomeViewReadyMs - $activityOnCreateMs
    } else {
        $null
    }
    $appHomeReadyMs = if ($mainActivityToNativeHomeViewReadyMs -ne $null) {
        $mainActivityToNativeHomeViewReadyMs
    } else {
        $mainActivityToHomeFirstFrameMs
    }
    $systemAnrBeforeDisplayed = Test-SystemAnrBeforeDisplayed $logcatLines $packageName
    $systemLmkCount = Count-SystemLmkKills $logcatLines $packageName
    $appLmkCount = Count-AppLmkKills $logcatLines $packageName
    $startupDiagnosis = Get-StartupDiagnosis $mainActivityToHomeFirstFrameMs $displayedMs $systemAnrBeforeDisplayed $launchMetric
    $memSummary = Parse-MeminfoSummary $meminfoLines
    $totalPssKb = $memSummary.totalPssKb
    $privateDirtyKb = $memSummary.privateDirtyKb
    $gfxSummary = Parse-GfxSummary $gfxinfoLines

    $hasFatalException = Test-LinesHavePattern $filteredLogcat "FATAL EXCEPTION|AndroidRuntime"
    $hasAppAnr = Test-LinesHavePattern $filteredLogcat "ANR in\s+$escapedPackage"
    $hasSystemAnr = Test-LinesHavePattern $filteredLogcat "ANR in\s+(?!$escapedPackage)"
    $hasAnrDialogWindow = Test-LinesHavePattern $windowLines "Application Not Responding|Process system isn't responding"
    $hasFocusedAnrDialog = Test-LinesHavePattern $windowLines "mCurrentFocus=.*Application Not Responding|mCurrentFocus=.*Process system isn't responding"
    $focusedAnrOwner = Get-FocusedAnrOwner $windowLines
    $hasFocusedSystemAnrDialog = $hasFocusedAnrDialog -and $focusedAnrOwner -eq "system"
    $focusedAnrLikelyStale = $hasFocusedAnrDialog -and (-not $hasSystemAnr) -and (-not $hasAppAnr)
    $hasWebViewCrash = Test-LinesHavePattern $filteredLogcat "WebView.*(crash|renderer|RenderProcessGone)|RenderProcessGone|renderer gone"
    $appKilledByLmk = Test-LinesHavePattern $filteredLogcat "lowmemorykiller.*Kill '$escapedPackage'|lowmemorykiller.*Kill .*?$escapedPackage"
    $runtimeRendererGoneCount = Count-LinesWithPattern $filteredLogcat "WEBVIEW_RENDERER_GONE"
    $runtimeRecoveryReadyCount = Count-LinesWithPattern $filteredLogcat "WEBVIEW_RECOVERY_READY"
    $runtimeExitCleanupStartedCount = Count-LinesWithPattern $filteredLogcat "WEBVIEW_EXIT_CLEANUP_STARTED"
    $runtimeExitCleanupSucceededCount = Count-LinesWithPattern $filteredLogcat "WEBVIEW_EXIT_CLEANUP_SUCCEEDED"
    $runtimeExitCleanupTimeoutCount = Count-LinesWithPattern $filteredLogcat "WEBVIEW_EXIT_CLEANUP_TIMEOUT"
    $runtimeExitCleanupFailedCount = Count-LinesWithPattern $filteredLogcat "WEBVIEW_EXIT_CLEANUP_FAILED"
    $runtimeMemoryTrimHandledCount = Count-LinesWithPattern $filteredLogcat "MEMORY_TRIM_HANDLED"
    $rendererGoneUnhandled = $runtimeRendererGoneCount -gt $runtimeRecoveryReadyCount
    $exitCleanupTimeoutRepeated = $runtimeExitCleanupTimeoutCount -gt 1
    $appStartCount = Count-LinesWithPattern $logcatLines "Start proc \d+:$escapedPackage/"
    $appRestarted = $appStartCount -gt 1
    $pidText = ($pidLines | ForEach-Object { "$_".Trim() } | Where-Object { $_ }) -join " "
    $appAliveAfterIdle = $pidText.Trim().Length -gt 0
    $selectedStartupPidValue = if ($selectedStartupTimeline -and $selectedStartupTimeline.Count -gt 0) {
        @($selectedStartupTimeline)[0].pid
    } else {
        $null
    }

    $summary = [ordered]@{
        timestamp = Split-Path -Leaf $outDir
        package = $packageName
        variant = $variant
        serial = if ($resolvedSerial.Trim()) { $resolvedSerial.Trim() } else { $null }
        adb = $adb
        apkPath = $apkPath
        launchComponent = $resolvedComponent
        outDir = (Resolve-Path $outDir).Path
        idleSeconds = $idleSeconds
        perfettoTrace = if ($perfettoTrace) { $perfettoTrace.FullName } else { $null }
        totalTimeMs = $totalTime
        waitTimeMs = $waitTime
        launchMetricMs = $launchMetric
        launchMetricSource = $launchMetricSource
        displayedMs = $displayedMs
        appStartupMs = $appStartupMs
        appHomeReadyMs = $appHomeReadyMs
        mainActivityToHomeFirstFrameMs = $mainActivityToHomeFirstFrameMs
        mainActivityToNativeHomeViewReadyMs = $mainActivityToNativeHomeViewReadyMs
        composeSetContentStartMs = $composeSetContentStartMs
        nativeHomeViewReadyMs = $nativeHomeViewReadyMs
        nativeHomeMainIdleMs = $nativeHomeMainIdleMs
        nativeSurvivalIdleReadyMs = $nativeSurvivalIdleReadyMs
        nativeHomeDirectLinkLabelLoadedMs = $nativeHomeDirectLinkLabelLoadedMs
        systemAnrBeforeDisplayed = $systemAnrBeforeDisplayed
        systemLmkCount = $systemLmkCount
        appLmkCount = $appLmkCount
        selectedStartupPid = $selectedStartupPidValue
        startupDiagnosis = $startupDiagnosis
        startupTimeline = $startupTimeline
        selectedStartupTimeline = $selectedStartupTimeline
        startupShell = [ordered]@{
            homeFirstFrameShell = $homeFirstFrameShell
            nativeSurvivalHome = $nativeSurvivalHome
            composeStartedBeforeHomeFirstFrame = $composeStartedBeforeHomeFirstFrame
            adminSettingsSyncFallbackBeforeHomeFirstFrame = $adminSettingsSyncFallbackBeforeHomeFirstFrame
            adminSettingsLoadBeforeHomeFirstFrame = $adminSettingsLoadBeforeHomeFirstFrame
        }
        totalPssKb = $totalPssKb
        totalPssMb = if ($totalPssKb -ne $null) { [math]::Round($totalPssKb / 1024, 2) } else { $null }
        privateDirtyKb = $privateDirtyKb
        privateDirtyMb = if ($privateDirtyKb -ne $null) { [math]::Round($privateDirtyKb / 1024, 2) } else { $null }
        privateCleanKb = $memSummary.privateCleanKb
        swapPssDirtyKb = $memSummary.swapPssDirtyKb
        gfx = $gfxSummary
        process = [ordered]@{
            pidAfterIdle = $pidText
            aliveAfterIdle = $appAliveAfterIdle
            startProcCount = $appStartCount
            restartedDuringRun = $appRestarted
        }
        flags = [ordered]@{
            fatalException = $hasFatalException
            appAnr = $hasAppAnr
            systemAnr = $hasSystemAnr
            anrDialogWindowPresent = $hasAnrDialogWindow
            focusedAnrDialog = $hasFocusedAnrDialog
            focusedSystemAnrDialog = $hasFocusedSystemAnrDialog
            focusedAnrOwner = $focusedAnrOwner
            focusedAnrLikelyStale = $focusedAnrLikelyStale
            systemAnrBeforeDisplayed = $systemAnrBeforeDisplayed
            appKilledByLowMemoryKiller = $appKilledByLmk
            appLowMemoryKillerCount = $appLmkCount
            systemLowMemoryKillerCount = $systemLmkCount
            webViewCrash = $hasWebViewCrash
            rendererGoneUnhandled = $rendererGoneUnhandled
            exitCleanupTimeoutRepeated = $exitCleanupTimeoutRepeated
        }
        runtime = [ordered]@{
            webViewRendererGoneCount = $runtimeRendererGoneCount
            webViewRecoveryReadyCount = $runtimeRecoveryReadyCount
            webViewExitCleanupStartedCount = $runtimeExitCleanupStartedCount
            webViewExitCleanupSucceededCount = $runtimeExitCleanupSucceededCount
            webViewExitCleanupTimeoutCount = $runtimeExitCleanupTimeoutCount
            webViewExitCleanupFailedCount = $runtimeExitCleanupFailedCount
            memoryTrimHandledCount = $runtimeMemoryTrimHandledCount
            rendererGoneUnhandled = $rendererGoneUnhandled
            exitCleanupTimeoutRepeated = $exitCleanupTimeoutRepeated
        }
        acceptance = [ordered]@{
            launchMetricUnder8000 = if ($launchMetric -ne $null) { $launchMetric -lt 8000 } else { $null }
            appHomeReadyUnder5000 = if ($appHomeReadyMs -ne $null) { $appHomeReadyMs -lt 5000 } else { $null }
            mainActivityToHomeFirstFrameUnder5000 = if ($mainActivityToHomeFirstFrameMs -ne $null) { $mainActivityToHomeFirstFrameMs -lt 5000 } else { $null }
            nativeSurvivalNoComposeBeforeFirstFrame = if ($nativeSurvivalHome -and $composeStartedBeforeHomeFirstFrame -ne $null) { -not $composeStartedBeforeHomeFirstFrame } else { $null }
            noAdminSettingsSyncFallbackBeforeHomeFirstFrame = if ($adminSettingsSyncFallbackBeforeHomeFirstFrame -ne $null) { -not $adminSettingsSyncFallbackBeforeHomeFirstFrame } else { $null }
            totalPssUnder35Mb = if ($totalPssKb -ne $null) { $totalPssKb -lt (35 * 1024) } else { $null }
            appAliveAfterIdle = $appAliveAfterIdle
            noFatalException = -not $hasFatalException
            noAppAnr = -not $hasAppAnr
            noFocusedSystemAnrDialog = -not $hasFocusedSystemAnrDialog
            noActiveFocusedSystemAnrDialog = -not $hasFocusedSystemAnrDialog -or $focusedAnrLikelyStale
            noAppLowMemoryKiller = -not $appKilledByLmk
            noWebViewCrash = -not $hasWebViewCrash
            noUnhandledWebViewRendererGone = -not $rendererGoneUnhandled
            noRepeatedExitCleanupTimeout = -not $exitCleanupTimeoutRepeated
        }
    }

    $summary | ConvertTo-Json -Depth 8 |
        Out-File -FilePath (Join-Path $outDir "summary.json") -Encoding utf8

    $focusedAnrOwnerSuffix = if ($focusedAnrOwner) { " ($focusedAnrOwner)" } else { "" }
    $selectedStartupPidText = if ($selectedStartupPidValue -ne $null) { "$selectedStartupPidValue" } else { "not parsed" }

    $summaryMarkdown = @(
        "# CBX Low-RAM QA Summary",
        "",
        "- Timestamp: $($summary.timestamp)",
        "- Package: $packageName",
        "- Variant: $variant",
        "- Device serial: $(if ($resolvedSerial.Trim()) { $resolvedSerial.Trim() } else { '(default adb device)' })",
        "- Launch component: $resolvedComponent",
        "- Perfetto trace: $(if ($perfettoTrace) { $perfettoTrace.Name } else { 'not captured' })",
        "- Launch metric: $(if ($launchMetric -ne $null) { "$launchMetric ms ($launchMetricSource)" } else { 'not parsed' })",
        "- TotalTime: $(if ($totalTime -ne $null) { "$totalTime ms" } else { 'not parsed' })",
        "- WaitTime: $(if ($waitTime -ne $null) { "$waitTime ms" } else { 'not parsed' })",
        "- Displayed: $(if ($displayedMs -ne $null) { "$displayedMs ms" } else { 'not parsed' })",
        "- App startup to Home first frame: $(if ($appStartupMs -ne $null) { "$appStartupMs ms from process marker baseline" } else { 'not parsed' })",
        "- App Home ready metric: $(if ($appHomeReadyMs -ne $null) { "$appHomeReadyMs ms" } else { 'not parsed' })",
        "- MainActivity to Home first frame: $(if ($mainActivityToHomeFirstFrameMs -ne $null) { "$mainActivityToHomeFirstFrameMs ms" } else { 'not parsed' })",
        "- MainActivity to native Home view ready: $(if ($mainActivityToNativeHomeViewReadyMs -ne $null) { "$mainActivityToNativeHomeViewReadyMs ms" } else { 'not parsed' })",
        "- Compose setContent marker: $(if ($composeSetContentStartMs -ne $null) { "$composeSetContentStartMs ms" } else { 'not parsed' })",
        "- Native Home view ready marker: $(if ($nativeHomeViewReadyMs -ne $null) { "$nativeHomeViewReadyMs ms" } else { 'not parsed' })",
        "- Native Home main idle marker: $(if ($nativeHomeMainIdleMs -ne $null) { "$nativeHomeMainIdleMs ms" } else { 'not parsed' })",
        "- Native survival idle-ready marker: $(if ($nativeSurvivalIdleReadyMs -ne $null) { "$nativeSurvivalIdleReadyMs ms" } else { 'not parsed' })",
        "- Native Direct Link label loaded: $(if ($nativeHomeDirectLinkLabelLoadedMs -ne $null) { "$nativeHomeDirectLinkLabelLoadedMs ms" } else { 'not parsed' })",
        "- Native survival Home: $nativeSurvivalHome",
        "- Compose started before Home first frame: $(if ($composeStartedBeforeHomeFirstFrame -ne $null) { "$composeStartedBeforeHomeFirstFrame" } else { 'not parsed' })",
        "- Admin settings sync fallback before Home first frame: $(if ($adminSettingsSyncFallbackBeforeHomeFirstFrame -ne $null) { "$adminSettingsSyncFallbackBeforeHomeFirstFrame" } else { 'not parsed' })",
        "- Admin settings IO load before Home first frame: $(if ($adminSettingsLoadBeforeHomeFirstFrame -ne $null) { "$adminSettingsLoadBeforeHomeFirstFrame" } else { 'not parsed' })",
        "- Selected startup PID: $selectedStartupPidText",
        "- System ANR before Displayed: $(if ($systemAnrBeforeDisplayed -ne $null) { "$systemAnrBeforeDisplayed" } else { 'not parsed' })",
        "- System lowmemorykiller count: $systemLmkCount",
        "- App lowmemorykiller count: $appLmkCount",
        "- Startup diagnosis: $startupDiagnosis",
        "- Total PSS: $(if ($totalPssKb -ne $null) { "$totalPssKb KB / $([math]::Round($totalPssKb / 1024, 2)) MB" } else { 'not parsed' })",
        "- Private Dirty: $(if ($privateDirtyKb -ne $null) { "$privateDirtyKb KB / $([math]::Round($privateDirtyKb / 1024, 2)) MB" } else { 'not parsed' })",
        "- App alive after idle: $appAliveAfterIdle",
        "- App start proc count: $appStartCount",
        "- App restarted during run: $appRestarted",
        "- Fatal exception: $hasFatalException",
        "- App ANR: $hasAppAnr",
        "- System ANR: $hasSystemAnr",
        "- ANR dialog window present: $hasAnrDialogWindow",
        "- Focused ANR dialog: $hasFocusedAnrDialog$focusedAnrOwnerSuffix",
        "- Focused ANR likely stale: $focusedAnrLikelyStale",
        "- App killed by lowmemorykiller: $appKilledByLmk",
        "- WebView crash: $hasWebViewCrash",
        "- Runtime renderer gone count: $runtimeRendererGoneCount",
        "- Runtime recovery ready count: $runtimeRecoveryReadyCount",
        "- Runtime renderer gone unhandled: $rendererGoneUnhandled",
        "- Runtime exit cleanup timeout count: $runtimeExitCleanupTimeoutCount",
        "- Runtime repeated cleanup timeout: $exitCleanupTimeoutRepeated",
        "- Runtime memory trim handled count: $runtimeMemoryTrimHandledCount",
        "- Frames: $($gfxSummary.totalFrames)",
        "- Janky frames: $($gfxSummary.jankyFrames) ($($gfxSummary.jankyPercent)%)",
        "",
        "## Acceptance",
        "",
        "- Launch metric < 8000 ms: $($summary.acceptance.launchMetricUnder8000)",
        "- App Home ready < 5000 ms: $($summary.acceptance.appHomeReadyUnder5000)",
        "- MainActivity to Home first frame < 5000 ms: $($summary.acceptance.mainActivityToHomeFirstFrameUnder5000)",
        "- Native survival no Compose before first frame: $($summary.acceptance.nativeSurvivalNoComposeBeforeFirstFrame)",
        "- No admin settings sync fallback before Home first frame: $($summary.acceptance.noAdminSettingsSyncFallbackBeforeHomeFirstFrame)",
        "- Total PSS < 35 MB: $($summary.acceptance.totalPssUnder35Mb)",
        "- App alive after idle: $($summary.acceptance.appAliveAfterIdle)",
        "- No fatal exception: $($summary.acceptance.noFatalException)",
        "- No app ANR: $($summary.acceptance.noAppAnr)",
        "- No focused system ANR dialog: $($summary.acceptance.noFocusedSystemAnrDialog)",
        "- No active focused system ANR dialog: $($summary.acceptance.noActiveFocusedSystemAnrDialog)",
        "- No app lowmemorykiller kill: $($summary.acceptance.noAppLowMemoryKiller)",
        "- No WebView crash: $($summary.acceptance.noWebViewCrash)",
        "- No unhandled WebView renderer gone: $($summary.acceptance.noUnhandledWebViewRendererGone)",
        "- No repeated exit cleanup timeout: $($summary.acceptance.noRepeatedExitCleanupTimeout)",
        "",
        "## Selected Startup Timeline",
        ""
    )
    if ($selectedStartupTimeline -and $selectedStartupTimeline.Count -gt 0) {
        $summaryMarkdown += @(
            "| PID | Event | Elapsed ms | Extra |",
            "| ---: | --- | ---: | --- |"
        )
        foreach ($item in $selectedStartupTimeline) {
            $safeExtra = "$($item.extra)" -replace "\|", "/"
            $summaryMarkdown += "| $($item.pid) | $($item.event) | $($item.elapsedMs) | $safeExtra |"
        }
        $summaryMarkdown += ""
    } else {
        $summaryMarkdown += @(
            "No ``StartupTimeline`` markers were found in ``logcat-full.txt``.",
            ""
        )
    }
    if ($startupTimeline -and $startupTimeline.Count -gt $selectedStartupTimeline.Count) {
        $summaryMarkdown += @(
            "## All Startup Timeline Markers",
            "",
            "| PID | Event | Elapsed ms | Extra |",
            "| ---: | --- | ---: | --- |"
        )
        foreach ($item in $startupTimeline) {
            $safeExtra = "$($item.extra)" -replace "\|", "/"
            $summaryMarkdown += "| $($item.pid) | $($item.event) | $($item.elapsedMs) | $safeExtra |"
        }
        $summaryMarkdown += ""
    }
    $summaryMarkdown += @(
        "## Startup Diagnosis",
        "",
        "- Diagnosis: $startupDiagnosis",
        "- Interpretation: $(if ($mainActivityToHomeFirstFrameMs -ne $null -and $mainActivityToHomeFirstFrameMs -lt 5000 -and (($displayedMs -ne $null -and $displayedMs -ge 8000) -or ($launchMetric -ne $null -and $launchMetric -ge 8000))) { 'App first-frame markers are fast, so slow Android Displayed/WaitTime is likely system or emulator pressure.' } elseif ($mainActivityToHomeFirstFrameMs -ne $null -and $mainActivityToHomeFirstFrameMs -ge 5000) { 'App Home first-frame markers are slow and should be optimized before blaming the system.' } else { 'Timeline evidence is incomplete; use launch/memory/logcat flags as the primary signal.' })",
        "",
        "## Artifacts",
        "",
        "- ``am-start-W.txt``",
        "- ``pidof-after-idle.txt``",
        "- ``meminfo-home.txt``",
        "- ``meminfo-local-home.txt``",
        "- ``gfxinfo-home.txt``",
        "- ``gfxinfo-framestats-home.txt``",
        "- ``dumpsys-window.txt``",
        "- ``dumpsys-activity-top.txt``",
        "- ``dumpsys-activity-processes.txt``",
        "- ``dumpsys-package.txt``",
        "- ``device-abi.txt``",
        "- ``home.png``",
        "- ``startup-*.pftrace`` when ``-TraceStartup`` is used",
        "- ``logcat-filtered.txt``",
        "- ``logcat-full.txt``"
    )
    $summaryMarkdown | Out-File -FilePath (Join-Path $outDir "summary.md") -Encoding utf8

    return $summary
}

function Invoke-LowRamGate([string]$repoRoot, [string]$outDir, [string]$mode) {
    if ($mode -eq "None") {
        return
    }

    $gateScript = Join-Path $repoRoot "tools\android\lowram-gate.ps1"
    if (-not (Test-Path $gateScript -PathType Leaf)) {
        throw "Low-RAM gate script not found: $gateScript"
    }

    & powershell -NoProfile -ExecutionPolicy Bypass -File $gateScript `
        -SummaryPath (Join-Path $outDir "summary.json") `
        -Mode $mode
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

$repoRoot = Get-RepoRoot
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
if (-not $OutDir.Trim()) {
    $OutDir = Join-Path $repoRoot "dist\lowram-runs\$timestamp"
}
if ($ParseExisting -and $ParseExistingPath.Trim()) {
    $OutDir = $ParseExistingPath.Trim()
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$OutDir = (Resolve-Path $OutDir).Path

if ($ParseExisting) {
    $summary = Write-Summary `
        -outDir $OutDir `
        -packageName $Package `
        -serial $Serial `
        -adb "" `
        -variant $Variant `
        -apkPath $ApkPath `
        -component "" `
        -idleSeconds $IdleSeconds
    Write-Output "Low-RAM QA artifacts: $OutDir"
    Write-Output "Launch metric: $(if ($summary.launchMetricMs -ne $null) { "$($summary.launchMetricMs) ms ($($summary.launchMetricSource))" } else { 'not parsed' })"
    Write-Output "App Home ready: $(if ($summary.appHomeReadyMs -ne $null) { "$($summary.appHomeReadyMs) ms" } else { 'not parsed' })"
    Write-Output "MainActivity to Home first frame: $(if ($summary.mainActivityToHomeFirstFrameMs -ne $null) { "$($summary.mainActivityToHomeFirstFrameMs) ms" } else { 'not parsed' })"
    Write-Output "Startup diagnosis: $($summary.startupDiagnosis)"
    Write-Output "Total PSS: $(if ($summary.totalPssKb -ne $null) { "$($summary.totalPssKb) KB" } else { 'not parsed' })"
    Write-Output "Flags: fatal=$($summary.flags.fatalException) appAnr=$($summary.flags.appAnr) systemAnr=$($summary.flags.systemAnr) focusedAnr=$($summary.flags.focusedAnrDialog) lmk=$($summary.flags.appKilledByLowMemoryKiller) webviewCrash=$($summary.flags.webViewCrash)"
    Invoke-LowRamGate $repoRoot $OutDir $GateMode
    exit 0
}

$adb = Find-Adb $repoRoot
$Serial = Resolve-TargetSerial $adb $Serial

if ($Build) {
    Invoke-GradleBuild $repoRoot $Variant $OutDir
}

if ($Install -or $Build) {
    if (-not $ApkPath.Trim()) {
        $ApkPath = Resolve-DefaultApkPath $repoRoot $Variant
    }
    if (-not (Test-Path $ApkPath)) {
        throw "APK not found: $ApkPath. Build it first or pass -ApkPath."
    }
    $ApkPath = (Resolve-Path $ApkPath).Path
}

Invoke-Adb $adb "" @("devices", "-l") |
    Out-File -FilePath (Join-Path $OutDir "adb-devices.txt") -Encoding utf8

if ($SetViewport720x1280) {
    Invoke-Adb $adb $Serial @("shell", "wm", "size", "720x1280") |
        Out-File -FilePath (Join-Path $OutDir "wm-size.txt") -Encoding utf8
}

Invoke-Adb $adb $Serial @("shell", "getprop") |
    Out-File -FilePath (Join-Path $OutDir "device-getprop.txt") -Encoding utf8
Invoke-Adb $adb $Serial @("shell", "getprop", "ro.product.cpu.abilist") |
    Out-File -FilePath (Join-Path $OutDir "device-abi.txt") -Encoding utf8
Invoke-Adb $adb $Serial @("shell", "cat", "/proc/meminfo") |
    Out-File -FilePath (Join-Path $OutDir "proc-meminfo.txt") -Encoding utf8

if ($UninstallFirst) {
    Invoke-Adb $adb $Serial @("uninstall", $Package) |
        Out-File -FilePath (Join-Path $OutDir "uninstall.txt") -Encoding utf8
}

if ($Install -or $Build) {
    $installLines = Invoke-Adb $adb $Serial @("install", "-r", $ApkPath)
    Write-TextFile (Join-Path $OutDir "install.txt") $installLines
    if (($installLines -join "`n") -notmatch "Success") {
        throw "Install failed. See install.txt."
    }
    if ($PostInstallSettleSeconds -gt 0) {
        Start-Sleep -Seconds $PostInstallSettleSeconds
    }
}

Invoke-Adb $adb $Serial @("shell", "dumpsys", "package", $Package) |
    Out-File -FilePath (Join-Path $OutDir "dumpsys-package.txt") -Encoding utf8
Invoke-Adb $adb $Serial @("shell", "pm", "path", $Package) |
    Out-File -FilePath (Join-Path $OutDir "pm-path.txt") -Encoding utf8

Invoke-Adb $adb $Serial @("logcat", "-c") | Out-Null
Invoke-Adb $adb $Serial @("shell", "am", "force-stop", $Package) | Out-Null
Start-Sleep -Seconds 1

$startupTraceBaseName = "startup-$timestamp.pftrace"
$startupTraceDevicePath = "/data/misc/perfetto-traces/$startupTraceBaseName"
$startupTraceHostPath = Join-Path $OutDir $startupTraceBaseName
$startupTraceStarted = $false
if ($TraceStartup) {
    try {
        Invoke-Adb $adb $Serial @("shell", "rm", "-f", $startupTraceDevicePath) | Out-Null
        $perfettoStartLines = Invoke-Adb $adb $Serial @(
            "shell",
            "perfetto",
            "--background-wait",
            "-o",
            $startupTraceDevicePath,
            "-t",
            "$($StartupTraceSeconds)s",
            "--app",
            $Package,
            "sched",
            "freq",
            "idle",
            "am",
            "wm",
            "gfx",
            "view",
            "binder_driver",
            "dalvik"
        )
        Write-TextFile (Join-Path $OutDir "perfetto-start.txt") $perfettoStartLines
        $startupTraceStarted = $true
    } catch {
        Write-TextFile (Join-Path $OutDir "perfetto-start.txt") "Perfetto start failed: $($_.Exception.Message)"
    }
}

$component = Resolve-LaunchComponent $adb $Serial $Package
$startLines = Invoke-Adb $adb $Serial @("shell", "am", "start", "-W", "-n", $component)
Write-TextFile (Join-Path $OutDir "am-start-W.txt") $startLines

if ($IdleSeconds -gt 0) {
    Start-Sleep -Seconds $IdleSeconds
}

if ($TraceStartup -and $startupTraceStarted) {
    $minimumExtraWaitSeconds = [Math]::Max(0, $StartupTraceSeconds - $IdleSeconds + 2)
    if ($minimumExtraWaitSeconds -gt 0) {
        Start-Sleep -Seconds $minimumExtraWaitSeconds
    }
    try {
        $pullLines = Invoke-Adb $adb $Serial @("pull", $startupTraceDevicePath, $startupTraceHostPath)
        Write-TextFile (Join-Path $OutDir "perfetto-pull.txt") $pullLines
    } catch {
        Write-TextFile (Join-Path $OutDir "perfetto-pull.txt") "Perfetto pull failed: $($_.Exception.Message)"
    }
}

Invoke-Adb $adb $Serial @("shell", "pidof", "-s", $Package) |
    Out-File -FilePath (Join-Path $OutDir "pidof-after-idle.txt") -Encoding utf8

$meminfoLines = Invoke-Adb $adb $Serial @("shell", "dumpsys", "meminfo", $Package)
Write-TextFile (Join-Path $OutDir "meminfo-home.txt") $meminfoLines

Invoke-Adb $adb $Serial @("shell", "dumpsys", "meminfo", "--local", $Package) |
    Out-File -FilePath (Join-Path $OutDir "meminfo-local-home.txt") -Encoding utf8

$gfxinfoLines = Invoke-Adb $adb $Serial @("shell", "dumpsys", "gfxinfo", $Package)
Write-TextFile (Join-Path $OutDir "gfxinfo-home.txt") $gfxinfoLines

Invoke-Adb $adb $Serial @("shell", "dumpsys", "gfxinfo", $Package, "framestats") |
    Out-File -FilePath (Join-Path $OutDir "gfxinfo-framestats-home.txt") -Encoding utf8
Invoke-Adb $adb $Serial @("shell", "dumpsys", "window") |
    Out-File -FilePath (Join-Path $OutDir "dumpsys-window.txt") -Encoding utf8
Invoke-Adb $adb $Serial @("shell", "dumpsys", "activity", "top") |
    Out-File -FilePath (Join-Path $OutDir "dumpsys-activity-top.txt") -Encoding utf8
Invoke-Adb $adb $Serial @("shell", "dumpsys", "activity", "processes") |
    Out-File -FilePath (Join-Path $OutDir "dumpsys-activity-processes.txt") -Encoding utf8

$screenshotDevicePath = "/sdcard/cbx-lowram-home-$timestamp.png"
Invoke-Adb $adb $Serial @("shell", "screencap", "-p", $screenshotDevicePath) | Out-Null
Invoke-Adb $adb $Serial @("pull", $screenshotDevicePath, (Join-Path $OutDir "home.png")) |
    Out-File -FilePath (Join-Path $OutDir "screenshot-pull.txt") -Encoding utf8
Invoke-Adb $adb $Serial @("shell", "rm", "-f", $screenshotDevicePath) | Out-Null

$logcatLines = Invoke-Adb $adb $Serial @("logcat", "-d", "-v", "time")
Write-TextFile (Join-Path $OutDir "logcat-full.txt") $logcatLines
$filteredLogcat = $logcatLines | Where-Object {
    $_ -match [regex]::Escape($Package) -or
    $_ -match "FATAL EXCEPTION|ANR in|AndroidRuntime|WebView|crash|Exception|lowmemorykiller|Application Not Responding" -or
    $_ -match "ExamRuntimeHardening|WEBVIEW_RENDERER_GONE|WEBVIEW_RECOVERY_READY|WEBVIEW_EXIT_CLEANUP_|MEMORY_TRIM_HANDLED"
}
Write-TextFile (Join-Path $OutDir "logcat-filtered.txt") $filteredLogcat

$summary = Write-Summary `
    -outDir $OutDir `
    -packageName $Package `
    -serial $Serial `
    -adb $adb `
    -variant $Variant `
    -apkPath $ApkPath `
    -component $component `
    -idleSeconds $IdleSeconds

Write-Output "Low-RAM QA artifacts: $OutDir"
Write-Output "Launch metric: $(if ($summary.launchMetricMs -ne $null) { "$($summary.launchMetricMs) ms ($($summary.launchMetricSource))" } else { 'not parsed' })"
Write-Output "App Home ready: $(if ($summary.appHomeReadyMs -ne $null) { "$($summary.appHomeReadyMs) ms" } else { 'not parsed' })"
Write-Output "MainActivity to Home first frame: $(if ($summary.mainActivityToHomeFirstFrameMs -ne $null) { "$($summary.mainActivityToHomeFirstFrameMs) ms" } else { 'not parsed' })"
Write-Output "Startup diagnosis: $($summary.startupDiagnosis)"
Write-Output "Total PSS: $(if ($summary.totalPssKb -ne $null) { "$($summary.totalPssKb) KB" } else { 'not parsed' })"
Write-Output "Private Dirty: $(if ($summary.privateDirtyKb -ne $null) { "$($summary.privateDirtyKb) KB" } else { 'not parsed' })"
Write-Output "Flags: fatal=$($summary.flags.fatalException) appAnr=$($summary.flags.appAnr) systemAnr=$($summary.flags.systemAnr) focusedAnr=$($summary.flags.focusedAnrDialog) lmk=$($summary.flags.appKilledByLowMemoryKiller) webviewCrash=$($summary.flags.webViewCrash)"
Invoke-LowRamGate $repoRoot $OutDir $GateMode
