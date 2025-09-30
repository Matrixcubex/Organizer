package com.example.organizer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity  // ← ESTE IMPORT ES CLAVE

class ChatActivity : AppCompatActivity() {  // ← Y ESTA HERENCIA TAMBIÉN

    private lateinit var btnBack: Button
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var chatContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)
        chatContainer = findViewById(R.id.chat_container)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                addMessageToChat("Tú", message)
                etMessage.text.clear()
                simulateAssistantResponse(message)
            }
        }
    }

    private fun addMessageToChat(sender: String, message: String) {
        val messageView = TextView(this)
        messageView.text = "$sender: $message"
        messageView.setPadding(32, 16, 32, 16)
        messageView.textSize = 16f

        if (sender == "Tú") {
            messageView.setBackgroundColor(0xFFE3F2FD.toInt())
            messageView.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
        } else {
            messageView.setBackgroundColor(0xFFF5F5F5.toInt())
            messageView.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 8, 0, 8)
        messageView.layoutParams = layoutParams

        chatContainer.addView(messageView)
    }

    private fun simulateAssistantResponse(userMessage: String) {
        val response = when {
            userMessage.contains("hola", ignoreCase = true) -> "¡Hola! ¿En qué puedo ayudarte?"
            userMessage.contains("agenda", ignoreCase = true) -> "Puedo ayudarte a agendar una cita. ¿Para qué día y hora?"
            userMessage.contains("recordatorio", ignoreCase = true) -> "Puedo crear un recordatorio. ¿De qué se trata?"
            else -> "Entendido. ¿Necesitas ayuda con algo específico?"
        }

        addMessageToChat("Asistente", response)
    }
}