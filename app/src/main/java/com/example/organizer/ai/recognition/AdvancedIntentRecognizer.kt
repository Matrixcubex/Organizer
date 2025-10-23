// AdvancedIntentRecognizer.kt - VERSIÓN GEMINI
package com.example.organizer.ai.recognition

import android.content.Context
import android.util.Log
import com.example.organizer.ai.GeminiAIClient
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention
import kotlinx.coroutines.runBlocking

class AdvancedIntentRecognizer(private val context: Context) {

    private val aiClient = GeminiAIClient(context)
    private val fallbackRecognizer = SimpleIntentRecognizer()

    fun recognizeIntentWithAI(userInput: String): ParsedCommand {
        return runBlocking {
            try {
                // Usar Gemini para clasificación más inteligente
                val aiResponse = aiClient.processUserInput("""
                    Clasifica esta solicitud en EXACTAMENTE UNA de estas categorías:
                    AGENDA, RECORDATORIO, UBICACION, CONTACTO, BUSQUEDA, EMERGENCIA, CHAT
                    
                    Reglas:
                    - AGENDA: para programar eventos, citas, reuniones
                    - RECORDATORIO: para alarmas, recordatorios, avisos  
                    - UBICACION: para mapas, direcciones, rutas, ubicaciones
                    - CONTACTO: para llamadas, contactos, teléfonos
                    - BUSQUEDA: para buscar información, explicar temas
                    - EMERGENCIA: para urgencias, ayuda, emergencias
                    - CHAT: para conversación general, preguntas
                    
                    Solicitud: "$userInput"
                    
                    Responde SOLO con la palabra de la categoría en MAYÚSCULAS.
                """.trimIndent())

                Log.d("GEMINI_INTENT", "Categoría detectada: $aiResponse")

                val cleanResponse = aiResponse.trim().uppercase()
                val intention = mapResponseToIntention(cleanResponse)

                ParsedCommand(
                    intention = intention,
                    confidence = 0.92f,
                    parameters = extractParameters(userInput, intention),
                    rawText = userInput
                )

            } catch (e: Exception) {
                Log.e("GEMINI_INTENT", "Error con Gemini, usando fallback", e)
                // Fallback al reconocedor simple existente
                fallbackRecognizer.recognizeIntent(userInput)
            }
        }
    }

    private fun mapResponseToIntention(response: String): UserIntention {
        return when {
            response.contains("AGENDA") -> UserIntention.Agenda
            response.contains("RECORDATORIO") -> UserIntention.Recordatorio
            response.contains("UBICACION") -> UserIntention.Ubicacion
            response.contains("CONTACTO") -> UserIntention.Contacto
            response.contains("BUSQUEDA") -> UserIntention.Busqueda
            response.contains("EMERGENCIA") -> UserIntention.Contacto
            else -> UserIntention.ChatGeneral
        }
    }

    private fun extractParameters(input: String, intention: UserIntention): Map<String, String> {
        // Puedes mejorar esto usando Gemini también para extracción específica
        return when (intention) {
            UserIntention.Ubicacion -> mapOf("direccion" to extractLocation(input))
            UserIntention.Contacto -> mapOf("contacto" to extractContact(input))
            UserIntention.Busqueda -> mapOf("query" to extractSearchQuery(input))
            else -> mapOf("descripcion" to input)
        }
    }

    private fun extractLocation(input: String): String {
        return input.replace(Regex("(?i)mapa|ubicación|ubicacion|dónde|donde|cómo llegar|como llegar"), "").trim()
    }

    private fun extractContact(input: String): String {
        return input.replace(Regex("(?i)llamar|contactar|teléfono|telefono|número|numero"), "").trim()
    }

    private fun extractSearchQuery(input: String): String {
        return input.replace(Regex("(?i)buscar|encontrar|información|informacion|qué es|que es|explicar"), "").trim()
    }
}