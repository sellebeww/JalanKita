package org.umn.jalankita.fixtures

import org.umn.jalankita.data.local.NotificationEntity
import org.umn.jalankita.data.local.ReportEntity
import org.umn.jalankita.data.local.ReporterEntity
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus
import java.util.concurrent.TimeUnit
import kotlin.math.cos

/**
 * The worked examples a new account starts with.
 *
 * Two sets, because the app tells two stories. **Mine** shows the reporting
 * workflow end to end — one report at each stage. **The community's** gives the
 * map something to be a map of, the proximity alerts something to warn about,
 * and the reporter cards a range of track records to show, from a newcomer to a
 * verified regular.
 *
 * Ids are derived from (user, slug), so re-seeding refreshes the samples and can
 * never duplicate them.
 */
object SampleContent {

    /** Gading Serpong, Tangerang — the area the app was built around. */
    const val AREA_LATITUDE = -6.2568
    const val AREA_LONGITUDE = 106.6180

    private const val METERS_PER_DEGREE_LATITUDE = 111_320.0

    private data class Sample(
        val slug: String,
        val category: DamageCategory,
        val streetName: String,
        val description: String,
        /** Metres north (+) / south (−) of the area centre. */
        val north: Double,
        /** Metres east (+) / west (−) of the area centre. */
        val east: Double,
        val reportStatus: ReportStatus,
        val syncStatus: SyncStatus,
        val ageHours: Long,
        /** Null for the signed-in user's own reports. */
        val reporter: CommunityReporter? = null,
    )

    /**
     * Neighbours whose reports fill the map. Their counts are the whole point of
     * the reporter card: the same pothole means something different coming from
     * a verified regular than from a first-time reporter.
     */
    enum class CommunityReporter(
        val slug: String,
        val displayName: String,
        val memberSinceDays: Long,
        val totalReports: Int,
        val confirmedReports: Int,
        val completedReports: Int,
        val needsInfoReports: Int,
    ) {
        /** Long record, almost everything confirmed → "Pelapor Terverifikasi". */
        RINA("rina", "Rina Kusuma", 420, 24, 19, 12, 1),

        /** Solid but not spotless → "Pelapor Tepercaya". */
        ANDI("andi", "Andi Prasetyo", 260, 11, 7, 3, 2),

        /** Reports a lot, often sent back → "Pelapor Aktif". */
        DEWI("dewi", "Dewi Lestari", 95, 9, 3, 1, 3),

        /** Barely any history → "Pelapor Baru", shown without a score. */
        BAGAS("bagas", "Bagas Nugroho", 6, 2, 1, 0, 0),
    }

    private val mySamples = listOf(
        Sample(
            slug = "pothole",
            category = DamageCategory.POTHOLE,
            streetName = "Jl. Boulevard Gading Serpong",
            description = "Lubang selebar ± 60 cm dan sedalam ± 15 cm di lajur kiri, " +
                "tepat sebelum lampu merah. Sudah dua motor terjatuh di titik ini.",
            north = 1500.0,
            east = 1050.0,
            reportStatus = ReportStatus.IN_PROGRESS,
            syncStatus = SyncStatus.SYNCED,
            ageHours = 26,
        ),
        Sample(
            slug = "flooding",
            category = DamageCategory.FLOODING,
            streetName = "Jl. Raya Serpong",
            description = "Genangan setinggi ± 20 cm menutup satu lajur sejak pagi. " +
                "Di bawah air ada lubang yang tidak terlihat pengendara.",
            north = -1500.0,
            east = 2780.0,
            reportStatus = ReportStatus.VERIFIED,
            syncStatus = SyncStatus.SYNCED,
            ageHours = 9,
        ),
        Sample(
            slug = "asphalt",
            category = DamageCategory.DAMAGED_ASPHALT,
            streetName = "Jl. Scientia Boulevard",
            description = "Aspal terkelupas sepanjang ± 12 meter setelah dilalui truk " +
                "proyek. Kerikil lepas berhamburan ke bahu jalan.",
            north = 650.0,
            east = -200.0,
            reportStatus = ReportStatus.COMPLETED,
            syncStatus = SyncStatus.SYNCED,
            ageHours = 22 * 24,
        ),
        Sample(
            slug = "subsidence",
            category = DamageCategory.SUBSIDENCE,
            streetName = "Jl. Kelapa Dua Raya",
            description = "Badan jalan amblas ± 15 cm di atas bekas galian pipa air. " +
                "Permukaan bergelombang dan sulit terlihat pada malam hari.",
            north = 2070.0,
            east = -1400.0,
            reportStatus = ReportStatus.PENDING,
            syncStatus = SyncStatus.PENDING_SYNC,
            ageHours = 3,
        ),
        Sample(
            slug = "crack",
            category = DamageCategory.CRACK,
            streetName = "Jl. Raya Legok",
            description = "Retak menyerupai kulit buaya di tengah lajur, mulai " +
                "melebar setelah hujan beberapa hari terakhir.",
            north = 3040.0,
            east = -4200.0,
            reportStatus = ReportStatus.NEEDS_INFO,
            syncStatus = SyncStatus.SYNCED,
            ageHours = 6 * 24,
        ),
    )

