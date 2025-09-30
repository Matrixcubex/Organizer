package com.example.organizer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class TelefonoActivity : AppCompatActivity() {

    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telefono)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }
}