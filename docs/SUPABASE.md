# Menghubungkan JalanKita ke Supabase

Integrasi autentikasi, database laporan, dan penyimpanan foto sudah ada di aplikasi. Aktivasi membutuhkan proyek Supabase milik pengelola aplikasi.

1. Buat proyek Supabase pada akun Anda.
2. Jalankan [schema.sql](../supabase/schema.sql) melalui SQL Editor proyek.
3. Aktifkan autentikasi email. Jika konfirmasi email aktif, pengguna perlu mengonfirmasi alamat sebelum masuk.
4. Isi URL proyek dan kunci publik pada `local.properties`:

```properties
SUPABASE_URL=https://PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=PUBLIC_PROJECT_KEY
GOOGLE_MAPS_API_KEY=
```

Gunakan kunci publik/anon, bukan `service_role`. `local.properties` diabaikan Git dan tidak ikut dikirim ke repository. Konfigurasi dibaca saat build, sehingga APK perlu dibuat ulang setelah nilai diubah.

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Daftarkan akun melalui aplikasi setelah backend dikonfigurasi. Akun yang sebelumnya dibuat lokal tidak otomatis menjadi akun Supabase; laporan lama tetap disimpan pada perangkat.

## Pemeriksaan koneksi

- Daftar dan masuk menggunakan email Anda.
- Ambil foto kerusakan, pilih kategori, dan sertakan lokasi.
- Kirim laporan; periksa baris milik pengguna di tabel `reports` dan foto pada bucket `report-images`.
- Matikan koneksi, buat laporan baru, lalu aktifkan koneksi kembali. Laporan tertunda seharusnya tersinkron tanpa duplikasi.
- Pastikan pengguna lain tidak dapat mengubah laporan milik Anda.

Server belum dianggap aktif sebelum langkah di atas berhasil. Google Maps tidak diperlukan untuk menghubungkan Supabase.
