// EventNotificationReceiver.kt - VERSIÓN CORREGIDA
package com.example.organizer.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.organizer.R
import java.util.*

class EventNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = intent.getStringExtra("event_title") ?: "Recordatorio"
        val description = intent.getStringExtra("event_description") ?: ""
        val time = intent.getStringExtra("event_time") ?: ""
        val isDaily = intent.getBooleanExtra("is_daily", false)
        val notificationId = intent.getIntExtra("notification_id", Random().nextInt(10000))

        val notificationText = if (isDaily) {
            "Recordatorio diario: $description"
        } else {
            "Evento programado: $description a las $time"
        }

        val notification = NotificationCompat.Builder(context, "event_reminder_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}