// TextToSpeechService.kt - NUEVO ARCHIVO
package com.example.organizer.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class TextToSpeechService(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES")) ?: TextToSpeech.LANG_MISSING_DATA

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Idioma no soportado")
            } else {
                isInitialized = true
                pendingText?.let { speak(it) }
            }
        } else {
            Log.e("TTS", "Inicialización fallida")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            pendingText = text
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}