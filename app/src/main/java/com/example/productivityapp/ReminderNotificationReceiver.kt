package com.example.productivityapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("REMINDER_ID", 0)
        val title = intent.getStringExtra("REMINDER_TITLE") ?: "Reminder"
        val description = intent.getStringExtra("REMINDER_DESCRIPTION") ?: ""
        val isPersistent = intent.getBooleanExtra("IS_PERSISTENT", false)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (isPersistent) {
            builder.setOngoing(true)
                .setTimeoutAfter(10 * 60 * 1000) // 10 minutes in milliseconds
        }

        notificationManager.notify(reminderId.toInt(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "ReminderChannel"
    }
}