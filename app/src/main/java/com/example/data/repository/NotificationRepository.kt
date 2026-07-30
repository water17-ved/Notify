package com.example.data.repository

import android.content.Context
import com.example.data.dao.CustomNotificationDao
import com.example.data.dao.NotificationLogDao
import com.example.data.model.CustomNotification
import com.example.data.model.NotificationLog
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val context: Context,
    private val notificationDao: CustomNotificationDao,
    private val logDao: NotificationLogDao
) {
    val allNotifications: Flow<List<CustomNotification>> = notificationDao.getAllNotifications()
    val allLogs: Flow<List<NotificationLog>> = logDao.getAllLogs()

    suspend fun saveNotification(notification: CustomNotification) {
        val id = notificationDao.insertNotification(notification)
        val saved = notification.copy(id = if (notification.id == 0L) id else notification.id)
        if (saved.isEnabled) {
            NotificationHelper.scheduleAlarm(context, saved)
        } else {
            NotificationHelper.cancelAlarm(context, saved.id)
        }
    }

    suspend fun toggleNotification(id: Long, isEnabled: Boolean) {
        notificationDao.setEnabled(id, isEnabled)
        val notification = notificationDao.getNotificationById(id)
        if (notification != null) {
            if (isEnabled) {
                NotificationHelper.scheduleAlarm(context, notification)
            } else {
                NotificationHelper.cancelAlarm(context, id)
            }
        }
    }

    suspend fun deleteNotification(notification: CustomNotification) {
        NotificationHelper.cancelAlarm(context, notification.id)
        notificationDao.deleteNotification(notification)
    }

    suspend fun triggerTestNotification(notification: CustomNotification) {
        NotificationHelper.showNotification(
            context = context,
            id = if (notification.id == 0L) System.currentTimeMillis() else notification.id,
            title = notification.title,
            message = notification.message,
            category = notification.category.name,
            priority = notification.priority
        )
    }

    suspend fun clearLogs() {
        logDao.clearAllLogs()
    }
}
