package com.example.organizer.ui.add

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.organizer.R
import com.example.organizer.data.DatabaseHelper
import com.example.organizer.data.model.Event
import com.example.organizer.databinding.FragmentAddEventBinding
import com.example.organizer.utils.ContactPicker
import com.example.organizer.utils.DatabaseProvider
import com.example.organizer.utils.NotificationHelper
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.*

class AddEventFragment : Fragment() {
    private var _binding: FragmentAddEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var contactPicker: ContactPicker
    private var selectedLocation: Pair<Double, Double>? = null
    private var selectedContact: Pair<String, String>? = null
    private var editingEvent: Event? = null

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val lat = data.getDoubleExtra("latitude", 0.0)
                val lng = data.getDoubleExtra("longitude", 0.0)
                selectedLocation = Pair(lat, lng)
                binding.locationButton.text = getString(R.string.location_selected)
                // Heurística 1: Feedback visual inmediato
                showSuccess("Ubicación seleccionada correctamente")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseProvider.get(requireContext())
        notificationHelper = NotificationHelper(requireContext())
        contactPicker = ContactPicker(this)

        arguments?.getParcelable<Event>("eventToEdit")?.let { event ->
            editingEvent = event
            populateFields(event)
            binding.statusContainer.visibility = View.VISIBLE
            setupDeleteButton() // Heurística 3: Control y libertad
        }

