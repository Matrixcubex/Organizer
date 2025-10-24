package com.example.organizer.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiAIClient(private val context: Context) {

    companion object {
        // ⚠️ REEMPLAZA CON TU API KEY REAL
        private const val GEMINI_API_KEY = "AIzaSyAQlbMl7WjVhQfwRKX8VoQuxb1fC1hceq4"
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"    }

    // EN GeminiAIClient.kt - REEMPLAZA el método processUserInput:
    suspend fun processUserInput(userInput: String): String = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()

            // ✅ PROMPT MÁS CORTO Y EFICIENTE
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", """
Analiza la solicitud y responde EXACTAMENTE en formato: [CLAVE]:[DATOS]

CLAVES: cita, recordatorio, contactos, maps, emergencia, internet, response

EJEMPLOS:
"Agenda cita" → "cita:Cita con doctor"
"Recuérdame algo" → "recordatorio:Comprar leche" 
"Llama a mamá" → "contactos:Llamar a mamá"
"Mapa hospital" → "maps:Hospital"
"Ambulancia" → "emergencia:Ambulancia"
"Busca gatos" → "internet:gatos"
"Hola" → "response:¡Hola!"

SOLICITUD: "$userInput"
                            """.trimIndent())
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 1024) // ✅ REDUCIDO
                    put("topP", 0.8)
                })
            }

            Log.d("GEMINI_REQUEST", "Enviando solicitud...")

            val body = requestBody.toString().toRequestBody("application/json".toMediaType())

            val url = "$GEMINI_URL?key=$GEMINI_API_KEY"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            Log.d("GEMINI_RESPONSE", "Código: ${response.code}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("GEMINI_RESPONSE", "Respuesta completa: $responseBody")

                val jsonResponse = JSONObject(responseBody)

                // ✅ MANEJO MEJORADO DE ERRORES Y RESPUESTAS VACÍAS
                if (jsonResponse.has("candidates")) {
                    val candidates = jsonResponse.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)

                        // ✅ VERIFICAR SI HAY CONTENIDO VÁLIDO
                        if (candidate.has("content") &&
                            candidate.getJSONObject("content").has("parts")) {

                            val content = candidate.getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                                .trim()

                            Log.d("GEMINI_CONTENT", "Contenido procesado: $content")

                            // Validar formato
                            return@withContext if (isValidFormat(content)) {
                                content
                            } else {
                                "response:$content" // Fallback con formato
                            }
                        } else {
                            Log.e("GEMINI_ERROR", "Respuesta sin contenido válido")
                            return@withContext "response:No pude procesar tu solicitud. Intenta de nuevo."
                        }
                    }
                }

                // ✅ VERIFICAR SI HAY ERROR EN LA RESPUESTA
                if (jsonResponse.has("error")) {
                    val error = jsonResponse.getJSONObject("error")
                    val errorMsg = error.getString("message")
                    Log.e("GEMINI_ERROR", "Error API: $errorMsg")
                    return@withContext "response:Error del servicio: $errorMsg"
                }

            } else {
                val errorBody = response.body?.string()
                Log.e("GEMINI_ERROR", "Error HTTP ${response.code}: $errorBody")
            }

            return@withContext "response:Error de conexión. Intenta nuevamente."

        } catch (e: Exception) {
            Log.e("GEMINI_CLIENT", "Error: ${e.message}")
            return@withContext "response:Error de conexión. Verifica tu internet."
        }
    }

    // ✅ MÉTODO AUXILIAR MEJORADO
    private fun isValidFormat(response: String): Boolean {
        if (response.isBlank()) return false

        val validKeys = listOf("cita", "recordatorio", "contactos", "maps", "emergencia", "internet", "response")
        val parts = response.split(":", limit = 2)

        return parts.size == 2 &&
                validKeys.contains(parts[0].trim().lowercase()) &&
                parts[1].trim().isNotBlank()
    }


}