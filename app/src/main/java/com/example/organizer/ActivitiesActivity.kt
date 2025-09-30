package com.example.organizer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton

class ActivitiesActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var btnAgenda: ImageButton
    private lateinit var btnRecordatorio: ImageButton
    private lateinit var btnUbicacion: ImageButton
    private lateinit var btnContactar: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activities)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        // CAMBIA Button por ImageButton
        btnAgenda = findViewById(R.id.btn_agenda)
        btnRecordatorio = findViewById(R.id.btn_recordatorio)
        btnUbicacion = findViewById(R.id.btn_ubicacion)
        btnContactar = findViewById(R.id.btn_contactar)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnAgenda.setOnClickListener {
            val intent = Intent(this, AgendaActivity::class.java)
            startActivity(intent)
        }

        btnRecordatorio.setOnClickListener {
            val intent = Intent(this, RecordatorioActivity::class.java)
            startActivity(intent)
        }

        btnUbicacion.setOnClickListener {
            val intent = Intent(this, UbicacionActivity::class.java)
            startActivity(intent)
        }

        btnContactar.setOnClickListener {
            val intent = Intent(this, TelefonoActivity::class.java)
            startActivity(intent)
        }
    }
}