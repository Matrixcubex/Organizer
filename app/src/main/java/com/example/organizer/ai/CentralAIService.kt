package com.example.organizer.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.organizer.UbicacionActivity
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.UserIntention
import kotlinx.coroutines.runBlocking
class CentralAIService(private val context: Context) {

    private val aiClient = GeminiAIClient(context)

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

    // ✅ PROCESAR CITA
    private fun processAppointment(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "📅 Procesando cita: $data")
        return Action(
            intention = UserIntention.Agenda,
            parameters = mapOf(
                "descripcion" to data,
                "tipo" to "cita"
            ),
            response = "✅ Cita agendada: $data",
            execute = {
                Toast.makeText(context, "Cita agendada: $data", Toast.LENGTH_LONG).show()
                // Aquí podrías integrar con tu AgendaProcessor existente
            }
        )
    }

    // ✅ PROCESAR RECORDATORIO
    private fun processReminder(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "🔔 Procesando recordatorio: $data")
        return Action(
            intention = UserIntention.Recordatorio,
            parameters = mapOf(
                "mensaje" to data,
                "tipo" to "recordatorio"
            ),
            response = "⏰ Recordatorio configurado: $data",
            execute = {
                Toast.makeText(context, "Recordatorio: $data", Toast.LENGTH_LONG).show()
                // Aquí podrías integrar con tu ReminderProcessor existente
            }
        )
    }

    // ✅ PROCESAR CONTACTOS
    private fun processContact(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "📞 Procesando contacto: $data")
        return Action(
            intention = UserIntention.Contacto,
            parameters = mapOf(
                "contacto" to data,
                "tipo" to "contacto_normal"
            ),
            response = "📞 Contactando: $data",
            execute = {
                makePhoneCall()
            }
        )
    }

    // ✅ PROCESAR MAPAS
    private fun processMaps(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "🗺️ Procesando maps: $data")
        return Action(
            intention = UserIntention.Ubicacion,
            parameters = mapOf(
                "direccion" to data,
                "tipo" to "navegacion"
            ),
            response = "🗺️ Calculando ruta a: $data",
            execute = {
                openMapActivity(data)
            }
        )
    }

    // ✅ PROCESAR EMERGENCIA
    private fun processEmergency(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "🚨 Procesando emergencia: $data")
        return Action(
            intention = UserIntention.Contacto,
            parameters = mapOf(
                "emergencia" to "true",
                "contacto" to "emergencia"
            ),
            response = "🚨 Activando protocolo de emergencia: $data",
            execute = {
                callEmergency()
            }
        )
    }

    // ✅ PROCESAR BÚSQUEDA EN INTERNET
    private fun processInternet(data: String, originalInput: String): Action {
        Log.d("AI_ACTION", "🌐 Procesando internet: $data")
        return Action(
            intention = UserIntention.Busqueda,
            parameters = mapOf(
                "query" to data,
                "tipo" to "busqueda_web"
            ),
            response = "🔍 Buscando en internet: $data",
            execute = {
                performWebSearch(data)
            }
        )
    }

    // ✅ PROCESAR RESPUESTA GENERAL
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

            val searchUrl = "https://www.google.com/search?q=${Uri.encode(cleanQuery)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("WEB_SEARCH", "Error: ${e.message}")
            Toast.makeText(context, "Error al buscar en internet", Toast.LENGTH_SHORT).show()
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

    private fun makePhoneCall() {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PHONE_CALL", "Error: ${e.message}")
            Toast.makeText(context, "Error al realizar llamada", Toast.LENGTH_SHORT).show()
        }
    }
}