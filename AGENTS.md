# CBX Exam Lock: panduan agen

Browser ujian terkunci (lockdown) untuk Android, `com.coblax.examlock`, label "CBX Lock", SMKN 1 Tanjungpandan. Siswa memindai QR ujian terenkripsi (atau membuka link ujian tersimpan), lolos daftar periksa kesiapan HP, menyematkan layar (screen pinning), lalu mengerjakan ujian di WebView terkunci sementara aplikasi terus mengawasi tanda curang (overlay, pindah aplikasi, clipboard, Bluetooth, ADB/root, rekam/mirroring layar, VPN, lokasi palsu, jam HP, dll.). Admin membuat QR di aplikasi yang sama.

- Percakapan dengan pemilik proyek: bahasa Indonesia. Kode, komentar, dan pesan commit: bahasa Inggris (lihat riwayat git). Teks UI dwibahasa (lihat Aturan penting).
- `README.md` (bahasa Indonesia) = spesifikasi produk: alur siswa/admin, perilaku daftar periksa, aturan coba-ulang jaringan, profil RAM rendah, daftar uji asap. Baca sebelum mengubah perilaku yang dilihat pengguna, dan perbarui bila perilaku berubah.
- Situs yang dibuka CBX Lock adalah CBT milik pemilik proyek sendiri, **CBX Exam System** (Next.js, repo terpisah `coblax/CBX-Exam-System`, di WSL `Ubuntu-CBX` `/var/www/CBX-Exam-System`, produksi `https://exam.smkn1tanjungpandan.sch.id`). Proyek lain: jangan diubah kecuali diminta. Server produksinya berisi data siswa ASLI.

## Cara kerja

- Pedoman Karpathy terpasang sebagai skill `karpathy-guidelines` (`.claude/skills/karpathy-guidelines/SKILL.md`), berlaku untuk semua agen:
  - **Pikir dulu**: sebutkan asumsi, tanyakan yang ambigu, tawarkan cara yang lebih sederhana bila ada.
  - **Sederhana**: kode minimum untuk masalahnya, tanpa fitur/abstraksi/konfigurasi yang tidak diminta.
  - **Perubahan bedah**: sentuh hanya yang perlu, ikuti gaya kode yang ada, jangan merapikan kode di sekitarnya; kode mati yang bukan buatanmu cukup disebutkan.
  - **Terarah ke tujuan**: ubah tugas menjadi kriteria yang bisa diuji (test yang gagal dulu, lalu lolos), verifikasi sampai terbukti.
  - Aturan proyek di berkas ini dan keputusan pemilik proyek selalu didahulukan.
- Verifikasi sebelum menyatakan selesai: minimal `:app:testDebugUnitTest`; perubahan WebView/UI/native juga androidTest yang terkait di emulator (lihat Perintah). Laporkan apa adanya, termasuk yang belum diuji.
- Git: commit langsung ke `main` dan push hanya bila diminta pemilik proyek. Jangan commit `local.properties`, `KeyStore/`, `dist/`, atau APK (sudah di `.gitignore`).
- Rilis (naik versi + build) hanya bila diminta. Langkahnya di bagian Rilis.
- Konfirmasi dulu sebelum memasang alat/SDK baru atau mematikan layanan (mis. `wsl --shutdown`: distro `Ubuntu-CBX` menjalankan server CBT).
- Jangan menulis password admin, keystore, atau token Telegram di kode, dokumen, maupun memori.

## Stack

- Kotlin + Jetpack Compose (Material 3), ditambah pustaka JNI C++17 (`app/src/main/cpp`, lib `examlock_native`).
- minSdk 24, target/compile 36, Java 11. Versi ada di `app/build.gradle.kts` (`versionCode`, `versionName`); modul installer membacanya dari sana.
- Uji: JUnit4 + `org.json` (JVM), androidx.test + Compose UI test (androidTest).

## Modul

