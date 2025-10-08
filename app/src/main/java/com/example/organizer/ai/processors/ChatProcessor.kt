package com.example.organizer.ai.processors

import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention

class ChatProcessor {
    fun process(parsedCommand: ParsedCommand): Action {
        val respuesta = generateResponse(parsedCommand.rawText)

        return Action(
            intention = UserIntention.ChatGeneral,
            parameters = emptyMap(),
            response = respuesta,
            execute = null
        )
    }

    private fun generateResponse(input: String): String {
        return when {
            input.contains("hola", ignoreCase = true) -> "¡Hola! ¿En qué puedo ayudarte?"
            input.contains("cómo estás", ignoreCase = true) -> "¡Estoy bien, gracias! Listo para ayudarte"
            input.contains("gracias", ignoreCase = true) -> "¡De nada! ¿Algo más en lo que pueda ayudarte?"
            input.contains("adiós", ignoreCase = true) -> "¡Hasta luego! Que tengas un buen día"
            else -> "Entendido. ¿Necesitas ayuda con agenda, recordatorios, ubicación o contactos?"
        }
    }
}