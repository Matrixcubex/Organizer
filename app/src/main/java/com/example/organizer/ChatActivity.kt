package com.example.organizer

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.organizer.ai.CentralAIService
import com.example.organizer.ai.models.InputType

class ChatActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollView: ScrollView

    private lateinit var aiService: CentralAIService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        aiService = CentralAIService(this)

        initViews()
        setupClickListeners()

        // Mensaje de bienvenida del asistente
        addMessageToChat("¡Hola! Soy tu asistente personal. Puedo ayudarte con:\n• Agendar citas 📅\n• Crear recordatorios 🔔\n• Buscar información 🔍\n• Llamadas de emergencia 🚨\n• Y mucho más...", false)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)
        chatContainer = findViewById(R.id.chat_container)
        scrollView = findViewById(R.id.scroll_view)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSend.setOnClickListener {
            sendMessage()
        }

        // Habilitar/deshabilitar botón enviar
        etMessage.addTextChangedListener {
            btnSend.isEnabled = it?.toString()?.trim()?.isNotEmpty() == true
        }

        // Enviar con Enter
        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val message = etMessage.text.toString().trim()
        if (message.isNotEmpty()) {
            addMessageToChat(message, true)
            etMessage.text.clear()
            processUserMessage(message)
        }
    }

    private fun processUserMessage(userMessage: String) {
        showTypingIndicator()

        Thread {
            try {
                val action = aiService.processInput(userMessage, InputType.TEXT)

                runOnUiThread {
                    removeTypingIndicator()
                    addMessageToChat(action.response, false)
                    action.execute?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    removeTypingIndicator()
                    addMessageToChat("❌ Ocurrió un error. Por favor, intenta de nuevo.", false)
                }
            }
        }.start()
    }

    private fun addMessageToChat(message: String, isUser: Boolean) {
        runOnUiThread {
            val layoutRes = if (isUser) R.layout.layout_message_user else R.layout.layout_message_assistant
            val messageView = LayoutInflater.from(this).inflate(layoutRes, chatContainer, false)

            val tvMessage = messageView.findViewById<TextView>(R.id.tv_message)
            tvMessage.text = message

            chatContainer.addView(messageView)
            scrollToBottom()
        }
    }

    private fun showTypingIndicator() {
        runOnUiThread {
            val typingView = LayoutInflater.from(this).inflate(R.layout.layout_message_assistant, chatContainer, false)
            typingView.id = R.id.typing_indicator
            val tvMessage = typingView.findViewById<TextView>(R.id.tv_message)
            tvMessage.text = "escribiendo..."
            tvMessage.setTypeface(tvMessage.typeface, android.graphics.Typeface.ITALIC)

            chatContainer.addView(typingView)
            scrollToBottom()
        }
    }

    private fun removeTypingIndicator() {
        runOnUiThread {
            val typingView = chatContainer.findViewById<View>(R.id.typing_indicator)
            typingView?.let {
                chatContainer.removeView(it)
            }
        }
    }

    private fun scrollToBottom() {
        chatContainer.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}