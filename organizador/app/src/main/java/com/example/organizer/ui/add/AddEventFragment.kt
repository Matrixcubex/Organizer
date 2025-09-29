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
        }

        setupUI()
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
                .build()

            datePicker.addOnPositiveButtonClickListener { millis ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.dateEditText.setText(dateFormat.format(Date(millis)))
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupTimePicker() {
        binding.timeEditText.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setTitleText(getString(R.string.select_time))
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val time = String.format("%02d:%02d", timePicker.hour, timePicker.minute)
                binding.timeEditText.setText(time)
            }

            timePicker.show(parentFragmentManager, "TIME_PICKER")
        }
    }

    private fun setupContactButton() {
        binding.contactButton.setOnClickListener {
            contactPicker.pickContact { name, id ->
                selectedContact = Pair(name, id)
                binding.contactButton.text = name
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
                if (editingEvent != null) {
                    updateEvent()
                } else {
                    saveEvent()
                }
                findNavController().popBackStack()
            }
        }
    }

    private fun validateForm(): Boolean {
        return when {
            binding.titleEditText.text.isNullOrEmpty() -> {
                showError(getString(R.string.error_title_required))
                false
            }
            binding.dateEditText.text.isNullOrEmpty() -> {
                showError(getString(R.string.error_date_required))
                false
            }
            binding.timeEditText.text.isNullOrEmpty() -> {
                showError(getString(R.string.error_time_required))
                false
            }
            selectedContact == null -> {
                showError(getString(R.string.error_contact_required))
                false
            }
            else -> true
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun saveEvent() {
        val event = createEventFromInput()
        val eventId = dbHelper.addEvent(event)
        if (eventId != -1L) {
            notificationHelper.scheduleNotification(event)
            showSuccess()
        }
    }

    private fun updateEvent() {
        val event = createEventFromInput().copy(id = editingEvent?.id ?: 0)
        val rowsAffected = dbHelper.updateEvent(event)
        if (rowsAffected > 0) {
            showSuccess()
        }
    }

    private fun createEventFromInput(): Event {
        return Event(
            title = binding.titleEditText.text.toString(),
            type = binding.typeSpinner.selectedItem?.toString() ?: "",
            contactName = selectedContact?.first ?: "",
            contactId = selectedContact?.second ?: "",
            locationLat = selectedLocation?.first ?: 0.0,
            locationLng = selectedLocation?.second ?: 0.0,
            description = binding.descriptionEditText.text.toString(),
            date = binding.dateEditText.text.toString(),
            time = binding.timeEditText.text.toString(),
            status = if (editingEvent != null) binding.statusSpinner.selectedItem?.toString() ?: "Pendiente" else "Pendiente",
            reminder = binding.reminderSpinner.selectedItem?.toString() ?: "Sin recordatorio"
        )
    }

    private fun showSuccess() {
        Toast.makeText(requireContext(), getString(R.string.event_saved), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}