- `:app`: aplikasi ujian.
- `:installer`: "CBX Installer" (`com.coblax.examlock.installer`, sengaja minSdk 21). Task `bundleCbxApk` bergantung pada `:app:assembleRelease`, menyalin `app-release.apk` ke assets beserta `cbx.properties` (ukuran, SHA-256, versionCode, ABI), lalu memasangnya lewat PackageInstaller setelah memeriksa HP. Kegagalan dilaporkan ke Telegram hanya dari build release.
- `:baselineprofile`: generator Baseline Profile (`BaselineProfileGenerator.kt`); hasilnya digabung ke `app/src/main/generated/baselineProfiles/`.

## Perintah

Windows / PowerShell. `git` tidak ada di PATH (lihat `CLAUDE.md` untuk lokasinya).

```powershell
.\gradlew.bat :app:compileDebugKotlin          # cek kompilasi cepat
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest            # unit test JVM
.\gradlew.bat :app:testDebugUnitTest --tests "com.coblax.examlock.ExamQrCodecTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.coblax.examlock.ExamQrCodecTest.someMethod"
.\gradlew.bat :app:connectedDebugAndroidTest    # instrumentasi, butuh HP/emulator
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.coblax.examlock.ui.exam.ExamWebViewNetworkTest"
.\gradlew.bat :app:lintDebug                    # konfigurasi: app/lint.xml
.\gradlew.bat :installer:testDebugUnitTest
.\gradlew.bat :installer:assembleRelease        # build release aplikasi + installer
```

- Task `assemble/bundle/package*Release` langsung gagal bila tanda tangan release belum diatur (`release.keystore.path`, `release.keystore.password`, `release.key.alias`, `release.key.password` di `local.properties`, atau env `CBX_RELEASE_KEYSTORE*`).
- Kunci `local.properties` lain yang dibaca Gradle: `telegram.bot.token`, `telegram.bug.chat.id`, `telegram.release.diagnostics.enabled`, `signing.sha256.release`, `signing.sha256.debug`, `maps.api.key`. Semuanya disamarkan XOR(115)+Base64 ke `BuildConfig.*_OBF`. Kredensial Telegram selalu masuk debug dan `lowRamQa`, masuk release hanya bila diagnostik release diaktifkan.
- Build type: `debug`, `release` (R8 + resource shrink, aset hash dex), `lowRamQa` (mirip release, profileable, tanda tangan debug, diagnostik aktif).
- Build release/lowRamQa membuat `assets/dex.sha256` dari keluaran R8 (`GenerateDexHashAssetTask`); `IntegrityGuard` membandingkannya saat aplikasi berjalan.
- Baseline profile: `.\gradlew.bat :app:generateBaselineProfile -Pandroid.testInstrumentationRunnerArguments.cbxAdminPassword=<pw>` di emulator API 33+ (atau API 28+ yang di-root) tanpa APK release terpasang. Password admin jangan pernah ditulis di kode.
- Skrip QA RAM rendah: `tools/android/lowram-qa.ps1` (build/pasang/jalankan alur, tulis `dist/lowram-runs/*/summary.json`) dan `tools/android/lowram-gate.ps1` (menilai summary). `lowram-qa.ps1` masih memakai `-Package` lama `com.example.coblaxexamlock`; berikan `-Package com.coblax.examlock`.
- Daftar periksa manual: `docs/CRITICAL_SMOKE_TEST_CHECKLIST.md` (setelah refaktor besar), `docs/NDK_PARITY_ROLLOUT_CHECKLIST.md` (perubahan jalur native).

## Arsitektur

### Masuk dan navigasi
`MainActivity` adalah satu-satunya activity. Ia menentukan `LowRamProfile` dulu; di HP dengan memori sangat kecil ia menampilkan beranda berbasis View (tanpa Compose) dan baru memulai Compose saat pengguna bertindak. Akar Compose: `ui/app/AppContent` → `AppHostContent`, yang memilih layar menurut `model.AppScreen` (`Home`, `CustomQrAdmin`, `ExamWebView`, `SecretAdmin`). Tidak ada pustaka navigasi.

Manifest mendeklarasikan `configChanges` untuk uiMode/keyboard/ukuran layar supaya activity **tidak** dibuat ulang di tengah ujian (pembuatan ulang melepas sematan layar dan mengakhiri sesi). Jangan hapus dan jangan bergantung pada pembuatan ulang.

