package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.CustomNotification
import com.example.data.model.NotificationPriority
import com.example.data.model.RepeatMode
import com.example.receiver.NotificationActionReceiver
import com.example.receiver.NotificationReceiver
import java.util.Calendar

object NotificationHelper {
    const val CHANNEL_HIGH_ID = "custom_notify_high"
    const val CHANNEL_DEFAULT_ID = "custom_notify_default"
    const val CHANNEL_LOW_ID = "custom_notify_low"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_MESSAGE = "extra_message"
    const val EXTRA_CATEGORY = "extra_category"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val highChannel = NotificationChannel(
                CHANNEL_HIGH_ID,
                "Urgent Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority reminders with pop-up banner and sound"
                enableVibration(true)
                enableLights(true)
            }

            val defaultChannel = NotificationChannel(
                CHANNEL_DEFAULT_ID,
                "Standard Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standard custom notifications"
            }

            val lowChannel = NotificationChannel(
                CHANNEL_LOW_ID,
                "Silent Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent background notifications"
            }

            notificationManager.createNotificationChannels(
                listOf(highChannel, defaultChannel, lowChannel)
            )
        }
    }

    fun scheduleAlarm(context: Context, notification: CustomNotification) {
        if (!notification.isEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(EXTRA_TITLE, notification.title)
            putExtra(EXTRA_MESSAGE, notification.message)
            putExtra(EXTRA_CATEGORY, notification.category.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var triggerTime = notification.triggerTimeMillis
        val now = System.currentTimeMillis()

        if (triggerTime <= now) {
            triggerTime = getNextTriggerTime(notification.triggerTimeMillis, notification.repeatMode)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, notificationId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showNotification(
        context: Context,
        id: Long,
        title: String,
        message: String,
        category: String = "GENERAL",
        priority: NotificationPriority = NotificationPriority.HIGH
    ) {
        val channelId = when (priority) {
            NotificationPriority.HIGH -> CHANNEL_HIGH_ID
            NotificationPriority.DEFAULT -> CHANNEL_DEFAULT_ID
            NotificationPriority.LOW -> CHANNEL_LOW_ID
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra(EXTRA_NOTIFICATION_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (id * 1000 + 1).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_MARK_DONE"
            putExtra(EXTRA_NOTIFICATION_ID, id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (id * 1000 + 2).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(
                when (priority) {
                    NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
                    NotificationPriority.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
                    NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
                }
            )
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Snooze 10m", snoozePendingIntent)
            .addAction(0, "Mark Done", donePendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id.toInt(), builder.build())
    }

    fun getNextTriggerTime(initialTimeMillis: Long, repeatMode: RepeatMode): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = initialTimeMillis }

        if (repeatMode == RepeatMode.ONCE) {
            return if (target.before(now)) now.timeInMillis + 5000 else target.timeInMillis
        }

        while (target.before(now)) {
            when (repeatMode) {
                RepeatMode.ONCE -> break
                RepeatMode.EVERY_5_MINS -> target.add(Calendar.MINUTE, 5)
                RepeatMode.HOURLY -> target.add(Calendar.HOUR_OF_DAY, 1)
                RepeatMode.DAILY -> target.add(Calendar.DAY_OF_YEAR, 1)
                RepeatMode.WEEKLY -> target.add(Calendar.DAY_OF_YEAR, 7)
                RepeatMode.MON_FRI -> {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                    while (target.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        target.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    ) {
                        target.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                RepeatMode.WEEKENDS -> {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                    while (target.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY &&
                        target.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
                    ) {
                        target.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }
        }
        return target.timeInMillis
    }
}
