// ChatProcessor.kt COMPLETO CORREGIDO:
package com.example.organizer.ai.processors

import android.content.Context
import android.util.Log // ← AGREGAR ESTE IMPORT
import com.example.organizer.ai.GeminiAIClient
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention
import kotlinx.coroutines.runBlocking

class ChatProcessor(private val context: Context) {

    private val aiClient = GeminiAIClient(context)

    fun process(parsedCommand: ParsedCommand): Action {
        val respuesta = generateResponseWithAI(parsedCommand.rawText)

        // ✅ VERIFICAR si la respuesta contiene comando de búsqueda en internet
        if (respuesta.contains("INTERNET_SEARCH:")) {
            val query = respuesta.replace("INTERNET_SEARCH:", "").trim()
            Log.d("CHAT_DEBUG", "🔍 Detectada búsqueda en internet: $query")

            return Action(
                intention = UserIntention.Busqueda,
                parameters = mapOf("query" to query),
                response = "🌐 Buscando en internet: $query",
                execute = {
                    // Usar WebSearchProcessor para abrir el navegador
                    performWebSearch(query)
                }
            )
        }

        return Action(
            intention = UserIntention.ChatGeneral,
            parameters = emptyMap(),
            response = respuesta,
            execute = null
        )
    }

    // ✅ NUEVO MÉTODO: Realizar búsqueda en internet (copiado de WebSearchProcessor)
    private fun performWebSearch(query: String) {
        try {
            // Limpiar y codificar la query para URL
            val cleanQuery = query
                .replace("INTERNET_SEARCH:", "")
                .replace("buscar", "")
                .replace("en internet", "")
                .replace("video de", "")
                .replace("vídeo de", "")
                .trim()

            val searchUrl = "https://www.google.com/search?q=${android.net.Uri.encode(cleanQuery)}"

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(searchUrl)).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Verificar si hay apps que puedan manejar la intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback: abrir en navegador por defecto
                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(searchUrl))
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateResponseWithAI(input: String): String {
        return runBlocking {
            try {
                aiClient.processUserInput(input)
            } catch (e: Exception) {
                // Fallback a respuestas básicas si Gemini falla
                generateFallbackResponse(input)
            }
        }
    }

    private fun generateFallbackResponse(input: String): String {
        return when {
            input.contains("hola", ignoreCase = true) -> "¡Hola! ¿En qué puedo ayudarte?"
            input.contains("cómo estás", ignoreCase = true) -> "¡Estoy bien, gracias! Listo para ayudarte"
            input.contains("gracias", ignoreCase = true) -> "¡De nada! ¿Algo más en lo que pueda ayudarte?"
            input.contains("adiós", ignoreCase = true) -> "¡Hasta luego! Que tengas un buen día"
            else -> "Entendido. ¿Necesitas ayuda con agenda, recordatorios, ubicación, contactos o búsquedas?"
        }
    }
}