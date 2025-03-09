package com.maayn.mealmate.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.maayn.mealmate.MainActivity
import com.maayn.mealmate.R


class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val mealName = intent?.getStringExtra("mealName") ?: "Meal Reminder"

            val notificationManager =
                it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelId = "meal_channel"
            val channelName = "Meal Reminders"

            // Check if the notification channel exists, if not create it
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Create the channel only if it doesn't exist
                if (notificationManager.getNotificationChannel(channelId) == null) {
                    val channel = NotificationChannel(
                        channelId, channelName, NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Channel for meal reminders"
                        enableLights(true)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }

            // Build the notification
            val notification = NotificationCompat.Builder(it, channelId)
                .setSmallIcon(R.drawable.meal_mate_icon)
                .setContentTitle("Meal Reminder")
                .setContentText("It's time for $mealName!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                .build()

            // Show the notification
            notificationManager.notify(1001, notification)
        }
    }
}

