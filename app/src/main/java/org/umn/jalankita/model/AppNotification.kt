package org.umn.jalankita.model

data class AppNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val reportId: String?,
    val isRead: Boolean,
    val createdAt: Long,
)
