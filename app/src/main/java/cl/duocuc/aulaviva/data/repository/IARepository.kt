package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.domain.repository.IIARepository

import android.content.Context
import android.util.Log
import cl.duocuc.aulaviva.BuildConfig
import cl.duocuc.aulaviva.data.remote.Content
import cl.duocuc.aulaviva.data.remote.GeminiApiService
import cl.duocuc.aulaviva.data.remote.GeminiRequest
import cl.duocuc.aulaviva.data.remote.GenerationConfig
import cl.duocuc.aulaviva.data.remote.Part
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * `IARepository2` — versión limpia y estable basada en la versión anterior del usuario.
 * - Usa `Context` en el constructor para compatibilidad con el código original
 * - Conserva llamadas a Gemini via Retrofit y manejo local con PDFBox
 * - Implementa chunking, compresión y chat stateful
 */
class IARepository(private val context: Context) : IIARepository {

    companion object {
        private const val TAG = "AulaViva_IA"
    }

    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY

    private val googleAiModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = GEMINI_API_KEY,
            generationConfig = com.google.ai.client.generativeai.type.generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 8192
            }
        )
    }

    private var chatSession: com.google.ai.client.generativeai.Chat? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val geminiService = retrofit.create(GeminiApiService::class.java)

    init {
        try {
            PDFBoxResourceLoader.init(context)
            Log.d(TAG, "✅ PDFBox Android inicializado correctamente")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error inicializando PDFBox: ${e.message}")
        }
    }

    private suspend fun llamarGemini(prompt: String): String {
        return withContext(Dispatchers.IO) {
            withTimeout(60_000L) {
                var intento = 0
                var ultimoError: Exception? = null
                val maxIntentos = 3
                while (intento < maxIntentos) {
                    try {
                        val request = GeminiRequest(
                            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                            generationConfig = GenerationConfig(
                                temperature = 0.6f, topP = 0.9f, maxOutputTokens = 4096
                            )
                        )
                        val response = geminiService.generateContent(GEMINI_API_KEY, request)
                        if (response.isSuccessful) {
                            val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (!text.isNullOrBlank()) return@withTimeout text
                            throw Exception("Respuesta vacía de Gemini")
                        } else {
                            throw Exception("HTTP ${'$'}{response.code()}: ${'$'}{response.message()}")
                        }
                    } catch (e: Exception) {
                        ultimoError = e
                        intento++
                        if (intento < maxIntentos && (e is SocketTimeoutException || e.message?.contains("HTTP 503") == true || e.message?.contains("HTTP 504") == true || e.message?.contains("vacía") == true)) {
                            try { Thread.sleep(800L * intento) } catch (_: InterruptedException) {}
                            continue
                        } else if (intento < maxIntentos) {
                            try { Thread.sleep(400L) } catch (_: InterruptedException) {}
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

    override suspend fun analizarPdfConIA(nombreClase: String, pdfUrl: String?): String {
        return try {
            if (pdfUrl.isNullOrEmpty()) return "⚠️ No se proporcionó URL del PDF"
            analizarPDFInteligente(pdfUrl, "Analiza el PDF y entrega un informe pedagógico para la clase: $nombreClase")
        } catch (e: Exception) {
            "⚠️ Error analizando PDF: ${e.message ?: "Desconocido"}"
        }
    }

    private suspend fun analizarPDFInteligente(pdfUrl: String, prompt: String): String = withContext(Dispatchers.IO) {
        try {
            analizarConGoogleAI(pdfUrl, prompt)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Google AI falló: ${e.message}, usando PDFBox fallback")
            analizarConPDFBox(pdfUrl, prompt)
        }
    }

    private suspend fun analizarConGoogleAI(pdfUrl: String, prompt: String): String {
        val pdfFile = descargarPDFATempFile(pdfUrl)
        if (pdfFile.length() <= 20_000_000L) {
            val pdfBytes = pdfFile.readBytes()
            val contenidoMultimodal = content { text(prompt); blob("application/pdf", pdfBytes) }
            val response = withTimeout(90_000) { googleAiModel.generateContent(contenidoMultimodal) }
            return response.text ?: throw Exception("Respuesta vacía de Google AI SDK")
        } else {
            val chunks = extractTextChunksFromPdf(pdfFile)
            if (chunks.isEmpty()) throw Exception("No se pudo extraer texto del PDF")
            val summaries = mutableListOf<String>()
            for ((index, chunk) in chunks.withIndex()) {
                val chunkPrompt = """
                    $prompt

                    He recibido la parte ${index + 1} de ${chunks.size} del PDF:
                    ${chunk.take(30_000)}

                    Instrucción: Resume los puntos clave y extrae conceptos específicos de esta parte. Responde en español.
                """.trimIndent()
                try { summaries.add(llamarGemini(chunkPrompt)) } catch (e: Exception) { Log.w(TAG, "⚠️ Error resumiendo chunk ${index + 1}: ${e.message}"); summaries.add("[Error resumiendo chunk ${index + 1}: ${e.message}]") }
            }
            val combined = summaries.joinToString("\n\n---\n\n")
            val finalPrompt = """
                $prompt

                He recibido el PDF en partes y he resumido cada sección. A continuación están los resúmenes parciales:
                $combined

                INSTRUCCIÓN FINAL: A partir de los resúmenes parciales, genera un análisis coherente del PDF completo, mencionando conceptos y detalles específicos.
            """.trimIndent()
            return llamarGemini(finalPrompt)
        }
    }

    private suspend fun analizarConPDFBox(pdfUrl: String, prompt: String): String {
        val pdfFile = descargarPDFATempFile(pdfUrl)
        val chunks = extractTextChunksFromPdf(pdfFile)
        val totalChars = chunks.sumOf { it.length }
        if (totalChars < 30_000) {
            val textoPdf = chunks.joinToString("\n\n")
            val promptCompleto = """
                $prompt

                📎 CONTENIDO DEL PDF (extraído con PDFBox):
                ---
                Caracteres extraídos: ${'$'}{textoPdf.length}

                CONTENIDO COMPLETO:
                $textoPdf
                ---

                IMPORTANTE: Analiza TODO el contenido del PDF proporcionado.
            """.trimIndent()
            return llamarGemini(promptCompleto)
        } else {
            val compressed = compressChunksForSingleCall(chunks)
            val finalPrompt = """
                $prompt

                CONTENIDO COMPRIMIDO:
                $compressed

                INSTRUCCIÓN FINAL: A partir del contenido comprimido, genera un análisis coherente del PDF completo.
            """.trimIndent()
            return llamarGemini(finalPrompt)
        }
    }

    private suspend fun descargarPDFATempFile(url: String): File = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()
        val request = okhttp3.Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("HTTP ${'$'}{response.code}")
        val body = response.body ?: throw java.io.IOException("Body vacío")
        val tempFile = File.createTempFile("aulaviva_pdf_", ".pdf", context.cacheDir)
        FileOutputStream(tempFile).use { out -> body.byteStream().use { input -> input.copyTo(out) } }
        tempFile
    }

    private fun extractTextChunksFromPdf(file: File, pagesPerChunk: Int = 5): List<String> {
        val out = mutableListOf<String>()
        try {
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly())
            val total = document.numberOfPages
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            var page = 1
            while (page <= total) {
                stripper.startPage = page
                stripper.endPage = (page + pagesPerChunk - 1).coerceAtMost(total)
                out.add(stripper.getText(document))
                page += pagesPerChunk
            }
            document.close()
        } catch (e: Exception) { Log.w(TAG, "⚠️ [PDFBox] Error extrayendo: ${e.message}") }
        return out
    }

    private fun compressChunksForSingleCall(chunks: List<String>, maxChars: Int = 40_000): String {
        if (chunks.isEmpty()) return ""
        val b = StringBuilder()
        for ((i, c) in chunks.withIndex()) {
            if (b.length >= maxChars) break
            b.append("-- PARTE ${i + 1} --\n")
            b.append(c.take(1500))
            b.append("\n\n")
        }
        var i = 0
        while (b.length < maxChars && i < chunks.size) {
            b.append("-- MUESTRA ${i + 1} (tail) --\n")
            b.append(chunks[i].takeLast(500))
            b.append("\n\n")
            i++
        }
        return if (b.length > maxChars) b.toString().take(maxChars) else b.toString()
    }

    override suspend fun generarIdeasParaClase(nombreClase: String, descripcion: String, pdfUrl: String?): String {
        return try {
            val contexto = if (!pdfUrl.isNullOrEmpty()) "\n📎 Material de apoyo disponible" else ""
            val prompt = """
                Eres un consultor en innovación educativa. Clase: $nombreClase
                Descripción: $descripcion$contexto

                Genera 4 ideas prácticas para trabajo en clase, con objetivo pedagógico y pasos.
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            "$resultado\n\n(Generado por Gemini)"
        } catch (e: Exception) { "⚠️ Error: ${e.message ?: "Desconocido"}" }
    }

    // ----- Compatibility wrappers expected by callers (IAViewModel, Activities)
    override suspend fun sugerirActividades(nombreClase: String, descripcion: String, pdfUrl: String?): String {
        return generarActividadesInteractivas(nombreClase, descripcion, pdfUrl)
    }

    override suspend fun estructurarClasePorTiempo(nombreClase: String, descripcion: String, duracion: String, pdfUrl: String?): String {
        val promptExtra = "Duración: $duracion"
        return generarIdeasParaClase(nombreClase, "$descripcion\n$promptExtra", pdfUrl)
    }

    override suspend fun resumirContenidoPdf(nombre: String, descripcion: String, archivoNombre: String): String {
        return try {
            analizarPdfConIA(nombre, archivoNombre)
        } catch (e: Exception) {
            "⚠️ Resumen no disponible: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun generarGuiaPresentacion(nombre: String, descripcion: String, pdfUrl: String?): String {
        return generarIdeasParaClase(nombre, descripcion, pdfUrl)
    }

    override suspend fun generarEjerciciosParaAlumno(nombre: String, descripcion: String, pdfUrl: String?): String {
        return generarActividadesInteractivas(nombre, descripcion, pdfUrl)
    }

    override suspend fun crearResumenEstudioParaAlumno(nombre: String, descripcion: String, archivoNombre: String): String {
        return generarIdeasParaClase(nombre, descripcion, archivoNombre)
    }

    override suspend fun generarActividadesInteractivas(nombreClase: String, descripcion: String, nombrePdf: String?): String {
        return try {
            val contexto = if (!nombrePdf.isNullOrEmpty()) "\n📎 Material: $nombrePdf" else ""
            val prompt = """
                Diseña 3 actividades interactivas para la clase $nombreClase. Descripción: $descripcion$contexto
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            "$resultado\n\n(Generado por Gemini)"
        } catch (e: Exception) { "⚠️ Error: ${e.message ?: "Desconocido"}" }
    }

    override suspend fun explicarConceptosParaAlumno(nombreClase: String, descripcion: String, nombrePdf: String?): String {
        return try {
            val contexto = if (!nombrePdf.isNullOrEmpty()) "\n📄 Material: $nombrePdf" else ""
            val prompt = """
                Eres un tutor. Explica en lenguaje simple los conceptos principales de $nombreClase. Descripción: $descripcion$contexto
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            "$resultado\n\n(Generado por Gemini)"
        } catch (e: Exception) { "⚠️ Error: ${e.message ?: "Desconocido"}" }
    }

    override suspend fun iniciarChatConContexto(nombreClase: String, descripcion: String, pdfUrl: String?, respuestaInicial: String) {
        withContext(Dispatchers.IO) {
            try {
                val historial = mutableListOf<com.google.ai.client.generativeai.type.Content>()
                val promptInicial = """
                    CONTEXTO DE LA CLASE:
                    Clase: $nombreClase
                    Descripción: $descripcion
                """.trimIndent()
                val contenidoUsuario = if (!pdfUrl.isNullOrEmpty()) {
                    val pdfFile = descargarPDFATempFile(pdfUrl)
                    if (pdfFile.length() <= 20_000_000L) {
                        val pdfBytes = pdfFile.readBytes()
                        content("user") { text(promptInicial); blob("application/pdf", pdfBytes) }
                    } else {
                        val chunks = extractTextChunksFromPdf(pdfFile)
                        val compressed = compressChunksForSingleCall(chunks)
                        content("user") { text(promptInicial + "\n\nCONTENIDO_COMPRIMIDO:\n" + compressed) }
                    }
                } else content("user") { text(promptInicial) }
                historial.add(contenidoUsuario)
                historial.add(content("model") { text(respuestaInicial) })
                chatSession = googleAiModel.startChat(history = historial)
                Log.d(TAG, "✅ [CHAT] Sesión iniciada con ${historial.size} mensajes")
            } catch (e: Exception) { Log.e(TAG, "❌ [CHAT] Error iniciando: ${e.message}"); chatSession = null }
        }
    }

    override suspend fun enviarMensajeChat(mensaje: String): String {
        return withContext(Dispatchers.IO) {
            if (chatSession != null) {
                try { val response = chatSession!!.sendMessage(mensaje); return@withContext response.text ?: "Sin respuesta de la IA" }
                catch (e: Exception) { Log.e(TAG, "❌ [CHAT] Error: ${e.message}"); throw e }
            } else { Log.w(TAG, "⚠️ [CHAT] No hay sesión activa, fallback stateless"); return@withContext llamarGemini(mensaje) }
        }
    }

}
