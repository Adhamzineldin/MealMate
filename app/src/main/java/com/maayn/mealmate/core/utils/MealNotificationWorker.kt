package com.maayn.mealmate.core.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.maayn.mealmate.R
import java.util.*

class MealNotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    @SuppressLint("ServiceCast")
    private fun createNotificationChannel() {
        val channelId = "meal_channel"
        val channelName = "Meal Reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Channel for meal reminders"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.WRITE_CALENDAR])
    override fun doWork(): Result {
        createNotificationChannel()

        val mealName = inputData.getString("mealName") ?: return Result.failure()
        val mealTimeMillis = inputData.getLong("mealTimeMillis", -1)

        if (mealTimeMillis == -1L) return Result.failure()

        showNotification(mealName)
        addMealToCalendar(mealName, mealTimeMillis)

        // 🔥 Cancel this worker explicitly (optional, OneTimeWork auto-cleans up)
        WorkManager.getInstance(applicationContext).cancelWorkById(id)

        return Result.success()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(mealName: String) {
        val notification = NotificationCompat.Builder(applicationContext, "meal_channel")
            .setContentTitle("Meal Reminder")
            .setContentText("Reminder: It's time to prepare $mealName!")
            .setSmallIcon(R.drawable.meal_mate_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(0, notification)
    }

    private fun addMealToCalendar(mealName: String, mealTimeMillis: Long) {
        val calendarId = getPrimaryCalendarId() ?: return

        val startMillis = mealTimeMillis
        val endMillis = mealTimeMillis + 60 * 60 * 1000 // 1 hour duration

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, "Prepare $mealName")
            put(CalendarContract.Events.DESCRIPTION, "Meal preparation reminder.")
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        val uri = applicationContext.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        if (uri == null) {
            Toast.makeText(applicationContext, "Failed to add to calendar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        val uri = CalendarContract.Calendars.CONTENT_URI

        val cursor = applicationContext.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val calId = it.getLong(0)
                val isPrimary = it.getInt(1) != 0
                if (isPrimary) return calId
            }
        }
        return null
    }
}
