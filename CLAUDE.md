# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

CBX Exam Lock (`com.coblax.examlock`, label "CBX Lock") is an Android lockdown browser for school exams. A student scans an encrypted exam QR (or opens a saved exam link), passes a device-readiness checklist, screen-pins the app, and takes the exam in a locked-down WebView while the app keeps watching for cheating signals (overlays, app switch, clipboard, Bluetooth, ADB/root, screen recording/mirroring, VPN, fake location, device time, etc.). Admins generate the QRs inside the same app.

- Kotlin + Jetpack Compose (Material 3), plus a C++17 JNI library (`app/src/main/cpp`, lib `examlock_native`).
- minSdk 24, target/compile 36, Java 11. Version lives in `app/build.gradle.kts` (`versionCode`, `versionName`); the installer module parses it from there.
- README.md (Indonesian) is the product spec: student/admin flows, checklist behaviour, network-retry rules, low-RAM profiles, smoke-test list. Read it before changing user-facing behaviour.
- The owner writes in Indonesian; UI copy is bilingual (see i18n below).

## Modules

- `:app` — the exam app.
- `:installer` — "CBX Installer" (`com.coblax.examlock.installer`, minSdk 21 on purpose). Its `bundleCbxApk` task depends on `:app:assembleRelease`, copies `app-release.apk` into assets with a `cbx.properties` (size, SHA-256, versionCode, ABIs), and installs it via PackageInstaller after checking the phone. It reports failures to Telegram only from release builds.
- `:baselineprofile` — Baseline Profile generator (`BaselineProfileGenerator.kt`); profile output is merged into `app/src/main/generated/baselineProfiles/`.

## Commands (Windows / PowerShell)

`git` is not on PATH in this environment.

```powershell
.\gradlew.bat :app:compileDebugKotlin          # fast compile check
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest            # JVM unit tests
.\gradlew.bat :app:testDebugUnitTest --tests "com.coblax.examlock.ExamQrCodecTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.coblax.examlock.ExamQrCodecTest.someMethod"
.\gradlew.bat :app:connectedDebugAndroidTest    # instrumentation, needs device/emulator
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.coblax.examlock.ui.exam.ExamWebViewNetworkTest"
.\gradlew.bat :app:lintDebug                    # config: app/lint.xml
.\gradlew.bat :installer:testDebugUnitTest
.\gradlew.bat :installer:assembleRelease        # builds app release + installer
```

- Any `assemble/bundle/package*Release` task fails fast unless release signing is configured (`release.keystore.path`, `release.keystore.password`, `release.key.alias`, `release.key.password` in `local.properties`, or `CBX_RELEASE_KEYSTORE*` env vars).
- Other `local.properties` keys read by Gradle: `telegram.bot.token`, `telegram.bug.chat.id`, `telegram.release.diagnostics.enabled`, `signing.sha256.release`, `signing.sha256.debug`, `maps.api.key`. They are XOR(115)+Base64 obfuscated into `BuildConfig.*_OBF`. Telegram credentials are compiled into debug and `lowRamQa` always, into release only when release diagnostics are enabled.
- Build types: `debug`, `release` (R8 + resource shrink, dex hash asset), `lowRamQa` (release-like, profileable, debug-signed, diagnostics on).
- Release/lowRamQa builds generate `assets/dex.sha256` from the R8 output (`GenerateDexHashAssetTask`); `IntegrityGuard` compares it at runtime.
- Baseline profile: `.\gradlew.bat :app:generateBaselineProfile -Pandroid.testInstrumentationRunnerArguments.cbxAdminPassword=<pw>` on an API 33+ emulator (or rooted API 28+) without the release APK installed. Never put the admin password in code.
- Low-RAM QA scripts: `tools/android/lowram-qa.ps1` (build/install/drive flows, write `dist/lowram-runs/*/summary.json`) and `tools/android/lowram-gate.ps1` (evaluate a summary). Note `lowram-qa.ps1` still defaults `-Package` to the old `com.example.coblaxexamlock`; pass `-Package com.coblax.examlock`.
- Manual checklists: `docs/CRITICAL_SMOKE_TEST_CHECKLIST.md` (after big refactors), `docs/NDK_PARITY_ROLLOUT_CHECKLIST.md` (native path changes).

## Architecture

