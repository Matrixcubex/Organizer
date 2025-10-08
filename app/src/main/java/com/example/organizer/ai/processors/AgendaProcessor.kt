package com.example.organizer.ai.processors

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention
import com.example.organizer.data.DatabaseHelper
import com.example.organizer.data.model.Event
import java.text.SimpleDateFormat
import java.util.*

class AgendaProcessor(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun process(parsedCommand: ParsedCommand): Action {
        val (titulo, descripcion, fecha, hora) = extractEventDetails(parsedCommand.rawText)

        return Action(
            intention = UserIntention.Agenda,
            parameters = mapOf(
                "titulo" to titulo,
                "descripcion" to descripcion,
                "fecha" to fecha,
                "hora" to hora
            ),
            response = "✅ Cita agendada: $titulo para el $fecha a las $hora",
            execute = {
                val eventId = saveEventToDatabase(titulo, descripcion, fecha, hora, "cita")
                showCreationNotification(titulo, "Cita agendada para el $fecha a las $hora")
                scheduleEventNotification(eventId, titulo, descripcion, fecha, hora)
            }
        )
    }

    private fun extractEventDetails(input: String): EventDetails {
        // Lógica mejorada de extracción
        val titulo = when {
            input.contains("cita médica", ignoreCase = true) -> "Cita Médica"
            input.contains("reunión", ignoreCase = true) -> "Reunión"
            input.contains("doctor", ignoreCase = true) -> "Consulta Médica"
            else -> "Cita: ${input.take(30)}..."
        }

        return EventDetails(
            titulo = titulo,
            descripcion = input,
            fecha = extractDate(input) ?: getDefaultDate(),
            hora = extractTime(input) ?: getDefaultTime()
        )
    }

    private fun extractDate(input: String): String? {
        val patterns = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd"
        )

        patterns.forEach { pattern ->
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                // Buscar patrones de fecha en el texto
                val regex = "\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}".toRegex()
                val match = regex.find(input)
                match?.value?.let { dateStr ->
                    sdf.parse(dateStr)
                    return dateStr
                }
            } catch (e: Exception) {
                // Continuar con el siguiente patrón
            }
        }
        return null
    }

    private fun extractTime(input: String): String? {
        val timeRegex = "\\b(?:2[0-3]|[01]?[0-9]):[0-5][0-9]\\b".toRegex()
        return timeRegex.find(input)?.value
    }

    private fun saveEventToDatabase(titulo: String, descripcion: String, fecha: String, hora: String, tipo: String): Long {
        val event = Event(
            title = titulo,
            type = tipo,
            contactName = "",
            contactId = "",
            locationLat = 0.0,
            locationLng = 0.0,
            description = descripcion,
            date = fecha,
            time = hora,
            status = "Pendiente",
            reminder = "30" // minutos antes
        )
        return dbHelper.addEvent(event)
    }

    private fun showCreationNotification(titulo: String, mensaje: String) {
        // Notificación temporal en consola por ahora
        println("📱 NOTIFICACIÓN: $titulo - $mensaje")
        // En una siguiente fase implementaremos NotificationManager
    }

    private fun scheduleEventNotification(eventId: Long, titulo: String, descripcion: String, fecha: String, hora: String) {
        // Aquí programarías la notificación para la fecha/hora de la cita
        // usando AlarmManager o WorkManager
        // Por ahora solo mostramos notificación inmediata de creación
    }

    private fun getDefaultDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getDefaultTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, 1) // 1 hora desde ahora
        return sdf.format(calendar.time)
    }

    data class EventDetails(
        val titulo: String,
        val descripcion: String,
        val fecha: String,
        val hora: String
    )
}