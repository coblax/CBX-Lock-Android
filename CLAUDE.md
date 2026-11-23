@AGENTS.md

# Catatan khusus Claude Code

Peta proyek, arsitektur, aturan, dan langkah rilis ada di AGENTS.md (diimpor di atas). Bagian ini berisi cara kerja hemat token dan resep operasional di laptop ini.

## Hemat token

- Mulai dari peta di AGENTS.md; jangan menjelajah folder yang sudah dijelaskan di sana.
- Cari dulu (Grep), baru baca bagian yang perlu dengan offset/limit. Terutama berkas besar (1000+ baris): `ui/app/AppHostContent.kt`, `ui/geofence/GeofenceAreaEditor.kt`, `ui/admin/SecretAdminScreen.kt`, `ui/admin/AdminHomeScreen.kt`, `ui/exam/ExamRuntimeStartSessionCoordinator.kt`, `ui/exam/ExamRuntimeDiagnosticsOps.kt`, `runtime/TelegramSectionDetails.kt`, `ui/exam/ExamDiagnosticSnapshot.kt`.
- Jangan baca: `build/`, `.gradle/`, `.kotlin/`, `app/.cxx/`, `app/src/main/generated/` (profil baseline), `dist/`, `perf-artifacts/`, log lama di akar repo (`*.log`, `be*.txt`, `errors*.txt`). `KeyStore/` diblokir di `.claude/settings.json`; `local.properties` berisi rahasia: baca hanya baris yang perlu, jangan tampilkan nilainya.
- Hasil unit test: hitung dari XML `app/build/test-results/testDebugUnitTest/*.xml` (atribut `tests`/`failures`) dengan grep, jangan baca laporannya utuh.
- Build Gradle yang lama (unit test penuh ±2 menit setelah hangat, release ±18 menit) jalankan di latar belakang dan tunggu notifikasinya.
- Verifikasi seperlunya: `:app:testDebugUnitTest` untuk perubahan logika; androidTest hanya kelas yang terkait (cara cepat di Resep uji), bukan `connectedDebugAndroidTest` penuh.

## Lingkungan (Windows)

- Shell: PowerShell 5.1 dan Git Bash. Python tidak terpasang (alias Microsoft Store), jadi pakai Edit atau sed untuk perubahan berkas.
- Git ada di `C:\Program Files\Git` (dipasang lewat winget), tidak ada di PATH sesi: di PowerShell segarkan dulu `$env:Path = [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')`. Push butuh `$env:GCM_INTERACTIVE='always'; $env:GIT_TERMINAL_PROMPT='1'` (login GitHub `coblax` tersimpan di GCM). Repo: `coblax/CBX-Lock-Android`, cabang `main`. Pesan commit panjang lewat `git commit -F <berkas>` (kutipan PowerShell merusak pesan multi-baris).
- JDK: `JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'` (tidak diset global). Daemon Gradle memakai toolchain JDK 21 (unduhan otomatis ke `~/.gradle/jdks`). Setelah build release jalankan `.\gradlew.bat --stop` supaya RAM kembali.
- Android SDK: `C:\Users\ibism\AppData\Local\Android\Sdk` (build-tools terbaru untuk `apksigner`/`aapt2`, `platform-tools\adb.exe`, `emulator\emulator.exe`). `sdkmanager.bat`/`avdmanager.bat` memecah argumen di `;` bila lewat cmd: pakai `--package_file=`.
- Emulator (google_apis x86_64, bisa root, RAM 2 GB): `CBX_API24_Android7`, `CBX_API28_Android9`, `CBX_API30_Android11`, `CBX_API36_Android16`. Jalankan satu saja: `emulator.exe -avd <nama> -no-window -no-audio -no-snapshot-save -no-boot-anim` di latar, tunggu `getprop sys.boot_completed` = 1, matikan dengan `adb emu kill` sebelum build release.
- RAM laptop ±7,7 GB. WSL `Ubuntu-CBX` (server CBT develop) memakai ±4 GB saat hidup; emulator + Gradle bersamaan bisa membuat adb timeout. Jangan `wsl --shutdown` tanpa izin pemilik proyek.
- Membaca CBX Exam System dari Git Bash: `MSYS_NO_PATHCONV=1 wsl.exe -d Ubuntu-CBX --cd /var/www/CBX-Exam-System -e bash -c '…'` (tanpa `MSYS_NO_PATHCONV`, Git Bash merusak path `/var/...`; `bash -lc` mengembang `$var` terlalu awal). Hanya baca; repo itu punya AGENTS.md/CLAUDE.md sendiri.

## Resep uji

- androidTest cepat (lebih stabil daripada `connectedDebugAndroidTest`): `.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest`, lalu `adb install -r -t app/build/outputs/apk/debug/app-debug.apk`, `adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`, `adb shell am instrument -w -e class <FQCN> com.coblax.examlock.test/androidx.test.runner.AndroidJUnitRunner`.
- Emulator API 36 yang baru menyala kadang gagal instrumentasi "failed to complete startup" (ANR saat GMS memperbarui diri): tunggu semenit lalu ulangi.
- Uji mulai ulang saat tersemat (build debug): `adb shell settings put secure lock_to_app_enabled 1`, lalu `adb shell am broadcast -n com.coblax.examlock/.DebugRestartReceiver --ez pin true`. Dialog konfirmasi sematan adalah jendela SystemUI yang tidak terlihat uiautomator dan FLAG_SECURE membuat screencap hitam, jadi ketuk "GOT IT" tanpa melihat: (860,2150) di API 24, (860,2300) di API 36 (1080x2400). Cek `adb shell dumpsys activity activities | grep mLockTaskModeState` (PINNED), kirim broadcast yang sama tanpa `pin`, lalu pastikan state NONE dan PID baru (`adb shell pidof com.coblax.examlock`). `am task lock <id>` tidak menyematkan; extra `am start` tidak sampai, karena itu pakai receiver.
- Screencap: `adb exec-out screencap -p > berkas.png` dari Git Bash (redirect PowerShell merusak PNG). Layar ujian ber-FLAG_SECURE selalu hitam.
- Markup CBX Exam System untuk lembar Info: `src/app/ujian/[pesertaId]/exam-client.tsx` (bilah atas, `StatusSimpan`) dan `src/app/siswa/page.tsx` (header beranda). Ubah `ExamSiteInfoProbeTest` bila markup itu berubah.