### Entry and navigation
`MainActivity` is the single activity. It resolves a `LowRamProfile` first; on very low-memory phones it shows a View-based home shell (no Compose) and only starts Compose when the user acts. Compose root is `ui/app/AppContent` → `AppHostContent`, which switches on `model.AppScreen` (`Home`, `CustomQrAdmin`, `ExamWebView`, `SecretAdmin`). There is no navigation library.

The manifest declares `configChanges` for uiMode/keyboard/screen size so the activity is **not** recreated mid-exam (recreation dropped screen pinning and ended the session). Don't remove those or rely on recreation.

### Exam QR format (`ExamQrCodec.kt`)
`CBXEL2:` + base64url(IV‖AES-GCM ciphertext) of a `|`-separated field list. The AES key is derived (SHA-256) from a seed embedded in both Kotlin and `cpp/qr_crypto.cpp` — they must stay identical. Payload versions are additive for backward compatibility: v7 = base, v8 only when security bypasses are set, v9 only when a minimum app version is set. Keep emitting the lowest version a QR needs so older apps can still read it. `CBXEL1:` is rejected.

### Native bridge pattern (`nativebridge/`)
Security-sensitive operations (QR crypto, admin secret PBKDF2 verify, bypass device binding/MAC, geofence geometry, /proc scans, TracerPid, dex hash, runtime string decoding, clipboard snapshot) exist **twice**: in C++ and as a Kotlin fallback. `NativeSecurityBridge.invokeOrFallback` runs native and falls back to Kotlin on load failure, exception or unusable result. `NativeBridgeTestControl` (debug only) can force either backend; androidTest `Native*ParityTest` classes assert both produce the same results.

When changing one of these algorithms, change both sides and run the parity tests. JNI uses `RegisterNatives` in `JNI_OnLoad` against `NativeBridgeBindings` (+ `ClipboardNormalizedItemInput`, `NativeClipboardSnapshotCore`); those are `-keep` in `proguard-rules.pro`. Renaming them or their members breaks native silently in release.

### Exam runtime (`ui/exam/`, the bulk of the code)
The session is one large Compose screen split across many files to stay below the size ART compiles cheaply:
- `ExamRuntimeSessionImpl` — the session composable; builds the ops objects.
- `ExamRuntimeSessionCore` — one object carrying all shared session state/handlers, passed to sections instead of capturing dozens of locals. Rebuilt every composition.
- `*Ops` classes (`ExamRuntimeDiagnosticsOps`, `ExamRuntimeMonitoringOps`, `ExamRuntimeSecurityOps`) — behaviour; `*State` files — state holders; `*Effects` files — `LaunchedEffect`/`DisposableEffect` monitors (pinning, overlay, clipboard, network, tamper, location).
- Start flow: `ExamRuntimeStartSessionCoordinator` (prechecks: network with recovery, device time, location/geofence, health blockers) → `ExamRuntimeStartExam*Controller`.
- WebView: `ExamWebViewClient`, `ExamWebViewInfrastructure`, `ExamWebViewNavigationState`, `ExamRuntimeServerProbe`, `ExamRuntimeNetworkPoller`. The retry/backoff/insecure-cert rules are in the README "Halaman Ujian dan Koneksi" section. Never auto-retry POSTs, and never show the exam host to students.
- JS injected into the exam page (built-in keyboard, fullscreen hooks) lives in `config/ExamScripts.kt`.
- Stalls (`ExamStallRecovery.kt`): the client's watchdog fires after `ExamPageStallTimeoutMillis` with no progress, reads `ExamStallProbeScript` *before* `stopLoading()` (stopping marks hung requests finished), and counts `navigationState.consecutiveStalls`. A reload after a stall uses `LOAD_NO_CACHE`. The stall card shows `buildExamStallSummaryLines` (exam host redacted); the full line and history persist in `persistence/ExamStallLogStore` (shown in Secret Admin → Diagnostics). "Restart app" (`AppProcessRestarter` + `AppRestartActivity` in the `:restart` process) runs the exit flow, then `AppProcessRestarter.awaitUnpinned` (stopLockTask + wait for `LOCK_TASK_MODE_NONE`; restarting while pinned makes Android block the trampoline with its unpin prompt), then kills and relaunches the process; exiting with `flowUiState.unresolvedStall` set does the same. The restart skips the usual WebView wipe on exit and on the next start (`ExamStallLogStore.markKeepWebViewSession`, once, 30 min): the exam server ties a running exam to a device id in localStorage and keeps unsent answers in IndexedDB. The full restart can't run inside instrumentation (it kills the test process); debug builds have `adb shell am broadcast -n com.coblax.examlock/.DebugRestartReceiver`. Error texts the client words itself carry a marker (`(SSL: `, `(BLANK: `, `(STALL: `) that `resolveWebViewLoadErrorMessage` must list, or the student gets the generic fallback.
- The footer is Info · Reload · Exit. Info opens `ExamProctorInfoOverlay`, drawn inline over the page (not a Dialog window) so window focus never moves and the app-switch guard is not involved. While open it runs `ExamSiteInfoProbeScript` every 5 s (via `ExamRuntimeChromeActions.probeExamSite`), which reads CBX Exam System's DOM (`/ujian/<id>` top bar, `/siswa` header) because that site keeps no participant data in storage. If the site's markup changes, update the script and `ExamSiteInfoProbeTest` (androidTest).
- Server identification: the WebView User-Agent is the stock WebView UA + the Secret Admin marker (default `CBXExamLockAndroid`, see `composeExamWebViewUserAgent`). The optional `X-CBX-App-Key` header (`ExamAppKeyHeaders.kt`) goes only to the exam host, via three paths: `SecureExamWebView.loadUrl`, re-issuing GET links/redirects in `shouldOverrideUrlLoading`, and a fetch/XHR hook script (document-start where supported, else injected on page start). Plain HTML form POSTs cannot carry it. `ExamRequestIdentityTest` (androidTest) checks what a local server actually receives.
- `viewmodel/ExamRuntimeViewModel` holds only a thin UI-state/dialog layer (actions → state, one-shot effects). Most session state is Compose state in the Core/State classes.

