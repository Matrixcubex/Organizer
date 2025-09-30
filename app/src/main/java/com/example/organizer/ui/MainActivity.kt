package com.example.organizer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var btnActivate: ImageButton
    private lateinit var btnActivities: ImageButton
    private lateinit var btnChat: ImageButton
    private lateinit var tvStatus: TextView

    private var isAssistantActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        // CAMBIA Button por ImageButton
        btnActivate = findViewById(R.id.btn_activate)
        btnActivities = findViewById(R.id.btn_activities)
        btnChat = findViewById(R.id.btn_chat)
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
    }

    private fun toggleVoiceAssistant() {
        isAssistantActive = !isAssistantActive
        updateUI()
    }

    private fun updateUI() {
        if (isAssistantActive) {
            tvStatus.text = "Asistente activo - Di 'AcompañaMe'"
            // Si quieres cambiar la imagen según el estado:
            // btnActivate.setImageResource(R.drawable.ic_activate_active)
        } else {
            tvStatus.text = "Asistente pausado - Toca para activar"
            // btnActivate.setImageResource(R.drawable.ic_activate_inactive)
        }
    }
}