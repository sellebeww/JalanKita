package org.umn.jalankita.model

/**
 * Where the report stands with the authority handling it.
 * Deliberately separate from [SyncStatus] — the two answer different questions.
 */
enum class ReportStatus(val id: String, val label: String) {
    PENDING("PENDING", "Menunggu Verifikasi"),
    VERIFIED("VERIFIED", "Terverifikasi"),
    IN_PROGRESS("IN_PROGRESS", "Sedang Ditangani"),
    COMPLETED("COMPLETED", "Selesai"),
    NEEDS_INFO("NEEDS_INFO", "Butuh Info Tambahan");

    companion object {
        fun fromId(id: String?): ReportStatus = entries.firstOrNull { it.id == id } ?: PENDING
    }
}
