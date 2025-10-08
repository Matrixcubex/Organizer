package com.example.organizer.ai.processors

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention
import java.net.URLEncoder

class SearchProcessor(private val context: Context) {

    fun process(parsedCommand: ParsedCommand): Action {
        val query = extractSearchQuery(parsedCommand.rawText)

        return Action(
            intention = UserIntention.Busqueda,
            parameters = mapOf("query" to query),
            response = "🔍 Buscando: $query",
            execute = {
                performSearch(query)
            }
        )
    }

    private fun extractSearchQuery(input: String): String {
        val searchKeywords = listOf("buscar", "encuentra", "muestra", "qué es", "quien es")
        val query = input.replace(Regex("(?i)${searchKeywords.joinToString("|")}"), "").trim()
        return if (query.length > 100) query.take(100) + "..." else query
    }

    private fun performSearch(query: String) {
        val searchUrl = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}