    /**
     * Distances from the area centre are chosen so the alerts can be seen
     * working: two inside the 300 m warning radius, more inside the 2 km
     * "sekitar Anda" list, one outside it, and one already repaired — which must
     * never be warned about.
     */
    private val communitySamples = listOf(
        Sample(
            slug = "community-pothole-near",
            category = DamageCategory.POTHOLE,
            streetName = "Jl. Scientia Boulevard",
            description = "Lubang menganga di mulut putaran balik, tepat di jalur " +
                "motor. Sudah ditandai ranting oleh warga.",
            north = 120.0,
            east = 60.0,
            reportStatus = ReportStatus.VERIFIED,
            syncStatus = SyncStatus.SYNCED,
            ageHours = 5,
            reporter = CommunityReporter.RINA,
        ),
        Sample(
            slug = "community-flooding-near",
            category = DamageCategory.FLOODING,
            streetName = "Jl. Ki Hajar Dewantara",
            description = "Air menggenang selebar satu lajur setiap hujan deras, " +
                "menutupi lubang di bawahnya.",
            north = -180.0,
            east = 150.0,
            reportStatus = ReportStatus.PENDING,
            syncStatus = SyncStatus.SYNCED,
            ageHours = 14,
            reporter = CommunityReporter.ANDI,
        ),
        Sample(
            slug = "community-asphalt",
            category = DamageCategory.DAMAGED_ASPHALT,
            streetName = "Jl. Gading Serpong Boulevard",
            description = "Permukaan aspal mengelupas sepanjang ± 20 meter di depan " +
                "pintu masuk perumahan.",
            north = 700.0,
            east = 420.0,
            reportStatus = ReportStatus.IN_PROGRESS,
            syncStatus = SyncStatus.SYNCED,
            ageHours = 3 * 24,
            reporter = CommunityReporter.DEWI,
        ),
        Sample(
            slug = "community-subsidence",
            category = DamageCategory.SUBSIDENCE,
            streetName = "Jl. Kelapa Dua Raya",
            description = "Jalan bergelombang sepanjang ± 30 meter setelah proyek " +
                "saluran air, terasa jelas saat dilewati.",
            north = 1350.0,
            east = -620.0,
            reportStatus = ReportStatus.PENDING,
            syncStatus = SyncStatus.SYNCED,
            ageHours = 30,
            reporter = CommunityReporter.BAGAS,
        ),
        Sample(
            slug = "community-shoulder",
            category = DamageCategory.DAMAGED_SHOULDER,
            streetName = "Jl. Raya Kelapa Dua",
            description = "Bahu jalan longsor ± 50 cm setelah hujan; tepi aspal " +
                "menggantung dan rawan bagi pesepeda.",
            north = -2400.0,
            east = 1100.0,
            reportStatus = ReportStatus.VERIFIED,
            syncStatus = SyncStatus.SYNCED,
            ageHours = 2 * 24,
            reporter = CommunityReporter.RINA,
        ),
        Sample(
            // Already repaired: appears on the map, never in an alert.
            slug = "community-fixed",
            category = DamageCategory.CRACK,
            streetName = "Jl. Boulevard Diponegoro",
            description = "Retak memanjang yang sudah ditambal petugas minggu lalu.",
            north = 210.0,
            east = -140.0,
            reportStatus = ReportStatus.COMPLETED,
            syncStatus = SyncStatus.SYNCED,
            ageHours = 8 * 24,
            reporter = CommunityReporter.ANDI,
        ),
    )

