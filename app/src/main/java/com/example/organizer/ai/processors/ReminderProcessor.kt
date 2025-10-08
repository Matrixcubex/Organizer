package com.example.organizer.ai.processors

import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention
import com.example.organizer.data.DatabaseHelper
import com.example.organizer.data.model.Event
import java.text.SimpleDateFormat
import java.util.*

class ReminderProcessor(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun process(parsedCommand: ParsedCommand): Action {
        val (titulo, descripcion, hora) = extractReminderDetails(parsedCommand.rawText)

        return Action(
            intention = UserIntention.Recordatorio,
            parameters = mapOf(
                "titulo" to titulo,
                "descripcion" to descripcion,
                "hora" to hora
            ),
            response = "🔔 Recordatorio diario creado: $titulo a las $hora",
            execute = {
                val eventId = saveDailyReminder(titulo, descripcion, hora)
                showCreationNotification(titulo, "Recordatorio diario a las $hora")
                scheduleDailyReminder(eventId, titulo, descripcion, hora)
            }
        )
    }

    private fun extractReminderDetails(input: String): ReminderDetails {
        val titulo = when {
            input.contains("medic", ignoreCase = true) -> "💊 Tomar Medicina"
            input.contains("comida", ignoreCase = true) -> "🍽️ Hora de Comer"
            input.contains("ejercicio", ignoreCase = true) -> "🏃 Ejercicio"
            else -> "🔔 Recordatorio: ${input.take(30)}..."
        }

        return ReminderDetails(
            titulo = titulo,
            descripcion = input,
            hora = extractTime(input) ?: "09:00"
        )
    }

    private fun extractTime(input: String): String? {
        val timeRegex = "\\b(?:2[0-3]|[01]?[0-9]):[0-5][0-9]\\b".toRegex()
        return timeRegex.find(input)?.value
    }

    private fun saveDailyReminder(titulo: String, descripcion: String, hora: String): Long {
        val event = Event(
            title = titulo,
            type = "recordatorio_diario",
            contactName = "",
            contactId = "",
            locationLat = 0.0,
            locationLng = 0.0,
            description = descripcion,
            date = "DIARIO", // Indicador de recordatorio diario
            time = hora,
            status = "Activo",
            reminder = "0" // Notificar justo a la hora
        )
        return dbHelper.addEvent(event)
    }

    private fun showCreationNotification(titulo: String, mensaje: String) {
        println("📱 NOTIFICACIÓN: $titulo - $mensaje")
    }

    private fun scheduleDailyReminder(eventId: Long, titulo: String, descripcion: String, hora: String) {
        // Programar notificación diaria usando AlarmManager
        // Por ahora solo notificación de creación
    }

    data class ReminderDetails(
        val titulo: String,
        val descripcion: String,
        val hora: String
    )
}