package com.maayn.mealmate.core.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.maayn.mealmate.MainActivity
import com.maayn.mealmate.R

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
                return Result.failure()
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("MealNotificationWorker", "Worker failed", e)
            return Result.failure()
        }
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
}