package com.example.organizer.ai.models

sealed class UserIntention {
    object Agenda : UserIntention()
    object Recordatorio : UserIntention()
    object Ubicacion : UserIntention()
    object Contacto : UserIntention()
    object Busqueda : UserIntention()
    object ChatGeneral : UserIntention()
    object Desconocido : UserIntention()
}

enum class InputType {
    VOICE, TEXT
}