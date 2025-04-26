package com.maayn.mealmate.core.utils

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import androidx.work.BackoffPolicy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import android.os.Bundle
import com.maayn.mealmate.R

class MealNotificationHelper(private val context: Context) {

    // Add a meal event to calendar immediately
    fun addMealToCalendar(mealName: String, mealTimeMillis: Long): Boolean {
        if (!hasCalendarPermission()) {
            Log.e("MealNotificationHelper", "Missing calendar permission")
            return false
        }

        try {
            val calendarId = getPrimaryCalendarId()
            if (calendarId == null) {
                Log.e("MealNotificationHelper", "No primary calendar found")
                return false
            }

            val startMillis = mealTimeMillis
            val endMillis = mealTimeMillis + 60 * 60 * 1000 // 1 hour duration
            val currentMillis = System.currentTimeMillis()

            Log.d("MealNotificationHelper", "Adding event: $mealName at time: $startMillis with calendarId: $calendarId")

            // Check if time is in the future
            if (startMillis <= currentMillis) {
                Log.e("MealNotificationHelper", "Cannot add past events to calendar: $startMillis <= $currentMillis")
                return false
            }

            // Create event with details
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

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

            if (uri != null) {
                val eventId = uri.lastPathSegment?.toLong()
                Log.d("MealNotificationHelper", "Successfully added event with ID: $eventId")

                // Add multiple reminders to the event
                if (eventId != null) {
                    addRemindersToEvent(eventId)
                }

                // Trigger calendar sync
                triggerCalendarSync()

                return true
            } else {
                Log.e("MealNotificationHelper", "Failed to insert event into calendar")
                return false
            }
        } catch (e: Exception) {
            Log.e("MealNotificationHelper", "Exception when adding event to calendar", e)
            return false
        }
    }

    fun addRemindersToEvent(eventId: Long) {
        try {
            // Add reminders (15 min and 5 min before)
            val reminderTimes = listOf(15, 5)

            reminderTimes.forEach { minutes ->
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, minutes)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }

                context.contentResolver.insert(
                    CalendarContract.Reminders.CONTENT_URI,
                    reminderValues
                )

                Log.d("MealNotificationHelper", "Added $minutes minute reminder to event $eventId")
            }
        } catch (e: Exception) {
            Log.e("MealNotificationHelper", "Failed to add reminders to event", e)
        }
    }

    fun triggerCalendarSync() {
        try {
            // Request sync for the calendar provider
            val authority = CalendarContract.AUTHORITY
            ContentResolver.requestSync(null, authority, Bundle())
            Log.d("MealNotificationHelper", "Calendar sync requested")
        } catch (e: Exception) {
            Log.e("MealNotificationHelper", "Failed to trigger calendar sync", e)
        }
    }

    fun getPrimaryCalendarId(): Long? {
        val contentResolver: ContentResolver = context.contentResolver

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
        Log.e("MealNotificationHelper", "No calendars found on the device")
        return null
    }

    fun hasCalendarPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Schedule a notification for the meal
    fun scheduleMealNotification(mealName: String, mealTimeMillis: Long, recipeId: String, notifyAt: Long) {
        val workRequest = OneTimeWorkRequestBuilder<MealNotificationWorker>()
            .setInitialDelay(notifyAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "mealName" to mealName,
                    "mealTimeMillis" to mealTimeMillis,
                    "recipeId" to recipeId
                )
            )
            .addTag("meal_notification_$recipeId")
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // Cancel any existing notification for this meal
        WorkManager.getInstance(context).cancelAllWorkByTag("meal_notification_$recipeId")

        // Schedule the new notification
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "meal_notification_$recipeId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }
}