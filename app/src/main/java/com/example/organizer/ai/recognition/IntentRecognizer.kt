package com.example.organizer.ai.recognition

import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention
import java.util.*
import kotlin.math.min

class IntentRecognizer {

    fun recognizeIntent(userInput: String): ParsedCommand {
        val lowerInput = userInput.lowercase(Locale.getDefault())
        val words = lowerInput.split("\\s+".toRegex())

        // Analizar con múltiples estrategias
        val agendaScore = calculateAgendaScore(lowerInput, words)
        val reminderScore = calculateReminderScore(lowerInput, words)
        val locationScore = calculateLocationScore(lowerInput, words)
        val contactScore = calculateContactScore(lowerInput, words)
        val searchScore = calculateSearchScore(lowerInput, words)
        val emergencyScore = calculateEmergencyScore(lowerInput, words)

        // Encontrar la intención con mayor puntuación
        val scores = mapOf(
            UserIntention.Agenda to agendaScore,
            UserIntention.Recordatorio to reminderScore,
            UserIntention.Ubicacion to locationScore,
            UserIntention.Contacto to contactScore,
            UserIntention.Busqueda to searchScore
        )

        val bestIntention = scores.maxByOrNull { it.value }?.key ?: UserIntention.ChatGeneral
        val bestScore = scores[bestIntention] ?: 0f

        return when {
            emergencyScore > 0.8 -> ParsedCommand(
                intention = UserIntention.Contacto,
                confidence = emergencyScore,
                parameters = mapOf("emergencia" to "true"),
                rawText = userInput
            )
            bestScore > 0.6 -> ParsedCommand(
                intention = bestIntention,
                confidence = bestScore,
                parameters = extractParameters(userInput, bestIntention),
                rawText = userInput
            )
            else -> ParsedCommand(
                intention = UserIntention.ChatGeneral,
                confidence = 0.7f,
                rawText = userInput
            )
        }
    }

    private fun calculateAgendaScore(input: String, words: List<String>): Float {
        val agendaKeywords = mapOf(
            "agendar" to 2.0f, "cita" to 2.0f, "programar" to 1.5f, "reunión" to 1.5f,
            "evento" to 1.5f, "calendario" to 1.0f, "reservar" to 1.0f, "consultorio" to 1.0f,
            "doctor" to 1.0f, "médico" to 1.0f, "hospital" to 1.0f
        )

        val dateTimeIndicators = listOf(
            "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo",
            "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
            "septiembre", "octubre", "noviembre", "diciembre", "mañana", "tarde", "noche"
        )

        var score = calculateKeywordScore(input, agendaKeywords)

        // Bonus por indicadores de fecha/hora
        if (dateTimeIndicators.any { input.contains(it) }) {
            score += 0.3f
        }

        // Bonus por patrones de hora
        if (containsTimePattern(input)) {
            score += 0.2f
        }

        return min(score, 1.0f)
    }

    private fun calculateReminderScore(input: String, words: List<String>): Float {
        val reminderKeywords = mapOf(
            "recordatorio" to 2.0f, "recordar" to 1.8f, "aviso" to 1.5f, "notificación" to 1.5f,
            "alarma" to 1.5f, "recordarme" to 1.5f, "avisarme" to 1.2f, "notificarme" to 1.2f,
            "diario" to 1.0f, "todos los días" to 1.0f, "cada día" to 1.0f
        )

        var score = calculateKeywordScore(input, reminderKeywords)

        // Bonus por acciones recurrentes
        val recurringActions = listOf("tomar", "medicina", "comer", "ejercicio", "dormir", "despertar")
        if (recurringActions.any { input.contains(it) }) {
            score += 0.2f
        }

        return min(score, 1.0f)
    }

    private fun calculateLocationScore(input: String, words: List<String>): Float {
        val locationKeywords = mapOf(
            "dónde" to 2.0f, "donde" to 2.0f, "ubicación" to 1.8f, "ubicacion" to 1.8f,
            "dirección" to 1.5f, "direccion" to 1.5f, "mapa" to 1.5f, "cómo llegar" to 1.8f,
            "como llegar" to 1.8f, "localizar" to 1.2f, "encontrar" to 1.0f, "sitio" to 1.0f,
            "lugar" to 1.0f
        )

        return min(calculateKeywordScore(input, locationKeywords), 1.0f)
    }

    private fun calculateContactScore(input: String, words: List<String>): Float {
        val contactKeywords = mapOf(
            "llamar" to 2.0f, "telefonear" to 1.5f, "marcar" to 1.5f, "contactar" to 1.5f,
            "número" to 1.2f, "numero" to 1.2f, "teléfono" to 1.2f, "telefono" to 1.2f,
            "hablar con" to 1.0f, "comunicar" to 1.0f
        )

        return min(calculateKeywordScore(input, contactKeywords), 1.0f)
    }

