package com.maayn.mealmate.core.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.maayn.mealmate.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val mealName = intent?.getStringExtra("mealName") ?: "Meal Reminder"

            val notificationManager =
                it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelId = "meal_reminder_channel"
            val channelName = "Meal Reminders"

            // Create notification channel (required for Android 8+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(it, channelId)
                .setSmallIcon(R.drawable.meal_mate_icon)
                .setContentTitle("Meal Reminder")
                .setContentText("It's time for $mealName!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build()

            notificationManager.notify(1001, notification)
        }
    }
}
