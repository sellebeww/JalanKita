package org.umn.jalankita.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.umn.jalankita.model.AppNotification

@Entity(
    tableName = "notifications",
    indices = [Index("userId"), Index("createdAt")],
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val reportId: String?,
    val isRead: Boolean,
    val createdAt: Long,
)

fun NotificationEntity.toDomain(): AppNotification = AppNotification(
    id = id,
    userId = userId,
    title = title,
    message = message,
    reportId = reportId,
    isRead = isRead,
    createdAt = createdAt,
)
