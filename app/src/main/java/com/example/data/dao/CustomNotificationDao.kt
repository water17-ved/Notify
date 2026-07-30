package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CustomNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomNotificationDao {
    @Query("SELECT * FROM custom_notifications ORDER BY isEnabled DESC, triggerTimeMillis ASC")
    fun getAllNotifications(): Flow<List<CustomNotification>>

    @Query("SELECT * FROM custom_notifications WHERE isEnabled = 1")
    suspend fun getActiveNotificationsDirect(): List<CustomNotification>

    @Query("SELECT * FROM custom_notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: Long): CustomNotification?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: CustomNotification): Long

    @Update
    suspend fun updateNotification(notification: CustomNotification)

    @Delete
    suspend fun deleteNotification(notification: CustomNotification)

    @Query("DELETE FROM custom_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE custom_notifications SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE custom_notifications SET lastTriggeredAt = :triggeredAt WHERE id = :id")
    suspend fun updateLastTriggered(id: Long, triggeredAt: Long)
}
