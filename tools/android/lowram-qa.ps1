param(
    [string]$Serial = "",
    [string]$ApkPath = "",
    [string]$Package = "com.example.coblaxexamlock",
    [string]$OutDir = "",
    [switch]$Install,
    [switch]$SetViewport720x1280
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
    $content | Out-File -FilePath $path -Encoding utf8
}

function Parse-TotalTime($lines) {
    foreach ($line in $lines) {
        if ("$line" -match "TotalTime:\s*(\d+)") {
            return [int]$matches[1]
        }
    }
    return $null
}

function Parse-TotalPssKb($lines) {
    foreach ($line in $lines) {
        if ("$line" -match "^\s*TOTAL\s+(\d+)\s+") {
            return [int]$matches[1]
        }
    }
    return $null
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

function Test-LogHasPattern($lines, [string]$pattern) {
    foreach ($line in $lines) {
        if ("$line" -match $pattern) {
            return $true
        }
    }
    return $false
}

$repoRoot = Get-RepoRoot
$adb = Find-Adb $repoRoot
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
if (-not $OutDir.Trim()) {
    $OutDir = Join-Path $repoRoot "dist\lowram-runs\$timestamp"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$Serial = Resolve-TargetSerial $adb $Serial

if ($Install) {
    if (-not $ApkPath.Trim()) {
        $ApkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
    }
    if (-not (Test-Path $ApkPath)) {
        throw "APK not found: $ApkPath. Build it first or pass -ApkPath."
    }
    Invoke-Adb $adb $Serial @("install", "-r", (Resolve-Path $ApkPath).Path) |
        Tee-Object -FilePath (Join-Path $OutDir "install.txt") | Out-Null
}

Invoke-Adb $adb "" @("devices", "-l") |
    Out-File -FilePath (Join-Path $OutDir "adb-devices.txt") -Encoding utf8

if ($SetViewport720x1280) {
    Invoke-Adb $adb $Serial @("shell", "wm", "size", "720x1280") |
        Out-File -FilePath (Join-Path $OutDir "wm-size.txt") -Encoding utf8
}

Invoke-Adb $adb $Serial @("shell", "getprop") |
    Out-File -FilePath (Join-Path $OutDir "device-getprop.txt") -Encoding utf8
Invoke-Adb $adb $Serial @("shell", "cat", "/proc/meminfo") |
    Out-File -FilePath (Join-Path $OutDir "proc-meminfo.txt") -Encoding utf8

Invoke-Adb $adb $Serial @("logcat", "-c") | Out-Null
Invoke-Adb $adb $Serial @("shell", "am", "force-stop", $Package) | Out-Null
Start-Sleep -Seconds 1

$component = Resolve-LaunchComponent $adb $Serial $Package
$startLines = Invoke-Adb $adb $Serial @("shell", "am", "start", "-W", "-n", $component)
Write-TextFile (Join-Path $OutDir "am-start-W.txt") $startLines

Start-Sleep -Seconds 30

$meminfoLines = Invoke-Adb $adb $Serial @("shell", "dumpsys", "meminfo", $Package)
Write-TextFile (Join-Path $OutDir "meminfo-home.txt") $meminfoLines

$gfxinfoLines = Invoke-Adb $adb $Serial @("shell", "dumpsys", "gfxinfo", $Package)
Write-TextFile (Join-Path $OutDir "gfxinfo-home.txt") $gfxinfoLines

$framestatsLines = Invoke-Adb $adb $Serial @("shell", "dumpsys", "gfxinfo", $Package, "framestats")
Write-TextFile (Join-Path $OutDir "gfxinfo-framestats-home.txt") $framestatsLines

$screenshotDevicePath = "/sdcard/cbx-lowram-home-$timestamp.png"
Invoke-Adb $adb $Serial @("shell", "screencap", "-p", $screenshotDevicePath) | Out-Null
Invoke-Adb $adb $Serial @("pull", $screenshotDevicePath, (Join-Path $OutDir "home.png")) |
    Out-File -FilePath (Join-Path $OutDir "screenshot-pull.txt") -Encoding utf8
Invoke-Adb $adb $Serial @("shell", "rm", "-f", $screenshotDevicePath) | Out-Null

$logcatLines = Invoke-Adb $adb $Serial @("logcat", "-d", "-v", "time")
Write-TextFile (Join-Path $OutDir "logcat-full.txt") $logcatLines
$filteredLogcat = $logcatLines | Where-Object {
    $_ -match [regex]::Escape($Package) -or
    $_ -match "FATAL EXCEPTION|ANR in|AndroidRuntime|WebView|crash|Exception"
}
Write-TextFile (Join-Path $OutDir "logcat-filtered.txt") $filteredLogcat

$totalTime = Parse-TotalTime $startLines
$totalPssKb = Parse-TotalPssKb $meminfoLines
$gfxSummary = Parse-GfxSummary $gfxinfoLines
$hasFatalException = Test-LogHasPattern $filteredLogcat "FATAL EXCEPTION|AndroidRuntime"
$hasAnr = Test-LogHasPattern $filteredLogcat "ANR in\s+$([regex]::Escape($Package))"
$hasWebViewCrash = Test-LogHasPattern $filteredLogcat "WebView.*(crash|renderer|RenderProcessGone)|RenderProcessGone|renderer gone"

$summary = [ordered]@{
    timestamp = $timestamp
    package = $Package
    serial = if ($Serial.Trim()) { $Serial.Trim() } else { $null }
    adb = $adb
    launchComponent = $component
    outDir = (Resolve-Path $OutDir).Path
    totalTimeMs = $totalTime
    totalPssKb = $totalPssKb
    totalPssMb = if ($totalPssKb -ne $null) { [math]::Round($totalPssKb / 1024, 2) } else { $null }
    gfx = $gfxSummary
    flags = [ordered]@{
        fatalException = $hasFatalException
        anr = $hasAnr
        webViewCrash = $hasWebViewCrash
    }
    acceptance = [ordered]@{
        totalTimeUnder8000 = if ($totalTime -ne $null) { $totalTime -lt 8000 } else { $null }
        totalPssUnder35Mb = if ($totalPssKb -ne $null) { $totalPssKb -lt (35 * 1024) } else { $null }
        noFatalException = -not $hasFatalException
        noAnr = -not $hasAnr
        noWebViewCrash = -not $hasWebViewCrash
    }
}

$summaryJson = $summary | ConvertTo-Json -Depth 8
$summaryJson | Out-File -FilePath (Join-Path $OutDir "summary.json") -Encoding utf8

$summaryMarkdown = @(
    "# CBX Low-RAM QA Summary",
    "",
    "- Timestamp: $timestamp",
    "- Package: $Package",
    "- Device serial: $(if ($Serial.Trim()) { $Serial.Trim() } else { '(default adb device)' })",
    "- Launch component: $component",
    "- TotalTime: $(if ($totalTime -ne $null) { "$totalTime ms" } else { 'not parsed' })",
    "- Total PSS: $(if ($totalPssKb -ne $null) { "$totalPssKb KB / $([math]::Round($totalPssKb / 1024, 2)) MB" } else { 'not parsed' })",
    "- Frames: $($gfxSummary.totalFrames)",
    "- Janky frames: $($gfxSummary.jankyFrames) ($($gfxSummary.jankyPercent)%)",
    "- Fatal exception: $hasFatalException",
    "- ANR: $hasAnr",
    "- WebView crash: $hasWebViewCrash",
    "",
    "## Acceptance",
    "",
    "- TotalTime < 8000 ms: $($summary.acceptance.totalTimeUnder8000)",
    "- Total PSS < 35 MB: $($summary.acceptance.totalPssUnder35Mb)",
    "- No fatal exception: $($summary.acceptance.noFatalException)",
    "- No ANR: $($summary.acceptance.noAnr)",
    "- No WebView crash: $($summary.acceptance.noWebViewCrash)",
    "",
    "## Artifacts",
    "",
    '- `am-start-W.txt`',
    '- `meminfo-home.txt`',
    '- `gfxinfo-home.txt`',
    '- `gfxinfo-framestats-home.txt`',
    '- `home.png`',
    '- `logcat-filtered.txt`',
    '- `logcat-full.txt`'
)
$summaryMarkdown | Out-File -FilePath (Join-Path $OutDir "summary.md") -Encoding utf8

Write-Output "Low-RAM QA artifacts: $((Resolve-Path $OutDir).Path)"
Write-Output "TotalTime: $(if ($totalTime -ne $null) { "$totalTime ms" } else { 'not parsed' })"
Write-Output "Total PSS: $(if ($totalPssKb -ne $null) { "$totalPssKb KB" } else { 'not parsed' })"
Write-Output "Flags: fatal=$hasFatalException anr=$hasAnr webviewCrash=$hasWebViewCrash"
