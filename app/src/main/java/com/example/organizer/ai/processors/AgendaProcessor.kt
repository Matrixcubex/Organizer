package com.example.organizer.ai.processors

import android.content.Context
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention
import com.example.organizer.data.DatabaseHelper
import com.example.organizer.data.model.Event
import com.example.organizer.utils.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*

class AgendaProcessor(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val notificationHelper = NotificationHelper(context)

    fun process(parsedCommand: ParsedCommand): Action {
        // Heurística 1: Logging para visibilidad del proceso
        android.util.Log.d("AGENDA_PROCESSOR", "Procesando comando: ${parsedCommand.rawText}")

        val (titulo, descripcion, fecha, hora) = extractEventDetails(parsedCommand.rawText)

        return Action(
            intention = UserIntention.Agenda,
            parameters = mapOf(
                "titulo" to titulo,
                "descripcion" to descripcion,
                "fecha" to fecha,
                "hora" to hora
            ),
            response = "✅ Cita agendada: **$titulo**\n📅 Fecha: $fecha\n⏰ Hora: $hora\n📱 Recibirás una notificación 30 minutos antes.",
            execute = {
                try {
                    val eventId = saveEventToDatabase(titulo, descripcion, fecha, hora, "cita")
                    // Programar notificación REAL
                    val event = Event(
                        id = eventId,
                        title = titulo,
                        type = "cita",
                        contactName = "",
                        contactId = "",
                        locationLat = 0.0,
                        locationLng = 0.0,
                        description = descripcion,
                        date = fecha,
                        time = hora,
                        status = "Pendiente",
                        reminder = "30" // 30 minutos antes
                    )
                    notificationHelper.scheduleNotification(event)

                    // Heurística 1: Confirmación adicional
                    android.util.Log.d("AGENDA_PROCESSOR", "Evento guardado con ID: $eventId")
                } catch (e: Exception) {
                    // Heurística 9: Manejo de errores claro
                    android.util.Log.e("AGENDA_PROCESSOR", "Error al guardar evento: ${e.message}")
                }
            }
        )
    }

    private fun extractEventDetails(input: String): EventDetails {
        val lowerInput = input.lowercase()

        // Heurística 2: Compatibilidad sistema-mundo real - Detectar lenguaje natural
        val fecha = when {
            lowerInput.contains("mañana") -> getTomorrowDate()
            lowerInput.contains("pasado mañana") -> getDayAfterTomorrow()
            lowerInput.contains("próximo lunes") -> getNextWeekday(Calendar.MONDAY)
            lowerInput.contains("próximo martes") -> getNextWeekday(Calendar.TUESDAY)
            lowerInput.contains("próximo miércoles") -> getNextWeekday(Calendar.WEDNESDAY)
            lowerInput.contains("próximo jueves") -> getNextWeekday(Calendar.THURSDAY)
            lowerInput.contains("próximo viernes") -> getNextWeekday(Calendar.FRIDAY)
            lowerInput.contains("próximo sábado") -> getNextWeekday(Calendar.SATURDAY)
            lowerInput.contains("próximo domingo") -> getNextWeekday(Calendar.SUNDAY)
            lowerInput.contains("hoy") -> getTodayDate()
            else -> extractDate(input) ?: getDefaultDate()
        }

        val hora = when {
            lowerInput.contains("mañana") -> "09:00"
            lowerInput.contains("mediodía") || lowerInput.contains("medio día") -> "12:00"
            lowerInput.contains("tarde") && lowerInput.contains("temprano") -> "15:00"
            lowerInput.contains("tarde") -> "17:00"
            lowerInput.contains("noche") -> "20:00"
            lowerInput.contains("medianoche") -> "00:00"
            else -> extractTime(input) ?: getDefaultTime()
        }

        val titulo = extractNaturalTitle(input)
        val descripcion = buildNaturalDescription(input, titulo, fecha, hora)

        return EventDetails(titulo, descripcion, fecha, hora)
    }

    private fun extractNaturalTitle(input: String): String {
        return when {
            input.contains("cita médica", ignoreCase = true) -> "👨‍⚕️ Cita Médica"
            input.contains("dentista", ignoreCase = true) -> "🦷 Cita con el Dentista"
            input.contains("reunión", ignoreCase = true) -> "👥 Reunión"
            input.contains("cumpleaños", ignoreCase = true) -> "🎂 Cumpleaños"
            input.contains("aniversario", ignoreCase = true) -> "💑 Aniversario"
            input.contains("fiesta", ignoreCase = true) -> "🎉 Fiesta"
            input.contains("conferencia", ignoreCase = true) -> "📊 Conferencia"
            input.contains("entrevista", ignoreCase = true) -> "💼 Entrevista"
            input.contains("examen", ignoreCase = true) -> "📝 Examen"
            input.contains("vacaciones", ignoreCase = true) -> "🌴 Vacaciones"
            input.contains("doctor", ignoreCase = true) -> "👨‍⚕️ Consulta Médica"
            input.contains("médico", ignoreCase = true) -> "👨‍⚕️ Consulta Médica"
            input.contains("hospital", ignoreCase = true) -> "🏥 Visita al Hospital"
            else -> "📅 Evento: ${input.take(30)}..."
        }
    }

    private fun buildNaturalDescription(input: String, titulo: String, fecha: String, hora: String): String {
        val descripcion = StringBuilder()
        descripcion.append("Evento creado por comando de voz: \"$input\"\n")
        descripcion.append("Agendado para el $fecha a las $hora")

        // Extraer detalles adicionales del input
        val detalles = input.replace(titulo, "").trim()
        if (detalles.isNotEmpty() && detalles.length > 10) {
            descripcion.append("\nDetalles: $detalles")
        }

        return descripcion.toString()
    }

    private fun extractDate(input: String): String? {
        val patterns = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "dd/MM/yy", "dd-MM-yy"
        )

        patterns.forEach { pattern ->
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                val regex = when (pattern) {
                    "dd/MM/yyyy" -> "\\d{1,2}/\\d{1,2}/\\d{4}".toRegex()
                    "dd-MM-yyyy" -> "\\d{1,2}-\\d{1,2}-\\d{4}".toRegex()
                    "yyyy-MM-dd" -> "\\d{4}-\\d{1,2}-\\d{1,2}".toRegex()
                    "dd/MM/yy" -> "\\d{1,2}/\\d{1,2}/\\d{2}".toRegex()
                    "dd-MM-yy" -> "\\d{1,2}-\\d{1,2}-\\d{2}".toRegex()
                    else -> null
                }
                regex?.find(input)?.value?.let { dateStr ->
                    sdf.parse(dateStr)
                    // Convertir al formato estándar
                    val standardFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    return standardFormat.format(sdf.parse(dateStr))
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

    private fun getDefaultDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getDefaultTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, 1) // 1 hora desde ahora
        return sdf.format(calendar.time)
    }

    private fun getTomorrowDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return sdf.format(calendar.time)
    }

    private fun getDayAfterTomorrow(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 2)
        return sdf.format(calendar.time)
    }

    private fun getNextWeekday(weekday: Int): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, weekday)
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return sdf.format(calendar.time)
    }

    data class EventDetails(
        val titulo: String,
        val descripcion: String,
        val fecha: String,
        val hora: String
    )
}