### Format QR ujian (`ExamQrCodec.kt`)
`CBXEL2:` + base64url(IV‖ciphertext AES-GCM) dari daftar field yang dipisah `|`. Kunci AES diturunkan (SHA-256) dari seed yang tertanam di Kotlin dan `cpp/qr_crypto.cpp`; keduanya harus identik. Versi payload bersifat tambahan demi kompatibilitas: v7 = dasar, v8 hanya bila ada bypass keamanan, v9 hanya bila ada versi aplikasi minimal. Selalu keluarkan versi terendah yang dibutuhkan QR supaya aplikasi lama tetap bisa membacanya. `CBXEL1:` ditolak.

### Pola jembatan native (`nativebridge/`)
Operasi yang sensitif keamanan (kripto QR, verifikasi PBKDF2 rahasia admin, pengikatan perangkat/MAC bypass, geometri geofence, pindai /proc, TracerPid, hash dex, dekode string runtime, snapshot clipboard) ada **dua kali**: di C++ dan sebagai fallback Kotlin. `NativeSecurityBridge.invokeOrFallback` menjalankan native lalu jatuh ke Kotlin bila gagal dimuat, melempar exception, atau hasilnya tidak bisa dipakai. `NativeBridgeTestControl` (hanya debug) bisa memaksa salah satu backend; androidTest `Native*ParityTest` memastikan keduanya memberi hasil sama.

Saat mengubah salah satu algoritma ini, ubah kedua sisi lalu jalankan parity test. JNI memakai `RegisterNatives` di `JNI_OnLoad` terhadap `NativeBridgeBindings` (+ `ClipboardNormalizedItemInput`, `NativeClipboardSnapshotCore`); semuanya `-keep` di `proguard-rules.pro`. Mengganti nama kelas atau anggotanya membuat native rusak diam-diam di release.

