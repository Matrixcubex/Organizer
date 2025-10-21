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
                val destino = extractDestination(userInput)
                Log.d("INTENT_DEBUG", "✅ Detectado: UBICACIÓN - Destino: '$destino'")
                ParsedCommand(
                    intention = UserIntention.Ubicacion,
                    confidence = 0.8f,
                    parameters = mapOf(
                        "direccion" to destino,
                        "tipo" to "navegacion"
                    ),
                    rawText = userInput
                )
            }

            // BÚSQUEDA EN INTERNET
            containsAny(lowerInput, listOf("buscar en internet", "buscar en web", "buscar online", "investigar en internet")) -> {
                val query = extractSearchQuery(userInput)
                Log.d("INTENT_DEBUG", "✅ Detectado: BÚSQUEDA WEB - Query: '$query'")
                ParsedCommand(
                    intention = UserIntention.Busqueda,
                    confidence = 0.9f,
                    parameters = mapOf(
                        "query" to query,
                        "tipo" to "busqueda_web"
                    ),
                    rawText = userInput
                )
            }

            // EXPLICAR TEMA (buscar + explicar)
            containsAny(lowerInput, listOf("explicar", "qué es", "que es", "quién es", "quien es", "cómo funciona", "como funciona", "dime sobre")) -> {
                val query = extractSearchQuery(userInput)
                Log.d("INTENT_DEBUG", "✅ Detectado: EXPLICAR TEMA - Query: '$query'")
                ParsedCommand(
                    intention = UserIntention.Busqueda,
                    confidence = 0.9f,
                    parameters = mapOf(
                        "query" to query,
                        "tipo" to "explicar"
                    ),
                    rawText = userInput
                )
            }

            // BÚSQUEDA NORMAL (fallback)
            containsAny(lowerInput, listOf("buscar", "encontrar", "información", "informacion", "muestra", "enseña")) -> {
                val query = extractSearchQuery(userInput)
                Log.d("INTENT_DEBUG", "✅ Detectado: BÚSQUEDA NORMAL - Query: '$query'")
                ParsedCommand(
                    intention = UserIntention.Busqueda,
                    confidence = 0.8f,
                    parameters = mapOf("query" to query),
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
                val contacto = extractContactName(userInput)
                Log.d("INTENT_DEBUG", "✅ Detectado: CONTACTO - Nombre: '$contacto'")
                ParsedCommand(
                    intention = UserIntention.Contacto,
                    confidence = 0.8f,
                    parameters = mapOf(
                        "contacto" to contacto,
                        "tipo" to "contacto_normal",
                        "nombre" to contacto
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

    // ✅ MÉTODO MEJORADO: Extraer destino de ubicación
    private fun extractDestination(input: String): String {
        val patterns = listOf(
            "ir a (.+)".toRegex(),
            "cómo llegar a (.+)".toRegex(),
            "como llegar a (.+)".toRegex(),
            "mapa de (.+)".toRegex(),
            "dirección a (.+)".toRegex(),
            "direccion a (.+)".toRegex(),
            "dónde está (.+)".toRegex(),
            "donde esta (.+)".toRegex(),
            "dónde queda (.+)".toRegex(),
            "donde queda (.+)".toRegex()
        )

        patterns.forEach { pattern ->
            val match = pattern.find(input.lowercase())
            if (match != null) {
                val destino = match.groupValues[1].trim()
                Log.d("INTENT_DEBUG", "   🗺️ Destino extraído: '$destino'")
                return destino
            }
        }

        // Si no encuentra patrón, usar palabras después de "mapa" o "ubicación"
        val words = input.split(" ")
        val locationKeywords = listOf("mapa", "ubicación", "ubicacion", "dónde", "donde", "cómo", "como", "llegar")

        val keywordIndex = words.indexOfFirst { it.lowercase() in locationKeywords }
        if (keywordIndex != -1 && keywordIndex < words.size - 1) {
            val destino = words.subList(keywordIndex + 1, words.size).joinToString(" ")
            Log.d("INTENT_DEBUG", "   🗺️ Destino por keywords: '$destino'")
            return destino
        }

        Log.d("INTENT_DEBUG", "   🗺️ Destino fallback: usando input completo")
        return input // Fallback: usar todo el input
    }

    // ✅ MÉTODO MEJORADO: Extraer nombre de contacto
    private fun extractContactName(input: String): String {
        val patterns = listOf(
            "llamar a (.+)".toRegex(),
            "contactar a (.+)".toRegex(),
            "hablar con (.+)".toRegex(),
            "marcar a (.+)".toRegex(),
            "telefonear a (.+)".toRegex()
        )

        patterns.forEach { pattern ->
            val match = pattern.find(input.lowercase())
            if (match != null) {
                val contacto = match.groupValues[1].trim()
                Log.d("INTENT_DEBUG", "   📞 Contacto extraído: '$contacto'")
                return contacto
            }
        }

        // Buscar nombre después de palabras clave
        val contactKeywords = listOf("llamar", "contactar", "hablar", "marcar", "telefonear")
        val words = input.split(" ")
        val keywordIndex = words.indexOfFirst { it.lowercase() in contactKeywords }

        if (keywordIndex != -1 && keywordIndex < words.size - 1) {
            val contacto = words.subList(keywordIndex + 1, words.size).joinToString(" ")
            Log.d("INTENT_DEBUG", "   📞 Contacto por keywords: '$contacto'")
            return contacto
        }

        Log.d("INTENT_DEBUG", "   📞 Contacto no detectado - usando vacío")
        return "" // No se detectó nombre específico
    }

    // ✅ NUEVO MÉTODO: Extraer consulta de búsqueda
    private fun extractSearchQuery(input: String): String {
        val patterns = listOf(
            "buscar (.+)".toRegex(),
            "buscar en internet (.+)".toRegex(),
            "buscar en web (.+)".toRegex(),
            "buscar online (.+)".toRegex(),
            "investigar en internet (.+)".toRegex(),
            "explicar (.+)".toRegex(),
            "qué es (.+)".toRegex(),
            "que es (.+)".toRegex(),
            "quién es (.+)".toRegex(),
            "quien es (.+)".toRegex(),
            "cómo funciona (.+)".toRegex(),
            "como funciona (.+)".toRegex(),
            "dime sobre (.+)".toRegex(),
            "información sobre (.+)".toRegex(),
            "informacion sobre (.+)".toRegex(),
            "encontrar (.+)".toRegex(),
            "muestra (.+)".toRegex(),
            "enseña (.+)".toRegex()
        )

        patterns.forEach { pattern ->
            val match = pattern.find(input.lowercase())
            if (match != null) {
                val query = match.groupValues[1].trim()
                Log.d("INTENT_DEBUG", "   🔍 Query extraída: '$query'")
                return query
            }
        }

        // Si no encuentra patrón, usar todo el input después de palabras clave
        val searchKeywords = listOf("buscar", "explicar", "qué es", "que es", "quién es", "quien es", "cómo", "como", "dime", "información", "informacion", "encontrar", "muestra", "enseña")
        val words = input.split(" ")
        val keywordIndex = words.indexOfFirst { it.lowercase() in searchKeywords }

        if (keywordIndex != -1 && keywordIndex < words.size - 1) {
            val query = words.subList(keywordIndex + 1, words.size).joinToString(" ")
            Log.d("INTENT_DEBUG", "   🔍 Query por keywords: '$query'")
            return query
        }

        Log.d("INTENT_DEBUG", "   🔍 Query fallback: usando input completo")
        return input // Fallback
    }
}