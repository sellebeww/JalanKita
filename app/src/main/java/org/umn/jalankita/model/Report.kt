package org.umn.jalankita.model

/** Domain model used by the UI layer. Room and Supabase each map to/from this. */
data class Report(
    val id: String,
    val userId: String,
    /** Who filed it. Null for older local rows; the UI falls back to "Warga". */
    val reporterName: String? = null,
    val photoPath: String?,
    val photoRemoteUrl: String?,
    val category: DamageCategory,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val streetName: String?,
    val reportStatus: ReportStatus,
    val syncStatus: SyncStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
) {
    /** Prefer the uploaded copy once it exists; fall back to the local file. */
    val displayImage: String? get() = photoRemoteUrl ?: photoPath

    val hasResolvedStreet: Boolean get() = !streetName.isNullOrBlank()

    val displayReporter: String get() = reporterName?.takeIf { it.isNotBlank() } ?: "Warga"

    /** Damage that is still out there; drives the proximity alerts. */
    val isUnresolved: Boolean get() = reportStatus != ReportStatus.COMPLETED
}
