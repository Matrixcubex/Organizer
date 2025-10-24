// ConsultFragment.kt - VERSIÓN COMPLETA CORREGIDA
package com.example.organizer.ui.consult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import android.util.Log

class ConsultFragment : Fragment() {
    private var _binding: FragmentConsultBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: EventAdapter
    private var currentFilter: String = "HOY" // Heurística 1: Estado visible

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
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        setupSpinners()
        setupTooltips()

        // ✅ DEBUG: Ver contenido de la base de datos
        debugDatabaseContents()

        loadEventsForToday()
        updateFilterStatus("Eventos de Hoy")
        setupRecyclerView()
        setupButtons()
        setupSpinners()
        setupTooltips() // Heurística 6: Reconocimiento antes que recuerdo

        // Heurística 7: Flexibilidad y eficiencia - Cargar eventos de hoy por defecto
        loadEventsForToday()

        // Heurística 1: Mostrar estado actual
        updateFilterStatus("Eventos de Hoy")
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(
            emptyList(),
            onItemClick = { event -> navigateToEditEvent(event) },
            onContactClick = { contactId -> showContactDetails(contactId) },
            onMapClick = { location -> showLocationOnMap(location) }
        )

        binding.eventsRecyclerView.adapter = adapter
        binding.eventsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Heurística 8: Diseño estético - Agregar divisor entre items
        binding.eventsRecyclerView.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
    }

    private fun setupButtons() {
        // Usar colores del sistema Android
        val primaryColor = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light)
        val selectedColor = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)

        // Estado inicial - Hoy seleccionado
        binding.btnToday.setBackgroundColor(selectedColor)

        binding.btnToday.setOnClickListener {
            loadEventsForToday()
            highlightSelectedButton(binding.btnToday)
            updateFilterStatus("Eventos de Hoy")
        }

        binding.btnRange.setOnClickListener {
            showDateRangePicker()
            highlightSelectedButton(binding.btnRange)
        }

        binding.btnMonth.setOnClickListener {
            loadEventsForCurrentMonth()
            highlightSelectedButton(binding.btnMonth)
            updateFilterStatus("Eventos del Mes Actual")
        }

        binding.btnYear.setOnClickListener {
            loadEventsForCurrentYear()
            highlightSelectedButton(binding.btnYear)
            updateFilterStatus("Eventos del Año Actual")
        }

        binding.filterButton.setOnClickListener {
            applyFilters()
            highlightSelectedButton(binding.filterButton)
            updateFilterStatus("Eventos Filtrados")
        }

        // Heurística 7: Flexibilidad y eficiencia - Botón de actualización rápida
        binding.btnRefresh.setOnClickListener {
            refreshEvents()
        }
    }

    private fun highlightSelectedButton(selectedButton: View) {
        val primaryColor = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light)
        val selectedColor = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)

        val buttons = listOf(binding.btnToday, binding.btnRange, binding.btnMonth, binding.btnYear, binding.filterButton)
        buttons.forEach { button ->
            button.setBackgroundColor(if (button == selectedButton) selectedColor else primaryColor)
        }
    }

    private fun setupSpinners() {
        // Configurar spinner de tipos
        ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            arrayListOf("Todos los tipos") + resources.getStringArray(R.array.event_types_array).toList()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.typeFilterSpinner.adapter = adapter
        }

        // Configurar spinner de estados
        ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            arrayListOf("Todos los estados") + resources.getStringArray(R.array.status_options).toList()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.statusFilterSpinner.adapter = adapter
        }
    }

    private fun setupTooltips() {
        // Heurística 6: Reconocimiento antes que recuerdo
        binding.btnToday.setOnLongClickListener {
            Toast.makeText(requireContext(), "Mostrar eventos programados para hoy", Toast.LENGTH_SHORT).show()
            true
        }
        binding.btnRange.setOnLongClickListener {
            Toast.makeText(requireContext(), "Seleccionar un rango de fechas específico", Toast.LENGTH_SHORT).show()
            true
        }
        binding.btnMonth.setOnLongClickListener {
            Toast.makeText(requireContext(), "Mostrar todos los eventos del mes actual", Toast.LENGTH_SHORT).show()
            true
        }
        binding.btnYear.setOnLongClickListener {
            Toast.makeText(requireContext(), "Mostrar todos los eventos del año actual", Toast.LENGTH_SHORT).show()
            true
        }
        binding.filterButton.setOnLongClickListener {
            Toast.makeText(requireContext(), "Aplicar filtros por tipo y estado del evento", Toast.LENGTH_SHORT).show()
            true
        }
        binding.btnRefresh.setOnLongClickListener {
            Toast.makeText(requireContext(), "Actualizar la lista de eventos", Toast.LENGTH_SHORT).show()
            true
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

        // Heurística 1: Visibilidad del estado
        val filterText = buildString {
            append("Filtros aplicados")
            if (type != null) append(" • Tipo: $type")
            if (status != null) append(" • Estado: $status")
            append(" • ${events.size} eventos")
        }

        showSuccess(filterText)
        updateFilterStatus("Eventos Filtrados")
    }

    // En ConsultFragment.kt - MODIFICAR el método loadEventsForToday:
    private fun loadEventsForToday() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

        // ✅ OBTENER TODOS LOS EVENTOS (no solo por fecha específica)
        val allEvents = dbHelper.getEvents()
        val todayEvents = allEvents.filter { event ->
            // Mostrar eventos de hoy O recordatorios diarios
            event.date == today || event.date == "DIARIO"
        }

        adapter.updateEvents(todayEvents)

        // Heurística 1: Feedback del resultado
        if (todayEvents.isEmpty()) {
            showInfo("No hay eventos programados para hoy")
        } else {
            showSuccess("${todayEvents.size} eventos encontrados para hoy")
        }

        updateEmptyState(todayEvents.isEmpty())
    }

    private fun loadEventsForCurrentMonth() {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val events = dbHelper.getEventsByMonth(month, year)
        adapter.updateEvents(events)

        if (events.isEmpty()) {
            showInfo("No hay eventos programados para este mes")
        } else {
            showSuccess("${events.size} eventos encontrados para el mes")
        }

        updateEmptyState(events.isEmpty())
    }

    private fun loadEventsForCurrentYear() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val events = dbHelper.getEventsByYear(year)
        adapter.updateEvents(events)

        if (events.isEmpty()) {
            showInfo("No hay eventos programados para este año")
        } else {
            showSuccess("${events.size} eventos encontrados para el año")
        }

        updateEmptyState(events.isEmpty())
    }
    private fun debugDatabaseContents() {
        val allEvents = dbHelper.getEvents()
        Log.d("DB_DEBUG", "=== CONTENIDO DE LA BASE DE DATOS ===")
        allEvents.forEach { event ->
            Log.d("DB_DEBUG", "ID: ${event.id}, Tipo: ${event.type}, Fecha: ${event.date}, Título: ${event.title}")
        }
        Log.d("DB_DEBUG", "Total eventos: ${allEvents.size}")
    }
    private fun refreshEvents() {
        // Heurística 1: Feedback de actualización
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRefresh.isEnabled = false

        when (currentFilter) {
            "HOY" -> loadEventsForToday()
            "MES" -> loadEventsForCurrentMonth()
            "AÑO" -> loadEventsForCurrentYear()
            else -> applyFilters()
        }

        // Simular carga
        binding.btnRefresh.postDelayed({
            binding.progressBar.visibility = View.GONE
            binding.btnRefresh.isEnabled = true
            showSuccess("Lista actualizada")
        }, 500)
    }

    private fun showDateRangePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Seleccionar rango de fechas")
            .setSelection(androidx.core.util.Pair(
                MaterialDatePicker.todayInUtcMilliseconds(),
                MaterialDatePicker.todayInUtcMilliseconds()
            ))
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first
            val end = selection.second
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val startDate = dateFormat.format(Date(start))
            val endDate = dateFormat.format(Date(end))
            val events = dbHelper.getEventsByDateRange(startDate, endDate)
            adapter.updateEvents(events)

            // Heurística 1: Feedback del rango seleccionado
            val rangeText = "Del ${dateFormat.format(Date(start))} al ${dateFormat.format(Date(end))}"
            if (events.isEmpty()) {
                showInfo("No hay eventos en el rango: $rangeText")
            } else {
                showSuccess("${events.size} eventos encontrados en: $rangeText")
            }

            updateFilterStatus("Rango: $rangeText")
            updateEmptyState(events.isEmpty())
        }

        datePicker.addOnNegativeButtonClickListener {
            // Heurística 3: Control y libertad - El usuario puede cancelar
            showInfo("Selección de rango cancelada")
        }

        datePicker.show(parentFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun updateFilterStatus(status: String) {
        binding.filterStatusText.text = "• $status"
        binding.filterStatusText.visibility = View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.emptyStateText.text = "📅 No se encontraron eventos\n\nPuedes crear un nuevo evento desde el menú principal o usando el asistente de voz."
        } else {
            binding.emptyStateText.visibility = View.GONE
        }
    }

    private fun navigateToEditEvent(event: Event) {
        // Heurística 3: Control y libertad - Navegación clara
        findNavController().navigate(
            R.id.action_consultFragment_to_addEventFragment,
            Bundle().apply { putParcelable("eventToEdit", event) }
        )
    }

    private fun showContactDetails(contactId: String) {
        // Heurística 9: Mensajes informativos
        showInfo("Función de detalles de contacto en desarrollo")
    }

    private fun showLocationOnMap(location: Pair<Double, Double>) {
        val (lat, lng) = location
        showInfo("Función de mapa en desarrollo - Ubicación: $lat, $lng")
    }

    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), "✅ $message", Toast.LENGTH_SHORT).show()
    }

    private fun showInfo(message: String) {
        Toast.makeText(requireContext(), "ℹ️ $message", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}