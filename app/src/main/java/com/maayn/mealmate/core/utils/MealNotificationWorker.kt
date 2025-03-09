package com.maayn.mealmate.core.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.maayn.mealmate.R

class MealNotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    // Create the notification channel only once (usually in your Application class or Activity)
    @SuppressLint("ServiceCast")
    private fun createNotificationChannel() {
        val channelId = "meal_channel"
        val channelName = "Meal Reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // For Android 8.0 and above, ensure channel exists before showing notifications
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Channel for meal reminders"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        // Ensure the channel is created first
        createNotificationChannel()

        val mealName = inputData.getString("mealName") ?: return Result.failure()

        // Create the notification
        val notification = NotificationCompat.Builder(applicationContext, "meal_channel")
            .setContentTitle("Meal Reminder")
            .setContentText("Reminder: It's time to prepare $mealName!")
            .setSmallIcon(R.drawable.meal_mate_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Show the notification
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(0, notification)

        return Result.success()
    }
}