        setupUI()
        setupRealTimeValidation() // Heurística 5: Prevención de errores
    }

    private fun populateFields(event: Event) {
        binding.apply {
            titleEditText.setText(event.title)
            dateEditText.setText(event.date)
            timeEditText.setText(event.time)
            descriptionEditText.setText(event.description)
            selectedContact = Pair(event.contactName, event.contactId)
            contactButton.text = event.contactName
            selectedLocation = Pair(event.locationLat, event.locationLng)
            if (event.locationLat != 0.0 || event.locationLng != 0.0) {
                locationButton.text = getString(R.string.location_selected)
            }

            // Configurar spinners
            (typeSpinner.adapter as? ArrayAdapter<String>)?.let { adapter ->
                val position = adapter.getPosition(event.type)
                if (position >= 0) typeSpinner.setSelection(position)
            }

            (reminderSpinner.adapter as? ArrayAdapter<String>)?.let { adapter ->
                val position = adapter.getPosition(event.reminder)
                if (position >= 0) reminderSpinner.setSelection(position)
            }

            (statusSpinner.adapter as? ArrayAdapter<String>)?.let { adapter ->
                val position = adapter.getPosition(event.status)
                if (position >= 0) statusSpinner.setSelection(position)
            }
        }
    }

    private fun setupUI() {
        setupSpinners()
        setupDatePicker()
        setupTimePicker()
        setupContactButton()
        setupLocationButton()
        setupSaveButton()
        setupTooltips() // Heurística 6: Reconocimiento antes que recuerdo
    }

    private fun setupSpinners() {
        // Tipo de evento
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.event_types_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.typeSpinner.adapter = adapter
        }

        // Recordatorio
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.reminder_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.reminderSpinner.adapter = adapter
        }

        // Estado (solo visible en edición)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.status_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.statusSpinner.adapter = adapter
            editingEvent?.status?.let { status ->
                val position = adapter.getPosition(status)
                if (position >= 0) binding.statusSpinner.setSelection(position)
            }
        }
    }

    private fun setupDatePicker() {
        binding.dateEditText.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_date))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { millis ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.dateEditText.setText(dateFormat.format(Date(millis)))
                binding.dateEditText.error = null // Limpiar error al seleccionar
                validateDateInRealTime(binding.dateEditText.text.toString())
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupTimePicker() {
        binding.timeEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setTitleText(getString(R.string.select_time))
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val time = String.format("%02d:%02d", timePicker.hour, timePicker.minute)
                binding.timeEditText.setText(time)
                binding.timeEditText.error = null // Limpiar error al seleccionar
                validateTimeInRealTime(time)
            }

            timePicker.show(parentFragmentManager, "TIME_PICKER")
        }
    }

    private fun setupContactButton() {
        binding.contactButton.setOnClickListener {
            contactPicker.pickContact { name, id ->
                selectedContact = Pair(name, id)
                binding.contactButton.text = name
                // Heurística 1: Feedback visual
                showSuccess("Contacto seleccionado: $name")
            }
        }
    }

    private fun setupLocationButton() {
        binding.locationButton.setOnClickListener {
            val intent = Intent(requireContext(), com.example.organizer.utils.MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            if (validateForm()) {
                // Heurística 1: Visibilidad del estado del sistema
                binding.saveButton.text = "Guardando..."
                binding.saveButton.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE

                // Pequeño delay para mostrar el cambio de estado
                binding.saveButton.postDelayed({
                    try {
                        if (editingEvent != null) {
                            updateEvent()
                        } else {
                            saveEvent()
                        }
                    } catch (e: Exception) {
                        showError("Error al guardar: ${e.message}")
                        // Heurística 1: Restaurar estado
                        binding.saveButton.text = "Guardar"
                        binding.saveButton.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                    }
                }, 500)
            }
        }
    }

    private fun setupDeleteButton() {
        binding.deleteButton.visibility = View.VISIBLE
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupTooltips() {
        // Heurística 6: Reconocimiento antes que recuerdo
        binding.typeSpinner.setOnLongClickListener {
            Toast.makeText(requireContext(), "Selecciona el tipo de evento", Toast.LENGTH_SHORT).show()
            true
        }
        binding.reminderSpinner.setOnLongClickListener {
            Toast.makeText(requireContext(), "Configura cuándo quieres ser notificado", Toast.LENGTH_SHORT).show()
            true
        }
        binding.contactButton.setOnLongClickListener {
            Toast.makeText(requireContext(), "Selecciona un contacto asociado al evento", Toast.LENGTH_SHORT).show()
            true
        }
        binding.locationButton.setOnLongClickListener {
            Toast.makeText(requireContext(), "Agrega una ubicación para el evento", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupRealTimeValidation() {
        // Heurística 5: Prevención de errores en tiempo real
        binding.titleEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateTitleInRealTime(binding.titleEditText.text.toString())
            }
        }

        binding.dateEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateDateInRealTime(binding.dateEditText.text.toString())
            }
        }

        binding.timeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateTimeInRealTime(binding.timeEditText.text.toString())
            }
        }
    }

    private fun validateTitleInRealTime(title: String) {
        if (title.isNotEmpty() && title.length < 2) {
            binding.titleEditText.error = "El título debe tener al menos 2 caracteres"
        } else {
            binding.titleEditText.error = null
        }
    }

    private fun validateDateInRealTime(date: String) {
        if (date.isNotEmpty() && !isValidDate(date)) {
            binding.dateEditText.error = "Formato inválido. Use dd/MM/yyyy"
        } else {
            binding.dateEditText.error = null
        }
    }

    private fun validateTimeInRealTime(time: String) {
        if (time.isNotEmpty() && !isValidTime(time)) {
            binding.timeEditText.error = "Formato inválido. Use HH:mm"
        } else {
            binding.timeEditText.error = null
        }
    }

    private fun showDeleteConfirmationDialog() {
        // Heurística 3: Control y libertad del usuario
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Evento")
            .setMessage("¿Estás seguro de que quieres eliminar este evento? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteEvent()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                // Heurística 3: El usuario tiene control para cancelar
                showSuccess("Eliminación cancelada")
            }
            .setNeutralButton("Más tarde") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteEvent() {
        editingEvent?.let { event ->
            val rowsDeleted = dbHelper.deleteEvent(event.id)
            if (rowsDeleted > 0) {
                showSuccess("Evento eliminado correctamente")
                findNavController().popBackStack()
            } else {
                showError("Error al eliminar el evento")
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validar título
        val title = binding.titleEditText.text.toString()
        if (title.isEmpty()) {
            binding.titleEditText.error = "El título es obligatorio"
            isValid = false
        } else if (title.length < 2) {
            binding.titleEditText.error = "El título debe tener al menos 2 caracteres"
            isValid = false
        } else {
            binding.titleEditText.error = null
        }

        // Validar fecha
        val fecha = binding.dateEditText.text.toString()
        if (fecha.isEmpty()) {
            binding.dateEditText.error = "La fecha es obligatoria"
            isValid = false
        } else if (!isValidDate(fecha)) {
            binding.dateEditText.error = "Formato de fecha inválido (dd/MM/yyyy)"
            isValid = false
        } else {
            binding.dateEditText.error = null
        }

        // Validar hora
        val hora = binding.timeEditText.text.toString()
        if (hora.isEmpty()) {
            binding.timeEditText.error = "La hora es obligatoria"
            isValid = false
        } else if (!isValidTime(hora)) {
            binding.timeEditText.error = "Formato de hora inválido (HH:mm)"
            isValid = false
        } else {
            binding.timeEditText.error = null
        }

        // Validar que la fecha no sea en el pasado
        if (isValid && isDateInPast(fecha, hora)) {
            binding.dateEditText.error = "No puedes agendar eventos en el pasado"
            isValid = false
        }

        return isValid
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidTime(time: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(time)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isDateInPast(date: String, time: String): Boolean {
        return try {
            val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val eventDateTime = dateTimeFormat.parse("$date $time")
            eventDateTime?.before(Date()) ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun showError(message: String) {
        // Heurística 9: Ayuda a reconocer errores
        Toast.makeText(requireContext(), "❌ $message", Toast.LENGTH_LONG).show()
    }

    private fun saveEvent() {
        val event = createEventFromInput()
        val eventId = dbHelper.addEvent(event)
        if (eventId != -1L) {
            // Crear una nueva instancia del evento con el ID correcto
            val eventWithId = event.copy(id = eventId)
            // Programar notificación REAL
            notificationHelper.scheduleNotification(eventWithId)
            showSuccess("✅ Evento guardado correctamente\n📱 Recibirás una notificación 30 minutos antes")
            findNavController().popBackStack()
        } else {
            showError("Error al guardar el evento en la base de datos")
            // Heurística 1: Restaurar estado en caso de error
            binding.saveButton.text = "Guardar"
            binding.saveButton.isEnabled = true
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun updateEvent() {
        val event = createEventFromInput().copy(id = editingEvent?.id ?: 0)
        val rowsAffected = dbHelper.updateEvent(event)
        if (rowsAffected > 0) {
            // Re-programar notificación
            notificationHelper.scheduleNotification(event)
            showSuccess("✅ Evento actualizado correctamente")
            findNavController().popBackStack()
        } else {
            showError("Error al actualizar el evento")
            binding.saveButton.text = "Guardar"
            binding.saveButton.isEnabled = true
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun createEventFromInput(): Event {
        return Event(
            title = binding.titleEditText.text.toString(),
            type = binding.typeSpinner.selectedItem?.toString() ?: "Cita",
            contactName = selectedContact?.first ?: "",
            contactId = selectedContact?.second ?: "",
            locationLat = selectedLocation?.first ?: 0.0,
            locationLng = selectedLocation?.second ?: 0.0,
            description = binding.descriptionEditText.text.toString(),
            date = binding.dateEditText.text.toString(),
            time = binding.timeEditText.text.toString(),
            status = if (editingEvent != null) binding.statusSpinner.selectedItem?.toString() ?: "Pendiente" else "Pendiente",
            reminder = binding.reminderSpinner.selectedItem?.toString() ?: "30_minutos"
        )
    }

    private fun showSuccess(message: String) {
        // Heurística 1: Feedback claro del estado
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}