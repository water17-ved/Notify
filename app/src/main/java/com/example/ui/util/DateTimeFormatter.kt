package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateTimeFormatter {

    fun formatDateTime(timeMillis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timeMillis }

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        val isToday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = tomorrow.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "Today at ${timeFormat.format(target.time)}"
            isTomorrow -> "Tomorrow at ${timeFormat.format(target.time)}"
            else -> "${dateFormat.format(target.time)} at ${timeFormat.format(target.time)}"
        }
    }

    fun getTimeRemainingString(timeMillis: Long, isEnabled: Boolean): String {
        if (!isEnabled) return "Disabled"
        val diff = timeMillis - System.currentTimeMillis()
        if (diff <= 0) return "Triggered / Pending"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "In $days day${if (days > 1) "s" else ""}"
            hours > 0 -> "In $hours hr${if (hours > 1) "s" else ""} ${minutes % 60} min"
            minutes > 0 -> "In $minutes min${if (minutes > 1) "s" else ""}"
            else -> "In < 1 min"
        }
    }

    fun formatShortTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }

    fun formatShortDate(year: Int, month: Int, dayOfMonth: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        return SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)
    }
}