    fun reportsFor(
        userId: String,
        reporterName: String,
        now: Long = System.currentTimeMillis(),
    ): List<ReportEntity> = (mySamples + communitySamples).map { sample ->
        val createdAt = now - TimeUnit.HOURS.toMillis(sample.ageHours)
        val owner = sample.reporter?.let { reporterId(it) } ?: userId
        ReportEntity(
            id = sampleId(userId, sample.slug),
            userId = owner,
            reporterName = sample.reporter?.displayName ?: reporterName,
            photoPath = null,
            photoRemoteUrl = null,
            category = sample.category.id,
            description = sample.description,
            latitude = AREA_LATITUDE + sample.north / METERS_PER_DEGREE_LATITUDE,
            longitude = AREA_LONGITUDE + sample.east / metersPerDegreeLongitude(),
            streetName = sample.streetName,
            reportStatus = sample.reportStatus.id,
            syncStatus = sample.syncStatus.id,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    /** Track records for the neighbours whose reports were just seeded. */
    fun communityReporters(now: Long = System.currentTimeMillis()): List<ReporterEntity> =
        CommunityReporter.entries.map { reporter ->
            ReporterEntity(
                id = reporterId(reporter),
                name = reporter.displayName,
                joinedAt = now - TimeUnit.DAYS.toMillis(reporter.memberSinceDays),
                totalReports = reporter.totalReports,
                confirmedReports = reporter.confirmedReports,
                completedReports = reporter.completedReports,
                needsInfoReports = reporter.needsInfoReports,
            )
        }

    fun notificationsFor(userId: String, now: Long = System.currentTimeMillis()): List<NotificationEntity> =
        listOf(
            notification(
                userId = userId,
                slug = "welcome",
                title = "Selamat datang di JalanKita",
                message = "Laporan contoh sudah disiapkan agar Anda bisa melihat alur " +
                    "aplikasi. Buka Panduan Kerusakan untuk contoh visualnya.",
                reportSlug = null,
                ageHours = 27,
                isRead = true,
                now = now,
            ),
            notification(
                userId = userId,
                slug = "asphalt-done",
                title = "Perbaikan selesai",
                message = "Laporan Aspal Rusak di Jl. Scientia Boulevard telah " +
                    "selesai ditangani. Terima kasih atas laporan Anda.",
                reportSlug = "asphalt",
                ageHours = 20 * 24,
                isRead = true,
                now = now,
            ),
            notification(
                userId = userId,
                slug = "pothole-progress",
                title = "Laporan sedang ditangani",
                message = "Laporan Jalan Berlubang di Jl. Boulevard Gading Serpong " +
                    "sudah masuk jadwal perbaikan minggu ini.",
                reportSlug = "pothole",
                ageHours = 18,
                isRead = false,
                now = now,
            ),
            notification(
                userId = userId,
                slug = "crack-info",
                title = "Butuh informasi tambahan",
                message = "Petugas meminta foto tambahan untuk laporan Retak Jalan " +
                    "di Jl. Raya Legok agar panjang retakan bisa diukur.",
                reportSlug = "crack",
                ageHours = 5 * 24,
                isRead = false,
                now = now,
            ),
            notification(
                userId = userId,
                slug = "flooding-verified",
                title = "Laporan terverifikasi",
                message = "Genangan Air di Jl. Raya Serpong sudah diverifikasi " +
                    "petugas dan diteruskan ke dinas terkait.",
                reportSlug = "flooding",
                ageHours = 7,
                isRead = false,
                now = now,
            ),
        )

    /** Longitude degrees shrink with latitude; at ~6°S the difference is ~0.6%. */
    private fun metersPerDegreeLongitude(): Double =
        METERS_PER_DEGREE_LATITUDE * cos(Math.toRadians(AREA_LATITUDE))

    private fun reporterId(reporter: CommunityReporter): String =
        sampleId("jalankita-community", "reporter-${reporter.slug}")

    private fun notification(
        userId: String,
        slug: String,
        title: String,
        message: String,
        reportSlug: String?,
        ageHours: Long,
        isRead: Boolean,
        now: Long,
    ) = NotificationEntity(
        id = sampleId(userId, "notif-$slug"),
        userId = userId,
        title = title,
        message = message,
        reportId = reportSlug?.let { sampleId(userId, it) },
        isRead = isRead,
        createdAt = now - TimeUnit.HOURS.toMillis(ageHours),
    )

    /**
     * A stable UUID per (scope, slug) pair. Derived rather than random so
     * re-running the seeder overwrites the same rows instead of adding new ones.
     */
    private fun sampleId(scope: String, slug: String): String {
        val digest = java.security.MessageDigest.getInstance("MD5")
            .digest("jalankita-sample:$scope:$slug".toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }
        return buildString {
            append(hex.substring(0, 8)); append('-')
            append(hex.substring(8, 12)); append('-')
            append('4'); append(hex.substring(13, 16)); append('-')
            append('8'); append(hex.substring(17, 20)); append('-')
            append(hex.substring(20, 32))
        }
    }
}
