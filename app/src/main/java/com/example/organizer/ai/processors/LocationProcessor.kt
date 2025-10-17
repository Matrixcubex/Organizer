// LocationProcessor.kt - REEMPLAZAR completamente:

package com.example.organizer.ai.processors

import android.content.Context
import android.content.Intent
import com.example.organizer.UbicacionActivity
import com.example.organizer.ai.models.Action
import com.example.organizer.ai.models.ParsedCommand
import com.example.organizer.ai.models.UserIntention

class LocationProcessor(private val context: Context) {

    fun process(parsedCommand: ParsedCommand): Action {
        val direccion = parsedCommand.parameters["direccion"] ?: ""
        val tipo = parsedCommand.parameters["tipo"] ?: "navegacion"

        return Action(
            intention = UserIntention.Ubicacion,
            parameters = parsedCommand.parameters,
            response = when {
                direccion.isNotEmpty() -> "🗺️ Abriendo mapa con destino: $direccion"
                else -> "🗺️ Abriendo mapa con tu ubicación actual"
            },
            execute = {
                openMapActivity(direccion, tipo)
            }
        )
    }

    private fun openMapActivity(destination: String, type: String) {
        val intent = Intent(context, UbicacionActivity::class.java).apply {
            putExtra("DESTINO", destination)
            putExtra("TIPO", type)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}