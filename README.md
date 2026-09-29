# JalanKita

> Laporkan kerusakan jalan dengan mudah, cepat, dan tepat.

Aplikasi Android native untuk melaporkan kerusakan jalan secara gotong royong.
Dibangun **offline-first**: laporan selalu tersimpan di perangkat lebih dulu, lalu
dikirim ke server sendiri saat koneksi tersedia.

---

## Identitas visual

Logo JalanKita memadukan jalan berbentuk **J**, marka jalan, dan satu titik
lokasi. Logo yang sama digunakan pada Beranda, Login, loading, dan ikon
launcher adaptif (termasuk ikon monokrom).

- [Logo SVG](docs/brand/jalankita-logo.svg)
- [Simbol transparan](docs/brand/jalankita-mark.svg)
- [Logo horizontal](docs/brand/jalankita-wordmark.svg)
- [Panduan aset](docs/brand/README.md)

Beranda menempatkan tombol **Lapor Kerusakan** dengan kontras tinggi, akses
langsung ke peta dan panduan, serta ringkasan kontribusi yang membuka Laporan
Saya. Peringatan kerusakan terdekat tetap mendapat prioritas di atas beranda.
Tombol mengikuti tinggi teks agar label tidak terpotong saat font diperbesar.

## 1. Ringkasan teknis

| | |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arsitektur | MVVM + Repository, offline-first |
| Database lokal | Room (sumber kebenaran untuk UI) |
| Sinkronisasi | WorkManager (`NetworkType.CONNECTED`, exponential backoff) |
| Backend | Supabase — Auth, PostgreSQL, Storage |
| Peta | Google Maps SDK + `maps-compose` |
| Kamera | CameraX |
| Lokasi | `FusedLocationProviderClient` + `Geocoder` |
| Gambar | Coil 3 |
| minSdk / targetSdk / compileSdk | 27 / 36 / 37 |
| AGP / Gradle / Kotlin | 9.1.1 / 9.3.1 / 2.3.20 |

> **Catatan AGP 9:** sejak AGP 9.0 dukungan Kotlin sudah *built-in*. Plugin
> `org.jetbrains.kotlin.android` **tidak boleh** di-apply (build akan gagal).
> Versi Kotlin ditentukan oleh KGP di classpath (`alias(...) apply false` di
> `build.gradle.kts` root) dan diaktifkan lewat `android.builtInKotlin=true`.

---

## 2. Arsitektur

```
Composable  ──►  ViewModel  ──►  Repository  ──┬──►  Room (AppDatabase)      ← sumber kebenaran UI
                                               └──►  Supabase (Postgrest/Storage)

Room  ──►  WorkManager (SyncReportsWorker)  ──►  Supabase
```

Aturan yang dipegang:

* **Composable** tidak pernah menyentuh Room atau jaringan langsung.
* **ViewModel** hanya mengelola state layar.
* **Repository** mengoordinasi lokal + remote dan memetakan model.
* **DAO** hanya query Room. **Worker** hanya sinkronisasi latar belakang.
* UI membaca `Flow` dari Room, jadi tidak ada tombol "refresh" manual.

### Struktur paket

```
org.umn.jalankita
├── data
│   ├── demo       DemoCredentials, SampleContent, SampleDataSeeder, DemoSyncEngine
│   ├── local      AppDatabase (+migrasi), ReportDao/Entity, NotificationDao,
│   │              ReporterDao/Entity, SessionStore
│   ├── location   LocationProvider (GPS, stream posisi, reverse geocoding),
│   │              NearbyAlertMonitor
│   ├── media      PhotoStorage (kompresi + penyimpanan file)
│   ├── remote     SupabaseClientProvider, SupabaseRepository, RemoteModels
│   ├── repository AuthRepository, SupabaseAuthRepository, DemoAuthRepository,
│   │              ReportRepository, ReportSyncTrigger
│   └── worker     ReportSyncEngine, SyncReportsWorker, WorkManagerSyncTrigger
├── di             AppContainer (DI manual), appViewModel()
├── model          Report, ReportStatus, SyncStatus, DamageCategory, DamageSeverity,
│                  ReporterProfile, ReporterLevel, UserProfile, AuthState, ThemeMode
├── ui
│   ├── components AppButton, AppCard, AppTextField, AppTopBar, StatusBadge,
│   │              ReportCard, DamageIllustration, NearbyAlertCard, TrustMeter,
│   │              BrandLoader, AuroraBackground, EmptyState/ErrorState,
│   │              OfflineBanner, BottomNavigationBar, LoadingView, peta, BrandLogo
│   ├── navigation AppNavigation, Routes
│   ├── permissions PermissionUtils
│   ├── screens    Splash/Loading, Login, ForgotPassword, Register, Home,
│   │              DamageGuide, NewReport, Camera, ConfirmReport, ReportSuccess,
│   │              LocationPicker, Map, ReportHistory, ReportDetail,
│   │              ReporterProfile, Notification, Profile, Settings
│   └── theme      Color, Theme, Type, Shape, Dimens (Spacing/Dimens)
├── util           NetworkMonitor/ConnectivityMonitor, Validators, Formatters, Geo,
│                  ErrorMessages, NotificationPresenter, StatusNotifier
├── viewmodel      Auth, Home, Report, History, ReportDetail, Reporter,
│                  Notification, Profile, Settings
├── JalanKitaApp
└── MainActivity
```

