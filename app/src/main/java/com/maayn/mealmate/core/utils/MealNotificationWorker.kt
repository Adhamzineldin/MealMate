package com.maayn.mealmate.core.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.maayn.mealmate.MainActivity
import com.maayn.mealmate.R
import java.util.*
import android.os.Bundle
class MealNotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val CHANNEL_ID = "meal_reminder_channel"
    private val NOTIFICATION_ID = 101

    @SuppressLint("ServiceCast")
    private fun createNotificationChannel() {
        val channelId = CHANNEL_ID
        val channelName = "Meal Reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel for meal reminders"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("MealNotificationWorker", "Notification channel created: $channelId")
        }
    }

    override fun doWork(): Result {
        try {
            Log.d("MealNotificationWorker", "Starting meal notification work")
            createNotificationChannel()

            val mealName = inputData.getString("mealName") ?: return Result.failure()
            val mealTimeMillis = inputData.getLong("mealTimeMillis", -1)
            val recipeId = inputData.getString("recipeId") ?: ""

            if (mealTimeMillis == -1L) {
                Log.e("MealNotificationWorker", "Invalid meal time")
                return Result.failure()
            }

            // Show notification
            try {
                showNotification(mealName, recipeId)
                Log.d("MealNotificationWorker", "Notification shown for $mealName")
            } catch (e: Exception) {
                Log.e("MealNotificationWorker", "Failed to show notification", e)
            }

            // Add to calendar if permission available
            try {
                if (hasCalendarPermission()) {
                    Log.d("MealNotificationWorker", "Calendar permission granted, adding event")
                    val success = addMealToCalendar(mealName, mealTimeMillis)
                    if (success) {
                        Log.d("MealNotificationWorker", "Successfully added event to calendar")
                    } else {
                        Log.e("MealNotificationWorker", "Failed to add event to calendar")
                    }
                } else {
                    Log.e("MealNotificationWorker", "Missing calendar permissions")
                }
            } catch (e: Exception) {
                Log.e("MealNotificationWorker", "Exception when adding to calendar", e)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("MealNotificationWorker", "Worker failed", e)
            return Result.failure()
        }
    }

    private fun hasCalendarPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showNotification(mealName: String, recipeId: String = "") {
        // Create intent to open the app when notification is clicked
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (recipeId.isNotEmpty()) {
                putExtra("RECIPE_ID", recipeId)
                putExtra("OPEN_RECIPE_DETAILS", true)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create "Start Cooking" action
        val startCookingIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = "START_COOKING_ACTION"
            if (recipeId.isNotEmpty()) {
                putExtra("RECIPE_ID", recipeId)
                putExtra("OPEN_RECIPE_DETAILS", true)
                putExtra("START_COOKING", true)
            }
        }

        val startCookingPendingIntent = PendingIntent.getActivity(
            applicationContext,
            1,
            startCookingIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create calendar action
        val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.android.calendar/time")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val calendarPendingIntent = PendingIntent.getActivity(
            applicationContext,
            2,
            calendarIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Large icon
        val largeIcon = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.meal_mate_icon)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Time to Cook: $mealName")
            .setContentText("It's time to prepare your meal. Tap to see recipe details!")
            .setSmallIcon(R.drawable.meal_mate_icon)
            .setLargeIcon(largeIcon)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("It's time to prepare $mealName! Get ready to cook this delicious meal. All ingredients and instructions are ready for you."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_cook, "Start Cooking", startCookingPendingIntent)
            .addAction(R.drawable.ic_calendar, "View Calendar", calendarPendingIntent)
            .setColor(Color.GREEN)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            Log.d("MealNotificationWorker", "Notification posted successfully")
        } catch (e: SecurityException) {
            Log.e("MealNotificationWorker", "No permission to post notifications", e)
        }
    }

    public fun addMealToCalendar(mealName: String, mealTimeMillis: Long): Boolean {
        try {
            val calendarId = getPrimaryCalendarId()
            if (calendarId == null) {
                Log.e("MealNotificationWorker", "No primary calendar found")
                return false
            }

            val startMillis = mealTimeMillis
            val endMillis = mealTimeMillis + 60 * 60 * 1000 // 1 hour duration
            val currentMillis = System.currentTimeMillis()

            Log.d("MealNotificationWorker", "Preparing to add event: $mealName at time: $startMillis with calendarId: $calendarId")

            // Check if time is in the future
            if (startMillis <= currentMillis) {
                Log.e("MealNotificationWorker", "Cannot add past events to calendar: $startMillis <= $currentMillis")
                return false
            }

            // Create event with more details
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "Prepare $mealName")
                put(CalendarContract.Events.DESCRIPTION, "Meal preparation reminder from MealMate app. Take time to gather all ingredients and follow recipe instructions.")
                put(CalendarContract.Events.EVENT_LOCATION, "Kitchen")
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 1)
                put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PRIVATE)
            }

            val uri = applicationContext.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

            if (uri != null) {
                val eventId = uri.lastPathSegment?.toLong()
                Log.d("MealNotificationWorker", "Successfully added event with ID: $eventId")

                // Add multiple reminders to the event
                if (eventId != null) {
                    addRemindersToEvent(eventId)
                }

                // Trigger calendar sync
                triggerCalendarSync()

                return true
            } else {
                Log.e("MealNotificationWorker", "Failed to insert event into calendar")
                return false
            }
        } catch (e: Exception) {
            Log.e("MealNotificationWorker", "Exception when adding event to calendar", e)
            return false
        }
    }

    public fun addRemindersToEvent(eventId: Long) {
        try {
            // Add multiple reminders (15 min and 5 min before)
            val reminderTimes = listOf(15, 5)

            reminderTimes.forEach { minutes ->
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, minutes)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }

                applicationContext.contentResolver.insert(
                    CalendarContract.Reminders.CONTENT_URI,
                    reminderValues
                )

                Log.d("MealNotificationWorker", "Added $minutes minute reminder to event $eventId")
            }
        } catch (e: Exception) {
            Log.e("MealNotificationWorker", "Failed to add reminders to event", e)
        }
    }

    public fun triggerCalendarSync() {
        try {
            // Request sync for the calendar provider
            val authority = CalendarContract.AUTHORITY
            ContentResolver.requestSync(null, authority, Bundle())
            Log.d("MealNotificationWorker", "Calendar sync requested")
        } catch (e: Exception) {
            Log.e("MealNotificationWorker", "Failed to trigger calendar sync", e)
        }
    }

    public fun getPrimaryCalendarId(): Long? {
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
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=#{CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}",
            null,
            CalendarContract.Calendars._ID + " ASC LIMIT 1"
        )

        anyCalendarQuery?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }

        // No calendars found
        Log.e("MealNotificationWorker", "No calendars found on the device")
        return null
    }
}