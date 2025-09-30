package com.example.organizer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class UbicacionActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var etDireccion: EditText
    private lateinit var btnBuscar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ubicacion)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etDireccion = findViewById(R.id.et_direccion)
        btnBuscar = findViewById(R.id.btn_buscar)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnBuscar.setOnClickListener {
            // Aquí irá la lógica para Google Maps
        }
    }
}