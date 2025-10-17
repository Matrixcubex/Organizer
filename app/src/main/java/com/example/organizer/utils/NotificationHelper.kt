// NotificationHelper.kt - VERSIÓN CORREGIDA
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
import java.text.SimpleDateFormat
import java.util.*

class NotificationHelper(private val context: Context) {
    private val channelId = "event_reminder_channel"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
                description = "Notificaciones para eventos y recordatorios programados"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(event: Event) {
        try {
            val triggerTime = calculateTriggerTime(event)
            if (triggerTime <= System.currentTimeMillis()) {
                // Si el tiempo ya pasó, no programar
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, EventNotificationReceiver::class.java).apply {
                putExtra("event_title", event.title)
                putExtra("event_description", event.description ?: "Recordatorio")
                putExtra("event_time", event.time)
                putExtra("notification_id", event.id.toInt())
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.id.toInt(), // ID único para cada notificación
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            // Notificación inmediata de confirmación
            showImmediateNotification(
                "Recordatorio Programado",
                "${event.title} a las ${event.time}"
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateTriggerTime(event: Event): Long {
        val calendar = Calendar.getInstance()

        if (event.date == "DIARIO") {
            // Para recordatorios diarios
            val timeParts = event.time.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts.getOrNull(1)?.toInt() ?: 0

            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)

            // Si la hora ya pasó hoy, programar para mañana
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Para eventos con fecha específica
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val eventDate = dateFormat.parse(event.date)

            val timeParts = event.time.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts.getOrNull(1)?.toInt() ?: 0

            calendar.time = eventDate
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)

            // Aplicar recordatorio (30 minutos antes)
            val reminderMinutes = event.reminder.toIntOrNull() ?: 30
            calendar.add(Calendar.MINUTE, -reminderMinutes)
        }

        return calendar.timeInMillis
    }

    private fun showImmediateNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // Para recordatorios diarios
    fun scheduleDailyReminder(event: Event) {
        val calendar = Calendar.getInstance()
        val timeParts = event.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts.getOrNull(1)?.toInt() ?: 0

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)

        // Si la hora ya pasó, programar para mañana
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventNotificationReceiver::class.java).apply {
            putExtra("event_title", event.title)
            putExtra("event_description", event.description ?: "Recordatorio diario")
            putExtra("event_time", event.time)
            putExtra("is_daily", true)
            putExtra("notification_id", event.id.toInt())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Programar repetición diaria
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}