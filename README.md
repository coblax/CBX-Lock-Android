# COBLAX EXAM LOCK

Panduan singkat penggunaan aplikasi dari instalasi sampai pelaksanaan ujian dan pengiriman survei diagnostik.

## Ringkasan

COBLAX EXAM LOCK adalah aplikasi Android untuk membantu membuka ujian dalam mode yang lebih aman, dengan fitur:

- scan QR ujian terenkripsi
- direct link ujian cepat
- checklist keamanan perangkat
- screen pinning / app lock flow
- pemantauan keyboard, clipboard, Bluetooth, accessibility, ADB, root, overlay, dan app switch
- fallback guard untuk app switch saat screen pinning dibypass atau tidak aktif
- hardening overlay level 2 untuk floating app
- warning non-fatal jika perangkat offline terlalu lama saat ujian
- capture identitas peserta dari storage web app CBT yang kompatibel
- kirim diagnostik per-bagian ke Telegram support

Versi produksi saat ini:

- `2.1.0 (210)`

Perubahan terbaru:

- IntegrityGuard dan reverse-engineering checks untuk deteksi tamper lebih awal
- panel `Security Health` di Secret Admin untuk status integritas dan reverse-engineering
- hardening `Clipboard`, `Overlay`, `App Switch`, `ADB`, dan `Root` dengan bypass tamper-aware
- fallback `App Switch` guard saat screen pinning tidak aktif
- detail checklist admin dan diagnostik Telegram yang lebih kaya
- participant context untuk app CBT yang menyimpan auth di storage web app
- warning khusus jika perangkat offline terlalu lama saat ujian

## 1. Instalasi APK

### Di perangkat siswa

1. Pastikan file APK sudah dikirim ke siswa.
2. Buka file APK dari File Manager atau browser.
3. Jika Android menolak pemasangan:
   Aktifkan izin `Install unknown apps` untuk aplikasi yang dipakai membuka APK.
4. Tekan `Install`.
5. Setelah selesai, buka aplikasi `COBLAX EXAM LOCK`.

### Jika instalasi gagal

Hal yang perlu dicek:

- versi Android terlalu rendah
- ruang penyimpanan tidak cukup
- ada APK versi lama dengan signature berbeda
- file APK rusak atau belum selesai terunduh
- pemasangan dari sumber tidak dikenal belum diizinkan

## 2. Persiapan Perangkat Sebelum Ujian

Sebelum ujian dimulai, pastikan:

- baterai cukup
- koneksi internet stabil
- Bluetooth mati
- accessibility service tidak aktif
- USB debugging (`ADB`) tidak aktif
- perangkat tidak terdeteksi root
- keyboard yang dipakai adalah keyboard aman / keyboard bawaan

Catatan:

- `Developer Mode` saat ini boleh aktif untuk kebutuhan pengembangan, tetapi `ADB` tetap harus mati
- beberapa keyboard OEM yang aman sudah didukung, termasuk Samsung, Xiaomi, OPPO, Realme, Vivo, Infinix, Tecno, dan Gboard

## 3. Mengenal Menu Utama

Pada halaman utama tersedia tiga jalur utama:

- `SCAN QR UJIAN`
  Untuk masuk ujian dari QR terenkripsi.
- `CUSTOM QR (ADMIN)`
  Untuk admin membuat QR ujian.
- `EXAM_SKANSATP`
  Untuk membuka portal ujian cepat melalui link langsung.

Di kanan atas tersedia pilihan bahasa:

- `EN` untuk English
- `ID` untuk Bahasa Indonesia

## 4. Cara Masuk Ujian

### Opsi A: Scan QR Ujian

1. Buka aplikasi.
2. Tekan `SCAN QR UJIAN`.
3. Arahkan kamera ke QR ujian.
4. Setelah QR terbaca, aplikasi akan masuk ke halaman persiapan.
5. Pastikan checklist sudah aman.
6. Tekan `MULAI UJIAN`.
7. Jika Android meminta `Screen Pinning`, konfirmasi pin aplikasi.
8. Setelah screen pinning aktif, ujian akan terbuka di WebView.

### Opsi B: Direct Link

1. Buka aplikasi.
2. Tekan `EXAM_SKANSATP`.
3. Masuk ke halaman persiapan.
4. Cek semua status perangkat.
5. Tekan `MULAI UJIAN`.

## 5. Memahami Checklist Otomatis

Checklist otomatis dipakai untuk memastikan perangkat aman sebelum ujian.

Bagian yang diperiksa:

- keyboard
- Bluetooth
- accessibility service
- developer mode / ADB
- root device
- overlay / floating app
- app switch
- clipboard
- screen pinning
- security health

Makna status umum:

- `Ready / Siap`
  Aman dan siap lanjut.
- `Safe / Aman`
  Tidak ada masalah terdeteksi.
- `Allowed / Diizinkan`
  Kondisi diketahui aktif tetapi sementara diizinkan.
- `Monitored / Dipantau`
  Sedang dipantau dan akan memicu alarm jika melanggar.
- `Fallback`
  Aplikasi akan memakai keyboard internal jika keyboard sistem tidak aman.
- `Danger / Bahaya`
  Ada sinyal kuat atau pelanggaran yang perlu segera ditangani.

