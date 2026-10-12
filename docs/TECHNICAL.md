# Panduan teknis JalanKita

## Arsitektur

Aplikasi Android menggunakan Kotlin, Jetpack Compose, Material 3, MVVM, dan Repository. Room menjadi sumber data tampilan; WorkManager menangani pengiriman laporan ke Supabase. CameraX mengambil foto, Coil memuat gambar, dan layanan lokasi perangkat menyediakan koordinat.

```text
Layar → ViewModel → Repository → Room
                                  ↓
                             WorkManager → Supabase
```

## Penyimpanan dan pengiriman

1. Draf memiliki UUID tetap agar percobaan ulang tidak membuat laporan baru di server.
2. Foto tersimpan di direktori privat aplikasi; laporan ditulis ke Room lebih dahulu.
3. Antrean sinkronisasi dibuat sebelum upaya kirim langsung.
4. Saat server tersedia, foto diunggah dan laporan ditulis ke Supabase.
5. Status menjadi `SYNCED` hanya setelah pengiriman dikonfirmasi server.

Tanpa konfigurasi Supabase, akun dan laporan hanya tersimpan pada perangkat. Koneksi internet saja tidak mengubah laporan menjadi terkirim. Tidak ada akun bawaan atau pengisian laporan otomatis pada instalasi baru. Data contoh untuk pengujian berada di sumber unit test.

Kirim langsung dan pekerjaan latar belakang berbagi mutex agar tidak mengunggah laporan bersamaan. Pembatalan coroutine diteruskan; laporan yang terputus saat `SYNCING` diambil kembali pada percobaan berikutnya.

| Status | Makna |
|---|---|
| `reportStatus` | Tindak lanjut: menunggu, terverifikasi, ditangani, selesai, atau butuh info |
| `syncStatus` | Pengiriman perangkat: tertunda, berlangsung, terkirim, atau gagal |

Room menggunakan migrasi eksplisit versi 1 ke 2 untuk menjaga laporan lokal. Detail tabel tersedia di `app/schemas/` pada root proyek.

## Supabase

Ikuti [panduan Supabase](SUPABASE.md). Skema database, kebijakan akses, dan penyimpanan foto tersedia di [schema.sql](../supabase/schema.sql).

## Lokasi dan peta

Google Maps bersifat opsional dan belum diaktifkan. Tanpa key, pengguna dapat memasukkan koordinat manual. GPS dapat menyediakan koordinat tanpa internet; nama jalan memerlukan reverse geocoding. Peringatan sekitar menggunakan jarak dari koordinat dan mengecualikan laporan berstatus selesai.

## Tampilan dan media

Tema terang, gelap, serta ukuran teks mengikuti pengaturan aplikasi dan perangkat. Token tampilan berada di `ui/theme/`. Foto laporan berasal dari kamera pengguna. Gambar kategori merupakan ilustrasi panduan dan menjadi cadangan ketika foto tidak tersedia atau gagal dimuat.

## Menjalankan dan memeriksa

Gunakan JDK 17 dan SDK Android sesuai `app/build.gradle.kts`.

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
```

Pengujian instrumentasi membutuhkan perangkat atau emulator. Baca [catatan verifikasi](VERIFICATION.md) untuk hasil aktual.
