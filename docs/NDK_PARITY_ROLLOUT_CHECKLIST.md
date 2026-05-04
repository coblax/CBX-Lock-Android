# NDK Parity Rollout Checklist

Use this before enabling broad user rollout for the hybrid NDK security path.

## Gate

Strict gate rules:
- `assembleDebug` must pass
- `assembleRelease` must pass
- parity instrumentation must pass on `x86_64` and `arm64-v8a`
- manual smoke must pass on `armeabi-v7a`, `arm64-v8a`, and `x86_64`
- Kotlin fallback remains enabled until the whole matrix is green

Legend:
- `PASS`
- `FAIL`
- `NOTE`
- `N/A`

## Device Matrix

| Device | ABI | Android | Result | Note |
| --- | --- | --- | --- | --- |
| Physical phone | `armeabi-v7a` | 7.x |  |  |
| Physical phone | `arm64-v8a` | 8+ |  |  |
| Emulator | `x86_64` | Current test image |  |  |

## Native Load + Fallback

| Check | Result | Note |
| --- | --- | --- |
| Native library loads successfully on each target ABI |  |  |
| Debug parity tests can force native path |  |  |
| Debug parity tests can force Kotlin fallback path |  |  |
| App remains stable when native path is unavailable and fallback is used |  |  |

## Automated Parity

| Check | Result | Note |
| --- | --- | --- |
| `NativeReverseEngineeringParityTest` passes on `x86_64` |  |  |
| `NativeReverseEngineeringParityTest` passes on `arm64-v8a` |  |  |
| `NativeIntegrityGuardParityTest` passes on `x86_64` |  |  |
| `NativeIntegrityGuardParityTest` passes on `arm64-v8a` |  |  |
| `NativeGeofenceParityTest` passes on `x86_64` |  |  |
| `NativeGeofenceParityTest` passes on `arm64-v8a` |  |  |

## Manual Smoke

| Check | Result | Note |
| --- | --- | --- |
| Preparation screen renders normally |  |  |
| `Refresh All Security Checks` still responds |  |  |
| Geofence editor and viewer still open/close correctly |  |  |
| Start exam flow still works |  |  |
| One runtime warning/dialog can be acknowledged cleanly |  |  |
| Integrity / reverse-engineering diagnostics still look sane |  |  |
| No ABI-specific crash or blank screen appears |  |  |

## Rollout Decision

- Overall result:
- Safe for end-user rollout:
- Remaining risks or follow-ups:
