package com.example.organizer.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.organizer.R
import com.example.organizer.data.model.Event

class EventAdapter(
    private var events: List<Event>,
    private val onItemClick: (Event) -> Unit,
    private val onContactClick: (String) -> Unit,
    private val onMapClick: (Pair<Double, Double>) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.eventTitle)
        val date: TextView = itemView.findViewById(R.id.eventDate)
        val time: TextView = itemView.findViewById(R.id.eventTime)
        val contact: TextView = itemView.findViewById(R.id.eventContact)
        val description: TextView = itemView.findViewById(R.id.eventDescription)
        val status: TextView = itemView.findViewById(R.id.eventStatus)
        val type: TextView = itemView.findViewById(R.id.eventType)

        fun bind(event: Event) {
            title.text = event.title
            date.text = event.date
            time.text = event.time
            contact.text = event.contactName
            description.text = event.description
            status.text = event.status
            type.text = event.type

            itemView.setOnClickListener { onItemClick(event) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_table, parent, false)
        return EventViewHolder(view)
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