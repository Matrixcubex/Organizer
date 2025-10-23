// REEMPLAZAR GeminiAIClient.kt completo:
package com.example.organizer.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class GeminiAIClient(private val context: Context) {

    companion object {
        // ⚠️ REEMPLAZA ESTA KEY CON LA TUYA de Google AI Studio
        private const val GEMINI_API_KEY = "AIzaSyAxSvv7DP4BE7agPwOob7fjcgkJvLsq1zU"
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
    }

    // EN GeminiAIClient.kt - MEJORAR el método processUserInput:
    // EN GeminiAIClient.kt - MEJORAR el prompt:
    suspend fun processUserInput(userInput: String): String = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()

            val requestBody = JSONObject().apply {
                put("contents", arrayOf(
                    JSONObject().apply {
                        put("parts", arrayOf(
                            JSONObject().apply {
                                put("text", """
                                Eres el cerebro central de una app de organización. Analiza QUÉ QUIERE el usuario y responde naturalmente.

                                CONTEXTO Y FUNCIONALIDADES:
                                - Tienes acceso a: mapas, agenda, recordatorios, búsquedas web, contactos de emergencia
                                - Responde en español, máximo 2-3 líneas
                                - Sé amable, útil y directo

                                EJEMPLOS:
                                Usuario: "Necesito ir al hospital más cercano"
                                Tú: "Te ayudo con la ruta al hospital más cercano 🗺️"

                                Usuario: "Busca videos de gatos en internet"  
                                Tú: "Buscando videos de gatos en internet 🐱"

                                Usuario: "Agenda una cita con el dentista el viernes"
                                Tú: "Agendando cita con el dentista para el viernes 📅"

                                Usuario: "Hola, cómo estás?"
                                Tú: "¡Hola! Estoy bien, listo para ayudarte con lo que necesites 😊"

                                Usuario: "Qué es la inteligencia artificial?"
                                Tú: "La inteligencia artificial es... [explicación breve]"

                                Ahora analiza esta solicitud y responde naturalmente:

                                Usuario: "$userInput"
                            """.trimIndent())
                            }
                        ))
                    }
                ))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 300)
                })
            }

            val body = requestBody.toString().toRequestBody("application/json".toMediaType())

            val url = "$GEMINI_URL?key=$GEMINI_API_KEY"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("GEMINI_RESPONSE", "Respuesta: $responseBody")

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val content = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    return@withContext content.trim()
                }
            } else {
                Log.e("GEMINI_ERROR", "Error: ${response.code}")
            }

            return@withContext "Lo siento, hubo un error. ¿Podrías intentarlo de nuevo?"

        } catch (e: Exception) {
            Log.e("GEMINI_CLIENT", "Error: ${e.message}")
            return@withContext "Error de conexión. Verifica tu internet."
        }
    }
}