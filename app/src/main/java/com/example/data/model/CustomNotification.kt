package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RepeatMode(val label: String) {
    ONCE("One-time"),
    EVERY_5_MINS("Every 5 minutes (Test)"),
    HOURLY("Hourly"),
    DAILY("Every Day"),
    MON_FRI("Mon to Fri"),
    WEEKENDS("Weekends"),
    WEEKLY("Every Week")
}

enum class NotificationPriority(val label: String) {
    HIGH("High (Sound & Banner)"),
    DEFAULT("Standard"),
    LOW("Low (Silent)")
}

enum class NotificationCategory(val label: String, val colorHex: Long) {
    GENERAL("General", 0xFF6366F1),
    WORK("Work", 0xFF3B82F6),
    HEALTH("Health", 0xFF10B981),
    PERSONAL("Personal", 0xFFEC4899),
    URGENT("Urgent", 0xFFEF4444),
    STUDY("Study", 0xFF8B5CF6)
}

@Entity(tableName = "custom_notifications")
data class CustomNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val triggerTimeMillis: Long,
    val repeatMode: RepeatMode = RepeatMode.ONCE,
    val category: NotificationCategory = NotificationCategory.GENERAL,
    val priority: NotificationPriority = NotificationPriority.HIGH,
    val isEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null
)
