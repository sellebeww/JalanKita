package org.umn.jalankita.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.umn.jalankita.data.local.ReportEntity
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.Report
import java.time.Instant

/** Row shapes for the Supabase tables; see supabase/schema.sql. */

@Serializable
data class RemoteProfile(
    val id: String,
    val name: String,
    val email: String,
)

@Serializable
data class RemoteReport(
    val id: String,
    @SerialName("user_id") val userId: String,
    val category: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    @SerialName("street_name") val streetName: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("report_status") val reportStatus: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class RemoteNotification(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val message: String,
    @SerialName("report_id") val reportId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

fun Report.toRemote(photoUrl: String?): RemoteReport = RemoteReport(
    id = id,
    userId = userId,
    category = category.id,
    description = description,
    latitude = latitude,
    longitude = longitude,
    streetName = streetName,
    photoUrl = photoUrl ?: photoRemoteUrl,
    reportStatus = reportStatus.id,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
)

fun RemoteReport.toEntity(existing: ReportEntity?): ReportEntity {
    val created = createdAt.toEpochMillisOrNow()
    return ReportEntity(
        id = id,
        userId = userId,
        photoPath = existing?.photoPath,
        photoRemoteUrl = photoUrl ?: existing?.photoRemoteUrl,
        category = DamageCategory.fromId(category).id,
        description = description,
        latitude = latitude,
        longitude = longitude,
        streetName = streetName ?: existing?.streetName,
        reportStatus = reportStatus,
        // Anything that came back from the server is, by definition, synced.
        syncStatus = existing?.syncStatus ?: org.umn.jalankita.model.SyncStatus.SYNCED.id,
        createdAt = created,
        updatedAt = updatedAt.toEpochMillisOrNow(),
        syncAttempts = existing?.syncAttempts ?: 0,
        lastSyncError = existing?.lastSyncError,
    )
}

fun String.toEpochMillisOrNow(): Long =
    runCatching { Instant.parse(this).toEpochMilli() }
        .recoverCatching { Instant.parse(this + "Z").toEpochMilli() }
        .getOrElse { System.currentTimeMillis() }
