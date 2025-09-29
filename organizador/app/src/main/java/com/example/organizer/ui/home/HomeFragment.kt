package com.example.organizer.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organizer.R
import com.example.organizer.data.DatabaseHelper
import com.example.organizer.data.model.Event
import com.example.organizer.databinding.FragmentHomeBinding
import com.example.organizer.utils.DatabaseProvider

class HomeFragment : Fragment() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dbHelper = DatabaseProvider.get(requireContext())
        val binding = FragmentHomeBinding.inflate(inflater, container, false)

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        val events = dbHelper.getEvents()
        recyclerView.adapter = EventAdapter(
            events,
            onItemClick = { /* Manejar click en evento */ },
            onContactClick = { /* Manejar click en contacto */ },
            onMapClick = { /* Manejar click en mapa */ }
        )

        return binding.root
    }
}