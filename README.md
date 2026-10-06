# CBX Exam Lock

CBX Exam Lock adalah aplikasi Android untuk membantu pelaksanaan ujian online dengan alur yang lebih terkunci, terpantau, dan mudah didiagnosis oleh admin.

Aplikasi ini menjaga fungsi utama ujian tetap sederhana untuk siswa: scan QR atau buka link ujian tersimpan, cek kesiapan perangkat, lalu masuk ke halaman ujian di WebView dengan mode penguncian.

## Status Versi

- App label: `CBX Lock`
- Package: `com.coblax.examlock`
- Version: `3.3.7 (379)`
- Minimum Android: API 24 (Android 7.0)
- Target Android: API 36
- Diuji di emulator Android 7.0, 9, 11, dan 16

## Fitur Utama

- Scan QR ujian terenkripsi (kamera atau dari file gambar).
- Link ujian tersimpan untuk membuka portal ujian cepat.
- Custom QR Admin untuk membuat QR ujian dengan jadwal, geofence, bypass keamanan per ujian, dan versi CBX Lock minimal.
- Checklist persiapan sebelum ujian, dicek ulang otomatis.
- Dialog Mulai ujian dengan daftar tahap yang dicentang satu per satu.
- Screen pinning / lock task flow, termasuk tombol Batal bila siswa menekan "No thanks".
- WebView ujian dengan kontrol refresh, status jaringan dan server, dan fallback keyboard internal.
- Ujian tetap terbuka selama servernya online, juga bila sertifikat HTTPS-nya bermasalah atau situsnya `http`; siswa diberi peringatan dan admin melihatnya di diagnostik.
- Deteksi dan pemantauan keyboard, clipboard, Bluetooth, accessibility, ADB, root, overlay, split screen, perekam layar, mirroring, app switch, VPN, fake location, device time, emulator, dan aplikasi kloning.
- Geofence circle/polygon untuk membatasi lokasi ujian. Tanpa geofence, aplikasi tidak meminta lokasi sama sekali.
- Alarm dan dialog pelanggaran saat sesi ujian berjalan.
- Laporan diagnostik per bagian ke Telegram support (mati secara default).
- CBX Installer: memasang CBX Lock dan melaporkan setiap kegagalan pemasangan ke admin.
- Tampilan terang/gelap, Bahasa Indonesia dan English.

## Alur Siswa

1. Buka aplikasi `CBX Lock`.
2. Pilih `Pindai QR ujian` atau `Link ujian tersimpan`.
3. Ikuti checklist persiapan dan perbaiki item yang wajib, misalnya Bluetooth, ADB, keyboard, overlay, atau accessibility.
4. Tekan `Aktifkan Screen Pinning`, lalu `Got it` / `Pin` saat Android bertanya. Jika terlanjur menekan `No thanks`, tekan `Batal` lalu ulangi.
5. Tekan `Mulai ujian`.
6. Kerjakan ujian di WebView.

## Alur Admin

1. Buka `Untuk admin · Custom QR` dari halaman utama.
2. Isi URL ujian (`https`, atau `http` untuk server sekolah sendiri), nama ujian, jadwal mulai, dan jadwal selesai.
3. Atur opsi lokasi jika diperlukan:
   - tanpa geofence
   - circle geofence
   - polygon geofence
4. Opsional:
   - bypass keamanan untuk ujian ini saja (tetap tercatat di laporan)
   - wajibkan versi CBX Lock minimal, beserta link installer terbaru
5. Generate QR.
6. Bagikan QR ke siswa atau simpan sebagai link ujian.

QR dengan bypass hanya bisa dibaca v3.2.51 ke atas, dan QR dengan versi minimal hanya bisa dibaca v3.3.4 ke atas. QR tanpa keduanya tetap terbaca oleh versi lama.

## Checklist Keamanan

CBX Exam Lock memeriksa beberapa sinyal perangkat sebelum dan selama ujian:

- koneksi jaringan, VPN, dan jam perangkat
- keyboard aktif
- Bluetooth
- accessibility service
- USB debugging / ADB
- root indicator dan SELinux
- overlay / floating window dan split screen
- perekam layar dan mirroring
- app switch
- clipboard
- screen pinning
- lokasi dan fake location (hanya jika ujian memakai geofence)
- emulator dan aplikasi kloning
- integritas APK dan sinyal reverse-engineering
- versi CBX Lock (jika QR mewajibkan versi minimal)

Item dikelompokkan menjadi `Perbaiki dulu` dan `Saran (opsional)`. Item yang tidak berlaku untuk ujian tersebut, misalnya Lokasi tanpa geofence, tertulis `Tak perlu`.

## Halaman Ujian dan Koneksi

- Sertifikat server bermasalah (kedaluwarsa, self-signed, domain tidak cocok) atau situs `http`: halaman ujian tetap dibuka. Di bawah header muncul peringatan "Koneksi situs ujian tidak aman", status server menjadi `online, tidak aman`, dan diagnostik mencatat penyebabnya. Koneksi seperti ini bisa disadap di jaringan yang sama, jadi perbarui sertifikat server secepatnya.
- Sertifikat rusak milik situs pihak ketiga (CDN, analytics) tetap ditolak.
- Halaman yang terbuka tetapi kosong karena file skrip/tampilan gagal dimuat, atau karena Android System WebView terlalu lama, menampilkan pesan dan tombol `Muat ulang`.
- Pemuatan pertama selalu mengambil halaman terbaru dari server (cache lama hanya dipakai saat jaringan tidak stabil).