Preparation checklist (`ui/preparation/`) and runtime monitoring share detectors in the root package (`*Hardening.kt`, `*Guard.kt`) and `runtime/` (Bluetooth, keyboard, network, overlay scanner, screen recorder, mirroring, multi-window, `SecurityDetectorCache`).

### Admin
- `ui/admin/CustomQrAdminScreen` — generate exam QRs (schedule, geofence via `ui/geofence/GeofenceAreaEditor`, per-exam bypasses, min app version).
- `SecretAdminScreen` — hidden admin settings, opened via a tap sequence + password (`AdminAuth` → native PBKDF2 verify, progressive backoff on both wall and monotonic clocks, `AdminAuthSession`). Device-level bypasses are stored as MAC'd, device-bound, counter-protected envelopes (`persistence/Bypass*`).

### Diagnostics to Telegram (`runtime/Telegram*`)
Off by default (admin toggle). Per-checklist-section reports are formatted, chunked, rate-limited, retried, and persisted in an offline queue (`TelegramMessageQueue` + `TelegramPersistentQueue`; `TelegramMessageQueueHolder.initialize` runs in `MainActivity.onCreate`). The design spec is `.kiro/specs/telegram-retry-offline-queue/`.

### Low-RAM support
`LowRamSupport.kt` / `LowRamComposition.kt` / `DeviceSurvivalPolicy.kt` define Normal/Low/Ultra profiles (thresholds in README). Profiles scale polling intervals, QR decode size, diagnostic log length and server-probe cadence. On memory-critical callbacks, non-core checks pause, but pin/overlay/app-switch/clipboard guards must keep running. WebView is created only when the session starts; Maps/Places only when the geofence editor opens.

## Conventions enforced by tests

- **i18n:** strings are inline English/Indonesian pairs: `tr("English", "Indonesia")` in Composables, `localized(lang, en, id)` elsewhere (`i18n/UiLanguageSupport.kt`). Not Android string resources. Every new user-facing string needs both.
- **Encoding:** `SourceTextEncodingTest` fails on invalid UTF-8 or mojibake in `app/src/main` (`.kt/.java/.xml/.js/.html`). Write files as UTF-8 without re-encoding existing text.
- **Detection strings:** root/hook/RE package names and markers in `config/SecurityRules.kt`, `IntegrityGuard.kt`, `ReverseEngineeringGuard.kt`, `runtime/SecurityDiagnosticsRuntime.kt` must be stored Base64+XOR-encoded (`RuntimeStringDecoder`) with a trailing comment giving the plaintext. `DetectionStringObfuscationTest` checks both that no plaintext slips in and that each comment matches its decoded value.
- Release R8 strips all `android.util.Log` calls and repackages classes. Don't rely on logs in release, and keep `@JavascriptInterface` methods (kept by rule).
- Unit tests are plain JVM (JUnit4 + `org.json`); anything needing Android/WebView/native is an androidTest. Debug-only test helpers live in `app/src/debug/`.