### Runtime ujian (`ui/exam/`, bagian terbesar kode)
Sesi ujian adalah satu layar Compose besar yang dipecah ke banyak berkas supaya tetap di bawah ukuran yang murah dikompilasi ART:
- `ExamRuntimeSessionImpl`: composable sesi; membangun objek ops.
- `ExamRuntimeSessionCore`: satu objek berisi semua state/handler bersama, dioper ke bagian-bagian alih-alih menangkap puluhan variabel lokal. Dibangun ulang tiap komposisi.
- Kelas `*Ops` (`ExamRuntimeDiagnosticsOps`, `ExamRuntimeMonitoringOps`, `ExamRuntimeSecurityOps`) = perilaku; berkas `*State` = penampung state; berkas `*Effects` = monitor `LaunchedEffect`/`DisposableEffect` (sematan, overlay, clipboard, jaringan, tamper, lokasi).
- Alur mulai: `ExamRuntimeStartSessionCoordinator` (prapemeriksaan: jaringan dengan pemulihan, jam HP, lokasi/geofence, pemblokir kesehatan) → `ExamRuntimeStartExam*Controller`.
- WebView: `ExamWebViewClient`, `ExamWebViewInfrastructure`, `ExamWebViewNavigationState`, `ExamRuntimeServerProbe`, `ExamRuntimeNetworkPoller`. Aturan coba-ulang/backoff/sertifikat tidak aman ada di README bagian "Halaman Ujian dan Koneksi". Jangan pernah mengulang POST otomatis, dan jangan pernah menampilkan host ujian ke siswa.
- JS yang disuntikkan ke halaman ujian (keyboard bawaan, hook fullscreen) ada di `config/ExamScripts.kt`.
- Halaman macet (`ExamStallRecovery.kt`): watchdog client menyala setelah `ExamPageStallTimeoutMillis` tanpa kemajuan, membaca `ExamStallProbeScript` *sebelum* `stopLoading()` (berhenti memuat menandai request yang menggantung sebagai selesai), dan menghitung `navigationState.consecutiveStalls`. Muat ulang setelah macet memakai `LOAD_NO_CACHE`. Kartu macet menampilkan `buildExamStallSummaryLines` (host ujian disamarkan); baris lengkap dan riwayatnya disimpan permanen di `persistence/ExamStallLogStore` (tampil di Secret Admin → Diagnostik).
- Mulai ulang aplikasi (`AppProcessRestarter` + `AppRestartActivity` di proses `:restart`): jalankan alur keluar, lalu `AppProcessRestarter.awaitUnpinned` (stopLockTask + tunggu `LOCK_TASK_MODE_NONE`; mulai ulang saat masih tersemat membuat Android menahan trampoline dengan prompt lepas-sematan), lalu matikan dan buka lagi prosesnya. Keluar saat `flowUiState.unresolvedStall` aktif melakukan hal yang sama. Mulai ulang ini melewati pembersihan WebView biasa saat keluar dan saat mulai berikutnya (`ExamStallLogStore.markKeepWebViewSession`, sekali, 30 menit): server ujian mengikat ujian yang berjalan ke ID perangkat di localStorage dan menyimpan jawaban yang belum terkirim di IndexedDB. Mulai ulang penuh tidak bisa dijalankan di dalam instrumentasi (proses test ikut mati); build debug punya `adb shell am broadcast -n com.coblax.examlock/.DebugRestartReceiver` (`--ez pin true` untuk menyematkan dulu).
- Teks error yang disusun client sendiri memakai penanda (`(SSL: `, `(BLANK: `, `(STALL: `) yang wajib terdaftar di `resolveWebViewLoadErrorMessage`, kalau tidak siswa mendapat pesan umum.
- Footer: Info · Muat ulang · Keluar. Info membuka `ExamProctorInfoOverlay`, digambar langsung di atas halaman (bukan jendela Dialog) supaya fokus jendela tidak pindah dan penjaga pindah-aplikasi tidak terlibat. Selama terbuka, ia menjalankan `ExamSiteInfoProbeScript` tiap 5 detik (lewat `ExamRuntimeChromeActions.probeExamSite`) untuk membaca DOM CBX Exam System (bilah atas `/ujian/<id>`, header `/siswa`), karena situs itu tidak menyimpan data peserta di storage. Bila markup situs berubah, perbarui skrip itu dan `ExamSiteInfoProbeTest` (androidTest).
- Identitas ke server: User-Agent WebView = UA WebView bawaan + penanda dari Secret Admin (bawaan `CBXExamLockAndroid`, lihat `composeExamWebViewUserAgent`). Header opsional `X-CBX-App-Key` (`ExamAppKeyHeaders.kt`, nilai bawaan tersamar di `SecureStrings.OBF_DEFAULT_EXAM_APP_KEY`; jangan ditulis polos di dokumen) hanya dikirim ke host ujian lewat tiga jalur: `SecureExamWebView.loadUrl`, menerbitkan ulang link/redirect GET di `shouldOverrideUrlLoading`, dan skrip hook fetch/XHR (document-start bila didukung, selain itu disuntikkan saat halaman mulai). POST form HTML biasa tidak bisa membawanya. `ExamRequestIdentityTest` (androidTest) memeriksa apa yang benar-benar diterima server lokal.
- Link ujian tersimpan bawaan: `https://exam.smkn1tanjungpandan.sch.id` (`SecureStrings.OBF_FAST_EXAM_URL`); link bawaan lama dimigrasikan lewat `resolveSavedFastExamUrl`.
- `viewmodel/ExamRuntimeViewModel` hanya lapisan tipis UI-state/dialog (aksi → state, efek sekali jalan). Sebagian besar state sesi adalah state Compose di kelas Core/State.

Daftar periksa persiapan (`ui/preparation/`) dan pemantauan runtime berbagi detektor di paket akar (`*Hardening.kt`, `*Guard.kt`) dan `runtime/` (Bluetooth, keyboard, jaringan, pemindai overlay, perekam layar, mirroring, multi-window, `SecurityDetectorCache`).

### Admin
- `ui/admin/CustomQrAdminScreen`: membuat QR ujian (jadwal, geofence lewat `ui/geofence/GeofenceAreaEditor`, bypass per ujian, versi aplikasi minimal).
- `SecretAdminScreen`: setelan admin tersembunyi, dibuka lewat urutan ketukan + password (`AdminAuth` → verifikasi PBKDF2 native, jeda bertingkat di jam dinding dan jam monotonik, `AdminAuthSession`). Bypass tingkat perangkat disimpan sebagai amplop ber-MAC, terikat perangkat, dan dilindungi penghitung (`persistence/Bypass*`).

