package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val title: String,
    val message: String,
    val category: String,
    val triggeredAt: Long = System.currentTimeMillis(),
    val status: String = "DELIVERED" // DELIVERED, SNOOZED, DISMISSED
)
