package com.example.organizer.ai.processors

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention

class LocationProcessor(private val context: Context) {

    fun process(parsedCommand: ParsedCommand): Action {
        val direccion = parsedCommand.parameters["direccion"] ?: "ubicación actual"

        return Action(
            intention = UserIntention.Ubicacion,
            parameters = parsedCommand.parameters,
            response = "🗺️ Abriendo mapa para: $direccion",
            execute = {
                openMap(direccion)
            }
        )
    }

    private fun openMap(location: String) {
        val uri = when {
            location.contains("ubicación actual", ignoreCase = true) ->
                "geo:0,0?q=current+location"
            else ->
                "geo:0,0?q=${Uri.encode(location)}"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps") // Intentar abrir Google Maps

        // Si Google Maps no está instalado, usar intent genérico
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Intent genérico para cualquier app de mapas
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(fallbackIntent)
        }
    }
}