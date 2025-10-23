// VoiceAssistantActivity.kt - NUEVO ARCHIVO
package com.example.organizer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.organizer.ai.CentralAIService
import com.example.organizer.ai.voice.TextToSpeechService
import com.example.organizer.ai.voice.VoiceRecognitionService
import com.example.organizer.databinding.ActivityVoiceAssistantBinding

class VoiceAssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceAssistantBinding
    private lateinit var voiceRecognition: VoiceRecognitionService
    private lateinit var textToSpeech: TextToSpeechService
    private lateinit var aiService: CentralAIService
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnStartListening: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        initServices()
        setupClickListeners()
    }

    private fun initViews() {
        tvStatus = binding.tvStatus
        tvResult = binding.tvResult
        btnStartListening = binding.btnStartListening
    }

    private fun initServices() {
        voiceRecognition = VoiceRecognitionService(this)
        textToSpeech = TextToSpeechService(this)
        aiService = CentralAIService(this)
    }

    private fun setupClickListeners() {
        btnStartListening.setOnClickListener {
            startVoiceRecognition()
        }
    }

    private fun startVoiceRecognition() {
        tvStatus.text = "Escuchando..."
        btnStartListening.isEnabled = false

        voiceRecognition.startListening(
            resultCallback = { recognizedText ->
                runOnUiThread {
                    tvResult.text = "Dijiste: $recognizedText"
                    tvStatus.text = "Procesando..."
                    processWithAI(recognizedText)
                }
            },
            errorCallback = { errorMessage ->
                runOnUiThread {
                    tvStatus.text = "Error: $errorMessage"
                    btnStartListening.isEnabled = true
                }
            }
        )
    }

    private fun processWithAI(userInput: String) {
        // Usar el servicio central de IA
        val action = aiService.processInput(userInput, CentralAIService.InputType.VOICE) // ← CORREGIDO


        runOnUiThread {
            tvResult.text = "Tú: $userInput\n\nAsistente: ${action.response}"
            tvStatus.text = "Listo"
            btnStartListening.isEnabled = true

            // Leer la respuesta en voz alta
            textToSpeech.speak(action.response)

            // Ejecutar la acción si existe
            action.execute?.invoke()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecognition.destroy()
        textToSpeech.shutdown()
    }
}