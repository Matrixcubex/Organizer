// app/src/main/java/com/example/organizer/receiver/EventNotificationReceiver.kt
package com.example.organizer.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.organizer.R

class EventNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "event_reminder_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(intent.getStringExtra("event_title"))
            .setContentText(intent.getStringExtra("event_description"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}