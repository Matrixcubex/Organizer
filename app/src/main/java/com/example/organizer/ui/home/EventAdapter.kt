package com.example.organizer.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.organizer.databinding.ItemEventBinding
import com.example.organizer.data.model.Event

class EventAdapter(
    private var events: List<Event>,
    private val onItemClick: (Event) -> Unit,
    private val onContactClick: (String) -> Unit,
    private val onMapClick: (Pair<Double, Double>) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.eventTitle.text = event.title
            binding.eventDescription.text = event.description ?: ""
            binding.eventTime.text = event.time

            // ✅ DIFERENCIAR ENTRE EVENTOS Y RECORDATORIOS
            if (event.date == "DIARIO") {
                binding.eventDate.text = "🔄 Diario"
                binding.eventDate.setTextColor(Color.BLUE)
            } else {
                binding.eventDate.text = event.date
                binding.eventDate.setTextColor(Color.BLACK)
            }

            // Set contact if available
            if (event.contactName.isNotEmpty()) {
                binding.eventContact.text = "📞 ${event.contactName}"
                binding.eventContact.setOnClickListener {
                    onContactClick(event.contactId)
                }
                binding.eventContact.visibility = android.view.View.VISIBLE
            } else {
                binding.eventContact.visibility = android.view.View.GONE
            }

            // Set event type and status
            binding.eventType.text = event.type
            binding.eventStatus.text = event.status ?: "Pendiente"

            // Set location click if available
            if (event.locationLat != 0.0 && event.locationLng != 0.0) {
                binding.root.setOnClickListener {
                    onMapClick(Pair(event.locationLat, event.locationLng))
                }
            } else {
                binding.root.setOnClickListener { onItemClick(event) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}