## 6. Saat Ujian Berjalan

Di mode ujian, aplikasi menampilkan status ringkas pada bar bawah:

- status jaringan
- status baterai
- tombol refresh
- tombol menu

Indikator jaringan dapat tampil seperti:

- `ON - WiFi`
- `ON - Cellular`
- `OFFLINE`

Jika provider seluler terdeteksi, indikator akan tampil seperti:

- `ON - Cellular (Nama Provider)`

Jika terjadi pelanggaran keamanan, aplikasi dapat menampilkan dialog atau alarm, misalnya:

- keyboard tidak aman
- Bluetooth aktif saat ujian
- overlay / floating app terdeteksi
- app switch saat ujian
- accessibility aktif saat ujian
- ADB aktif saat ujian
- root indicator terdeteksi

Jika perangkat offline terlalu lama saat ujian:

- aplikasi menampilkan warning dialog non-fatal
- warning hanya muncul sekali untuk setiap episode offline
- setelah koneksi kembali normal, warning akan siap aktif lagi untuk outage berikutnya

## 7. Mengirim Survei atau Diagnostik ke Telegram

Di halaman persiapan, setiap item checklist memiliki ikon Telegram kecil di sisi kiri.

### Fungsi tombol ini

Tombol ini dipakai untuk mengirim survei teknis / diagnostik perangkat ke grup Telegram support agar tim bisa menganalisis kendala siswa.

### Cara mengirim

1. Buka halaman persiapan ujian.
2. Tekan ikon Telegram pada item yang ingin dilaporkan.
3. Konfirmasi pengiriman.
4. Tunggu proses pengiriman selesai.
5. Jika data panjang, aplikasi akan membagi laporan menjadi beberapa pesan otomatis.

### Data yang ikut terkirim

- data sesuai item yang dipilih (misalnya keyboard, Bluetooth, accessibility, ADB, root, clipboard, atau screen pinning)
- status sesi ujian
- ringkasan device dan versi aplikasi
- event log relevan pada item tersebut
- untuk app CBT yang kompatibel, data peserta yang aman seperti `display_name`, `username`, `user_id`, `role`, `kode_kelas`, dan `kode_ruang`

### Catatan izin tambahan

Pengiriman diagnostik tidak meminta izin tambahan.
Jika izin lokasi/telepon tidak diberikan, beberapa detail jaringan bisa tidak tampil.

## 8. Langkah Rekomendasi Untuk Siswa

Sebelum mulai ujian, siswa disarankan:

1. Isi baterai minimal 50%.
2. Gunakan jaringan yang stabil.
3. Tutup aplikasi lain.
4. Matikan Bluetooth.
5. Pastikan accessibility tidak aktif.
6. Pastikan USB debugging tidak aktif.
7. Gunakan keyboard bawaan perangkat jika memungkinkan.
8. Buka aplikasi dan lakukan scan QR.
9. Pastikan checklist aman.
10. Tekan `MULAI UJIAN`.

## 9. Troubleshooting Cepat

### QR tidak terbaca

- bersihkan kamera
- tambah cahaya
- pastikan QR tidak blur
- coba ulang dari jarak yang lebih pas

### Screen pinning tidak aktif

- pastikan fitur pin aplikasi aktif di pengaturan Android
- ulangi proses `MULAI UJIAN`
- jika muncul dialog Android, jangan pilih `No thanks`

### Keyboard tidak muncul

- pastikan keyboard aman / keyboard bawaan dipakai
- jika keyboard tidak aman, aplikasi akan fallback ke keyboard internal

### Ujian terasa offline

- lihat indikator jaringan di bar bawah
- lakukan refresh
- cek perpindahan antara Wi-Fi dan seluler
- jika offline terlalu lama, aplikasi akan menampilkan warning dialog
- setelah koneksi kembali normal, indikator akan kembali sinkron otomatis

### Diagnostik Telegram gagal terkirim

- cek koneksi internet
- coba kirim ulang
- jika laporan terlalu panjang, aplikasi akan memecah pesan otomatis

## 10. Build Project

Untuk compile project:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Untuk build APK debug:

```powershell
.\gradlew.bat assembleDebug
```

Untuk build APK release (unsigned):

```powershell
.\gradlew.bat assembleRelease
```

Catatan:

- `assembleDebug` dipakai untuk siklus debug harian.
- pipeline DEX hash produksi ada pada build release, sehingga validasi integritas final tetap dibawa oleh APK rilis.

## 11. Catatan Implementasi Produksi

Sebelum APK dibagikan ke siswa:

- pastikan version name dan version code sudah dinaikkan
- uji scan QR di beberapa merek HP
- uji keyboard aman di perangkat Samsung, Xiaomi, OPPO, Vivo, Realme, Infinix, Tecno, dan Gboard
- uji screen pinning di Android yang berbeda
- uji floating app / overlay di Android 12+
- uji fallback `App Switch` saat screen pinning dibypass
- uji warning offline > 30 detik saat ujian aktif
- uji kirim diagnostik Telegram
- uji mode English dan Indonesian

## 12. Kontak Developer

GitHub:

- [https://github.com/coblax](https://github.com/coblax)

