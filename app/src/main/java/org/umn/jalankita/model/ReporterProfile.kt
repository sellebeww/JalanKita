package org.umn.jalankita.model

import kotlin.math.roundToInt

/**
 * How much weight a reporter's word carries.
 *
 * A civic reporting app lives or dies on whether its reports are believable, so
 * every report shows who filed it, and this is what "who" means: how much they
 * have reported, and how much of it the authority actually confirmed.
 *
 * The signals are only the ones the workflow really produces — there is no
 * "rejected" state in [ReportStatus], so accuracy is measured as *confirmed*
 * (VERIFIED / IN_PROGRESS / COMPLETED) against *sent back* (NEEDS_INFO).
 * Reports still waiting are excluded entirely: an unanswered report is not
 * evidence either way.
 */
data class ReporterProfile(
    val id: String,
    val name: String,
    val joinedAt: Long,
    val totalReports: Int = 0,
    /** Reports an authority accepted as real: VERIFIED, IN_PROGRESS or COMPLETED. */
    val confirmedReports: Int = 0,
    /** Subset of [confirmedReports] that has been repaired. */
    val completedReports: Int = 0,
    /** Reports sent back because they could not be acted on as filed. */
    val needsInfoReports: Int = 0,
) {
    val initials: String
        get() = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }

    /** Reports the authority has actually ruled on, either way. */
    val reviewedReports: Int get() = confirmedReports + needsInfoReports

    val pendingReports: Int get() = (totalReports - reviewedReports).coerceAtLeast(0)

    /** Share of reviewed reports that were confirmed; 0 when nothing is reviewed yet. */
    val accuracy: Float
        get() = if (reviewedReports == 0) 0f else confirmedReports.toFloat() / reviewedReports

    val accuracyPercent: Int get() = (accuracy * 100).roundToInt()

    /**
     * 0–100, weighted towards accuracy but needing some volume to reach the top.
     *
     * Accuracy alone would hand a perfect score to someone with one lucky
     * report, so volume contributes up to 30 points and saturates at 15 reviewed
     * reports — enough to tell a regular from a newcomer without turning the
     * score into a leaderboard.
     */
    val trustScore: Int
        get() {
            if (reviewedReports == 0) return 0
            val accuracyPoints = accuracy * 70f
            val volumePoints = 30f * (reviewedReports.coerceAtMost(VOLUME_CEILING) / VOLUME_CEILING.toFloat())
            return (accuracyPoints + volumePoints).roundToInt().coerceIn(0, 100)
        }

    /** True once there is enough history for the score to mean anything. */
    val hasTrackRecord: Boolean get() = reviewedReports >= MIN_REVIEWED_FOR_SCORE

    val level: ReporterLevel get() = ReporterLevel.of(this)

    private companion object {
        const val MIN_REVIEWED_FOR_SCORE = 3
        const val VOLUME_CEILING = 15
    }
}

/**
 * The badge shown next to a reporter's name. Thresholds are deliberately
 * conservative: "Terverifikasi" should be rare enough to mean something.
 */
enum class ReporterLevel(val label: String, val description: String) {
    NEW(
        label = "Pelapor Baru",
        description = "Belum cukup laporan yang ditinjau petugas untuk menilai akurasinya.",
    ),
    ACTIVE(
        label = "Pelapor Aktif",
        description = "Rutin melapor; sebagian laporannya sudah dikonfirmasi petugas.",
    ),
    TRUSTED(
        label = "Pelapor Tepercaya",
        description = "Sebagian besar laporannya terbukti benar saat diperiksa petugas.",
    ),
    VERIFIED(
        label = "Pelapor Terverifikasi",
        description = "Rekam jejak panjang dengan tingkat akurasi sangat tinggi.",
    );

    companion object {
        fun of(profile: ReporterProfile): ReporterLevel = when {
            !profile.hasTrackRecord -> NEW
            profile.trustScore >= 90 && profile.reviewedReports >= 10 -> VERIFIED
            profile.trustScore >= 70 -> TRUSTED
            else -> ACTIVE
        }
    }
}