### Design system

Semua layar mengambil token dari satu tempat, sehingga tidak ada dua halaman
yang menciptakan gayanya sendiri:

| Token | Berkas | Isi |
|---|---|---|
| Warna | `ui/theme/Color.kt` | Palet merek, netral, semantik status, palet ilustrasi |
| Tipografi | `ui/theme/Type.kt` | Skala Material lengkap (display → label) |
| Bentuk | `ui/theme/Shape.kt` | Tangga radius + `PillShape` |
| Jarak | `ui/theme/Dimens.kt` | `Spacing` (4–48dp) dan `Dimens` (tinggi tombol, ikon, dll.) |

Komponen bersama (`PrimaryButton`, `SecondaryButton`, `TertiaryButton`,
`AppCard`, `SectionCard`, `AppTextField`, `AppTopBar`, `EmptyState`,
`ErrorState`) dipakai di seluruh layar — layar tidak lagi memanggil
`Button`/`OutlinedTextField`/`TopAppBar` Material secara langsung.

Tema mengikuti sistem secara bawaan dan dapat dipaksa Terang/Gelap dari
**Profil → Pengaturan**. Komponen membaca `LocalIsDarkTheme`, bukan
`isSystemInDarkTheme()`, supaya warna badge ikut tema aplikasi.

Menunggu pun punya identitas sendiri: `RoadLoader` menampilkan logo jalan berbentuk huruf J
dengan cincin progres yang bergerak, dipakai di layar peluncuran
(`LoadingScreen`) maupun di `LoadingView` dalam aplikasi. Latarnya
`AuroraBackground` — gradien merek yang bergerak perlahan, juga dipakai sebagai
kepala halaman Login.

---

## 3. Dua status yang tidak boleh tertukar

| | `reportStatus` | `syncStatus` |
|---|---|---|
| Menjawab | Apa yang sedang dilakukan instansi | Apakah laporan sudah sampai ke server |
| Nilai | `PENDING`, `VERIFIED`, `IN_PROGRESS`, `COMPLETED`, `NEEDS_INFO` | `PENDING_SYNC`, `SYNCING`, `SYNCED`, `FAILED` |
| Disimpan di server | Ya | **Tidak** — murni urusan perangkat |

Contoh: `reportStatus = PENDING` + `syncStatus = SYNCED` berarti laporan sudah
terkirim dan sedang menunggu verifikasi. **Offline bukan `FAILED`** — offline
adalah `PENDING_SYNC`.

### Skema Room

Versi 2. Migrasi `1 → 2` ditulis eksplisit (menambah `reports.reporterName` dan
tabel `reporters`), **bukan** `fallbackToDestructiveMigration`: database ini
satu-satunya salinan laporan yang belum sampai ke server, jadi pembaruan versi
tidak boleh menghapusnya.

---

## 4. Cara kerja offline-first

Saat pengguna menekan **Kirim**:

1. `ReportViewModel` sudah memegang UUID yang dibuat di awal draf.
2. `ReportRepository.submitReport()` **menulis ke Room lebih dulu**, dengan
   `reportStatus = PENDING`, `syncStatus = PENDING_SYNC`.
3. UI langsung menampilkan laporan (Room mengalirkan `Flow`).
4. Pekerjaan sinkronisasi di-antrekan ke WorkManager.
5. Jika perangkat online, satu upaya kirim langsung dijalankan.
6. Jika offline, laporan tetap `PENDING_SYNC`. Tidak ada yang hilang.

