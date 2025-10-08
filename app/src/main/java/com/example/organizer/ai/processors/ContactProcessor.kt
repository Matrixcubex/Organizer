package com.example.organizer.ai.processors

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention

class ContactProcessor(private val context: Context) {

    fun process(parsedCommand: ParsedCommand): Action {
        val contacto = parsedCommand.parameters["contacto"] ?: "familiar"

        return Action(
            intention = UserIntention.Contacto,
            parameters = parsedCommand.parameters,
            response = "📞 Preparando llamada a: $contacto",
            execute = {
                makePhoneCall(contacto)
            }
        )
    }

    private fun makePhoneCall(contactName: String) {
        // Por ahora, abrir el marcador para que el usuario marque manualmente
        // En una versión futura, podríamos buscar en los contactos del teléfono

        val intent = Intent(Intent.ACTION_DIAL).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(intent)
    }
}