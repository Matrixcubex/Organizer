// REEMPLAZAR todo el archivo CentralAIService.kt con:
package com.example.organizer.ai

import android.content.Context
import android.util.Log
import com.example.organizer.ai.models.*
import com.example.organizer.ai.processors.*
import com.example.organizer.ai.recognition.SimpleIntentRecognizer

class CentralAIService(private val context: Context) {

    private val intentRecognizer = SimpleIntentRecognizer()
    private val agendaProcessor = AgendaProcessor(context)
    private val reminderProcessor = ReminderProcessor(context)
    private val locationProcessor = LocationProcessor(context)
    private val contactProcessor = ContactProcessor(context)
    private val searchProcessor = SearchProcessor(context)
    private val webSearchProcessor = WebSearchProcessor(context)
    private val emergencyProcessor = EmergencyProcessor(context)
    private val chatProcessor = ChatProcessor(context)

    // ENUM simple para tipo de input
    enum class InputType {
        TEXT, VOICE
    }
    private fun isInternetSearchRequest(input: String): Boolean {
        val searchPatterns = listOf(
            "buscar en internet",
            "buscar en la web",
            "buscar video",
            "buscar vídeo",
            "ver video",
            "ver vídeo",
            "youtube",
            "navegador",
            "chrome",
            "internet"
        )
        return searchPatterns.any { input.contains(it, ignoreCase = true) }
    }
    fun processInput(userInput: String, inputType: InputType): Action {
        Log.d("AI_DEBUG", "=== INICIANDO PROCESAMIENTO ===")
        Log.d("AI_DEBUG", "Input recibido: '$userInput'")

        // ✅ PRIMERO: Verificar si es una búsqueda directa en internet
        if (isInternetSearchRequest(userInput)) {
            Log.d("AI_DEBUG", "🎯 Detectada búsqueda directa en internet")
            val query = extractSearchQuery(userInput)
            return webSearchProcessor.processInternetSearch(query)
        }

        // ✅ SEGUNDO: Usar el reconocedor de intenciones para otros casos
        val parsedCommand = intentRecognizer.recognizeIntent(userInput) // ← CORRECTO (usa Simple por ahora)


        Log.d("AI_DEBUG", "Intención detectada: ${parsedCommand.intention}")
        Log.d("AI_DEBUG", "Confianza: ${parsedCommand.confidence}")

        val action = when (parsedCommand.intention) {
            UserIntention.Agenda -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando AgendaProcessor")
                agendaProcessor.process(parsedCommand)
            }
            UserIntention.Recordatorio -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando ReminderProcessor")
                reminderProcessor.process(parsedCommand)
            }
            UserIntention.Ubicacion -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando LocationProcessor")
                locationProcessor.process(parsedCommand)
            }
            UserIntention.Contacto -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando ContactProcessor")
                if (parsedCommand.parameters["emergencia"] == "true") {
                    Log.d("AI_DEBUG", "🚨 Es una EMERGENCIA")
                    emergencyProcessor.process(parsedCommand)
                } else {
                    Log.d("AI_DEBUG", "📞 Contacto normal")
                    contactProcessor.process(parsedCommand)
                }
            }
            UserIntention.Busqueda -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando WebSearchProcessor")
                webSearchProcessor.process(parsedCommand)
            }
            UserIntention.ChatGeneral -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando ChatProcessor")
                // ✅ El ChatProcessor ahora manejará tanto conversación normal como búsquedas en internet
                chatProcessor.process(parsedCommand)
            }
            else -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando handleUnknown")
                handleUnknown(parsedCommand)
            }
        }

        Log.d("AI_DEBUG", "✅ Acción generada: ${action.response}")
        Log.d("AI_DEBUG", "=== FIN PROCESAMIENTO ===\n")

        return action
    }
    private fun extractSearchQuery(input: String): String {
        return input.replace(Regex("(?i)buscar|en internet|en la web|video|vídeo|ver|youtube|navegador|chrome"), "").trim()
    }

    private fun handleUnknown(parsedCommand: ParsedCommand): Action {
        return Action(
            intention = UserIntention.ChatGeneral,
            parameters = emptyMap(),
            response = "No estoy seguro de qué necesitas. ¿Puedes ser más específico?\n\n" +
                    "Puedo ayudarte con:\n" +
                    "• Agendar citas 📅\n" +
                    "• Recordatorios 🔔\n" +
                    "• Buscar información en internet 🔍\n" +
                    "• Explicar temas 📚\n" +
                    "• Ubicaciones y rutas 🗺️\n" +
                    "• Llamadas 📞\n" +
                    "• Emergencias 🚨"
        )
    }
}