Ketika koneksi kembali, `SyncReportsWorker` berjalan (constraint
`NetworkType.CONNECTED`) dan untuk setiap laporan tertunda:

`PENDING_SYNC → SYNCING → unggah foto → upsert baris → simpan URL foto → SYNCED`

Kegagalan **tidak pernah** menghapus laporan atau foto lokal: `syncAttempts`
bertambah, `lastSyncError` dicatat, dan WorkManager mencoba lagi dengan
exponential backoff.

### Pencegahan duplikat

* `reports.id` adalah UUID yang dibuat di perangkat dan menjadi **primary key**
  di PostgreSQL.
* UUID itu dipakai ulang pada setiap percobaan — tidak pernah dibuat ulang.
* Penulisan memakai `upsert(onConflict = "id", ignoreDuplicates = true)`, jadi
  percobaan ulang untuk laporan yang sudah tersimpan adalah *no-op* — tidak
  menambah baris dan tidak menimpa status yang sudah diubah instansi.
* Unggahan foto memakai path tetap `report-images/{userId}/{reportId}.jpg`
  dengan `upsert = true`.

### Lokasi saat offline

Koordinat GPS tetap didapat tanpa internet. Yang butuh jaringan hanyalah
*reverse geocoding* (nama jalan). Karena itu nama jalan bersifat opsional: bila
tidak tersedia, UI menampilkan *"Lokasi tersimpan berdasarkan koordinat GPS."*
dan `SyncReportsWorker.resolveMissingStreetNames()` mengisinya belakangan saat
online.

---

## 5. Contoh kerusakan jalan

Aplikasi ini sulit dipahami dari formulir kosong, jadi setiap kategori kerusakan
punya contoh visual dan penjelasan yang muncul di beberapa tempat sekaligus.

### Ilustrasi

`ui/components/DamageIllustration.kt` **menggambar** setiap jenis kerusakan di
atas Canvas — bukan raster, sehingga tajam di segala ukuran dan tidak menambah
besar APK.

Disusun seperti foto jalan, bukan seperti ikon: gradien jarak (permukaan jauh
memantulkan lebih banyak cahaya langit), butiran agregat tiga lapis yang makin
kasar ke arah bawah bingkai, jalur roda yang lebih gelap karena terpoles
kendaraan, marka berperspektif yang aus tidak merata, satu sumber cahaya dari
kiri atas, bayangan jatuh untuk apa pun yang punya kedalaman, dan vignette di
sudut. Semua itu dipakai bersama, jadi yang berbeda antar kategori hanya
kerusakannya:

| Kategori | Yang digambar |
|---|---|
| Jalan Berlubang | Tepi runtuh memperlihatkan lapisan pondasi yang lebih pucat, dinding gelap bergradasi, bayangan jatuh, serpihan aspal berbayang di sekitarnya |
| Retak Jalan | Retakan bercabang dengan bibir yang tertimpa cahaya di satu sisi, ditambah gompal di bagian terlebar |
| Aspal Rusak | Lapisan terkelupas memperlihatkan pondasi, tebal lapisannya terlihat dari bayangan tepi, batu split berserakan |
| Jalan Tidak Rata | Cekungan yang dibentuk bayangan (bukan garis), marka ikut melengkung, retak kontur di bibir amblasan |
| Bahu Jalan Rusak | Tepi aspal menggantung dengan bayangan keras di atas tanah, marka tepi ikut terbawa |
| Genangan Air | Pantulan langit di tepi jauh, air gelap di tepi dekat, riak, dan lubang yang tersembunyi di bawahnya |
| Lainnya | Kerucut lalu lintas dengan pita reflektif dan bayangan |

Jumlah tekstur menyesuaikan ukuran: pada petak 64dp detail penuh hanya menjadi
bubur, jadi kerapatan butiran diskalakan dari `size.minDimension`. Seluruh
derau berasal dari hash deterministik, bukan `Random`, sehingga satu petak
tampil identik pada setiap rekomposisi.

Ilustrasi ini juga menjadi gambar cadangan pada `ReportImage`, sehingga laporan
tanpa foto tetap terbaca sekilas alih-alih menampilkan ikon "gambar rusak".

### Teks yang menyertainya

