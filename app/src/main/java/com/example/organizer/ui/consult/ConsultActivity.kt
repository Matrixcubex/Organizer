package com.example.organizer.ui.consult

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.organizer.R

class ConsultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consult)

        // Cargar el fragmento
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ConsultFragment())
                .commit()
        }
    }
}