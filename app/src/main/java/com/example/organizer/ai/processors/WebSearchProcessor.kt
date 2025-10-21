// Crear nuevo archivo: WebSearchProcessor.kt
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
        val tipoBusqueda = parsedCommand.parameters["tipo"] ?: "busqueda"

        return Action(
            intention = UserIntention.Busqueda,
            parameters = parsedCommand.parameters,
            response = when (tipoBusqueda) {
                "explicar" -> "🔍 Buscando información sobre: $query\n📚 Preparando explicación..."
                else -> "🌐 Buscando en internet: $query\n📱 Abriendo navegador..."
            },
            execute = {
                when (tipoBusqueda) {
                    "explicar" -> buscarYExplicar(query)
                    else -> buscarEnInternet(query)
                }
            }
        )
    }

    private fun buscarEnInternet(query: String) {
        val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun buscarYExplicar(query: String) {
        // Por ahora, redirigir a búsqueda normal
        // En la siguiente fase, integraremos IA real aquí
        val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)

        // TODO: Integrar con API de IA para generar explicaciones
        // Esto se implementará cuando agreguemos la IA real
    }
}