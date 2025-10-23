// VoiceRecognitionService.kt - NUEVO ARCHIVO
package com.example.organizer.ai.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.*
import android.os.Bundle
class VoiceRecognitionService(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var onResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun startListening(resultCallback: (String) -> Unit, errorCallback: (String) -> Unit) {
        this.onResult = resultCallback
        this.onError = errorCallback

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("VOICE", "Listo para escuchar")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("VOICE", "Inicio de habla detectado")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Nivel de audio, útil para visualización
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // No necesario para la mayoría de casos
                    }

                    override fun onEndOfSpeech() {
                        Log.d("VOICE", "Fin de habla detectado")
                    }

                    override fun onError(error: Int) {
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                            SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de red"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció el habla"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                            SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout de habla"
                            else -> "Error desconocido"
                        }
                        onError?.invoke(errorMessage)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.get(0) ?: ""
                        Log.d("VOICE", "Texto reconocido: $text")
                        onResult?.invoke(text)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        // Resultados parciales, útil para mostrar en tiempo real
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Eventos especiales
                    }
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } else {
            onError?.invoke("El reconocimiento de voz no está disponible en este dispositivo")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }
}