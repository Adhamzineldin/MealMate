package com.maayn.mealmate.core.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.maayn.mealmate.R
import java.util.*

class MealNotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val TAG = "MealNotificationWorker"

    @SuppressLint("ServiceCast")
    private fun createNotificationChannel() {
        val channelId = "meal_channel"
        val channelName = "Meal Reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel for meal reminders"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun doWork(): Result {
        try {
            createNotificationChannel()

            val mealName = inputData.getString("mealName") ?: return Result.failure()
            val mealTimeMillis = inputData.getLong("mealTimeMillis", -1)

            if (mealTimeMillis == -1L) return Result.failure()

            // Show notification
            try {
                showNotification(mealName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show notification", e)
            }

            // Add to calendar if permission available
            try {
                if (hasCalendarPermission()) {
                    val success = addMealToCalendar(mealName, mealTimeMillis)
                    if (success) {
                        Log.d(TAG, "Successfully added event to calendar")
                    } else {
                        Log.e(TAG, "Failed to add event to calendar")
                    }
                } else {
                    Log.e(TAG, "Missing calendar permissions")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception when adding to calendar", e)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            return Result.failure()
        }
    }

    private fun hasCalendarPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showNotification(mealName: String) {
        val notification = NotificationCompat.Builder(applicationContext, "meal_channel")
            .setContentTitle("Meal Reminder")
            .setContentText("Reminder: It's time to prepare $mealName!")
            .setSmallIcon(R.drawable.meal_mate_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to post notifications", e)
        }
    }

    private fun addMealToCalendar(mealName: String, mealTimeMillis: Long): Boolean {
        try {
            val calendarId = getPrimaryCalendarId()
            if (calendarId == null) {
                Log.e(TAG, "No primary calendar found")
                return false
            }

            val startMillis = mealTimeMillis
            val endMillis = mealTimeMillis + 60 * 60 * 1000 // 1 hour duration

            Log.d(TAG, "Preparing to add event: $mealName at time: $startMillis with calendarId: $calendarId")

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "Prepare $mealName")
                put(CalendarContract.Events.DESCRIPTION, "Meal preparation reminder from MealMate app")
                put(CalendarContract.Events.EVENT_LOCATION, "Home")
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 1)
            }

            val uri = applicationContext.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

            if (uri != null) {
                val eventId = uri.lastPathSegment?.toLong()
                Log.d(TAG, "Successfully added event with ID: $eventId")

                // Add a reminder to the event
                if (eventId != null) {
                    addReminderToEvent(eventId)
                }

                return true
            } else {
                Log.e(TAG, "Failed to insert event into calendar")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when adding event to calendar", e)
            return false
        }
    }

    private fun addReminderToEvent(eventId: Long) {
        try {
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 15) // Reminder 15 minutes before
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }

            applicationContext.contentResolver.insert(
                CalendarContract.Reminders.CONTENT_URI,
                reminderValues
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add reminder to event", e)
        }
    }

    private fun getPrimaryCalendarId(): Long? {
        val contentResolver: ContentResolver = applicationContext.contentResolver

        // First try getting the primary calendar
        val primaryCalendarQuery = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.IS_PRIMARY}=1",
            null,
            null
        )

        primaryCalendarQuery?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }

        // If no primary calendar, get the first available calendar
        val anyCalendarQuery = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            null,
            null,
            CalendarContract.Calendars._ID + " ASC LIMIT 1"
        )

        anyCalendarQuery?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }

        // No calendars found
        Log.e(TAG, "No calendars found on the device")
        return null
    }
}