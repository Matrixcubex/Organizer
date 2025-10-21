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
    private val webSearchProcessor = WebSearchProcessor(context) // ← NUEVO procesador
    private val emergencyProcessor = EmergencyProcessor(context)
    private val chatProcessor = ChatProcessor()

    fun processInput(userInput: String, inputType: InputType): Action {
        Log.d("AI_DEBUG", "=== INICIANDO PROCESAMIENTO ===")
        Log.d("AI_DEBUG", "Input recibido: '$userInput'")
        Log.d("AI_DEBUG", "Tipo: $inputType")

        // Usar el reconocedor de intenciones
        val parsedCommand = intentRecognizer.recognizeIntent(userInput)

        Log.d("AI_DEBUG", "Intención detectada: ${parsedCommand.intention}")
        Log.d("AI_DEBUG", "Confianza: ${parsedCommand.confidence}")
        Log.d("AI_DEBUG", "Parámetros: ${parsedCommand.parameters}")

        val action = when (parsedCommand.intention) {
            is UserIntention.Agenda -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando AgendaProcessor")
                agendaProcessor.process(parsedCommand)
            }
            is UserIntention.Recordatorio -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando ReminderProcessor")
                reminderProcessor.process(parsedCommand)
            }
            is UserIntention.Ubicacion -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando LocationProcessor")
                locationProcessor.process(parsedCommand)
            }
            is UserIntention.Contacto -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando ContactProcessor")
                if (parsedCommand.parameters["emergencia"] == "true") {
                    Log.d("AI_DEBUG", "🚨 Es una EMERGENCIA")
                    emergencyProcessor.process(parsedCommand)
                } else {
                    Log.d("AI_DEBUG", "📞 Contacto normal")
                    contactProcessor.process(parsedCommand)
                }
            }
            is UserIntention.Busqueda -> {
                // ✅ NUEVA LÓGICA: Distinguir entre tipos de búsqueda
                val tipoBusqueda = parsedCommand.parameters["tipo"] ?: ""
                Log.d("AI_DEBUG", "🎯 Tipo de búsqueda detectado: '$tipoBusqueda'")

                when {
                    tipoBusqueda.contains("explicar") -> {
                        Log.d("AI_DEBUG", "📚 Ejecutando WebSearchProcessor para EXPLICAR")
                        webSearchProcessor.process(parsedCommand)
                    }
                    tipoBusqueda.contains("web") || tipoBusqueda.contains("internet") -> {
                        Log.d("AI_DEBUG", "🌐 Ejecutando WebSearchProcessor para BÚSQUEDA WEB")
                        webSearchProcessor.process(parsedCommand)
                    }
                    else -> {
                        Log.d("AI_DEBUG", "🔍 Ejecutando SearchProcessor (búsqueda normal)")
                        searchProcessor.process(parsedCommand)
                    }
                }
            }
            is UserIntention.ChatGeneral -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando ChatProcessor")
                chatProcessor.process(parsedCommand)
            }
            is UserIntention.Desconocido -> {
                Log.d("AI_DEBUG", "🎯 Ejecutando handleUnknown")
                handleUnknown(parsedCommand)
            }
        }

        Log.d("AI_DEBUG", "✅ Acción generada: ${action.response}")
        Log.d("AI_DEBUG", "=== FIN PROCESAMIENTO ===\n")

        return action
    }

    private fun handleUnknown(parsedCommand: ParsedCommand): Action {
        return Action(
            intention = UserIntention.Desconocido,
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