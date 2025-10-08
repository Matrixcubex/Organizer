package com.example.organizer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.organizer.ai.CentralAIService
import com.example.organizer.ai.models.InputType

class ChatActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var chatContainer: LinearLayout

    private lateinit var aiService: CentralAIService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        aiService = CentralAIService(this) // ← Ahora debería funcionar

        initViews()
        setupClickListeners()

        addMessageToChat("Asistente", "¡Hola! Soy tu asistente personal. Puedo ayudarte con:\n• Agendar citas 📅\n• Crear recordatorios 🔔\n• Buscar información 🔍\n• Llamadas de emergencia 🚨\n• Y mucho más...")
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
                processUserMessage(message)
            }
        }

        // Habilitar/deshabilitar botón enviar
        etMessage.addTextChangedListener {
            btnSend.isEnabled = it?.toString()?.trim()?.isNotEmpty() == true
        }
    }

    private fun processUserMessage(userMessage: String) {
        showTypingIndicator()

        Thread {
            try {
                val action = aiService.processInput(userMessage, InputType.TEXT)

                runOnUiThread {
                    removeTypingIndicator()
                    addMessageToChat("Asistente", action.response)
                    action.execute?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    removeTypingIndicator()
                    addMessageToChat("Asistente", "❌ Ocurrió un error. Por favor, intenta de nuevo.")
                }
            }
        }.start()
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

        // Scroll corregido
        chatContainer.post {
            val scrollView = chatContainer.parent as? android.widget.ScrollView
            scrollView?.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun showTypingIndicator() {
        runOnUiThread {
            val typingView = TextView(this)
            typingView.id = android.R.id.custom // Usar ID temporal
            typingView.text = "Asistente: escribiendo..."
            typingView.setPadding(32, 16, 32, 16)
            typingView.textSize = 14f
            typingView.setBackgroundColor(0xFFF5F5F5.toInt())
            typingView.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
            typingView.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 8, 0, 8)
            typingView.layoutParams = layoutParams

            chatContainer.addView(typingView)

            chatContainer.post {
                val scrollView = chatContainer.parent as? android.widget.ScrollView
                scrollView?.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
    }

    private fun removeTypingIndicator() {
        runOnUiThread {
            val typingView = chatContainer.findViewById<TextView>(android.R.id.custom)
            typingView?.let {
                chatContainer.removeView(it)
            }
        }
    }
}