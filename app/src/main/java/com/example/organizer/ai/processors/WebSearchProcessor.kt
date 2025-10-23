// WebSearchProcessor.kt - ACTUALIZAR para manejar búsquedas en internet
package com.example.organizer.ai.processors

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention

class WebSearchProcessor(private val context: Context) {

    fun process(parsedCommand: ParsedCommand): Action {
        val query = parsedCommand.parameters["query"] ?: parsedCommand.rawText

        return Action(
            intention = UserIntention.Busqueda,
            parameters = parsedCommand.parameters,
            response = "🔍 Buscando en internet: $query",
            execute = {
                performWebSearch(query)
            }
        )
    }

    // ✅ NUEVO MÉTODO: Manejar búsquedas desde el chatbot general
    fun processInternetSearch(query: String): Action {
        return Action(
            intention = UserIntention.Busqueda,
            parameters = mapOf("query" to query, "tipo" to "internet"),
            response = "🌐 Abriendo navegador para buscar: $query",
            execute = {
                performWebSearch(query)
            }
        )
    }

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

            val searchUrl = "https://www.google.com/search?q=${Uri.encode(cleanQuery)}"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Verificar si hay apps que puedan manejar la intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback: abrir en navegador por defecto
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}