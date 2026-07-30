package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.model.NotificationLog
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getLongExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1L)
        if (notificationId == -1L) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId.toInt())

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val logDao = db.logDao()

                when (action) {
                    "ACTION_SNOOZE" -> {
                        val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE) ?: "Reminder"
                        val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE) ?: "Snoozed alert"
                        val snoozeTimeMillis = System.currentTimeMillis() + (10 * 60 * 1000)

                        val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
                            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, notificationId)
                            putExtra(NotificationHelper.EXTRA_TITLE, title)
                            putExtra(NotificationHelper.EXTRA_MESSAGE, message)
                        }

                        val snoozePendingIntent = PendingIntent.getBroadcast(
                            context,
                            notificationId.toInt(),
                            snoozeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                snoozeTimeMillis,
                                snoozePendingIntent
                            )
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                snoozeTimeMillis,
                                snoozePendingIntent
                            )
                        }

                        logDao.insertLog(
                            NotificationLog(
                                notificationId = notificationId,
                                title = title,
                                message = message,
                                category = "SNOOZE",
                                status = "SNOOZED"
                            )
                        )
                    }
                    "ACTION_MARK_DONE" -> {
                        logDao.insertLog(
                            NotificationLog(
                                notificationId = notificationId,
                                title = "Action Completed",
                                message = "Marked done from notification",
                                category = "DONE",
                                status = "COMPLETED"
                            )
                        )
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