`model/DamageCategory.kt` memuat seluruh salinan untuk tiap kategori —
`description`, `example` (kasus nyata), `photoTip`, dan `severity` — sehingga
kategori yang sama dijelaskan dengan kalimat yang sama di mana pun ia muncul.

### Di mana contoh itu tampil

| Tempat | Bentuk |
|---|---|
| **Beranda** → *Contoh Kerusakan Jalan* | Carousel empat kasus utama |
| **Panduan Kerusakan** (`DamageGuideScreen`) | Tiga langkah melapor + kartu penuh tiap kategori: ilustrasi, dampak, contoh kasus, tips foto |
| **Lapor Kerusakan** → pemilih kategori | Petak berilustrasi, bukan dropdown |
| **Kamera** | Tips foto kategori terpilih tampil di atas jendela bidik |
| **Laporan Saya / Peta / Detail** | Ilustrasi kategori sebagai gambar laporan |

Panduan dapat dibuka dari Beranda, dari ikon **?** di layar laporan, dan dari
**Profil → Panduan Kerusakan**.

### Laporan contoh

Pada masuk pertama (Mode Demo), `data/demo/SampleContent.kt` menulis lima
laporan di Gading Serpong, Tangerang, masing-masing berada di titik alur yang
berbeda — sehingga Beranda, Peta, Laporan Saya dan Notifikasi punya isi nyata:

| Kategori | Lokasi | Status laporan | Status sinkronisasi |
|---|---|---|---|
| Jalan Berlubang | Jl. Boulevard Gading Serpong | Sedang Ditangani | Tersinkron |
| Genangan Air | Jl. Raya Serpong | Terverifikasi | Tersinkron |
| Aspal Rusak | Jl. Scientia Boulevard | Selesai | Tersinkron |
| Jalan Tidak Rata | Jl. Kelapa Dua Raya | Menunggu Verifikasi | Menunggu Sinkronisasi |
| Retak Jalan | Jl. Raya Legok | Butuh Info Tambahan | Tersinkron |

Id-nya diturunkan dari `(userId, slug)` lewat hash, bukan acak — jadi menekan
**Muat Ulang Contoh** di Pengaturan menyegarkan tanggalnya tanpa pernah
menggandakan laporan. Laporan buatan pengguna tidak tersentuh.

---

## 6. Peringatan sekitar & kredibilitas pelapor

Dua fitur yang membuat laporan warga berguna bagi warga lain: tahu ada kerusakan
di jalur yang sedang dilalui, dan tahu siapa yang melaporkannya.

### 6.1 Peringatan jalan rusak di sekitar

`NearbyAlertMonitor` menggabungkan posisi perangkat dengan seluruh laporan yang
ada di perangkat, lalu mengurutkannya berdasarkan jarak.

| | |
|---|---|
| Radius "di sekitar Anda" | 2 km |
| Radius peringatan | 300 m |
| Sumber posisi | `LocationProvider.locationUpdates()` — `PRIORITY_BALANCED_POWER_ACCURACY`, minimal 20 m perpindahan |
| Jarak | Haversine (`util/Geo.kt`) |

Aturan yang dipegang:

* **Tidak ada pelacakan latar belakang.** Aliran lokasi hanya hidup selama ada
  layar yang mengamatinya (`SharingStarted.WhileSubscribed`). Tidak ada service,
  tidak ada geofence, tidak ada yang berjalan saat layar mati.
* **Kerusakan yang sudah diperbaiki tidak pernah diperingatkan.** Laporan
  berstatus `COMPLETED` dikecualikan — memperingatkan lubang yang sudah ditambal
  mengajari orang mengabaikan peringatan.
* **Satu notifikasi per titik per 30 menit**, dan hanya untuk yang terdekat.
  Rentetan notifikasi saat melintasi ruas rusak lebih buruk daripada tidak ada.
* **Izin lokasi diminta di kartunya sendiri**, bukan saat aplikasi dibuka.

Kartu di Beranda punya empat keadaan yang berbeda dan jujur: belum diizinkan,
sedang mencari posisi, ada peringatan (merah, berdenyut, bisa ditekan), ada
kerusakan dalam 2 km, atau tidak ada sama sekali. Notifikasi sistem memakai
kanal terpisah (`Peringatan Jalan Rusak`, `IMPORTANCE_HIGH`) agar tidak
tercampur dengan konfirmasi sinkronisasi.

### 6.2 Rekam jejak pelapor

