package com.example.organizer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class RecordatorioActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var etNombre: EditText
    private lateinit var etHora: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recordatorio)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etNombre = findViewById(R.id.et_nombre)
        etHora = findViewById(R.id.et_hora)
        etDescripcion = findViewById(R.id.et_descripcion)
        btnGuardar = findViewById(R.id.btn_guardar)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnGuardar.setOnClickListener {
            // Aquí irá la lógica para crear notificación
            finish()
        }
    }
}