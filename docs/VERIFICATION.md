# Verifikasi JalanKita

## Perubahan saat ini

- README menampilkan deskripsi singkat, screenshot aplikasi, dan ilustrasi jalan rusak.
- Gambar kategori jalan berlubang digunakan pada panduan dan pemilih kategori.
- Instalasi baru tidak membuat akun bawaan atau laporan otomatis. Data contoh hanya tersedia sebagai fixture unit test.
- Pengiriman menggunakan Supabase. Tanpa server, laporan tetap tersimpan lokal dan tidak diklaim terkirim.
- Pemuat gambar mendukung URL dan memiliki ilustrasi cadangan.

## Batas pemeriksaan

- Backend belum terhubung ke proyek Supabase; pengiriman server belum diuji langsung.
- Google Maps belum diaktifkan.
- Screenshot beranda yang tersedia berasal dari pemeriksaan visual sebelumnya. Pengambilan baru belum berhasil: emulator Pixel 6 kehilangan layanan Package Manager (`Can't find service: package`) saat pemasangan APK, sebelum aplikasi dapat dibuka. Ini bukan hasil uji UI yang lulus.
- Pemeriksaan lint sebelumnya dihentikan karena analisis Kotlin/UAST berlangsung lama; tidak dianggap lulus.

## Hasil pembaruan

- `testDebugUnitTest assembleDebug --offline --max-workers=2` dengan JDK 17 berhasil.
- 52 unit test lulus, tanpa kegagalan, error, atau tes yang dilewati.
- Tes penyimpanan lokal memastikan koneksi internet tanpa server tidak menghasilkan status terkirim.
- Pencarian istilah yang dihapus pada sumber aplikasi dan dokumentasi bersih.
- Seluruh tautan dokumentasi lokal valid; `git diff --check` bersih.