## Diagnostik Telegram

Diagnostik Telegram mati secara default. Setelah admin menyalakannya dari pengaturan admin, halaman persiapan menampilkan tombol untuk mengirim laporan teknis per item checklist ke Telegram support.

Laporan dapat berisi:

- status item checklist
- ringkasan perangkat
- versi aplikasi
- status sesi ujian
- event log terkait
- informasi peserta yang aman jika app CBT menyediakannya di storage WebView

Data panjang akan dipotong otomatis menjadi beberapa pesan.

## Perangkat Low-RAM

Target perangkat low-end tetap didukung tanpa menghapus fungsi utama:

- profile Low aktif untuk total RAM <= 2 GB, memoryClass <= 128 MB, atau Android low-RAM
- profile Ultra aktif untuk total RAM <= 1 GB, memoryClass <= 96 MB, available RAM <= 512 MB, memory pressure dari Android, atau Android 7.x dengan RAM <= 2 GB (di Android 7 browser ujian berbagi memori dengan aplikasi)
- user bisa memilih Auto/Normal/Low/Ultra dari `Pengaturan` (ikon gear) → `Performa` → `Ubah`
- profile Low: QR decode 1024px, polling 2x lebih jarang, log diagnostik 16 event, cek server tiap 60 detik
- profile Ultra: QR decode 720px, polling 6x lebih jarang, log diagnostik 8 event, cek server tiap 120 detik
- HP dengan memori sangat sedikit membuka beranda versi ringan (tanpa Compose) lebih dulu
- saat Android melapor memori kritis, cek server, cek ulang jaringan, dan pindai aplikasi perekam layar berhenti 90 detik; penjagaan inti (pin, overlay, app switch, clipboard) tetap jalan
- Baseline Profile: kode persiapan, Mulai, dan sesi ujian sudah dikompilasi saat instal
- WebView dibuat saat sesi ujian dimulai; MapView/Places hanya saat editor lokasi dibuka
- cache dan komponen tidak aktif dibersihkan saat Android mengirim memory pressure

## Build Project

Compile Kotlin debug:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Build APK debug:

```powershell
.\gradlew.bat :app:assembleDebug
```

Build APK release dan CBX Installer (installer ikut membangun APK release):

```powershell
.\gradlew.bat :installer:assembleRelease
```

Release build membutuhkan konfigurasi signing di `local.properties` atau environment variable yang sesuai. APK release ditandatangani v1+v2+v3.

Hasil yang dibagikan disimpan di `dist/` (tidak di-commit):

- `CBX-Lock-Exam-v<versi>-release.apk` + `.idsig`
- `CBX-Lock-Installer-v<versi>.apk`
- `INSTALL_NOTES.txt` (ukuran, SHA-256, dan daftar perubahan)

Versi diatur di `app/build.gradle.kts` (`versionCode`, `versionName`).

### Baseline Profile

Profil ada di `app/src/main/generated/baselineProfiles/` dan perlu dibuat ulang setelah perubahan besar pada kode ujian. Jalankan di emulator API 33+ atau emulator API 28+ yang sudah `adb root`, tanpa APK release CBX Lock terpasang (tanda tangannya berbeda):

```powershell
.\gradlew.bat :app:generateBaselineProfile -Pandroid.testInstrumentationRunnerArguments.cbxAdminPassword=<password admin>
```

Password admin hanya diberikan lewat argumen perintah, jangan disimpan di kode. Tanpa password, hanya alur beranda dan persiapan yang diprofilkan.

## CBX Installer

Modul `:installer` membawa APK CBX Lock release, memeriksa HP lebih dulu (versi Android, CPU, memori, keutuhan file, CBX lama yang bentrok, kebijakan perangkat), lalu memasang lewat PackageInstaller. Setiap kegagalan ditampilkan ke siswa dan dilaporkan ke Telegram admin. Laporan hanya dikirim dari build release; build debug hanya mencatatnya.

## File Lokal Yang Tidak Boleh Di-commit

Jangan commit file sensitif atau hasil build:

- `local.properties`
- `KeyStore/`
- `dist/`
- `*.apk`
- `*.aab`
- `.gradle/`
- `.kotlin/`
- `.idea/`
- `build/`
- `app/build/`
- `app/release/`

File tersebut sudah dimasukkan ke `.gitignore`.

## Smoke Test Rekomendasi

Sebelum APK dibagikan:

- install fresh APK di beberapa merek HP, atau lewat CBX Installer
- scan QR kamera dan scan dari file
- buka link ujian tersimpan
- uji screen pinning di beberapa versi Android, termasuk menekan `No thanks` lalu `Batal`
- uji keyboard bawaan dan Gboard
- uji Bluetooth aktif/nonaktif
- uji ADB aktif/nonaktif
- uji overlay/floating app
- uji root/fake-location signal jika tersedia
- uji geofence circle dan polygon, serta ujian tanpa geofence
- uji offline lebih dari 30 detik saat ujian
- uji memori kritis: `adb shell am send-trim-memory com.coblax.examlock RUNNING_CRITICAL`
- uji Bahasa Indonesia dan English

Tes instrumentasi `CrossVersionPlatformProbeTest` membaca sinyal yang berbeda antar versi Android (SELinux, sertifikat server ujian, aplikasi kloning) dan bisa dijalankan di tiap emulator.

## Kontak

- GitHub: [https://github.com/coblax](https://github.com/coblax)
