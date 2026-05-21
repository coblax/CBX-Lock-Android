# CBX Exam Lock

CBX Exam Lock adalah aplikasi Android untuk membantu pelaksanaan ujian online dengan alur yang lebih terkunci, terpantau, dan mudah didiagnosis oleh admin.

Aplikasi ini menjaga fungsi utama ujian tetap sederhana untuk siswa: scan QR atau buka direct link, cek kesiapan perangkat, lalu masuk ke halaman ujian di WebView dengan mode penguncian.

## Status Versi

- App label: `CBX Lock`
- Package: `com.example.coblaxexamlock`
- Version: `3.2.10 (330)`
- Minimum Android: API 24
- Target Android: API 36

## Fitur Utama

- Scan QR ujian terenkripsi.
- Direct Link untuk membuka portal ujian cepat.
- Custom QR Admin untuk membuat QR ujian dengan jadwal dan opsi geofence.
- Checklist keamanan sebelum ujian.
- Screen pinning / lock task flow.
- WebView ujian dengan kontrol refresh, status jaringan, dan fallback keyboard internal.
- Deteksi dan pemantauan keyboard, clipboard, Bluetooth, accessibility, ADB, root, overlay, app switch, fake location, dan device time.
- Geofence circle/polygon untuk membatasi lokasi ujian.
- Alarm dan dialog pelanggaran saat sesi ujian berjalan.
- Laporan diagnostik per bagian ke Telegram support.
- Dukungan mode Bahasa Indonesia dan English.

## Alur Siswa

1. Buka aplikasi `CBX Lock`.
2. Pilih `SCAN QR UJIAN` atau Direct Link.
3. Ikuti checklist perangkat.
4. Perbaiki item yang belum aman, misalnya Bluetooth, ADB, keyboard, overlay, atau accessibility.
5. Tekan `MULAI UJIAN`.
6. Konfirmasi screen pinning jika Android meminta.
7. Kerjakan ujian di WebView.

## Alur Admin

1. Buka `CUSTOM QR (ADMIN)` dari halaman utama.
2. Isi URL ujian, nama ujian, jadwal mulai, dan jadwal selesai.
3. Atur opsi lokasi jika diperlukan:
   - tanpa geofence
   - circle geofence
   - polygon geofence
4. Generate QR.
5. Bagikan QR ke siswa atau simpan sebagai Direct Link.

## Checklist Keamanan

CBX Exam Lock memeriksa beberapa sinyal perangkat sebelum dan selama ujian:

- koneksi jaringan
- keyboard aktif
- Bluetooth
- accessibility service
- USB debugging / ADB
- root indicator
- overlay / floating window
- app switch
- clipboard
- screen pinning
- fake location
- device time
- integrity dan reverse-engineering signal

Status seperti `Ready`, `Safe`, `Allowed`, `Monitored`, `Fallback`, dan `Danger` membantu admin atau siswa memahami tindakan yang perlu dilakukan.

## Diagnostik Telegram

Pada halaman persiapan, admin atau siswa dapat mengirim laporan teknis dari item checklist tertentu ke Telegram support.

Laporan dapat berisi:

- status item checklist
- ringkasan perangkat
- versi aplikasi
- status sesi ujian
- event log terkait
- informasi peserta yang aman jika app CBT menyediakannya di storage WebView

Data panjang akan dipotong otomatis menjadi beberapa pesan.

## Perangkat Low-RAM

Target perangkat low-end tetap didukung tanpa menghapus fungsi utama. Untuk perangkat RAM kecil, aplikasi mengutamakan:

- profile Low aktif untuk total RAM <= 2 GB, memoryClass <= 128 MB, atau Android low-RAM
- profile Ultra aktif untuk total RAM <= 1 GB, memoryClass <= 96 MB, available RAM <= 512 MB, atau Android memory pressure
- user bisa memilih Auto/Normal/Low/Ultra dari ikon gear di sebelah badge profil pada home
- profile Low memakai QR decode 1024px, polling 2x, log diagnostik 16 event, dan refresh cooldown 800ms
- profile Ultra memakai QR decode 720px, polling 4x, log diagnostik 12 event, dan refresh cooldown 1200ms
- load fitur berat hanya saat dibutuhkan
- WebView dibuat saat sesi ujian dimulai
- MapView/Places dibuat hanya saat editor lokasi dibuka
- bitmap QR dan image decode dibuat lebih hemat memori
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

Build APK release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Release build membutuhkan konfigurasi signing di `local.properties` atau environment variable yang sesuai.

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

- install fresh APK di beberapa merek HP
- scan QR kamera dan scan dari file
- buka Direct Link
- uji screen pinning di beberapa versi Android
- uji keyboard bawaan dan Gboard
- uji Bluetooth aktif/nonaktif
- uji ADB aktif/nonaktif
- uji overlay/floating app
- uji root/fake-location signal jika tersedia
- uji geofence circle dan polygon
- uji offline lebih dari 30 detik saat ujian
- uji kirim diagnostik Telegram
- uji Bahasa Indonesia dan English

## Kontak

- GitHub: [https://github.com/coblax](https://github.com/coblax)