    private fun calculateSearchScore(input: String, words: List<String>): Float {
        val searchKeywords = mapOf(
            "buscar" to 2.0f, "encontrar" to 1.5f, "información" to 1.5f, "informacion" to 1.5f,
            "qué es" to 1.8f, "que es" to 1.8f, "quién es" to 1.8f, "quien es" to 1.8f,
            "cómo funciona" to 1.5f, "como funciona" to 1.5f, "muestra" to 1.2f,
            "enseña" to 1.2f, "dime sobre" to 1.0f
        )

        return min(calculateKeywordScore(input, searchKeywords), 1.0f)
    }

    private fun calculateEmergencyScore(input: String, words: List<String>): Float {
        val emergencyKeywords = mapOf(
            "emergencia" to 3.0f, "ayuda" to 2.5f, "socorro" to 2.5f, "urgencia" to 2.0f,
            "accidente" to 2.0f, "peligro" to 1.8f, "auxilio" to 1.8f, "911" to 3.0f
        )

        return min(calculateKeywordScore(input, emergencyKeywords), 1.0f)
    }

    private fun calculateKeywordScore(input: String, keywords: Map<String, Float>): Float {
        var totalScore = 0f
        var matches = 0

        keywords.forEach { (keyword, weight) ->
            if (input.contains(keyword)) {
                totalScore += weight
                matches++
            }
        }

        // Normalizar score basado en número de matches y longitud del texto
        val baseScore = totalScore / 5.0f // Normalizar a escala 0-1
        val matchDensity = matches.toFloat() / keywords.size.coerceAtLeast(1)

        return (baseScore * 0.7f) + (matchDensity * 0.3f)
    }

    private fun containsTimePattern(input: String): Boolean {
        val timePatterns = listOf(
            "\\d{1,2}:\\d{2}".toRegex(), // 14:30
            "\\d{1,2}\\s*(am|pm)".toRegex(), // 2 pm
            "\\d{1,2}\\s*de la\\s*(mañana|tarde|noche)".toRegex() // 2 de la tarde
        )

        return timePatterns.any { it.containsMatchIn(input) }
    }

    private fun extractParameters(input: String, intention: UserIntention): Map<String, String> {
        return when (intention) {
            is UserIntention.Agenda -> extractAgendaParameters(input)
            is UserIntention.Recordatorio -> extractReminderParameters(input)
            is UserIntention.Ubicacion -> extractLocationParameters(input)
            is UserIntention.Contacto -> extractContactParameters(input)
            is UserIntention.Busqueda -> extractSearchParameters(input)
            else -> emptyMap()
        }
    }

    private fun extractAgendaParameters(input: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        // Extraer título/descripción
        val title = extractTitle(input, listOf("agendar", "cita", "programar", "reunión"))
        params["titulo"] = title
        params["descripcion"] = input

        // Extraer fecha (simplificado por ahora)
        params["fecha"] = extractDate(input) ?: "hoy"
        params["hora"] = extractTime(input) ?: "12:00"

        return params
    }

    private fun extractReminderParameters(input: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        val title = extractTitle(input, listOf("recordatorio", "recordar", "aviso"))
        params["titulo"] = title
        params["descripcion"] = input
        params["hora"] = extractTime(input) ?: "09:00"

        return params
    }

    private fun extractTitle(input: String, excludeWords: List<String>): String {
        var title = input
        excludeWords.forEach { word ->
            title = title.replace(word, "", ignoreCase = true)
        }
        return title.trim().take(50).ifEmpty { "Recordatorio" }
    }

    private fun extractDate(input: String): String? {
        // Implementación básica - mejorar después
        val datePatterns = listOf(
            "\\d{1,2}/\\d{1,2}/\\d{4}".toRegex(), // dd/MM/yyyy
            "\\d{1,2}-\\d{1,2}-\\d{4}".toRegex()  // dd-MM-yyyy
        )

        datePatterns.forEach { pattern ->
            val match = pattern.find(input)
            if (match != null) return match.value
        }

        // Detectar días de la semana
        val days = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
        days.forEach { day ->
            if (input.contains(day)) return day
        }

        return null
    }

    private fun extractTime(input: String): String? {
        val timePattern = "\\b(?:2[0-3]|[01]?[0-9]):[0-5][0-9]\\b".toRegex()
        return timePattern.find(input)?.value
    }

    private fun extractLocationParameters(input: String): Map<String, String> {
        return mapOf("direccion" to input)
    }

    private fun extractContactParameters(input: String): Map<String, String> {
        return mapOf("contacto" to "familiar")
    }

    private fun extractSearchParameters(input: String): Map<String, String> {
        val query = input.replace(Regex("(?i)buscar|encontrar|información|informacion|qué es|que es"), "").trim()
        return mapOf("query" to query)
    }
}