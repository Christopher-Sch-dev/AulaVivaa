package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.data.remote.Content
import cl.duocuc.aulaviva.data.remote.GeminiApiService
import cl.duocuc.aulaviva.data.remote.GeminiRequest
import cl.duocuc.aulaviva.data.remote.GenerationConfig
import cl.duocuc.aulaviva.data.remote.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 🤖 GEMINI AI con Retrofit - PROFESIONAL Y RÁPIDO
 * Modelo: gemini-2.5-flash (vigente octubre 2025)
 */
class IARepository {

    private val GEMINI_API_KEY = "AIzaSyA6e4Wle5UkV93rOKIWm4FIKTQBDaOy8EY"

    // ...existing code...
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // Retrofit configurado para Gemini API
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val geminiService = retrofit.create(GeminiApiService::class.java)

    init {
        println("✅ Gemini AI Retrofit activado - Modelo: gemini-2.5-flash")
    }

    /**
     * Llama a Gemini API con Retrofit
     */
    private suspend fun llamarGemini(prompt: String): String {
        return withContext(Dispatchers.IO) {
            withTimeout(40000L) {
                var intento = 0
                var ultimoError: Exception? = null
                val maxIntentos = 3
                while (intento < maxIntentos) {
                    try {
                        // Construir request según docs oficiales
                        val request = GeminiRequest(
                            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                            generationConfig = GenerationConfig(
                                temperature = 0.6f, topK = 40, topP = 0.9f, maxOutputTokens = 2048
                            )
                        )

                        // Hacer petición
                        val response = geminiService.generateContent(GEMINI_API_KEY, request)

                        if (response.isSuccessful) {
                            // Extraer texto de la respuesta
                            val text = response.body()
                                ?.candidates?.firstOrNull()
                                ?.content?.parts?.firstOrNull()
                                ?.text

                            if (!text.isNullOrBlank()) return@withTimeout text
                            throw Exception("Respuesta vacía de Gemini")
                        } else {
                            throw Exception("HTTP ${response.code()}: ${response.message()}")
                        }
                    } catch (e: Exception) {
                        ultimoError = e
                        intento++
                        if (intento < maxIntentos) {
                            // backoff simple
                            try {
                                Thread.sleep(500L * intento)
                            } catch (_: InterruptedException) {
                            }
                            continue
                        } else {
                            throw Exception("Error Gemini: ${e.message}")
                        }
                    }
                }
                throw Exception("Error Gemini: ${ultimoError?.message ?: "Desconocido"}")
            }
        }
    }

    fun markdownToHtml(md: String): String {
        var html = md
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        // Encabezados
        html = html
            .replace(Regex("""^# (.*)$""", RegexOption.MULTILINE), "<h1>$1</h1>")
            .replace(Regex("""^## (.*)$""", RegexOption.MULTILINE), "<h2>$1</h2>")
            .replace(Regex("""^### (.*)$""", RegexOption.MULTILINE), "<h3>$1</h3>")
        // Bullets y listas numeradas
        html = html
            .replace(Regex("""^-\s+(.*)$""", RegexOption.MULTILINE), "<br/>• $1")
            .replace(Regex("""^\d+\.\s+(.*)$""", RegexOption.MULTILINE), "<br/>$0")
        // Negrita e itálica
        html = html
            .replace(Regex("""\*\*(.*?)\*\*"""), "<b>$1</b>")
            .replace(Regex("""\*(.*?)\*"""), "<i>$1</i>")
        // Saltos de línea
        html = html.replace("\n", "<br/>")
        return "<html><head><meta charset='utf-8'/></head><body style='font-family:sans-serif;padding:12px'>$html</body></html>"
    }

    /**
     * 🤖 Genera respuesta personalizada para cualquier prompt
     */
    suspend fun generarRespuestaPersonalizada(prompt: String): String {
        return try {
            llamarGemini(prompt)
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n${e.message?.take(200) ?: "Desconocido"}"
        }
    }

    /**
     * 🤖 Genera resumen educativo
     */
    suspend fun generarResumen(textoClase: String): String {
        return try {
            val prompt = """
                Eres Gemini AI de Google. Analiza este contenido educativo y genera un resumen breve:
                
                $textoClase
                
                Responde con:
                📝 Tema principal (1 frase)
                🎯 3 puntos clave (bullets)
                💡 1 consejo de estudio
                
                Termina con: "⚡ Generado por Gemini AI Real de Google po LOCO e.e"
                
                Máximo 200 palabras, formato claro.
            """.trimIndent()

            llamarGemini(prompt)

        } catch (e: Exception) {
            """
            ⚠️ No pude conectar con Gemini ahora
            
            📝 RESUMEN LOCAL:
            
            📌 Tema: ${textoClase.take(80)}...
            
            🎯 Análisis:
            • ${textoClase.split(" ").size} palabras detectadas
            • Contenido educativo
            • Nivel intermedio
            
            💡 Verifica tu conexión a internet
            
            Error: ${e.message?.take(100) ?: "Desconocido"}
            """.trimIndent()
        }
    }

    /**
     * 🤖 Genera glosario de términos
     */
    suspend fun generarGlosario(textoClase: String): String {
        return try {
            val prompt = """
                Analiza este texto educativo y extrae 5 términos clave con definiciones:
                
                $textoClase
                
                Formato:
                📚 GLOSARIO
                
                📖 TÉRMINO 1
                   → Definición breve (1 línea)
                
                📖 TÉRMINO 2
                   → Definición breve
                
                (continúa con 5 términos)
                
                ⚡ Generado por Gemini AI de Google
                
                Máximo 250 palabras.
            """.trimIndent()

            llamarGemini(prompt)

        } catch (e: Exception) {
            """
            ⚠️ Error con Gemini
            
            📚 GLOSARIO LOCAL:
            
            📖 ANDROID
               → Sistema operativo móvil de Google
            
            📖 KOTLIN
               → Lenguaje de programación moderno
            
            📖 MVVM
               → Patrón arquitectónico para separar UI de lógica
            
            Error: ${e.message?.take(100) ?: "Desconocido"}
            """.trimIndent()
        }
    }

    /**
     * 🤖 Ideas pedagógicas para profesor
     */
    suspend fun analizarPDFParaProfesor(nombreArchivo: String, descripcionClase: String): String {
        return try {
            val prompt = """
                Soy asistente pedagógico. Dame 3 ideas concretas de actividades para esta clase:
                
                Archivo: $nombreArchivo
                Tema: $descripcionClase
                
                Formato:
                💡 IDEAS PARA LA CLASE:
                
                1. [Actividad 1] (tiempo)
                2. [Actividad 2] (tiempo)
                3. [Actividad 3] (tiempo)
                
                ⏱️ Tiempo total: 60 minutos
                
                ⚡ Generado por Gemini AI
                
                Máximo 200 palabras, ideas específicas y prácticas.
            """.trimIndent()

            llamarGemini(prompt)

        } catch (e: Exception) {
            """
            ⚠️ Error con Gemini
            
            💡 IDEAS LOCALES:
            
            📄 $nombreArchivo
            📝 $descripcionClase
            
            1. Presentación interactiva (15 min)
            2. Ejercicios prácticos (30 min)
            3. Discusión y conclusiones (15 min)
            
            Error: ${e.message?.take(100) ?: "Desconocido"}
            """.trimIndent()
        }
    }
}
