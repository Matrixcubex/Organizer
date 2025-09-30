package com.example.organizer.ui.consult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organizer.R
import com.example.organizer.data.DatabaseHelper
import com.example.organizer.data.model.Event
import com.example.organizer.databinding.FragmentConsultBinding
import com.example.organizer.ui.home.EventAdapter
import com.example.organizer.utils.DatabaseProvider
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class ConsultFragment : Fragment() {
    private var _binding: FragmentConsultBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsultBinding.inflate(inflater, container, false)
        dbHelper = DatabaseProvider.get(requireContext())
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        setupSpinners()
    }

    private fun setupRecyclerView() {
        // Ya no necesitas configurar el LayoutManager aquí (se hace en XML)
        adapter = EventAdapter(
            emptyList(),
            onItemClick = { event -> navigateToEditEvent(event) },
            onContactClick = { contactId -> showContactDetails(contactId) },
            onMapClick = { location -> showLocationOnMap(location) }
        )

        binding.eventsRecyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnToday.setOnClickListener { loadEventsForToday() }
        binding.btnRange.setOnClickListener { showDateRangePicker() }
        binding.btnMonth.setOnClickListener { loadEventsForCurrentMonth() }
        binding.btnYear.setOnClickListener { loadEventsForCurrentYear() }
        binding.filterButton.setOnClickListener { applyFilters() }
    }

    private fun setupSpinners() {
        // Configurar spinner de tipos
        ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.event_types_array).toList()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.typeFilterSpinner.adapter = adapter
        }

        // Configurar spinner de estados
        ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.status_options).toList()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.statusFilterSpinner.adapter = adapter
        }
    }

    private fun applyFilters() {
        val type = if (binding.typeFilterSpinner.selectedItemPosition == 0) null
        else binding.typeFilterSpinner.selectedItem.toString()
        val status = if (binding.statusFilterSpinner.selectedItemPosition == 0) null
        else binding.statusFilterSpinner.selectedItem.toString()

        val events = dbHelper.getEvents().filter { event ->
            (type == null || event.type == type) &&
                    (status == null || event.status == status)
        }

        adapter.updateEvents(events)
    }

    private fun loadEventsForToday() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())
        adapter.updateEvents(dbHelper.getEventsByDate(today))
    }

    private fun loadEventsForCurrentMonth() {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        adapter.updateEvents(dbHelper.getEventsByMonth(month, year))
    }

    private fun loadEventsForCurrentYear() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        adapter.updateEvents(dbHelper.getEventsByYear(year))
    }

    private fun showDateRangePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker().build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first
            val end = selection.second
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val startDate = dateFormat.format(Date(start))
            val endDate = dateFormat.format(Date(end))
            adapter.updateEvents(dbHelper.getEventsByDateRange(startDate, endDate))
        }
        datePicker.show(parentFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun navigateToEditEvent(event: Event) {
        findNavController().navigate(
            R.id.action_consultFragment_to_addEventFragment, // Asegúrate que esta acción existe en tu nav_graph.xml
            Bundle().apply { putParcelable("eventToEdit", event) }
        )
    }

    private fun showContactDetails(contactId: String) {
        // Implementar lógica para mostrar detalles del contacto
    }

    private fun showLocationOnMap(location: Pair<Double, Double>) {
        // Implementar lógica para mostrar mapa con la ubicación
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}