### Diagnostik ke Telegram (`runtime/Telegram*`)
Mati secara bawaan (toggle admin). Laporan per bagian daftar periksa diformat, dipecah, dibatasi laju, dicoba ulang, dan disimpan di antrean offline (`TelegramMessageQueue` + `TelegramPersistentQueue`; `TelegramMessageQueueHolder.initialize` berjalan di `MainActivity.onCreate`). Spesifikasi desain: `.kiro/specs/telegram-retry-offline-queue/`.

### Dukungan RAM rendah
`LowRamSupport.kt` / `LowRamComposition.kt` / `DeviceSurvivalPolicy.kt` mendefinisikan profil Normal/Low/Ultra (ambangnya di README). Profil mengatur interval polling, ukuran dekode QR, panjang log diagnostik, dan jeda probe server. Saat callback memori kritis, pemeriksaan non-inti dijeda, tetapi penjaga sematan/overlay/pindah-aplikasi/clipboard harus tetap berjalan. WebView baru dibuat saat sesi dimulai; Maps/Places baru saat editor geofence dibuka.

## Aturan penting (dijaga test)

- **i18n**: teks berupa pasangan Inggris/Indonesia langsung di kode: `tr("English", "Indonesia")` di Composable, `localized(lang, en, id)` di luar itu (`i18n/UiLanguageSupport.kt`). Bukan string resource Android. Setiap teks baru untuk pengguna wajib punya keduanya.
- **Encoding**: `SourceTextEncodingTest` gagal bila ada UTF-8 tidak valid atau mojibake di `app/src/main` (`.kt/.java/.xml/.js/.html`). Tulis berkas sebagai UTF-8 tanpa meng-encode ulang teks yang ada. Di dalam JS yang disuntikkan, karakter non-ASCII ditulis sebagai escape `\u`.
- **String deteksi**: nama paket dan penanda root/hook/RE di `config/SecurityRules.kt`, `IntegrityGuard.kt`, `ReverseEngineeringGuard.kt`, `runtime/SecurityDiagnosticsRuntime.kt` wajib disimpan ter-encode Base64+XOR (`RuntimeStringDecoder`) dengan komentar di akhir baris berisi teks aslinya. `DetectionStringObfuscationTest` memeriksa bahwa tidak ada teks asli yang lolos dan bahwa setiap komentar cocok dengan hasil dekodenya.
- R8 release membuang semua panggilan `android.util.Log` dan memindah paket kelas. Jangan bergantung pada log di release, dan pertahankan method `@JavascriptInterface` (dijaga aturan keep).
- Unit test adalah JVM murni (JUnit4 + `org.json`); apa pun yang butuh Android/WebView/native menjadi androidTest. Helper test khusus debug ada di `app/src/debug/`.
- Parameter baru di fungsi yang dipanggil test dengan argumen bernama (mis. `buildExamRuntimeChromeActionsForSession`) diberi nilai bawaan supaya test lama tetap terkompilasi.

## Rilis

Hanya bila diminta pemilik proyek.
1. Naikkan `versionCode` + `versionName` di `app/build.gradle.kts` dan baris `Version:` di `README.md`.
2. Unit test lolos, lalu `.\gradlew.bat :installer:assembleRelease` (±18 menit).
3. Verifikasi:
   - `apksigner verify --print-certs` sertifikat SHA-256 `e1fd5d35…e116e7` (kunci release yang dipakai sejak awal; v1+v2+v3, berkas `META-INF/CERT.*` ada).
   - `aapt2 dump badging` menunjukkan versi baru.
   - `DebugRestartReceiver` tidak ada di manifest release.
   - `assets/cbx.properties` di installer: SHA-256 sama dengan APK release.
4. Salin ke `dist/`: `CBX-Lock-Exam-v<versi>-release.apk` (+ `.idsig`) dan `CBX-Lock-Installer-v<versi>.apk`.
5. Perbarui `dist/INSTALL_NOTES.txt`: judul, ukuran byte, SHA-256 APK dan installer, serta bagian "Perubahan di v<versi>" dalam bahasa siswa/guru, bukan bahasa teknis.
