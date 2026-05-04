# Critical Smoke Test Checklist

Use this after large refactors or package-hygiene passes.

## Test Metadata

- Build under test:
- Device:
- Android version:
- Tester:
- Date:

Legend:
- `PASS`
- `FAIL`
- `NOTE`
- `N/A`

## Preflight

| Check | Result | Note |
| --- | --- | --- |
| App installs and opens from cold start |  |  |
| Home screen renders without crash |  |  |
| `Secret Admin` entry still works |  |  |
| `BuildConfig`-driven labels/version info still render correctly |  |  |

## Admin + Custom QR

| Check | Result | Note |
| --- | --- | --- |
| Open `Secret Admin` screen |  |  |
| Switch tabs in `Secret Admin` |  |  |
| Open `Custom QR` screen |  |  |
| Edit basic fields: exam name, URL, start/end time |  |  |
| Open `Circle` geofence editor |  |  |
| Open `Polygon` geofence editor |  |  |
| Search location in editor |  |  |
| Save geofence draft and return to `Custom QR` |  |  |
| Generate QR successfully |  |  |
| Generated QR preview/card still renders |  |  |
| Direct Link save/read flow still works |  |  |

## QR Intake

| Check | Result | Note |
| --- | --- | --- |
| Scan QR from camera |  |  |
| Scan QR from file/image picker |  |  |
| Parsed exam data appears correctly |  |  |
| Invalid QR still shows controlled error state |  |  |

## Preparation Screen

| Check | Result | Note |
| --- | --- | --- |
| Preparation screen opens without crash |  |  |
| `Refresh All Security Checks` responds |  |  |
| Network checklist item renders |  |  |
| Geofence checklist item renders |  |  |
| Anti-Fake-Location checklist item renders |  |  |
| Quick fixes still respond when available |  |  |
| `Open Geofence Map` opens viewer |  |  |
| Geofence map viewer closes cleanly |  |  |
| `Refresh Location Now` updates UI state |  |  |

## Start Exam Flow

| Check | Result | Note |
| --- | --- | --- |
| `Start Exam` proceeds when checklist is ready |  |  |
| WebView loads exam page |  |  |
| Bottom bar/chrome renders correctly |  |  |
| Built-in keyboard still opens when expected |  |  |
| Back handling / exit prompt still works |  |  |

## Runtime Warning / Violation

Trigger at least one runtime warning or dialog and verify the acknowledge path still works.

| Check | Result | Note |
| --- | --- | --- |
| Runtime dialog appears without crash |  |  |
| Dialog text/content looks correct |  |  |
| Acknowledge button works |  |  |
| Exam returns to stable runtime state after acknowledge |  |  |

Suggested runtime triggers:
- offline warning
- unstable network warning
- geofence warning
- anti-fake-location warning

## Regression Notes

- Any missing labels/import-related issue:
- Any screen that opened blank:
- Any callback/button that stopped responding:
- Any crash stack or reproduction steps:

## Sign-off

- Overall result:
- Safe to continue testing/build distribution:
