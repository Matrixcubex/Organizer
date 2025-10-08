package com.example.organizer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import com.example.organizer.ui.consult.ConsultActivity  // ← AÑADE ESTE IMPORT


class MainActivity : AppCompatActivity() {

    private lateinit var btnActivate: ImageButton
    private lateinit var btnActivities: ImageButton
    private lateinit var btnChat: ImageButton
    private lateinit var btnConsult: ImageButton  // NUEVO
    private lateinit var tvStatus: TextView

    private var isAssistantActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnActivate = findViewById(R.id.btn_activate)
        btnActivities = findViewById(R.id.btn_activities)
        btnChat = findViewById(R.id.btn_chat)
        btnConsult = findViewById(R.id.btn_consult)  // NUEVO
        tvStatus = findViewById(R.id.tv_status)

        updateUI()
    }

    private fun setupClickListeners() {
        btnActivate.setOnClickListener {
            toggleVoiceAssistant()
        }

        btnActivities.setOnClickListener {
            val intent = Intent(this, ActivitiesActivity::class.java)
            startActivity(intent)
        }

        btnChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        // NUEVO: Botón para consultar eventos
        btnConsult.setOnClickListener {
            val intent = Intent(this, ConsultActivity::class.java)
            startActivity(intent)
        }
    }

    private fun toggleVoiceAssistant() {
        isAssistantActive = !isAssistantActive
        updateUI()
    }

    private fun updateUI() {
        if (isAssistantActive) {
            tvStatus.text = "Asistente activo - Di 'AcompañaMe'"
        } else {
            tvStatus.text = "Asistente pausado - Toca para activar"
        }
    }
}