Setiap laporan menampilkan siapa pelapornya, dan baris itu bisa ditekan menuju
`ReporterProfileScreen`. Yang dinilai hanya sinyal yang benar-benar dihasilkan
alur kerja — tidak ada status "ditolak" di `ReportStatus`:

| Istilah | Artinya |
|---|---|
| Dikonfirmasi | `VERIFIED`, `IN_PROGRESS` atau `COMPLETED` — petugas memastikan kerusakannya nyata |
| Butuh Info | `NEEDS_INFO` — laporan dikembalikan karena belum bisa ditindaklanjuti |
| Menunggu | `PENDING` — **tidak dihitung**; laporan yang belum ditinjau bukan bukti apa pun |

```
akurasi    = dikonfirmasi / (dikonfirmasi + butuh info)
skor (0–100) = akurasi × 70  +  30 × min(ditinjau, 15) / 15
```

Bobot volume ada supaya satu laporan yang kebetulan benar tidak menghasilkan
skor sempurna; bobotnya berhenti di 15 laporan supaya skor tidak berubah menjadi
papan peringkat. Skor **tidak ditampilkan sama sekali** sebelum ada 3 laporan
yang ditinjau — layar mengatakan apa adanya bahwa belum ada dasar penilaian.

| Level | Syarat |
|---|---|
| Pelapor Baru | < 3 laporan ditinjau |
| Pelapor Aktif | skor < 70 |
| Pelapor Tepercaya | skor ≥ 70 |
| Pelapor Terverifikasi | skor ≥ 90 **dan** ≥ 10 laporan ditinjau |

Rekam jejak pengguna sendiri dihitung ulang dari baris Room miliknya
(`ReporterDao.refreshFromReports`) setiap kali ia mengirim laporan atau status
berubah, jadi angkanya tidak bisa melenceng. Untuk pelapor lain, angkanya datang
dari server (di Mode Demo: dari komunitas contoh) — karena riwayat seseorang
hampir selalu lebih panjang daripada yang tersimpan di perangkat ini.

Profil sendiri dapat dibuka lewat **Profil → Rekam Jejak Pelapor**, dengan
keterangan bahwa begitulah laporannya terlihat oleh warga lain.

---

## 7. Kredensial yang perlu Anda sediakan

Semua rahasia dibaca dari `local.properties` (sudah di-*gitignore*) atau dari
environment variable — tidak ada yang di-*hardcode* di kode Kotlin.

```properties
# local.properties
sdk.dir=/Users/you/Library/Android/sdk
SUPABASE_URL=https://xxxxxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOi...
GOOGLE_MAPS_API_KEY=AIza...
```

Lihat `local.properties.example`. Gunakan **anon key**, jangan pernah
`service_role` di aplikasi Android — kunci itu menembus Row Level Security.

### Perilaku bila kredensial belum ada

Aplikasi tetap terpasang dan berjalan, dengan penurunan fungsi yang jujur:

| Yang hilang | Yang terjadi |
|---|---|
| `SUPABASE_URL` / `SUPABASE_ANON_KEY` | **Mode Demo**: akun & laporan hanya di perangkat (Room + DataStore, password disimpan sebagai SHA-256 bersalt). Spanduk "Mode Demo" tampil di Login, Profil dan Pengaturan. `DemoSyncEngine` menggantikan Supabase sehingga siklus `PENDING_SYNC → SYNCING → SYNCED` tetap berjalan — tidak ada laporan yang menggantung selamanya. |
| `GOOGLE_MAPS_API_KEY` | Semua permukaan peta menampilkan placeholder; pemilihan lokasi manual jatuh ke input koordinat. Fitur lain normal. |

Mode Demo **bukan** pengganti backend — ia hanya supaya aplikasi bisa dijalankan
dan didemokan tanpa kredensial. Begitu Supabase dikonfigurasi,
`SupabaseAuthRepository` dan `ReportSyncEngine` yang dipakai.

#### Akun demo bawaan

| | |
|---|---|
| Nama | **Vassel** |
| Email | **vassel@gmail.com** |
| Password | **vassel123** |

Akun ini dibuat otomatis pada peluncuran pertama dan ditampilkan di layar Login
lengkap dengan tombol **Gunakan Akun Demo** yang mengisi formulir. Pada masuk
pertama, lima laporan contoh beserta notifikasinya di-*seed* ke Room (lihat
bagian berikutnya). *Seeding* hanya terjadi di Mode Demo — dengan Supabase aktif,
isi aplikasi adalah data asli pengguna.

