// app/src/main/java/com/example/organizer/data/model/Event.kt
package com.example.organizer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Event(
    val id: Long = 0,
    val title: String,
    val type: String,  // "Cita", "Junta", etc.
    val contactName: String,
    val contactId: String,  // ID del contacto en el sistema
    val locationLat: Double,
    val locationLng: Double,
    val description: String,
    val date: String,  // Formato: "dd/MM/yyyy"
    val time: String,  // Formato: "HH:mm"
    val status: String,  // "Pendiente", "Realizado", etc.
    val reminder: String  // "Ninguno", "10_minutos", "1_dia"
) : Parcelable