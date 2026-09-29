package org.umn.jalankita.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.Report
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus

/**
 * The local source of truth for reports.
 *
 * [id] is a client-generated UUID and is also the primary key on the server, which
 * is what makes retries idempotent — see SyncReportsWorker.
 */
@Entity(
    tableName = "reports",
    indices = [
        Index("userId"),
        Index("syncStatus"),
        Index("createdAt"),
    ],
)
data class ReportEntity(
    @PrimaryKey val id: String,
    val userId: String,
    /**
     * Denormalised so a community report can name its author without a join —
     * the same shape the server returns. [ReporterEntity] holds the track record.
     */
    val reporterName: String? = null,
    @ColumnInfo(name = "photoPath") val photoPath: String?,
    @ColumnInfo(name = "photoRemoteUrl") val photoRemoteUrl: String?,
    val category: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val streetName: String?,
    val reportStatus: String,
    val syncStatus: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
)

fun ReportEntity.toDomain(): Report = Report(
    id = id,
    userId = userId,
    reporterName = reporterName,
    photoPath = photoPath,
    photoRemoteUrl = photoRemoteUrl,
    category = DamageCategory.fromId(category),
    description = description,
    latitude = latitude,
    longitude = longitude,
    streetName = streetName,
    reportStatus = ReportStatus.fromId(reportStatus),
    syncStatus = SyncStatus.fromId(syncStatus),
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncAttempts = syncAttempts,
    lastSyncError = lastSyncError,
)

fun Report.toEntity(): ReportEntity = ReportEntity(
    id = id,
    userId = userId,
    reporterName = reporterName,
    photoPath = photoPath,
    photoRemoteUrl = photoRemoteUrl,
    category = category.id,
    description = description,
    latitude = latitude,
    longitude = longitude,
    streetName = streetName,
    reportStatus = reportStatus.id,
    syncStatus = syncStatus.id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncAttempts = syncAttempts,
    lastSyncError = lastSyncError,
)
