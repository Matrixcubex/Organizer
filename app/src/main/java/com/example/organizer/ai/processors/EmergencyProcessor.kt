package com.example.organizer.ai.processors

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention

class EmergencyProcessor(private val context: Context) {

    fun process(parsedCommand: ParsedCommand): Action {
        return Action(
            intention = UserIntention.Contacto,
            parameters = mapOf("tipo" to "emergencia"),
            response = "🚨 Llamando a contacto de emergencia...",
            execute = {
                makeEmergencyCall()
            }
        )
    }

    private fun makeEmergencyCall() {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:911") // o el número de emergencia configurado
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}