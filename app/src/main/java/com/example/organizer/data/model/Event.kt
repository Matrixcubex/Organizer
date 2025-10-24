// app/src/main/java/com/example/organizer/data/model/Event.kt
package com.example.organizer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Event.kt - Hacer campos opcionales
@Parcelize
data class Event(
    val id: Long = 0,
    val title: String,
    val type: String,
    val contactName: String = "",
    val contactId: String = "",
    val locationLat: Double = 0.0,
    val locationLng: Double = 0.0,
    val description: String? = null,
    val date: String,
    val time: String,
    val status: String? = "Pendiente",
    val reminder: String? = "30"
) : Parcelable