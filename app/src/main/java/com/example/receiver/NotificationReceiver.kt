package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.model.NotificationLog
import com.example.data.model.NotificationPriority
import com.example.data.model.RepeatMode
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getLongExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1L)
        if (notificationId == -1L) return

        val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE) ?: "Reminder"
        val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE) ?: "You have a scheduled alert!"
        val category = intent.getStringExtra(NotificationHelper.EXTRA_CATEGORY) ?: "GENERAL"

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val notificationDao = db.notificationDao()
                val logDao = db.logDao()

                val item = notificationDao.getNotificationById(notificationId)
                val priority = item?.priority ?: NotificationPriority.HIGH

                // Show Notification
                NotificationHelper.showNotification(
                    context = context,
                    id = notificationId,
                    title = title,
                    message = message,
                    category = category,
                    priority = priority
                )

                // Log entry
                logDao.insertLog(
                    NotificationLog(
                        notificationId = notificationId,
                        title = title,
                        message = message,
                        category = category,
                        triggeredAt = System.currentTimeMillis(),
                        status = "DELIVERED"
                    )
                )

                if (item != null) {
                    val now = System.currentTimeMillis()
                    if (item.repeatMode == RepeatMode.ONCE) {
                        notificationDao.updateNotification(
                            item.copy(isEnabled = false, lastTriggeredAt = now)
                        )
                    } else {
                        val nextTrigger = NotificationHelper.getNextTriggerTime(
                            initialTimeMillis = item.triggerTimeMillis,
                            repeatMode = item.repeatMode
                        )
                        val updated = item.copy(
                            triggerTimeMillis = nextTrigger,
                            lastTriggeredAt = now
                        )
                        notificationDao.updateNotification(updated)
                        NotificationHelper.scheduleAlarm(context, updated)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
