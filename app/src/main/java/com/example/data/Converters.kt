package com.example.data

import androidx.room.TypeConverter
import com.example.data.model.NotificationCategory
import com.example.data.model.NotificationPriority
import com.example.data.model.RepeatMode

class Converters {
    @TypeConverter
    fun fromRepeatMode(value: RepeatMode): String = value.name

    @TypeConverter
    fun toRepeatMode(value: String): RepeatMode = try {
        RepeatMode.valueOf(value)
    } catch (e: Exception) {
        RepeatMode.ONCE
    }

    @TypeConverter
    fun fromCategory(value: NotificationCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): NotificationCategory = try {
        NotificationCategory.valueOf(value)
    } catch (e: Exception) {
        NotificationCategory.GENERAL
    }

    @TypeConverter
    fun fromPriority(value: NotificationPriority): String = value.name

    @TypeConverter
    fun toPriority(value: String): NotificationPriority = try {
        NotificationPriority.valueOf(value)
    } catch (e: Exception) {
        NotificationPriority.HIGH
    }
}
