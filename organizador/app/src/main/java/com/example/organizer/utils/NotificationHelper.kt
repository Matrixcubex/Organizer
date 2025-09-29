// app/src/main/java/com/example/organizer/utils/NotificationHelper.kt
package com.example.organizer.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.organizer.R
import com.example.organizer.data.model.Event
import com.example.organizer.receiver.EventNotificationReceiver
import java.util.Calendar

class NotificationHelper(private val context: Context) {
    private val channelId = "event_reminder_channel"
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Eventos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para eventos programados"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(event: Event) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventNotificationReceiver::class.java).apply {
            putExtra("event_title", event.title)
            putExtra("event_description", event.description)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = calculateTriggerTime(event)
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    private fun calculateTriggerTime(event: Event): Long {
        val calendar = Calendar.getInstance().apply {
            // Parsear fecha y hora del evento (formato: dd/MM/yyyy y HH:mm)
            // ... implementación según tu formato de fecha/hora
        }

        // Ajustar según el recordatorio seleccionado
        return when (event.reminder) {
            "10_minutos" -> calendar.apply { add(Calendar.MINUTE, -10) }.timeInMillis
            "1_dia" -> calendar.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
            else -> calendar.timeInMillis // En el momento exacto
        }
    }
}