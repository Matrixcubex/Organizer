package com.example.organizer.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.organizer.UbicacionActivity
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.UserIntention
import com.example.organizer.data.DatabaseHelper
import com.example.organizer.data.model.Event
import com.example.organizer.utils.NotificationHelper
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class CentralAIService(private val context: Context) {

    private val aiClient = GeminiAIClient(context)
    private val dbHelper = DatabaseHelper(context)
    private val notificationHelper = NotificationHelper(context)

    enum class InputType {
        TEXT, VOICE
    }

    fun processInput(userInput: String, inputType: InputType): Action {
        Log.d("AI_DEBUG", "=== PROCESANDO CON IA ===")
        Log.d("AI_DEBUG", "Input: '$userInput'")

        return runBlocking {
            try {
                // ✅ OBTENER RESPUESTA CON CLAVE DE LA IA
                val aiResponse = aiClient.processUserInput(userInput)
                Log.d("AI_DEBUG", "🤖 Respuesta IA: $aiResponse")

                // ✅ PROCESAR SEGÚN LA CLAVE
                return@runBlocking processByKey(aiResponse, userInput)

            } catch (e: Exception) {
                Log.e("AI_DEBUG", "Error con IA: ${e.message}")
                // Fallback a respuesta general
                Action(
                    intention = UserIntention.ChatGeneral,
                    parameters = emptyMap(),
                    response = "⚠️ Error de conexión. Intenta nuevamente.",
                    execute = null
                )
            }
        }
    }

    // ✅ PROCESAR SEGÚN LA CLAVE DEVUELTA POR LA IA
    private fun processByKey(aiResponse: String, originalInput: String): Action {
        val parts = aiResponse.split(":", limit = 2)

        if (parts.size != 2) {
            // Si no viene en formato clave:valor, tratar como respuesta general
            return Action(
                intention = UserIntention.ChatGeneral,
                parameters = emptyMap(),
                response = aiResponse,
                execute = null
            )
        }

        val key = parts[0].trim().lowercase()
        val data = parts[1].trim()

        Log.d("AI_KEY", "Clave: '$key', Datos: '$data'")

        return when (key) {
            "cita" -> processAppointment(data, originalInput)
            "recordatorio" -> processReminder(data, originalInput)
            "contactos" -> processContact(data, originalInput)
            "maps" -> processMaps(data, originalInput)
            "emergencia" -> processEmergency(data, originalInput)
            "internet" -> processInternet(data, originalInput)
            "response" -> processResponse(data, originalInput)
            else -> processResponse(aiResponse, originalInput) // Fallback
        }
    }

    // ✅ PROCESAR CITA - IMPLEMENTACIÓN DIRECTA
    private fun processAppointment(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "📅 Procesando cita: $data")

        val (titulo, descripcion, fecha, hora) = extractEventDetails(originalInput)

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
                    Log.d("AGENDA_PROCESSOR", "Evento guardado con ID: $eventId")

                } catch (e: Exception) {
                    Log.e("AGENDA_PROCESSOR", "Error al guardar evento: ${e.message}")
                    Toast.makeText(context, "Error al guardar evento", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // ✅ PROCESAR RECORDATORIO - IMPLEMENTACIÓN DIRECTA
    private fun processReminder(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "🔔 Procesando recordatorio: $data")

        val (titulo, descripcion, hora) = extractReminderDetails(originalInput)

        return Action(
            intention = UserIntention.Recordatorio,
            parameters = mapOf(
                "titulo" to titulo,
                "descripcion" to descripcion,
                "hora" to hora
            ),
            response = "🔔 Recordatorio diario creado: $titulo a las $hora\n📱 Recibirás una notificación todos los días a esta hora.",
            execute = {
                try {
                    val eventId = saveDailyReminder(titulo, descripcion, hora)

                    val event = Event(
                        id = eventId,
                        title = titulo,
                        type = "recordatorio",
                        contactName = "",
                        contactId = "",
                        locationLat = 0.0,
                        locationLng = 0.0,
                        description = descripcion,
                        date = "DIARIO",
                        time = hora,
                        status = "Activo",
                        reminder = "0" // Notificar justo a la hora
                    )

                    notificationHelper.scheduleDailyReminder(event)
                    Log.d("REMINDER_PROCESSOR", "Recordatorio guardado con ID: $eventId")

                } catch (e: Exception) {
                    Log.e("REMINDER_PROCESSOR", "Error al guardar recordatorio: ${e.message}")
                    Toast.makeText(context, "Error al guardar recordatorio", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // ✅ MÉTODOS DE EXTRACCIÓN PARA EVENTOS
    private fun extractEventDetails(input: String): EventDetails {
        val lowerInput = input.lowercase()

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

    // ✅ MÉTODOS DE EXTRACCIÓN PARA RECORDATORIOS
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

    // ✅ MÉTODOS AUXILIARES DE EXTRACCIÓN
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

    // ✅ MÉTODOS DE GUARDADO EN BD
    private fun saveEventToDatabase(titulo: String, descripcion: String, fecha: String, hora: String, tipo: String): Long {
        val event = Event(
            title = titulo,
            type = "cita", // ✅ TIPO FIJO PARA EVENTOS
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

    private fun saveDailyReminder(titulo: String, descripcion: String, hora: String): Long {
        val event = Event(
            title = titulo,
            type = "recordatorio", // ✅ TIPO FIJO PARA RECORDATORIOS
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

    // ✅ MÉTODOS DE FECHA/HORA
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
        calendar.add(Calendar.HOUR, 1)
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

    // ✅ OTRAS ACCIONES (mantener igual)
    private fun processContact(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "📞 Procesando contacto: $data")

        // Extraer solo el nombre del contacto (eliminar "Llamar a ")
        val nombreContacto = data.replace("Llamar a ", "").trim()

        return Action(
            intention = UserIntention.Contacto,
            parameters = mapOf("contacto" to nombreContacto),
            response = "📞 Contactando: $nombreContacto",
            execute = {
                // Pasar el nombre del contacto a makePhoneCall
                makePhoneCall(nombreContacto)
            }
        )
    }

    private fun processMaps(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "🗺️ Procesando maps: $data")
        return Action(
            intention = UserIntention.Ubicacion,
            parameters = mapOf("direccion" to data),
            response = "🗺️ Calculando ruta a: $data",
            execute = { openMapActivity(data) }
        )
    }

    private fun processEmergency(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "🚨 Procesando emergencia: $data")
        return Action(
            intention = UserIntention.Contacto,
            parameters = mapOf("emergencia" to "true"),
            response = "🚨 Activando protocolo de emergencia: $data",
            execute = { callEmergency() }
        )
    }

    private fun processInternet(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "🌐 Procesando internet: $data")
        return Action(
            intention = UserIntention.Busqueda,
            parameters = mapOf("query" to data),
            response = "🔍 Buscando en internet: $data",
            execute = { performWebSearch(data) }
        )
    }

    private fun processResponse(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "💬 Procesando respuesta general: $data")
        return Action(
            intention = UserIntention.ChatGeneral,
            parameters = emptyMap(),
            response = data,
            execute = null
        )
    }

    // ✅ ACCIONES DE EJECUCIÓN
    private fun performWebSearch(query: String) {
        try {
            val cleanQuery = query
                .replace(Regex("(?i)buscar|en internet|en la web"), "")
                .trim()

            Log.d("WEB_SEARCH", "Buscando: '$cleanQuery'")

            // Intentar abrir WebView interno
            val intent = Intent(context, com.example.organizer.WebSearchActivity::class.java).apply {
                putExtra("QUERY", cleanQuery)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

        } catch (e: Exception) {
            Log.e("WEB_SEARCH", "Error: ${e.message}", e)
            Toast.makeText(context, "Error al buscar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMapActivity(destination: String) {
        try {
            val intent = Intent(context, UbicacionActivity::class.java).apply {
                putExtra("DESTINO", destination)
                putExtra("TIPO", "navegacion")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MAP_ACTION", "Error: ${e.message}")
            Toast.makeText(context, "Error al abrir el mapa", Toast.LENGTH_SHORT).show()
        }
    }

    private fun callEmergency() {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("EMERGENCY", "Error: ${e.message}")
            Toast.makeText(context, "Error al llamar emergencia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makePhoneCall(nombreContacto: String) {
        try {
            // Primero verificar permisos
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(context, "Se necesita permiso para llamar", Toast.LENGTH_SHORT).show()
                return
            }

            // Obtener el número del contacto (aquí necesitarías buscar en contactos)
            // Por ahora, como ejemplo, usar un número fijo o buscar
            val numeroTelefono = "123456789" // Esto debería venir de la búsqueda de contactos

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$numeroTelefono") // ← ¡ESTO FALTA!
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No hay aplicación para llamar", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("PHONE_CALL", "Error: ${e.message}")
            Toast.makeText(context, "Error al realizar llamada", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ DATA CLASSES INTERNAS
    data class EventDetails(
        val titulo: String,
        val descripcion: String,
        val fecha: String,
        val hora: String
    )

    data class ReminderDetails(
        val titulo: String,
        val descripcion: String,
        val hora: String
    )
}