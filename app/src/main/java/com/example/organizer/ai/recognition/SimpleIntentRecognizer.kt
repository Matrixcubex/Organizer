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
    // En SimpleIntentRecognizer.kt - AÑADIR estos métodos:

    private fun extractDestination(input: String): String {
        val patterns = listOf(
            "ir a (.+)".toRegex(),
            "cómo llegar a (.+)".toRegex(),
            "como llegar a (.+)".toRegex(),
            "mapa de (.+)".toRegex(),
            "dirección a (.+)".toRegex(),
            "direccion a (.+)".toRegex()
        )

        patterns.forEach { pattern ->
            val match = pattern.find(input.lowercase())
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        // Si no encuentra patrón, usar palabras después de "mapa" o "ubicación"
        val words = input.split(" ")
        val locationKeywords = listOf("mapa", "ubicación", "ubicacion", "dónde", "donde")

        val keywordIndex = words.indexOfFirst { it.lowercase() in locationKeywords }
        if (keywordIndex != -1 && keywordIndex < words.size - 1) {
            return words.subList(keywordIndex + 1, words.size).joinToString(" ")
        }

        return input // Fallback: usar todo el input
    }

    private fun extractContactName(input: String): String {
        val patterns = listOf(
            "llamar a (.+)".toRegex(),
            "contactar a (.+)".toRegex(),
            "hablar con (.+)".toRegex(),
            "marcar a (.+)".toRegex()
        )

        patterns.forEach { pattern ->
            val match = pattern.find(input.lowercase())
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        // Buscar nombre después de palabras clave
        val contactKeywords = listOf("llamar", "contactar", "hablar", "marcar")
        val words = input.split(" ")
        val keywordIndex = words.indexOfFirst { it.lowercase() in contactKeywords }

        if (keywordIndex != -1 && keywordIndex < words.size - 1) {
            return words.subList(keywordIndex + 1, words.size).joinToString(" ")
        }

        return "" // No se detectó nombre específico
    }
}