---

## 8. Setup

### 8.1 Supabase

1. Buat project di <https://supabase.com>.
2. Buka **SQL Editor**, tempel isi `supabase/schema.sql`, jalankan.
   Skrip ini membuat tabel `profiles`, `reports`, `notifications`, indeks,
   trigger, seluruh policy RLS, serta bucket Storage `report-images`.
3. **Authentication → Providers → Email**: aktifkan.
   Untuk demo cepat, matikan *Confirm email* supaya pendaftaran langsung
   membuka sesi. Bila dibiarkan aktif, aplikasi akan menampilkan pesan agar
   pengguna mengonfirmasi email lebih dulu.
4. **Project Settings → API**: salin *Project URL* dan *anon public key* ke
   `local.properties`.

### 8.2 Google Maps

1. Google Cloud Console → aktifkan **Maps SDK for Android**.
2. Buat API key, batasi ke package `org.umn.jalankita` + SHA-1 debug Anda:
   ```bash
   ./gradlew signingReport
   ```
3. Salin ke `GOOGLE_MAPS_API_KEY` di `local.properties`.
   Kunci disuntikkan ke manifest lewat `manifestPlaceholders`.

### 8.3 Menjalankan

```bash
./gradlew assembleDebug          # build APK debug
./gradlew installDebug           # pasang ke perangkat/emulator
./gradlew testDebugUnitTest      # unit test JVM
./gradlew connectedDebugAndroidTest   # test Room (butuh device/emulator)
```

Atau buka folder ini di Android Studio / IntelliJ IDEA lalu **Run**.

---

## 9. Izin

Diminta **pada saat fitur dipakai**, bukan saat aplikasi dibuka.

| Izin | Kapan diminta |
|---|---|
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Saat layar **Lapor Kerusakan Jalan** dibuka |
| `CAMERA` | Saat layar kamera dibuka |
| `POST_NOTIFICATIONS` (Android 13+) | Setelah laporan pertama berhasil disimpan |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Normal, tidak perlu persetujuan |

Penolakan izin tidak pernah membuat aplikasi *crash*: selalu ada penjelasan,
tombol coba lagi, dan jalan ke Pengaturan. Bila GPS gagal, tersedia
**"Pilih Lokasi di Peta"**.

---

## 10. Skenario demo

### 10.1 Masuk pertama kali

Splash → Login. Di Mode Demo, tekan **Gunakan Akun Demo** (mengisi
`vassel@gmail.com` / `vassel123`) → **Masuk**. Beranda menyapa *"Selamat …,
Vassel"*, lima laporan contoh sudah ada, dan carousel **Contoh Kerusakan Jalan**
menjelaskan apa yang layak dilaporkan. Centang **Ingat Saya** menentukan apakah
sesi bertahan setelah aplikasi ditutup; alamat email diingat, password tidak
pernah disimpan.

### 10.2 Online

Beranda → **Lapor Kerusakan** → izinkan lokasi → koordinat terdeteksi →
**Ketuk untuk mengambil foto** → pilih kategori (petak berilustrasi) → isi
deskripsi → **Lanjutkan** → Konfirmasi → **Kirim Laporan** → tersimpan di Room →
tersinkron → status **Tersinkron** → muncul di **Laporan Saya** dan di **Peta**.

### 10.3 Offline (yang utama)

1. Matikan internet (mode pesawat).
2. Beranda → **Lapor Kerusakan** → GPS tetap mendapat koordinat.
3. Ambil foto → pilih kategori → **Kirim**.
4. Laporan tersimpan, berstatus **Menunggu Sinkronisasi**.
   Layar sukses berbunyi *"Laporan Berhasil Disimpan"* — bukan gagal.
5. **Tutup aplikasi sepenuhnya, buka lagi** → laporan masih ada, foto masih ada.
6. Nyalakan internet.
7. Sinkronisasi mendeteksi koneksi (WorkManager, atau `DemoSyncEngine` di Mode
   Demo) → foto diunggah → baris di-upsert → `syncStatus` menjadi
   **Tersinkron**, UI berubah sendiri tanpa refresh.
8. Tidak ada laporan ganda.

