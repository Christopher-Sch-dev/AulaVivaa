package cl.duocuc.aulaviva.data.repository

import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 🤖 GEMINI AI - IMPLEMENTACIÓN REST DIRECTA
 * Usa la API REST de Google en lugar del SDK para mayor compatibilidad
 */
class IARepository {
    
    private val GEMINI_API_KEY = "AIzaSyA6e4Wle5UkV93rOKIWm4FIKTQBDaOy8EY"
    private val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
    
    init {
        println("✅ Gemini AI REST activado")
    }
    
    /**
     * Llama a Gemini API REST con manejo robusto de errores
     */
    private suspend fun llamarGeminiREST(prompt: String): String {
        return withTimeout(30000L) {
            try {
                val url = URL("$API_URL?key=$GEMINI_API_KEY")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Body JSON según documentación de Gemini
                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("topK", 40)
                        put("topP", 0.95)
                        put("maxOutputTokens", 1024)
                    })
                }
                
                // Enviar request
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }
                
                // Leer respuesta
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                    
                    // Parsear respuesta JSON
                    val jsonResponse = JSONObject(response)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val content = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        return@withTimeout content
                    } else {
                        throw Exception("Sin respuesta de Gemini")
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorResponse = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "Sin detalles"
                    }
                    throw Exception("HTTP $responseCode: $errorResponse")
                }
                
            } catch (e: Exception) {
                throw Exception("Error Gemini: ${e.message}")
            }
        }
    }
    
    /**
     * Genera resumen con REST API
     */
    suspend fun generarResumen(textoClase: String): String {
        return try {
            val prompt = """
                Eres Gemini AI de Google. Analiza este contenido educativo:
                
                $textoClase
                
                Genera un resumen breve con:
                📝 Tema principal (1 frase)
                🎯 3 puntos clave
                💡 1 consejo de estudio
                
                Termina con: "⚡ Generado por Gemini AI Real de Google"
            """.trimIndent()
            
            llamarGeminiREST(prompt)
            
        } catch (e: Exception) {
            """
            ⚠️ Error al conectar con Gemini
            
            📝 RESUMEN LOCAL:
            
            📌 Tema: ${textoClase.take(80)}...
            
            🎯 Análisis:
            • ${textoClase.split(" ").size} palabras
            • Contenido educativo
            • Nivel intermedio
            
            💡 Verifica conexión a internet
            
            Error: ${e.message?.take(100) ?: "Desconocido"}
            """.trimIndent()
        }
    }
    
    /**
     * Genera glosario con REST API
     */
    suspend fun generarGlosario(textoClase: String): String {
        return try {
            val prompt = """
                Analiza este texto educativo y extrae 5 términos clave:
                
                $textoClase
                
                Formato:
                📚 GLOSARIO
                📖 TÉRMINO → Definición breve
                
                ⚡ Generado por Gemini AI de Google
            """.trimIndent()
            
            llamarGeminiREST(prompt)
            
        } catch (e: Exception) {
            """
            ⚠️ Error con Gemini
            
            📚 GLOSARIO LOCAL:
            
            📖 ANDROID → Sistema operativo móvil
            📖 KOTLIN → Lenguaje de programación
            📖 MVVM → Patrón arquitectónico
            
            Error: ${e.message?.take(100) ?: "Desconocido"}
            """.trimIndent()
        }
    }
    
    /**
     * Ideas para profesor con REST API
     */
    suspend fun analizarPDFParaProfesor(nombreArchivo: String, descripcionClase: String): String {
        return try {
            val prompt = """
                Dame 3 ideas de actividades educativas para:
                
                Archivo: $nombreArchivo
                Tema: $descripcionClase
                
                Formato:
                💡 3 actividades concretas
                ⏱️ Tiempo: 60 minutos
                
                ⚡ Gemini AI
            """.trimIndent()
            
            llamarGeminiREST(prompt)
            
        } catch (e: Exception) {
            """
            ⚠️ Error con Gemini
            
            💡 IDEAS LOCALES:
            
            📄 $nombreArchivo
            
            1. Presentación interactiva (15 min)
            2. Ejercicios prácticos (30 min)
            3. Discusión grupal (15 min)
            
            Error: ${e.message?.take(100) ?: "Desconocido"}
            """.trimIndent()
        }
    }
}
