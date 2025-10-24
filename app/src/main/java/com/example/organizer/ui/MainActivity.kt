package com.example.organizer.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.organizer.ActivitiesActivity
import com.example.organizer.ChatActivity
import com.example.organizer.R
import com.example.organizer.ai.CentralAIService
import com.example.organizer.ui.consult.ConsultActivity
import java.util.Locale
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnActivate: ImageButton
    private lateinit var btnActivities: ImageButton
    private lateinit var btnChat: ImageButton
    private lateinit var btnConsult: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var transcriptionContainer: LinearLayout
    private lateinit var tvTranscription: TextView

    // ✅ SERVICIOS DE VOZ
    private lateinit var voiceRecognition: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var aiService: CentralAIService

    // ✅ ESTADOS
    private var isAssistantActive = false
    private var isRecording = false
    private var lastRecognizedText = ""

    // ✅ PERMISOS
    private companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initServices()
        setupClickListeners()
        updateUI()
    }

    private fun initViews() {
        btnActivate = findViewById(R.id.btn_activate)
        btnActivities = findViewById(R.id.btn_activities)
        btnChat = findViewById(R.id.btn_chat)
        btnConsult = findViewById(R.id.btn_consult)
        tvStatus = findViewById(R.id.tv_status)

        // ✅ CONTENEDOR DE TRANSCRIPCIÓN (agregado dinámicamente si no existe en XML)
        transcriptionContainer = findViewById(R.id.transcription_container) ?: run {
            // Si no existe en XML, lo creamos programáticamente
            val newContainer = LinearLayout(this).apply {
                id = R.id.transcription_container
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 20, 0, 20)
                }
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.rounded_bg)
            }

            val parentLayout = findViewById<LinearLayout>(R.id.main_container) ?:
            findViewById(android.R.id.content)
            (parentLayout as? LinearLayout)?.addView(newContainer, 2) // Insertar después del botón principal
            newContainer
        }

        // ✅ TEXTVIEW DE TRANSCRIPCIÓN
        tvTranscription = findViewById(R.id.tv_transcription) ?: run {
            val newTextView = TextView(this).apply {
                id = R.id.tv_transcription
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16)
                }
                text = "Tu voz aparecerá aquí..."
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
            }
            transcriptionContainer.addView(newTextView)
            newTextView
        }
    }

    private fun initServices() {
        aiService = CentralAIService(this)
        textToSpeech = TextToSpeech(this, this)

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            voiceRecognition = SpeechRecognizer.createSpeechRecognizer(this)
            voiceRecognition.setRecognitionListener(createRecognitionListener())
        }
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

        btnConsult.setOnClickListener {
            val intent = Intent(this, ConsultActivity::class.java)
            startActivity(intent)
        }
    }

    // ✅ TOGGLE PRINCIPAL - ACTIVAR/DESACTIVAR ASISTENTE
    private fun toggleVoiceAssistant() {
        if (!isAssistantActive) {
            // ✅ ACTIVAR ASISTENTE
            checkAudioPermission()
        } else {
            // ✅ DESACTIVAR ASISTENTE
            stopVoiceAssistant()
        }
    }

    // ✅ VERIFICAR PERMISOS DE AUDIO
    private fun checkAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceAssistant()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) -> {
                tvStatus.text = "Se necesita permiso de micrófono"
                requestAudioPermission()
            }

            else -> {
                requestAudioPermission()
            }
        }
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RECORD_AUDIO_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startVoiceAssistant()
                } else {
                    tvStatus.text = "Permiso denegado"
                    isAssistantActive = false
                    updateUI()
                }
            }
        }
    }

    // ✅ INICIAR ASISTENTE DE VOZ
    private fun startVoiceAssistant() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            tvStatus.text = "Voz no disponible"
            return
        }

        isAssistantActive = true
        isRecording = true
        lastRecognizedText = ""

        updateUI()
        startContinuousListening()


    }

    // ✅ DETENER ASISTENTE DE VOZ
    private fun stopVoiceAssistant() {
        isAssistantActive = false
        isRecording = false

        stopListening()
        updateUI()

        // ✅ PROCESAR EL TEXTO RECOLECTADO SI HAY
        if (lastRecognizedText.isNotBlank()) {
            processVoiceCommand(lastRecognizedText)
        }

        speak("Atendiendo solicitud")
    }

    // ✅ ESCUCHA CONTINUA
    private fun startContinuousListening() {
        if (!isRecording || !isAssistantActive) return

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "ES"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        voiceRecognition.startListening(intent)
    }

    private fun stopListening() {
        if (::voiceRecognition.isInitialized) {
            voiceRecognition.stopListening()
        }
    }

    // ✅ LISTENER DE RECONOCIMIENTO DE VOZ
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VOICE_DEBUG", "Listo para escuchar")
                runOnUiThread {
                    tvStatus.text = "Escuchando..."
                }
            }

            override fun onBeginningOfSpeech() {
                Log.d("VOICE_DEBUG", "Inicio de habla detectado")
                runOnUiThread {
                    tvStatus.text = "🎤 Grabando..."
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Nivel de audio
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("VOICE_DEBUG", "Fin de habla detectado")
                runOnUiThread {
                    tvStatus.text = "Procesando..."
                }
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de network"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció habla"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout de habla"
                    else -> "Error: $error"
                }

                Log.e("VOICE_ERROR", errorMessage)

                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    runOnUiThread {
                        tvStatus.text = "Permisos insuficientes"
                        checkAudioPermission()
                    }
                    return
                }

                // ✅ REINICIAR ESCUCHA SI ESTÁ ACTIVO
                if (isRecording && isAssistantActive) {
                    runOnUiThread {
                        startContinuousListening()
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0) ?: ""

                Log.d("VOICE_RESULTS", "Texto reconocido: '$text'")

                runOnUiThread {
                    // ✅ ACUMULAR TEXTO RECONOCIDO
                    if (text.isNotBlank()) {
                        lastRecognizedText = if (lastRecognizedText.isBlank()) {
                            text
                        } else {
                            "$lastRecognizedText $text"
                        }

                        tvTranscription.text = lastRecognizedText
                        tvStatus.text = "Texto capturado ✅"
                    }

                    // ✅ CONTINUAR GRABANDO SI ESTÁ ACTIVO
                    if (isRecording && isAssistantActive) {
                        startContinuousListening()
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = partialMatches?.get(0) ?: ""

                if (partialText.isNotEmpty()) {
                    Log.d("VOICE_PARTIAL", "Texto parcial: '$partialText'")

                    runOnUiThread {
                        // ✅ MOSTRAR TEXTO PARCIAL EN TIEMPO REAL
                        val displayText = if (lastRecognizedText.isBlank()) {
                            partialText
                        } else {
                            "$lastRecognizedText $partialText"
                        }
                        tvTranscription.text = displayText
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    // ✅ PROCESAR COMANDO DE VOZ
    private fun processVoiceCommand(recognizedText: String) {
        val cleanText = recognizedText.trim()

        if (cleanText.isEmpty()) {
            return
        }

        runOnUiThread {
            tvStatus.text = "🤖 Procesando..."
            tvTranscription.text = "Procesando: $cleanText"
        }

        Thread {
            try {
                val action = aiService.processInput(cleanText, CentralAIService.InputType.VOICE)

                runOnUiThread {
                    // ✅ MOSTRAR RESPUESTA
                    tvTranscription.text = "Tú: $cleanText\n\nAsistente: ${action.response}"
                    tvStatus.text = "Respuesta recibida ✅"

                    // ✅ LEER RESPUESTA EN VOZ ALTA (sin emojis)
                    val cleanResponse = removeEmojis(action.response)
                    speak(cleanResponse)

                    // ✅ EJECUTAR ACCIÓN
                    action.execute?.invoke()
                }

            } catch (e: Exception) {
                Log.e("VOICE_PROCESS", "Error: ${e.message}")
                runOnUiThread {
                    tvStatus.text = "❌ Error"
                    tvTranscription.text = "Error procesando: $cleanText"
                    speak("Lo siento, hubo un error")
                }
            }
        }.start()
    }

    // ✅ QUITAR EMOJIS PARA TTS
    private fun removeEmojis(text: String): String {
        return text.replace(Regex("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Sm}\\p{Sc}\\p{Sk}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // ✅ HABLAR TEXTO
    private fun speak(text: String) {
        if (::textToSpeech.isInitialized && !textToSpeech.isSpeaking) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // ✅ ACTUALIZAR UI
    private fun updateUI() {
        if (isAssistantActive) {
            tvStatus.text = if (isRecording) "Grabando..." else "Asistente activo"
            btnActivate.setImageResource(R.drawable.ic_activate) // Cambiar a ícono de desactivar
            transcriptionContainer.visibility = LinearLayout.VISIBLE
        } else {
            tvStatus.text = "Asistente inactivo"
            btnActivate.setImageResource(R.drawable.ic_deactivate) // Cambiar a ícono de activar
            transcriptionContainer.visibility = LinearLayout.VISIBLE
        }
    }

    // ✅ TEXT-TO-SPEECH INIT
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Idioma no soportado")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::voiceRecognition.isInitialized) {
            voiceRecognition.destroy()
        }
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}