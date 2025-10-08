package com.example.organizer.ai.recognition

import android.util.Log
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention

class SimpleIntentRecognizer {

    fun recognizeIntent(userInput: String): ParsedCommand {
        val lowerInput = userInput.lowercase()

        Log.d("INTENT_DEBUG", "🔄 Analizando input: '$userInput'")

        val result = when {
            // AGENDA - Múltiples patrones
            containsAny(lowerInput, listOf("agendar", "cita", "programar", "reunión", "evento", "calendario", "doctor", "médico", "hospital")) -> {
                Log.d("INTENT_DEBUG", "✅ Detectado: AGENDA")
                ParsedCommand(
                    intention = UserIntention.Agenda,
                    confidence = 0.9f,
                    parameters = mapOf(
                        "descripcion" to userInput,
                        "tipo" to "cita"
                    ),
                    rawText = userInput
                )
            }

            // RECORDATORIO - Múltiples patrones
            containsAny(lowerInput, listOf("recordatorio", "recordar", "aviso", "notificación", "alarma", "recordarme", "avisarme")) -> {
                Log.d("INTENT_DEBUG", "✅ Detectado: RECORDATORIO")
                ParsedCommand(
                    intention = UserIntention.Recordatorio,
                    confidence = 0.9f,
                    parameters = mapOf(
                        "mensaje" to userInput,
                        "tipo" to "recordatorio"
                    ),
                    rawText = userInput
                )
            }

            // UBICACIÓN
            containsAny(lowerInput, listOf("ubicación", "ubicacion", "dónde", "donde", "mapa", "cómo llegar", "como llegar", "dirección", "direccion", "lugar", "sitio")) -> {
                Log.d("INTENT_DEBUG", "✅ Detectado: UBICACIÓN")
                ParsedCommand(
                    intention = UserIntention.Ubicacion,
                    confidence = 0.8f,
                    parameters = mapOf("direccion" to userInput),
                    rawText = userInput
                )
            }

            // BÚSQUEDA
            containsAny(lowerInput, listOf("buscar", "encontrar", "información", "informacion", "qué es", "que es", "quién es", "quien es", "muestra", "enseña", "dime sobre")) -> {
                Log.d("INTENT_DEBUG", "✅ Detectado: BÚSQUEDA")
                ParsedCommand(
                    intention = UserIntention.Busqueda,
                    confidence = 0.8f,
                    parameters = mapOf("query" to userInput),
                    rawText = userInput
                )
            }

            // EMERGENCIA específica (debe ir ANTES de contacto normal)
            containsAny(lowerInput, listOf("emergencia", "ayuda", "socorro", "urgencia", "accidente", "911", "peligro", "auxilio")) -> {
                Log.d("INTENT_DEBUG", "✅ Detectado: EMERGENCIA")
                ParsedCommand(
                    intention = UserIntention.Contacto,
                    confidence = 0.95f,
                    parameters = mapOf(
                        "emergencia" to "true",
                        "contacto" to "emergencia",
                        "tipo" to "emergencia"
                    ),
                    rawText = userInput
                )
            }

            // CONTACTO normal
            containsAny(lowerInput, listOf("llamar", "contactar", "teléfono", "telefono", "número", "numero", "marcar", "hablar con")) -> {
                Log.d("INTENT_DEBUG", "✅ Detectado: CONTACTO")
                ParsedCommand(
                    intention = UserIntention.Contacto,
                    confidence = 0.8f,
                    parameters = mapOf(
                        "contacto" to "familiar",
                        "tipo" to "contacto_normal"
                    ),
                    rawText = userInput
                )
            }

            // CHAT GENERAL (fallback)
            else -> {
                Log.d("INTENT_DEBUG", "❓ No detectado - Usando CHAT GENERAL")
                ParsedCommand(
                    intention = UserIntention.ChatGeneral,
                    confidence = 0.7f,
                    rawText = userInput
                )
            }
        }

        Log.d("INTENT_DEBUG", "🎯 Resultado final: ${result.intention}")
        return result
    }

    private fun containsAny(input: String, keywords: List<String>): Boolean {
        keywords.forEach { keyword ->
            if (input.contains(keyword, ignoreCase = true)) {
                Log.d("INTENT_DEBUG", "   📍 Keyword encontrada: '$keyword'")
                return true
            }
        }
        return false
    }
}