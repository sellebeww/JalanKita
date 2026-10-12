package org.umn.jalankita.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.umn.jalankita.model.ReporterProfile

/**
 * A reporter's public track record.
 *
 * Kept as its own table rather than derived from [ReportEntity] because a
 * community report's author usually has history this device has never seen: the
 * counts come from the server (or, in local storage, from the seeded community) and
 * must survive even when only one of their reports is stored locally.
 */
@Entity(tableName = "reporters")
data class ReporterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val joinedAt: Long,
    val totalReports: Int = 0,
    val confirmedReports: Int = 0,
    val completedReports: Int = 0,
    val needsInfoReports: Int = 0,
)

fun ReporterEntity.toDomain(): ReporterProfile = ReporterProfile(
    id = id,
    name = name,
    joinedAt = joinedAt,
    totalReports = totalReports,
    confirmedReports = confirmedReports,
    completedReports = completedReports,
    needsInfoReports = needsInfoReports,
)

fun ReporterProfile.toEntity(): ReporterEntity = ReporterEntity(
    id = id,
    name = name,
    joinedAt = joinedAt,
    totalReports = totalReports,
    confirmedReports = confirmedReports,
    completedReports = completedReports,
    needsInfoReports = needsInfoReports,
)
