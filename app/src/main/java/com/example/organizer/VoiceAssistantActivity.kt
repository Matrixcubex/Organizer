package com.example.organizer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.organizer.ai.CentralAIService
import com.example.organizer.databinding.ActivityVoiceAssistantBinding
import java.util.Locale

class VoiceAssistantActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityVoiceAssistantBinding
    private lateinit var voiceRecognition: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var aiService: CentralAIService
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnStartListening: Button
    private lateinit var btnStopListening: Button

    // ✅ VARIABLES PARA DETECCIÓN DE "ACOMPÁÑAME"
    private var isWaitingForCommand = false
    private var activationPhrase = "acompáñame"
    private var lastRecognizedText = ""

    // ✅ CÓDIGO DE SOLICITUD DE PERMISOS
    private companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 1001
    }

    // ✅ BROADCAST RECEIVER PARA DETENER DESDE MAIN ACTIVITY
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "STOP_VOICE_ASSISTANT") {
                Log.d("VOICE_ASSISTANT", "Recibido comando para detener")
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ REGISTRAR BROADCAST RECEIVER
        val filter = IntentFilter("STOP_VOICE_ASSISTANT")
        LocalBroadcastManager.getInstance(this).registerReceiver(stopReceiver, filter)

        initViews()
        initServices()
        setupClickListeners()

        // ✅ VERIFICAR PERMISOS ANTES DE INICIAR
        checkAudioPermission()
    }

    private fun initViews() {
        tvStatus = binding.tvStatus
        tvResult = binding.tvResult
        btnStartListening = binding.btnStartListening
        btnStopListening = binding.btnStopListening

        tvStatus.text = "🔍 Verificando permisos..."
        btnStopListening.isEnabled = false
        btnStartListening.isEnabled = false
    }

    private fun initServices() {
        aiService = CentralAIService(this)
        textToSpeech = TextToSpeech(this, this)
    }

    private fun setupClickListeners() {
        btnStartListening.setOnClickListener {
            checkAudioPermission()
        }

        btnStopListening.setOnClickListener {
            stopListeningAndClose()
        }
    }

    // ✅ VERIFICAR Y SOLICITAR PERMISOS DE AUDIO
    private fun checkAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // ✅ PERMISO CONCEDIDO - INICIAR SERVICIOS
                Log.d("PERMISSION", "Permiso de audio concedido")
                initializeVoiceRecognition()
                startContinuousListening()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) -> {
                // ✅ EXPLICAR AL USUARIO POR QUÉ SE NECESITA EL PERMISO
                Toast.makeText(
                    this,
                    "El permiso de micrófono es necesario para el reconocimiento de voz",
                    Toast.LENGTH_LONG
                ).show()
                requestAudioPermission()
            }

            else -> {
                // ✅ SOLICITAR PERMISO DIRECTAMENTE
                requestAudioPermission()
            }
        }
    }

    // ✅ SOLICITAR PERMISO DE AUDIO
    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_CODE
        )
    }

    // ✅ MANEJAR RESPUESTA DE SOLICITUD DE PERMISOS
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RECORD_AUDIO_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ✅ PERMISO CONCEDIDO
                    Log.d("PERMISSION", "Usuario concedió permiso de audio")
                    Toast.makeText(this, "Permiso de micrófono concedido", Toast.LENGTH_SHORT).show()
                    initializeVoiceRecognition()
                    startContinuousListening()
                } else {
                    // ✅ PERMISO DENEGADO
                    Log.e("PERMISSION", "Usuario denegó permiso de audio")
                    tvStatus.text = "❌ Permiso de micrófono denegado"
                    Toast.makeText(
                        this,
                        "No se puede usar el reconocimiento de voz sin permiso de micrófono",
                        Toast.LENGTH_LONG
                    ).show()

                    // Habilitar botón para intentar nuevamente
                    btnStartListening.isEnabled = true
                    btnStartListening.text = "🎤 Solicitar Permiso"
                }
            }
        }
    }

    // ✅ INICIALIZAR RECONOCIMIENTO DE VOZ (SOLO SI HAY PERMISOS)
    private fun initializeVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "El reconocimiento de voz no está disponible", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        voiceRecognition = SpeechRecognizer.createSpeechRecognizer(this)
        voiceRecognition.setRecognitionListener(createRecognitionListener())

        // Actualizar UI
        btnStopListening.isEnabled = true
        btnStartListening.isEnabled = false
        tvStatus.text = "🎤 Iniciando asistente de voz..."
    }

    // ✅ DETENER Y CERRAR
    private fun stopListeningAndClose() {
        stopListening()
        finish()
    }

    // ✅ ESCUCHA CONTINUA CON DETECCIÓN DE "ACOMPÁÑAME"
    private fun startContinuousListening() {
        // Verificar permisos nuevamente por seguridad
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            checkAudioPermission()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Reconocimiento de voz no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        btnStartListening.isEnabled = false
        btnStopListening.isEnabled = true
        isWaitingForCommand = false

        tvStatus.text = "🎤 Escuchando... Di '$activationPhrase' seguido de tu comando"

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "ES"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di '$activationPhrase' seguido de tu comando")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        voiceRecognition.startListening(intent)
    }

    private fun stopListening() {
        if (::voiceRecognition.isInitialized) {
            voiceRecognition.stopListening()
        }
        btnStartListening.isEnabled = true
        btnStopListening.isEnabled = false
        tvStatus.text = "Escucha detenida"
        isWaitingForCommand = false
    }

    // ✅ LISTENER MEJORADO CON DETECCIÓN DE FRASE DE ACTIVACIÓN
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VOICE_DEBUG", "Listo para escuchar")
            }

            override fun onBeginningOfSpeech() {
                Log.d("VOICE_DEBUG", "Inicio de habla detectado")
                tvStatus.text = "🎤 Hablando..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Nivel de audio
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("VOICE_DEBUG", "Fin de habla detectado")
                tvStatus.text = "🔄 Procesando..."
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de network"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció el habla"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout de habla"
                    else -> "Error desconocido: $error"
                }

                Log.e("VOICE_ERROR", errorMessage)

                // ✅ MANEJAR ERROR DE PERMISOS ESPECÍFICAMENTE
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    runOnUiThread {
                        tvStatus.text = "❌ Permisos insuficientes"
                        Toast.makeText(this@VoiceAssistantActivity,
                            "Se necesitan permisos de micrófono", Toast.LENGTH_LONG).show()
                        checkAudioPermission()
                    }
                    return
                }

                // ✅ REINICIAR ESCUCHA EN CASO DE OTROS ERRORES
                runOnUiThread {
                    tvStatus.text = "Error: $errorMessage. Reiniciando..."
                    tvStatus.postDelayed({
                        if (!btnStartListening.isEnabled) {
                            startContinuousListening()
                        }
                    }, 1000)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0) ?: ""

                Log.d("VOICE_RESULTS", "Texto reconocido: '$text'")
                lastRecognizedText = text

                runOnUiThread {
                    tvResult.text = "🎤 Dijiste: $text"
                    processVoiceCommand(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // ✅ DETECCIÓN EN TIEMPO REAL DE "ACOMPÁÑAME"
                val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = partialMatches?.get(0) ?: ""

                if (partialText.isNotEmpty()) {
                    Log.d("VOICE_PARTIAL", "Texto parcial: '$partialText'")

                    // Detectar si contiene la frase de activación
                    if (partialText.lowercase().contains(activationPhrase) && !isWaitingForCommand) {
                        isWaitingForCommand = true
                        runOnUiThread {
                            tvStatus.text = "✅ Frase de activación detectada. Continúa con tu comando..."
                            speak("Te escucho")
                        }
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    // ✅ PROCESAR COMANDO DE VOZ CON DETECCIÓN DE "ACOMPÁÑAME"
    private fun processVoiceCommand(recognizedText: String) {
        val cleanText = recognizedText.trim()

        if (cleanText.isEmpty()) {
            restartListening()
            return
        }

        // ✅ EXTRAER COMANDO DESPUÉS DE "ACOMPÁÑAME"
        val finalCommand = if (cleanText.lowercase().contains(activationPhrase)) {
            val parts = cleanText.split(activationPhrase, ignoreCase = true)
            if (parts.size > 1) {
                parts[1].trim()
            } else {
                cleanText
            }
        } else if (isWaitingForCommand) {
            cleanText
        } else {
            runOnUiThread {
                tvStatus.text = "❌ Di '$activationPhrase' antes de tu comando. Escuchando..."
            }
            restartListening()
            return
        }

        if (finalCommand.isBlank()) {
            runOnUiThread {
                tvStatus.text = "No escuché tu comando. Intenta de nuevo."
            }
            restartListening()
            return
        }

        // ✅ PROCESAR CON IA
        runOnUiThread {
            tvStatus.text = "🤖 Procesando: $finalCommand"
        }

        Thread {
            try {
                val action = aiService.processInput(finalCommand, CentralAIService.InputType.VOICE)

                runOnUiThread {
                    // ✅ MOSTRAR RESPUESTA EN UI
                    tvResult.text = "🎤 Dijiste: $finalCommand\n\n🤖 Asistente: ${action.response}"

                    // ✅ LEER RESPUESTA EN VOZ ALTA (sin emojis)
                    val cleanResponse = removeEmojis(action.response)
                    speak(cleanResponse)

                    // ✅ EJECUTAR ACCIÓN SI EXISTE
                    action.execute?.invoke()

                    // ✅ REINICIAR ESCUCHA DESPUÉS DE PROCESAR
                    tvStatus.postDelayed({
                        if (!btnStartListening.isEnabled) {
                            tvStatus.text = "🎤 Escuchando... Di '$activationPhrase' seguido de tu comando"
                            isWaitingForCommand = false
                            startContinuousListening()
                        }
                    }, 2000)
                }

            } catch (e: Exception) {
                Log.e("VOICE_PROCESS", "Error procesando comando: ${e.message}")
                runOnUiThread {
                    tvStatus.text = "❌ Error procesando comando"
                    speak("Lo siento, hubo un error")
                    restartListening()
                }
            }
        }.start()
    }

    // ✅ QUITAR EMOJIS Y CARACTERES ESPECIALES PARA TTS
    private fun removeEmojis(text: String): String {
        return text.replace(Regex("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Sm}\\p{Sc}\\p{Sk}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // ✅ HABLAR TEXTO
    private fun speak(text: String) {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // ✅ REINICIAR ESCUCHA
    private fun restartListening() {
        runOnUiThread {
            if (!btnStartListening.isEnabled) {
                tvStatus.text = "🎤 Escuchando... Di '$activationPhrase' seguido de tu comando"
                isWaitingForCommand = false
                startContinuousListening()
            }
        }
    }

    // ✅ TEXT-TO-SPEECH INIT
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Idioma español no soportado", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("TTS", "Text-to-Speech inicializado correctamente")
            }
        } else {
            Toast.makeText(this, "Error inicializando Text-to-Speech", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ DESREGISTRAR BROADCAST RECEIVER
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stopReceiver)

        if (::voiceRecognition.isInitialized) {
            voiceRecognition.destroy()
        }
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}