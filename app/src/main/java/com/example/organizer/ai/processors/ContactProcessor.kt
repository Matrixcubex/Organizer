// ContactProcessor.kt - REEMPLAZAR completamente:

package com.example.organizer.ai.processors

import android.content.Context
import android.content.Intent
import com.example.organizer.TelefonoActivity
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention

class ContactProcessor(private val context: Context) {

    fun process(parsedCommand: ParsedCommand): Action {
        val contactoNombre = parsedCommand.parameters["nombre"] ?: ""
        val tipo = parsedCommand.parameters["tipo"] ?: "contacto_normal"

        return Action(
            intention = UserIntention.Contacto,
            parameters = parsedCommand.parameters,
            response = when {
                contactoNombre.isNotEmpty() -> "📞 Buscando contacto: $contactoNombre"
                else -> "📞 Abriendo lista de contactos"
            },
            execute = {
                openContactsActivity(contactoNombre, tipo)
            }
        )
    }

    private fun openContactsActivity(contactName: String, type: String) {
        val intent = Intent(context, TelefonoActivity::class.java).apply {
            putExtra("CONTACTO_BUSCADO", contactName)
            putExtra("TIPO", type)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}