Tombol **Kirim** berubah menjadi **Simpan Laporan** saat offline, dan spanduk
offline muncul di Beranda, Laporan Saya, formulir dan konfirmasi — supaya jelas
bahwa menunggu adalah keadaan normal, bukan kegagalan.

### 10.4 Peringatan di sekitar

1. Setel lokasi perangkat di sekitar Gading Serpong (di emulator:
   `adb emu geo fix 106.6180 -6.2568`).
2. Buka **Beranda** → izinkan lokasi pada kartu peringatan.
3. Kartu merah muncul: *"Hati-hati · 130 m dari Anda — Jalan Berlubang"*,
   beserta daftar kerusakan lain dalam radius 300 m, dan notifikasi sistem pada
   kanal **Peringatan Jalan Rusak**.
4. Buka tab **Peta**: tanpa Maps key, daftar kerusakan komunitas ditampilkan dan
   diurutkan dari yang paling dekat.

### 10.5 Rekam jejak pelapor

Buka salah satu laporan komunitas → baris **Dilaporkan oleh** → profil pelapor.
Empat pelapor contoh sengaja mewakili seluruh rentang kredibilitas: Rina Kusuma
(Terverifikasi), Andi Prasetyo (Tepercaya), Dewi Lestari (Aktif) dan Bagas
Nugroho (Baru — ditampilkan **tanpa** skor).

### 10.6 Tampilan

**Profil → Pengaturan → Tampilan** memaksa tema Terang atau Gelap. Seluruh
layar, badge status dan ilustrasi mengikuti tema yang dipilih, bukan tema
sistem.

---

## 11. Pengujian

`./gradlew testDebugUnitTest` — 55 tes JVM:

* `ValidatorsTest` — validasi email, password, nama, konfirmasi.
* `ReportMappingTest` — pemetaan Room ↔ domain ↔ Supabase, timestamp rusak,
  kategori tak dikenal, prioritas foto lokal vs remote.
* `StatusSemanticsTest` — pemisahan `reportStatus`/`syncStatus`, warna badge.
* `OfflineFirstReportTest` — inti spesifikasi: tulis Room lebih dulu, offline
  tetap `PENDING_SYNC` (bukan `FAILED`), upaya kirim langsung saat online,
  submit ulang tidak menggandakan, laporan `FAILED` ikut diambil untuk dicoba lagi.
* `DemoSyncEngineTest` — Mode Demo memegang aturan yang sama: offline tetap
  `PENDING_SYNC`, online menghasilkan `SYNCED` + notifikasi, sinkron ulang
  adalah *no-op*, dan status dari instansi tidak tertimpa.
* `SampleContentTest` — id contoh bersifat deterministik dan berbeda antar
  pengguna, setiap kategori utama punya contoh, *seeding* ulang tidak
  menggandakan, dan notifikasi tidak menunjuk laporan yang tidak ada.
* `PresentationLogicTest` — hitungan statistik, filter Laporan Saya, waktu
  relatif ("3 jam lalu"), pengukur kekuatan password, dan kelengkapan salinan
  panduan tiap kategori.
* `ProximityAlertTest` — jarak haversine, pengurutan dan pemotongan radius,
  kerusakan yang sudah diperbaiki tidak pernah diperingatkan, batas 300 m, dan
  penulisan jarak ("<10 m", "1,4 km").
* `ReporterCredibilityTest` — skor menolak muncul sebelum ada rekam jejak, satu
  laporan beruntung tidak menjadikan seseorang terverifikasi, laporan yang masih
  menunggu tidak dihitung, dan volume saja tidak bisa menutupi akurasi rendah.

`./gradlew connectedDebugAndroidTest` — `ReportDaoTest` menjalankan Room
sungguhan: query reaktif, seleksi `getPendingSyncReports()`, `markSynced`
membersihkan error, dan perubahan status dari server tidak mengganggu
pembukuan sinkronisasi lokal. **Butuh perangkat atau emulator.**

---

## 12. Yang masih perlu Anda lakukan

1. Isi `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_MAPS_API_KEY` di
   `local.properties`.
2. Jalankan `supabase/schema.sql` di project Supabase Anda.
3. Jalankan `./gradlew connectedDebugAndroidTest` pada perangkat/emulator untuk
   memverifikasi lapisan Room.
4. Untuk rilis: siapkan keystore, aktifkan `isMinifyEnabled`, dan tambahkan
   aturan ProGuard untuk Ktor/kotlinx-serialization bila diperlukan.
