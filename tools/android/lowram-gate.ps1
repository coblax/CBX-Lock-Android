param(
    [string]$SummaryPath = "",
    [ValidateSet("AppHealth", "StrictDevice")]
    [string]$Mode = "AppHealth"
)

$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    $scriptDir = Split-Path -Parent $PSCommandPath
    return (Resolve-Path (Join-Path $scriptDir "..\..")).Path
}

function Resolve-SummaryPath([string]$rawPath) {
    if ($rawPath.Trim()) {
        $candidate = $rawPath.Trim()
        if (Test-Path $candidate -PathType Container) {
            $candidate = Join-Path $candidate "summary.json"
        }
        if (-not (Test-Path $candidate -PathType Leaf)) {
            throw "summary.json not found: $candidate"
        }
        return (Resolve-Path $candidate).Path
    }

    $runRoot = Join-Path (Get-RepoRoot) "dist\lowram-runs"
    if (-not (Test-Path $runRoot -PathType Container)) {
        throw "No SummaryPath was provided and $runRoot does not exist."
    }

    $latest = Get-ChildItem -Path $runRoot -Directory |
        ForEach-Object {
            $summary = Join-Path $_.FullName "summary.json"
            if (Test-Path $summary -PathType Leaf) {
                Get-Item $summary
            }
        } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $latest) {
        throw "No summary.json files found under $runRoot."
    }
    return $latest.FullName
}

function Get-BooleanOrFalse($value) {
    if ($null -eq $value) {
        return $false
    }
    return [bool]$value
}

function Get-NumberOrNull($value) {
    if ($null -eq $value) {
        return $null
    }
    return [double]$value
}

function Add-Check(
    [System.Collections.Generic.List[object]]$checks,
    [string]$name,
    [bool]$passed,
    [string]$actual,
    [string]$expected
) {
    $checks.Add([ordered]@{
        name = $name
        passed = $passed
        actual = $actual
        expected = $expected
    })
}

$resolvedSummaryPath = Resolve-SummaryPath $SummaryPath
$summary = Get-Content -Path $resolvedSummaryPath -Raw | ConvertFrom-Json
$checks = New-Object System.Collections.Generic.List[object]

$totalPssKb = Get-NumberOrNull $summary.totalPssKb
$homeFirstFrameMs = Get-NumberOrNull $summary.mainActivityToHomeFirstFrameMs
$appHomeReadyMs = Get-NumberOrNull $summary.appHomeReadyMs
if ($null -eq $appHomeReadyMs) {
    $appHomeReadyMs = $homeFirstFrameMs
}
$launchMetricMs = Get-NumberOrNull $summary.launchMetricMs
$appLmkCount = Get-NumberOrNull $summary.flags.appLowMemoryKillerCount
if ($null -eq $appLmkCount) {
    $appLmkCount = Get-NumberOrNull $summary.appLmkCount
}
if ($null -eq $appLmkCount) {
    $appLmkCount = 0
}

$aliveAfterIdle = Get-BooleanOrFalse $summary.process.aliveAfterIdle
$fatalException = Get-BooleanOrFalse $summary.flags.fatalException
$appAnr = Get-BooleanOrFalse $summary.flags.appAnr
$appKilledByLmk = Get-BooleanOrFalse $summary.flags.appKilledByLowMemoryKiller
$webViewCrash = Get-BooleanOrFalse $summary.flags.webViewCrash
$rendererGoneUnhandled = Get-BooleanOrFalse $summary.flags.rendererGoneUnhandled
$exitCleanupTimeoutRepeated = Get-BooleanOrFalse $summary.flags.exitCleanupTimeoutRepeated
if ($null -ne $summary.runtime) {
    $rendererGoneUnhandled = Get-BooleanOrFalse $summary.runtime.rendererGoneUnhandled
    $exitCleanupTimeoutRepeated = Get-BooleanOrFalse $summary.runtime.exitCleanupTimeoutRepeated
}
$noActiveFocusedSystemAnr = if ($null -ne $summary.acceptance.noActiveFocusedSystemAnrDialog) {
    [bool]$summary.acceptance.noActiveFocusedSystemAnrDialog
} else {
    -not (Get-BooleanOrFalse $summary.flags.focusedSystemAnrDialog)
}
$noAdminSettingsSyncFallbackBeforeHomeFirstFrame = if (
    $null -ne $summary.acceptance -and
    $null -ne $summary.acceptance.noAdminSettingsSyncFallbackBeforeHomeFirstFrame
) {
    [bool]$summary.acceptance.noAdminSettingsSyncFallbackBeforeHomeFirstFrame
} else {
    $true
}

Add-Check $checks "App alive after idle" `
    $aliveAfterIdle `
    "$aliveAfterIdle" `
    "True"
Add-Check $checks "No fatal exception" `
    (-not $fatalException) `
    "$fatalException" `
    "False"
Add-Check $checks "No app ANR" `
    (-not $appAnr) `
    "$appAnr" `
    "False"
Add-Check $checks "No app lowmemorykiller" `
    ((-not $appKilledByLmk) -and $appLmkCount -eq 0) `
    "killed=$appKilledByLmk count=$appLmkCount" `
    "killed=False count=0"
Add-Check $checks "No WebView crash" `
    (-not $webViewCrash) `
    "$webViewCrash" `
    "False"
Add-Check $checks "No unhandled WebView renderer gone" `
    (-not $rendererGoneUnhandled) `
    "$rendererGoneUnhandled" `
    "False"
Add-Check $checks "No repeated exit cleanup timeout" `
    (-not $exitCleanupTimeoutRepeated) `
    "$exitCleanupTimeoutRepeated" `
    "False"
Add-Check $checks "Total PSS < 35 MB" `
    (($null -ne $totalPssKb) -and $totalPssKb -lt (35 * 1024)) `
    "$(if ($null -ne $totalPssKb) { "$totalPssKb KB" } else { 'not parsed' })" `
    "< 35840 KB"
Add-Check $checks "App Home ready < 5000 ms" `
    (($null -ne $appHomeReadyMs) -and $appHomeReadyMs -lt 5000) `
    "$(if ($null -ne $appHomeReadyMs) { "$appHomeReadyMs ms" } else { 'not parsed' })" `
    "< 5000 ms"
Add-Check $checks "No sync admin settings before Home first frame" `
    $noAdminSettingsSyncFallbackBeforeHomeFirstFrame `
    "$noAdminSettingsSyncFallbackBeforeHomeFirstFrame" `
    "True"

if ($Mode -eq "StrictDevice") {
    Add-Check $checks "Launch metric < 8000 ms" `
        (($null -ne $launchMetricMs) -and $launchMetricMs -lt 8000) `
        "$(if ($null -ne $launchMetricMs) { "$launchMetricMs ms" } else { 'not parsed' })" `
        "< 8000 ms"
    Add-Check $checks "No active focused system ANR" `
        $noActiveFocusedSystemAnr `
        "$noActiveFocusedSystemAnr" `
        "True"
}

$failed = @($checks | Where-Object { -not $_.passed })
$passed = $failed.Count -eq 0
$status = if ($passed) { "PASS" } else { "FAIL" }

Write-Output "Low-RAM gate ($Mode): $status"
Write-Output "Summary: $resolvedSummaryPath"
Write-Output "Package: $($summary.package)"
Write-Output "Timestamp: $($summary.timestamp)"

foreach ($check in $checks) {
    $prefix = if ($check.passed) { "[PASS]" } else { "[FAIL]" }
    Write-Output "$prefix $($check.name) (actual: $($check.actual), expected: $($check.expected))"
}

if (-not $passed) {
    exit 1
}
exit 0
