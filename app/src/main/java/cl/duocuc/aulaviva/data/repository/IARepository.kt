package cl.duocuc.aulaviva.data.repository

import android.app.Application
import android.util.Log
import cl.duocuc.aulaviva.BuildConfig
import cl.duocuc.aulaviva.data.remote.Content
import cl.duocuc.aulaviva.data.remote.GeminiApiService
import cl.duocuc.aulaviva.data.remote.GeminiRequest
import cl.duocuc.aulaviva.data.remote.GenerationConfig
import cl.duocuc.aulaviva.data.remote.Part
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

class IARepository(private val application: Application) {
    companion object { private const val TAG = "AulaViva_IA" }

    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY

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
        try { PDFBoxResourceLoader.init(application) } catch (_: Throwable) { Log.w(TAG, "PDFBox init failed") }
    }

    private suspend fun llamarGemini(prompt: String): String = withContext(Dispatchers.IO) {
        withTimeout(60_000L) {
            var intento = 0
            var ultimoError: Exception? = null
            while (intento < 3) {
                try {
                    val request = GeminiRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                        generationConfig = GenerationConfig(temperature = 0.6f, topP = 0.9f, maxOutputTokens = 4096)
                    )
                    val response = geminiService.generateContent(GEMINI_API_KEY, request)
                    if (response.isSuccessful) {
                        val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!text.isNullOrBlank()) return@withTimeout text
                        throw Exception("Respuesta vacía de Gemini")
                    } else {
                        throw Exception("HTTP ${response.code()}: ${response.message()}")
                    }
                } catch (e: Exception) {
                    ultimoError = e
                    intento++
                    if (intento < 3) Thread.sleep(300L * intento)
                    else throw Exception("Error Gemini: ${e.message}")
                }
            }
            throw Exception("Error Gemini: ${ultimoError?.message ?: "Desconocido"}")
        }
    }

    // ------------------ PDF helpers ------------------
    private suspend fun descargarPDFATempFile(url: String): File = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()
        val request = okhttp3.Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("Error HTTP al descargar PDF: ${response.code}")
        val body = response.body ?: throw java.io.IOException("Body vacío al descargar PDF")
        val tempFile = File.createTempFile("aulaviva_pdf_", ".pdf", application.cacheDir)
        FileOutputStream(tempFile).use { out -> body.byteStream().use { input -> input.copyTo(out) } }
        tempFile
    }

    private fun extractTextChunksFromPdf(file: File, pagesPerChunk: Int = 5): List<String> {
        val out = mutableListOf<String>()
        try {
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly())
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            val total = document.numberOfPages
            var p = 1
            while (p <= total) {
                stripper.startPage = p
                stripper.endPage = (p + pagesPerChunk - 1).coerceAtMost(total)
                out.add(stripper.getText(document))
                p += pagesPerChunk
            }
            document.close()
        } catch (e: Exception) {
            Log.w(TAG, "PDFBox extract error: ${e.message}")
        }
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
        if (b.length > maxChars) return b.toString().take(maxChars)
        return b.toString()
    }

    // ------------------ Public API (used by ViewModel) ------------------
    suspend fun generarIdeasParaClase(nombreClase: String, descripcion: String, pdfUrl: String?): String {
        val contexto = if (!pdfUrl.isNullOrBlank()) {
            val file = descargarPDFATempFile(pdfUrl)
            val chunks = extractTextChunksFromPdf(file)
            val compressed = compressChunksForSingleCall(chunks)
            "\n📎 Material: (resumen comprimido)\n$compressed"
        } else ""
            val prompt = """
                Eres un consultor en innovación educativa.
                Clase: $nombreClase
                Descripción: $descripcion$contexto
                Genera 5 ideas concretas y aplicables para clase.
            """.trimIndent()
        return llamarGemini(prompt)
    }

    suspend fun sugerirActividades(nombreClase: String, descripcion: String, pdfUrl: String?): String {
        val contexto = if (!pdfUrl.isNullOrBlank()) {
            val file = descargarPDFATempFile(pdfUrl)
            val chunks = extractTextChunksFromPdf(file)
            compressChunksForSingleCall(chunks)
        } else ""
        val prompt = """
            Eres un diseñador instruccional.
            Clase: $nombreClase
            Descripción: $descripcion
            Contexto: $contexto
            Diseña 4 actividades concretas.
        """.trimIndent()
        return llamarGemini(prompt)
    }

    suspend fun estructurarClasePorTiempo(nombreClase: String, descripcion: String, duracionMinutos: String, pdfUrl: String?): String {
        val prompt = """
            Eres un planificador educativo. Clase: $nombreClase. Duración: $duracionMinutos minutos.
            Descripción: $descripcion
            Estructura la clase en bloques y tiempos.
        """.trimIndent()
        return llamarGemini(prompt)
    }

    suspend fun analizarPdfConIA(nombreClase: String, pdfUrl: String?): String {
        if (pdfUrl.isNullOrBlank()) return "⚠️ No se proporcionó URL del PDF"
        val file = descargarPDFATempFile(pdfUrl)
        val chunks = extractTextChunksFromPdf(file)
        val total = chunks.joinToString("\n\n").take(20000)
        val prompt = """
            Analiza el PDF para la clase: $nombreClase
            Contenido (muestra):
            $total
            Proporciona: tema principal, conceptos clave y sugerencias pedagógicas.
        """.trimIndent()
        return llamarGemini(prompt)
    }

    suspend fun resumirContenidoPdf(nombreClase: String, descripcion: String, nombrePdf: String): String {
        val prompt = """
            Resume el contenido del PDF '$nombrePdf' para la clase $nombreClase. Máximo 300 palabras.
        """.trimIndent()
        return llamarGemini(prompt)
    }

    suspend fun generarGuiaPresentacion(nombreClase: String, descripcion: String): String {
        val prompt = """
            Crea una guía de presentación para la clase $nombreClase. Incluye introducción, puntos clave y cierre.
        """.trimIndent()
        return llamarGemini(prompt)
    }

    suspend fun generarActividadesInteractivas(nombreClase: String, descripcion: String, nombrePdf: String?): String {
        val prompt = """
            Genera actividades interactivas para la clase $nombreClase basadas en: $descripcion
        """.trimIndent()
        return llamarGemini(prompt)
    }

    suspend fun explicarConceptosParaAlumno(nombreClase: String, descripcion: String, nombrePdf: String?): String {
        val prompt = """
            Explica los conceptos clave de $nombreClase de forma simple y con ejemplos.
        """.trimIndent()
        return llamarGemini(prompt)
    }

    suspend fun generarEjerciciosParaAlumno(nombreClase: String, descripcion: String, pdfUrl: String?): String {
        val prompt = """
            Crea ejercicios prácticos para la clase $nombreClase.
        """.trimIndent()
        return llamarGemini(prompt)
    }

    suspend fun crearResumenEstudioParaAlumno(nombreClase: String, descripcion: String, nombrePdf: String?): String {
        val prompt = """
            Crea un resumen de estudio para $nombreClase que el alumno pueda usar para repasar.
        """.trimIndent()
        return llamarGemini(prompt)
    }
}
package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.BuildConfig
import cl.duocuc.aulaviva.data.remote.Content
import cl.duocuc.aulaviva.data.remote.GeminiApiService
import cl.duocuc.aulaviva.data.remote.GeminiRequest
import cl.duocuc.aulaviva.data.remote.GenerationConfig
import cl.duocuc.aulaviva.data.remote.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
package cl.duocuc.aulaviva.data.repository

import android.app.Application
import android.util.Log
import cl.duocuc.aulaviva.BuildConfig
import cl.duocuc.aulaviva.data.remote.Content
import cl.duocuc.aulaviva.data.remote.GeminiApiService
import cl.duocuc.aulaviva.data.remote.GeminiRequest
import cl.duocuc.aulaviva.data.remote.GenerationConfig
import cl.duocuc.aulaviva.data.remote.Part
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

class IARepository(private val application: Application) {
    companion object { private const val TAG = "AulaViva_IA" }

    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY

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
        try { PDFBoxResourceLoader.init(application) } catch (_: Throwable) {}
    }

    private suspend fun llamarGemini(prompt: String): String = withContext(Dispatchers.IO) {
        withTimeout(60_000L) {
            var intento = 0
            var ultimoError: Exception? = null
            while (intento < 3) {
                try {
                    val request = GeminiRequest(contents = listOf(Content(parts = listOf(Part(text = prompt)))) , generationConfig = GenerationConfig(temperature = 0.6f, topP = 0.9f, maxOutputTokens = 4096))
                    val response = geminiService.generateContent(GEMINI_API_KEY, request)
                    if (response.isSuccessful) {
                        val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!text.isNullOrBlank()) return@withTimeout text
                        throw Exception("Respuesta vacía de Gemini")
                    } else {
                        throw Exception("HTTP ${response.code()}: ${response.message()}")
                    }
                } catch (e: Exception) {
                    ultimoError = e
                    intento++
                    if (intento < 3 && (e is SocketTimeoutException || e.message?.contains("HTTP 5") == true)) {
                        Thread.sleep((500L * intento))
                        continue
                    } else if (intento < 3) {
                        Thread.sleep(300L)
                        continue
                    } else {
                        throw Exception("Error Gemini: ${e.message}")
                    }
                }
            }
            throw Exception("Error Gemini: ${ultimoError?.message ?: "Desconocido"}")
        }
    }

                println("✅ Gemini AI Retrofit activado - Modelo: gemini-2.5-flash")
            }

            /**
             * Llama a Gemini API con Retrofit
             */
            private suspend fun llamarGemini(prompt: String): String {
                return withContext(Dispatchers.IO) {
                    withTimeout(60000L) {
                        var intento = 0
                        var ultimoError: Exception? = null
                        val maxIntentos = 3
                        while (intento < maxIntentos) {
                            try {
                                // Construir request según docs oficiales
                                val request = GeminiRequest(
                                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                                    generationConfig = GenerationConfig(
                                        temperature = 0.6f, topP = 0.9f, maxOutputTokens = 4096
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
                                    val code = response.code()
                                    throw Exception("HTTP $code: ${response.message()}")
                                }
                            } catch (e: Exception) {
                                ultimoError = e
                                intento++
                                if (intento < maxIntentos && (e is SocketTimeoutException || e.message?.contains(
                                        "HTTP 503"
                                    ) == true || e.message?.contains("HTTP 504") == true || e.message?.contains(
                                        "vacía"
                                    ) == true)
                                ) {
                                    try {
                                        Thread.sleep((800L * intento))
                                    } catch (_: InterruptedException) {
                                    }
                                    continue
                                } else if (intento < maxIntentos) {
                                    try {
                                        Thread.sleep(400L)
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

            /**
             * 🔍 MÉTODO HÍBRIDO: Firebase AI Logic primero, PDFBox fallback
             *
             * Este método garantiza que la IA SIEMPRE pueda leer el PDF:
             * 1. INTENTA Firebase AI Logic (Base64 inline) - Mejor precisión, OCR integrado
             * 2. Si falla, usa PDFBox (extracción texto local) - Funciona offline
             *
             * @param pdfUrl URL del PDF en Supabase
             * @param prompt Instrucción para la IA
             * @return Respuesta de Gemini basada en el PDF real
             */
            private suspend fun analizarPDFInteligente(pdfUrl: String, prompt: String): String =
                withContext(Dispatchers.IO) {
                    Log.d(TAG, "🔍 [HÍBRIDO] Iniciando análisis inteligente de PDF...")
                    Log.d(TAG, "🔍 [HÍBRIDO] PDF URL: $pdfUrl")

                    try {
                        // INTENTO 1: Google AI Gemini (RECOMENDADO - OCR integrado)
                        Log.d(TAG, "🔥 [Google AI] Intentando método Google AI SDK...")
                        return@withContext analizarConGoogleAI(pdfUrl, prompt)

                    } catch (e: Exception) {
                        // INTENTO 2: PDFBox (FALLBACK - offline)
                        Log.w(TAG, "⚠️ [HÍBRIDO] Google AI falló: ${e.message}")
                        Log.d(TAG, "📄 [PDFBox] Cambiando a método fallback PDFBox...")
                        return@withContext analizarConPDFBox(pdfUrl, prompt)
                    }
                }

            /**
             * 🔥 Google AI SDK: Envía PDF completo (Base64 inline)
             * Gemini parsea el PDF server-side con OCR + tablas + gráficos.
             *
             * Ventajas:
             * - OCR integrado (lee PDFs escaneados)
             * - Parsea tablas y gráficos
             * - Mayor precisión que extraer texto local
             * - Compatible con Ktor 2.3.12 (no conflicto con Supabase)
             *
             * @param pdfUrl URL del PDF
             * @param prompt Instrucción para la IA
             * @return Respuesta de Gemini
             */
            private suspend fun analizarConGoogleAI(pdfUrl: String, prompt: String): String {
                Log.d(TAG, "📦 [Google AI] Descargando PDF desde Supabase...")

                // 1. Descargar PDF a archivo temporal (streaming)
                val pdfFile = descargarPDFATempFile(pdfUrl)
                Log.d(TAG, "📦 [Google AI] PDF descargado a archivo temporal: ${pdfFile.length()} bytes")

                if (pdfFile.length() <= 20_000_000L) {
                    // Si cabe en límite, enviar como blob (mantener comportamiento original)
                    val pdfBytes = pdfFile.readBytes()
                    val contenidoMultimodal = content {
                        text(prompt)
                        blob("application/pdf", pdfBytes)
                    }

                    Log.d(TAG, "🤖 [Google AI] Enviando a Gemini 2.5 (Developer API) con blob...")

                    // Generar respuesta con timeout
                    val response = withTimeout(90_000) { // 90 segundos
                        googleAiModel.generateContent(contenidoMultimodal)
                    }

                    val textoRespuesta =
                        response.text ?: throw Exception("Respuesta vacía de Google AI SDK")

                    Log.d(TAG, "✅ [Google AI] Respuesta recibida: ${textoRespuesta.length} caracteres")
                    Log.d(TAG, "✅ [Google AI] Primeros 150 chars: ${textoRespuesta.take(150)}...")

                    return textoRespuesta
                } else {
                    // Si es muy grande, evitar cargar en memoria y usar extracción por chunks vía PDFBox
                    Log.w(
                        TAG,
                        "⚠️ [Google AI] PDF >20MB, usando pipeline de extracción por chunks y síntesis con Gemini"
                    )
                    val chunks = extractTextChunksFromPdf(pdfFile)
                    // 1) Resumir cada chunk con Gemini para reducir tokens
                    val summaries = mutableListOf<String>()
                    for ((index, chunk) in chunks.withIndex()) {
                        val chunkPrompt = """
                            ${prompt}

                            ---
                            PARTE ${index + 1}/${chunks.size} DEL PDF:
                            ${chunk.take(30_000)}
                            ---
                            Instrucción: Resume los puntos clave y extrae conceptos específicos de esta parte. Responde en español.
                        """.trimIndent()

                        try {
                            val s = llamarGemini(chunkPrompt)
                            summaries.add(s)
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ [Google AI] Error resumiendo chunk ${index + 1}: ${e.message}")
                            summaries.add("[Error resumiendo chunk ${index + 1}: ${e.message}]")
                        }
                    }

                    // 2) Sintetizar todos los resúmenes en un único análisis
                    val combined = summaries.joinToString("\n\n---\n\n")
                    val finalPrompt = """
                        ${prompt}

                        He recibido el PDF en partes y he resumido cada sección. A continuación están los resúmenes parciales:
                        $combined

                        INSTRUCCIÓN FINAL: A partir de los resúmenes parciales, genera un análisis coherente del PDF completo, mencionando conceptos y detalles específicos.
                    """.trimIndent()

                    return llamarGemini(finalPrompt)
                }
            }

            /**
             * 📄 PDFBox: Extrae texto local y envía a Gemini (FALLBACK)
             *
             * Este método se usa cuando Firebase falla (red, timeout, límite API).
             * Extrae texto localmente y lo envía a Gemini vía Retrofit.
             */
            private suspend fun analizarConPDFBox(pdfUrl: String, prompt: String): String {
                Log.d(TAG, "📄 [PDFBox] Descargando PDF para extracción local...")
                val pdfFile = descargarPDFATempFile(pdfUrl)

                // Abrir documento en disco para evitar OOM
                val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(
                    pdfFile,
                    MemoryUsageSetting.setupTempFileOnly()
                )

                val totalPaginas = document.numberOfPages
                val chunks = extractTextChunksFromPdf(pdfFile)

                document.close()

                // 4. Si el texto total es pequeño, mandarlo completo; sino, resumir por chunks y sintetizar
                val totalChars = chunks.sumOf { it.length }
                Log.d(TAG, "📄 [PDFBox] Texto total extraído (chars): $totalChars")

                return if (totalChars < 30_000) {
                    val textoPdf = chunks.joinToString("\n\n")
                    Log.d(
                        TAG,
                        "📄 [PDFBox] Texto extraído pequeño, enviando completo (${textoPdf.length} chars)"
                    )
                    val promptCompleto = """
                        $prompt

                        📎 CONTENIDO DEL PDF (extraído con PDFBox):
                        ---
                        METADATA:
                        - Total de páginas: $totalPaginas
                        - Caracteres extraídos: ${textoPdf.length}

                        CONTENIDO COMPLETO:
                        $textoPdf
                        ---

                        IMPORTANTE: Analiza TODO el contenido del PDF proporcionado.
                        Menciona detalles ESPECÍFICOS que aparezcan en el texto.
                    """.trimIndent()

                    Log.d(TAG, "🤖 [PDFBox] Enviando texto extraído a Gemini vía Retrofit...")
                    val respuesta = llamarGemini(promptCompleto)
                    Log.d(TAG, "✅ [PDFBox] Respuesta recibida: ${respuesta.length} caracteres")
                    respuesta
                } else {
                    Log.d(TAG, "📄 [PDFBox] Texto grande, comprimiendo localmente antes de llamar a Gemini...")
                    val compressed = compressChunksForSingleCall(chunks)
                    val finalPrompt = """
                        ${prompt}

                        A continuación se presenta una representación comprimida del contenido del PDF (resúmenes y muestras representativas). Usa esto como contexto para analizar el documento completo y generar la respuesta solicitada.

                        CONTENIDO COMPRIMIDO:
                        $compressed

                        INSTRUCCIÓN FINAL: A partir del contenido comprimido, genera un análisis coherente del PDF completo, mencionando conceptos y detalles específicos.
                    """.trimIndent()

                    return llamarGemini(finalPrompt)
                }
            }

            /**
             * 📥 Helper: Descarga PDF desde URL
             *
             * @param url URL del PDF (Supabase Storage)
             * @return ByteArray con el contenido del PDF
             */
            private suspend fun descargarPDF(url: String): ByteArray {
                return withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(120, TimeUnit.SECONDS)
                        .build()

                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    val response = client.newCall(request).execute()

                    if (!response.isSuccessful) {
                        throw java.io.IOException("Error HTTP al descargar PDF: ${response.code}")
                    }

                    val bytes = response.body?.bytes()
                    if (bytes == null || bytes.isEmpty()) {
                        throw java.io.IOException("PDF descargado está vacío")
                    }

                    bytes
                }
            }

            /**
             * Descarga un PDF streaming a un archivo temporal dentro de cacheDir y lo retorna.
             */
            private suspend fun descargarPDFATempFile(url: String): File {
                return withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(120, TimeUnit.SECONDS)
                        .build()

                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    val response = client.newCall(request).execute()

                    if (!response.isSuccessful) {
                        throw java.io.IOException("Error HTTP al descargar PDF: ${response.code}")
                    }

                    val body = response.body ?: throw java.io.IOException("Body vacío al descargar PDF")

                    val tempFile = File.createTempFile("aulaviva_pdf_", ".pdf", application.cacheDir)
                    FileOutputStream(tempFile).use { out ->
                        body.byteStream().use { input ->
                            input.copyTo(out)
                        }
                    }

                    tempFile
                }
            }

            /**
             * Extrae chunks de texto desde un PDF en disco usando PDFBox, devolviendo una lista de strings por bloque.
             */
            private fun extractTextChunksFromPdf(file: File, pagesPerChunk: Int = 5): List<String> {
                val out = mutableListOf<String>()
                try {
                    val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(
                        file,
                        MemoryUsageSetting.setupTempFileOnly()
                    )
                    val totalPaginas = document.numberOfPages
                    val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                    var page = 1
                    while (page <= totalPaginas) {
                        stripper.startPage = page
                        stripper.endPage = (page + pagesPerChunk - 1).coerceAtMost(totalPaginas)
                        val chunk = stripper.getText(document)
                        out.add(chunk)
                        page += pagesPerChunk
                    }
                    document.close()
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ [PDFBox] Error extrayendo chunks: ${e.message}")
                    // fallback vacío
                }
                return out
            }

            /**
             * Comprime una lista de chunks en una representación reducida adecuada para
             * enviarse en una única llamada a Gemini. Selecciona muestras de cada chunk
             * y limita el tamaño total a `maxChars`.
             */
            private fun compressChunksForSingleCall(chunks: List<String>, maxChars: Int = 40_000): String {
                if (chunks.isEmpty()) return ""
                val builder = StringBuilder()
                for ((index, chunk) in chunks.withIndex()) {
                    if (builder.length >= maxChars) break
                    val head = chunk.take(1500)
                    builder.append("-- PARTE ${index + 1} --\\n")
                    builder.append(head)
                    builder.append("\\n\\n")
                }
                // Añadir muestras tail si hay espacio
                var i = 0
                while (builder.length < maxChars && i < chunks.size) {
                    val tail = chunks[i].takeLast(500)
                    builder.append("-- MUESTRA ${i + 1} (tail) --\\n")
                    builder.append(tail)
                    builder.append("\\n\\n")
                    i++
                }
                if (builder.length > maxChars) return builder.toString().take(maxChars)
                return builder.toString()
            }
                • **Cómo ejecutarla**: [Pasos breves]

                **ACTIVIDAD 4: [Nombre descriptivo]**
                • **Objetivo**: [Qué se busca lograr]
                • **Duración**: [X minutos]
                • **Tipo**: [Individual/Grupal/Plenaria]
                • **Materiales**: [Recursos necesarios]
                • **Cómo ejecutarla**: [Pasos breves]

                ⏱️ **TIEMPO TOTAL**: [Suma de duraciones]

                💡 **TIP DE IMPLEMENTACIÓN**
                [Consejo para que las actividades fluyan mejor]

                # ESTILO
                - Español chileno profesional
                - Instrucciones claras y ejecutables
                - Actividades variadas (no repetitivas)
                - Máximo 500 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n🎯 ACTIVIDADES GENERALES para $nomm
breClase\n\nPara actividades detalladas, verifica tu conexión.\n\nError: ${                          e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    /**
     * ⏱️ Estructura clase por tiempo
     */
    suspend fun estructurarClasePorTiempo(
        nombreClase: String,
        descripcion: String,
        duracionMinutos: String
    ): String {
        return try {
            val prompt = """
                # CONTEXTO Y ROL
                Eres un planificador educativo experto en gestión del tiempo para cla
ses universitarias chilenas.
                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion
                ⏱️ Duración total: $duracionMinutos

                # INSTRUCCIONES
                1. **Confirma el tema**: Identifica área y complejidad
                2. **Asume rol de planificador**: Experto en timing educativo
                3. **Estructura la clase**: Divide el tiempo de forma óptima

                # FORMATO DE RESPUESTA

                🎓 **ANÁLISIS INICIAL**
                • Tema: [tema identificado]
                • Complejidad: [Baja/Media/Alta]
                • Enfoque recomendado: [Teórico/Práctico/Mixto]

                ⏱️ **ESTRUCTURA TEMPORAL ($duracionMinutos)**

                **🟢 INICIO (__ min | 0:00 - __:__)**
                • **Saludo y contextualización** (__ min)
                  → [Qué hacer específicamente]
                • **Objetivos de la clase** (__ min)
                  → [Presentar metas claras]
                • **Activación de conocimientos previos** (__ min)
                  → [Pregunta o actividad breve]

                **🔵 DESARROLLO (__ min | __:__ - __:__)**
                • **Presentación del contenido principal** (__ min)
                  → [Explicación o demostración]
                • **Actividad práctica / Ejercicio** (__ min)
                  → [Trabajo individual o grupal]
                • **Retroalimentación grupal** (__ min)
                  → [Compartir resultados]
                • **Profundización** (__ min)
                  → [Ejemplos adicionales o dudas]

                **🟡 CIERRE (__ min | __:__ - __:__)**
                • **Síntesis de aprendizajes** (__ min)
                  → [Resumen colaborativo]
                • **Evaluación formativa** (__ min)
                  → [Quiz, pregunta, ticket de salida]
                • **Asignación de tareas** (__ min)
                  → [Si aplica, explicar homework]

                ⚠️ **TIEMPO DE BUFFER**: __ min (para imprevistos)

                💡 **TIPS DE GESTIÓN DEL TIEMPO**
                • [Consejo 1 para mantener el ritmo]
                • [Consejo 2 para no excederse]

                # ESTILO
                - Español chileno profesional
                - Tiempos precisos y realistas
                - Actividades claras en cada bloque
                - Máximo 450 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n⏱️ ESTRUCTURA BÁSICA ($duracionMin n
utos) para $nombreClase\n\nPara planificación detallada, verifica tu conexión.\n\nError: ${                                                                                               e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    /**
     * 📊 Analiza PDF con IA
     */
    suspend fun analizarPdfConIA(nombreClase: String): String {
        return try {
            val prompt = """
                # CONTEXTO Y ROL
                Eres un analista de materiales educativos para docentes universitario
s chilenos.
                # SITUACIÓN
                Un docente tiene un PDF para la clase: "$nombreClase"
                Necesita ideas concretas para aprovechar ese material en su clase.

                # INSTRUCCIONES
                1. **Identifica el tipo de clase**: Según el nombre, deduce el área
                2. **Asume rol apropiado**: Experto en esa disciplina
                3. **Propón 3 estrategias**: Formas efectivas de usar el PDF

                # FORMATO DE RESPUESTA

                🎓 **CONTEXTO DETECTADO**
                • Clase: $nombreClase
                • Área estimada: [disciplina]
                • Tipo de PDF probable: [ej: Artículo, Manual, Presentación]

                📊 **ESTRATEGIAS PARA APROVECHAR EL PDF**

                **ESTRATEGIA 1: LECTURA CRÍTICA GUIADA**
                • **Qué hacer**: Asignar secciones del PDF con preguntas específicas
                • **Cómo aplicarlo**: [Pasos concretos]
                • **Tiempo**: [X minutos]
                • **Beneficio para el aprendizaje**: [Por qué funciona]

                **ESTRATEGIA 2: DEBATE O DISCUSIÓN**
                • **Qué hacer**: Usar el PDF como base para debate
                • **Cómo aplicarlo**: [Pasos concretos]
                • **Tiempo**: [X minutos]
                • **Beneficio para el aprendizaje**: [Por qué funciona]

                **ESTRATEGIA 3: APLICACIÓN PRÁCTICA**
                • **Qué hacer**: Ejercicios basados en el contenido del PDF
                • **Cómo aplicarlo**: [Pasos concretos]
                • **Tiempo**: [X minutos]
                • **Beneficio para el aprendizaje**: [Por qué funciona]

                💡 **RECOMENDACIÓN ADICIONAL**
                [Un tip creativo para maximizar el valor del PDF]

                # ESTILO
                - Español chileno profesional
                - Estrategias concretas y aplicables
                - Enfoque en el valor pedagógico
                - Máximo 400 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n📊 ESTRATEGIAS GENERALES PARA PDF
de $nombreClase\n\nPara estrategias personalizadas, verifica tu conexión.\n\nError: ${                                                                                                    e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    /**
     * 📝 Resume contenido del PDF
     */
    suspend fun resumirContenidoPdf(
        nombreClase: String,
        descripcion: String,
        nombrePdf: String
    ): String {
        return try {
            val prompt = """
                # CONTEXTO Y ROL
                Eres un sintetizador de contenido educativo para docentes chilenos.

                # DATOS DEL MATERIAL
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion
                📄 Material PDF: $nombrePdf

                # INSTRUCCIONES
                1. **Confirma el tema**: Identifica el contenido principal
                2. **Asume rol apropiado**: Experto en esa área
                3. **Genera resumen estructurado**: Útil para preparar la clase

                # FORMATO DE RESPUESTA

                📄 **IDENTIFICACIÓN DEL MATERIAL**
                • Clase: $nombreClase
                • Tema central detectado: [tema principal]
                • Tipo de contenido: [Teórico/Práctico/Mixto]

                ## TEMA PRINCIPAL
                [Descripción clara del tema central en 2-3 líneas]

                ## CONCEPTOS CLAVE
                • **Concepto 1**: [Explicación breve]
                • **Concepto 2**: [Explicación breve]
                • **Concepto 3**: [Explicación breve]
                • **Concepto 4**: [Explicación breve]
                • **Concepto 5**: [Si aplica]

                ## CONCLUSIONES PRINCIPALES
                [Resumen de los aprendizajes fundamentales que el docente debe transm
itir]
                💡 **SUGERENCIA DIDÁCTICA**
                [Cómo el docente puede presentar este contenido de forma efectiva]

                # ESTILO
                - Español chileno profesional
                - Formato Markdown estructurado
                - Enfoque en lo esencial para el docente
                - Máximo 350 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n📝 RESUMEN BÁSICO de $nombreClase\\n\nPara resumen detallado, verifica tu conexión.\n\nError: ${                                        e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    /**
     * 🌟 Reordena temas para presentar (Guía de presentación)
     */
    suspend fun generarGuiaPresentacion(nombreClase: String, descripcion: String): St
ring {                                                                                       return try {
            val prompt = """
                # CONTEXTO Y ROL
                Eres un coach de oratoria y presentación para docentes universitarios
 chilenos.
                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Contenido: $descripcion

                # INSTRUCCIONES
                1. **Confirma el tema**: Identifica área y complejidad
                2. **Asume rol de coach**: Experto en presentación efectiva
                3. **Crea guía detallada**: Secuencia óptima para presentar el conten
ido
                # FORMATO DE RESPUESTA

                🎓 **ANÁLISIS DEL CONTENIDO**
                • Tema: $nombreClase
                • Complejidad: [Baja/Media/Alta]
                • Tipo de clase: [Magistral/Participativa/Práctica]

                ## 1. INTRODUCCIÓN SUGERIDA (2-3 minutos)

                **🎯 Gancho inicial**
                [Frase, pregunta o dato impactante para captar atención]

                **📌 Contextualización**
                [Por qué este tema es relevante para los estudiantes]

                **🎓 Objetivos de la clase**
                [Lo que aprenderán hoy, en lenguaje simple]

                ---

                ## 2. PUNTOS CLAVE A ENFATIZAR

                **Punto 1: [Concepto fundamental]**
                → Por qué es importante: [Relevancia]
                → Cómo explicarlo: [Estrategia]

                **Punto 2: [Concepto fundamental]**
                → Por qué es importante: [Relevancia]
                → Cómo explicarlo: [Estrategia]

                **Punto 3: [Concepto fundamental]**
                → Por qué es importante: [Relevancia]
                → Cómo explicarlo: [Estrategia]

                ---

                ## 3. EJEMPLOS PRÁCTICOS RECOMENDADOS

                • **Ejemplo 1**: [Caso real o analogía simple]
                • **Ejemplo 2**: [Ejercicio concreto]
                • **Ejemplo 3**: [Comparación útil]

                ---

                ## 4. PREGUNTAS PARA GENERAR PARTICIPACIÓN

                **Preguntas de inicio** (despertar interés):
                • [Pregunta abierta 1]
                • [Pregunta abierta 2]

                **Preguntas de desarrollo** (profundizar):
                • [Pregunta desafiante 1]
                • [Pregunta desafiante 2]

                **Pregunta de cierre** (sintetizar):
                • [Pregunta reflexiva final]

                ---

                💡 **TIPS DE PRESENTACIÓN**
                • [Consejo 1 para mantener la atención]
                • [Consejo 2 para manejar dudas]
                • [Consejo 3 para cerrar con impacto]

                # ESTILO
                - Español chileno profesional
                - Formato Markdown estructurado
                - Consejos prácticos y aplicables
                - Máximo 600 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n🎤 GUÍA BÁSICA DE PRESENTACIÓN parr
a $nombreClase\n\nPara guía detallada, verifica tu conexión.\n\nError: ${                            e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    /**
     * 💭 Ideas para dictar la clase (Actividades interactivas)
     */
    suspend fun generarActividadesInteractivas(
        nombreClase: String,
        descripcion: String,
        nombrePdf: String?
    ): String {
        return try {
            val contextoPdf =
                if (!nombrePdf.isNullOrEmpty()) "\n📎 Material de apoyo: $nombrePdf"
else ""                                                                                          val prompt = """
                # CONTEXTO Y ROL
                Eres un diseñador instruccional especializado en transformar contenid
o en experiencias de aprendizaje activo.
                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion$contextoPdf

                # INSTRUCCIONES
                1. **Confirma el tema**: Identifica área y nivel
                2. **Asume rol de diseñador**: Experto en aprendizaje activo
                3. **Transforma en clase interactiva**: Múltiples actividades variada
s
                # FORMATO DE RESPUESTA

                🎓 **CONTEXTO PEDAGÓGICO**
                • Tema: $nombreClase
                • Enfoque: [Constructivista/Colaborativo/Experiencial]
                • Nivel de interactividad: [Alto - múltiples dinámicas]

                ## 1. ACTIVIDADES PRÁCTICAS (3-5 propuestas)

                **📝 Actividad Individual: [Nombre]**
                • **Qué hacer**: [Descripción de la actividad]
                • **Tiempo**: [X minutos]
                • **Recursos**: [Materiales necesarios]
                • **Objetivo**: [Qué desarrolla en el estudiante]

                **👥 Actividad Grupal: [Nombre]**
                • **Qué hacer**: [Descripción de la actividad]
                • **Tiempo**: [X minutos]
                • **Recursos**: [Materiales necesarios]
                • **Objetivo**: [Qué desarrolla en el estudiante]

                **💡 Actividad Creativa: [Nombre]**
                • **Qué hacer**: [Descripción de la actividad]
                • **Tiempo**: [X minutos]
                • **Recursos**: [Materiales necesarios]
                • **Objetivo**: [Qué desarrolla en el estudiante]

                [Continuar con 2-3 actividades más]

                ---

                ## 2. PREGUNTAS DE REFLEXIÓN

                **Nivel inicial** (accesibles):
                1. [Pregunta simple sobre conceptos básicos]
                2. [Pregunta de experiencia personal]

                **Nivel intermedio** (analíticas):
                3. [Pregunta que requiere análisis]
                4. [Pregunta de relación entre conceptos]

                **Nivel avanzado** (críticas):
                5. [Pregunta que desafía suposiciones]
                6. [Pregunta de aplicación creativa]
                7. [Pregunta de evaluación]

                ---

                ## 3. EJERCICIOS GRUPALES

                **Dinámica 1: [Nombre]**
                • **Instrucciones paso a paso**:
                  1. [Paso 1]
                  2. [Paso 2]
                  3. [Paso 3]
                • **Tiempo**: [X minutos]
                • **Resultado esperado**: [Qué producen los grupos]

                **Dinámica 2: [Nombre]**
                • **Instrucciones paso a paso**:
                  1. [Paso 1]
                  2. [Paso 2]
                  3. [Paso 3]
                • **Tiempo**: [X minutos]
                • **Resultado esperado**: [Qué producen los grupos]

                ---

                ## 4. RECURSOS COMPLEMENTARIOS

                **Videos sugeridos**:
                • [Tema relacionado 1 - buscar en YouTube]
                • [Tema relacionado 2 - buscar en YouTube]

                **Artículos/Lecturas**:
                • [Tema para profundizar 1]
                • [Tema para profundizar 2]

                **Herramientas online**:
                • [Herramienta digital 1 - para qué sirve]
                • [Herramienta digital 2 - para qué sirve]

                ---

                💡 **CONSEJOS DE IMPLEMENTACIÓN**
                • [Tip 1 para que las actividades fluyan]
                • [Tip 2 para mantener la energía]
                • [Tip 3 para evaluar el aprendizaje]

                # ESTILO
                - Español chileno neutral y profesional
                - Formato Markdown estructurado
                - Actividades creativas pero prácticas
                - Máximo 700 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n🎮 ACTIVIDADES BÁSICAS para $nombrr
eClase\n\nPara actividades detalladas, verifica tu conexión.\n\nError: ${                            e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }
}
package cl.duocuc.aulaviva.data.repository

import android.app.Application
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
 * 🤖 GEMINI AI con Firebase AI Logic (Gemini Developer API)
 * Modelo: gemini-2.5-pro (vigente noviembre 2025)
 *
 * ✅ Firebase AI Logic con Gemini Developer API (15 req/min gratis)
 * ✅ PDFBox como fallback (offline, límite API)
 * ✅ Retrofit para funciones sin PDF
 */
class IARepository(private val application: Application) {

    companion object {
        private const val TAG = "AulaViva_IA"
    }

    // ✅ API Key cargada desde local.properties vía BuildConfig
    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY

    // ✅ Google AI SDK (Gemini Developer API)
    // Modelo actualizado a Gemini 2.5 Pro (Nov 2025) - State-of-the-art thinking model
    // Ideal para razonamiento complejo sobre documentos y memoria de chat
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

    // Sesión de chat persistente
    private var chatSession: com.google.ai.client.generativeai.Chat? = null

    // Cliente HTTP configurado con timeouts y logging
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
        // Inicializar PDFBox para Android (requerido para GlyphList y recursos)
        try {
            PDFBoxResourceLoader.init(application)
            Log.d(TAG, "✅ PDFBox Android inicializado correctamente")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error inicializando PDFBox: ${e.message}")
        }
        println("✅ Gemini AI Retrofit activado - Modelo: gemini-2.5-flash")
    }

    /**
     * Llama a Gemini API con Retrofit
     */
    private suspend fun llamarGemini(prompt: String): String {
        return withContext(Dispatchers.IO) {
            withTimeout(60000L) {
                var intento = 0
                var ultimoError: Exception? = null
                val maxIntentos = 3
                while (intento < maxIntentos) {
                    try {
                        // Construir request según docs oficiales
                        val request = GeminiRequest(
                            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                            generationConfig = GenerationConfig(
                                temperature = 0.6f, topP = 0.9f, maxOutputTokens = 4096
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
                            val code = response.code()
                            throw Exception("HTTP $code: ${response.message()}")
                        }
                    } catch (e: Exception) {
                        ultimoError = e
                        intento++
                        if (intento < maxIntentos && (e is SocketTimeoutException || e.message?.contains(
                                "HTTP 503"
                            ) == true || e.message?.contains("HTTP 504") == true || e.message?.contains(
                                "vacía"
                            ) == true)
                        ) {
                            try {
                                Thread.sleep((800L * intento))
                            } catch (_: InterruptedException) {
                            }
                            continue
                        } else if (intento < maxIntentos) {
                            try {
                                Thread.sleep(400L)
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

    /**
     * 🔍 MÉTODO HÍBRIDO: Firebase AI Logic primero, PDFBox fallback
     *
     * Este método garantiza que la IA SIEMPRE pueda leer el PDF:
     * 1. INTENTA Firebase AI Logic (Base64 inline) - Mejor precisión, OCR integrado
     * 2. Si falla, usa PDFBox (extracción texto local) - Funciona offline
     *
     * @param pdfUrl URL del PDF en Supabase
     * @param prompt Instrucción para la IA
     * @return Respuesta de Gemini basada en el PDF real
     */
    private suspend fun analizarPDFInteligente(pdfUrl: String, prompt: String): String =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "🔍 [HÍBRIDO] Iniciando análisis inteligente de PDF...")
            Log.d(TAG, "🔍 [HÍBRIDO] PDF URL: $pdfUrl")

            try {
                // INTENTO 1: Google AI Gemini (RECOMENDADO - OCR integrado)
                Log.d(TAG, "🔥 [Google AI] Intentando método Google AI SDK...")
                return@withContext analizarConGoogleAI(pdfUrl, prompt)

            } catch (e: Exception) {
                // INTENTO 2: PDFBox (FALLBACK - offline)
                Log.w(TAG, "⚠️ [HÍBRIDO] Google AI falló: ${e.message}")
                Log.d(TAG, "📄 [PDFBox] Cambiando a método fallback PDFBox...")
                return@withContext analizarConPDFBox(pdfUrl, prompt)
            }
        }

    /**
     * 🔥 Google AI SDK: Envía PDF completo (Base64 inline)
     * Gemini parsea el PDF server-side con OCR + tablas + gráficos.
     *
     * Ventajas:
     * - OCR integrado (lee PDFs escaneados)
     * - Parsea tablas y gráficos
     * - Mayor precisión que extraer texto local
     * - Compatible con Ktor 2.3.12 (no conflicto con Supabase)
     *
     * @param pdfUrl URL del PDF
     * @param prompt Instrucción para la IA
     * @return Respuesta de Gemini
     */
    private suspend fun analizarConGoogleAI(pdfUrl: String, prompt: String): String {
        Log.d(TAG, "📦 [Google AI] Descargando PDF desde Supabase...")

        // 1. Descargar PDF a archivo temporal (streaming)
        val pdfFile = descargarPDFATempFile(pdfUrl)
        Log.d(TAG, "📦 [Google AI] PDF descargado a archivo temporal: ${pdfFile.length()} bytes")

        if (pdfFile.length() <= 20_000_000L) {
            // Si cabe en límite, enviar como blob (mantener comportamiento original)
            val pdfBytes = pdfFile.readBytes()
            val contenidoMultimodal = content {
                text(prompt)
                blob("application/pdf", pdfBytes)
            }

            Log.d(TAG, "🤖 [Google AI] Enviando a Gemini 2.5 (Developer API) con blob...")

            // Generar respuesta con timeout
            val response = withTimeout(90_000) { // 90 segundos
                googleAiModel.generateContent(contenidoMultimodal)
            }

            val textoRespuesta =
                response.text ?: throw Exception("Respuesta vacía de Google AI SDK")

            Log.d(TAG, "✅ [Google AI] Respuesta recibida: ${textoRespuesta.length} caracteres")
            Log.d(TAG, "✅ [Google AI] Primeros 150 chars: ${textoRespuesta.take(150)}...")

            return textoRespuesta
        } else {
            // Si es muy grande, evitar cargar en memoria y usar extracción por chunks vía PDFBox
            Log.w(
                TAG,
                "⚠️ [Google AI] PDF >20MB, usando pipeline de extracción por chunks y síntesis con Gemini"
            )
            val chunks = extractTextChunksFromPdf(pdfFile)
            // 1) Resumir cada chunk con Gemini para reducir tokens
            val summaries = mutableListOf<String>()
            for ((index, chunk) in chunks.withIndex()) {
                val chunkPrompt = """
                    ${prompt}

                    ---
                    PARTE ${index + 1}/${chunks.size} DEL PDF:
                    ${chunk.take(30_000)}
                    ---
                    Instrucción: Resume los puntos clave y extrae conceptos específicos de esta parte. Responde en español.
                """.trimIndent()

                try {
                    val s = llamarGemini(chunkPrompt)
                    summaries.add(s)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ [Google AI] Error resumiendo chunk ${index + 1}: ${e.message}")
                    summaries.add("[Error resumiendo chunk ${index + 1}: ${e.message}]")
                }
            }

            // 2) Sintetizar todos los resúmenes en un único análisis
            val combined = summaries.joinToString("\n\n---\n\n")
            val finalPrompt = """
                ${prompt}

                He recibido el PDF en partes y he resumido cada sección. A continuación están los resúmenes parciales:
                $combined

                INSTRUCCIÓN FINAL: A partir de los resúmenes parciales, genera un análisis coherente del PDF completo, mencionando conceptos y detalles específicos.
            """.trimIndent()

            return llamarGemini(finalPrompt)
        }
    }

    /**
     * 📄 PDFBox: Extrae texto local y envía a Gemini (FALLBACK)
     *
     * Este método se usa cuando Firebase falla (red, timeout, límite API).
     * Extrae texto localmente y lo envía a Gemini vía Retrofit.
     *
        } else {
            // Si es muy grande, evitar múltiples llamadas externas: extraer texto en disco y comprimir localmente
            Log.w(TAG, "⚠️ [Google AI] PDF >20MB, extrayendo texto y comprimiendo localmente antes de llamar a Gemini")
            val chunks = extractTextChunksFromPdf(pdfFile)
            val compressed = compressChunksForSingleCall(chunks)

            val finalPrompt = """
                ${prompt}

                A continuación se presenta una representación comprimida del contenido del PDF (resúmenes y muestras representativas). Usa esto como contexto para analizar el documento completo y generar la respuesta solicitada.

                CONTENIDO COMPRIMIDO:
                $compressed

                IMPORTANTE: Basar la respuesta en el contenido provisto y mencionar detalles específicos cuando posible.
            """.trimIndent()

            return llamarGemini(finalPrompt)
        }

        document.close()

        // 4. Si el texto total es pequeño, mandarlo completo; sino, resumir por chunks y sintetizar
        val totalChars = chunks.sumOf { it.length }
        Log.d(TAG, "📄 [PDFBox] Texto total extraído (chars): $totalChars")

        return if (totalChars < 30_000) {
            val textoPdf = chunks.joinToString("\n\n")
            Log.d(
                TAG,
                "📄 [PDFBox] Texto extraído pequeño, enviando completo (${textoPdf.length} chars)"
            )
            val promptCompleto = """
                $prompt

                📎 CONTENIDO DEL PDF (extraído con PDFBox):
                ---
                METADATA:
                - Total de páginas: $totalPaginas
                - Caracteres extraídos: ${textoPdf.length}

                CONTENIDO COMPLETO:
                $textoPdf
                ---

                IMPORTANTE: Analiza TODO el contenido del PDF proporcionado.
                Menciona detalles ESPECÍFICOS que aparezcan en el texto.
            """.trimIndent()

            Log.d(TAG, "🤖 [PDFBox] Enviando texto extraído a Gemini vía Retrofit...")
            val respuesta = llamarGemini(promptCompleto)
            Log.d(TAG, "✅ [PDFBox] Respuesta recibida: ${respuesta.length} caracteres")
            respuesta
        } else {
            Log.d(TAG, "📄 [PDFBox] Texto grande, comprimiendo localmente antes de llamar a Gemini...")
            val compressed = compressChunksForSingleCall(chunks)
            val finalPrompt = """
                ${prompt}

                A continuación se presenta una representación comprimida del contenido del PDF (resúmenes y muestras representativas). Usa esto como contexto para analizar el documento completo y generar la respuesta solicitada.

                CONTENIDO COMPRIMIDO:
                $compressed

                INSTRUCCIÓN FINAL: A partir del contenido comprimido, genera un análisis coherente del PDF completo, mencionando conceptos y detalles específicos.
            """.trimIndent()

            return llamarGemini(finalPrompt)
        }
    }

    /**
     * 📥 Helper: Descarga PDF desde URL
     *
     * @param url URL del PDF (Supabase Storage)
     * @return ByteArray con el contenido del PDF
     */
    private suspend fun descargarPDF(url: String): ByteArray {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()

            val request = okhttp3.Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw java.io.IOException("Error HTTP al descargar PDF: ${response.code}")
            }

            val bytes = response.body?.bytes()
            if (bytes == null || bytes.isEmpty()) {
                throw java.io.IOException("PDF descargado está vacío")
            }

            bytes
        }
    }

    /**
     * Descarga un PDF streaming a un archivo temporal dentro de cacheDir y lo retorna.
     */
    private suspend fun descargarPDFATempFile(url: String): File {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()

            val request = okhttp3.Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw java.io.IOException("Error HTTP al descargar PDF: ${response.code}")
            }

            val body = response.body ?: throw java.io.IOException("Body vacío al descargar PDF")

            val tempFile = File.createTempFile("aulaviva_pdf_", ".pdf", application.cacheDir)
            FileOutputStream(tempFile).use { out ->
                body.byteStream().use { input ->
                    input.copyTo(out)
                }
            }

            tempFile
        }
    }

    /**
     * Extrae chunks de texto desde un PDF en disco usando PDFBox, devolviendo una lista de strings por bloque.
     */
    private fun extractTextChunksFromPdf(file: File, pagesPerChunk: Int = 5): List<String> {
        val out = mutableListOf<String>()
        try {
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(
                file,
                MemoryUsageSetting.setupTempFileOnly()
            )
            val totalPaginas = document.numberOfPages
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            var page = 1
            while (page <= totalPaginas) {
                stripper.startPage = page
                stripper.endPage = (page + pagesPerChunk - 1).coerceAtMost(totalPaginas)
                val chunk = stripper.getText(document)
                out.add(chunk)
                page += pagesPerChunk
            }
            document.close()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [PDFBox] Error extrayendo chunks: ${e.message}")
            // fallback vacío
        }
        return out
    }

    /**
     * Comprime una lista de chunks en una representación reducida adecuada para
     * enviarse en una única llamada a Gemini. Selecciona muestras de cada chunk
     * y limita el tamaño total a `maxChars`.
     */
    private fun compressChunksForSingleCall(chunks: List<String>, maxChars: Int = 40_000): String {
        if (chunks.isEmpty()) return ""
        val builder = StringBuilder()
        for ((index, chunk) in chunks.withIndex()) {
            if (builder.length >= maxChars) break
            val head = chunk.take(1500)
            builder.append("-- PARTE ${index + 1} --\n")
            builder.append(head)
            builder.append("\n\n")
        }
        // Añadir muestras tail si hay espacio
        var i = 0
        while (builder.length < maxChars && i < chunks.size) {
            val tail = chunks[i].takeLast(500)
            builder.append("-- MUESTRA ${i + 1} (tail) --\n")
            builder.append(tail)
            builder.append("\n\n")
            i++
        }
        if (builder.length > maxChars) return builder.toString().take(maxChars)
        return builder.toString()
    }

    /**
     * 💡 Genera ideas creativas para la clase
     */
    suspend fun generarIdeasParaClase(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        return try {
            val prompt = """
                # CONTEXTO Y ROL
                Eres un consultor en innovación educativa para docentes de educación superior chilena.

                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion

                # INSTRUCCIONES CRÍTICAS
                1. **Analiza TODO el PDF adjunto**: Lee completamente su contenido
                2. **En "CONTEXTO DETECTADO"**: Menciona EXPLÍCITAMENTE detalles del PDF (títulos, temas, conceptos específicos)
                3. **Genera ideas**: Basadas en el contenido REAL del PDF, no en suposiciones

                # FORMATO DE RESPUESTA

                🎯 **CONTEXTO DETECTADO**
                • Clase: $nombreClase
                • PDF analizado: [SI - menciona título o primeros temas del PDF]
                • Tema central detectado: [basado en PDF]
                • Área: [disciplina]
                • Público objetivo: [ej: Estudiantes primer año, profesionales]

                💡 **IDEAS PARA LA CLASE**

                1. **[Nombre de idea 1]**
                   Descripción breve de la actividad y su valor pedagógico (2-3 líneas)

                2. **[Nombre de idea 2]**
                   Descripción breve de la actividad y su valor pedagógico (2-3 líneas)

                3. **[Nombre de idea 3]**
                   Descripción breve de la actividad y su valor pedagógico (2-3 líneas)

                4. **[Nombre de idea 4]**
                   Descripción breve de la actividad y su valor pedagógico (2-3 líneas)

                5. **[Nombre de idea 5]**
                   Descripción breve de la actividad y su valor pedagógico (2-3 líneas)

                ⭐ **TIP ADICIONAL**
                [Una recomendación creativa que potencie la clase]

                # ESTILO
                - Español chileno profesional
                - Ideas concretas y aplicables
                - Balance entre innovación y practicidad
                - Máximo 350 palabras
            """.trimIndent()

            // ✅ USAR MÉTODO HÍBRIDO si hay PDF
            val resultado = if (!pdfUrl.isNullOrEmpty()) {
                Log.d(TAG, "💡 [IDEAS] PDF detectado, usando método híbrido Firebase AI/PDFBox...")
                analizarPDFInteligente(pdfUrl, prompt)
            } else {
                Log.d(TAG, "💡 [IDEAS] Sin PDF, llamada Gemini estándar")
                llamarGemini(prompt)
            }

            "$resultado\n\n🚬😶‍🌫️ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            Log.e(TAG, "💡 [IDEAS] Error: ${e.message}", e)
            "⚠️ Error al conectar con Gemini AI\n\n💡 IDEAS GENERALES para $nombreClase\n\nPara ideas personalizadas, verifica tu conexión.\n\nError: ${
                e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    /**
     * 🎯 Sugiere actividades dinámicas
     */
    suspend fun sugerirActividades(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String? = null
    ): String {
        return try {
            val prompt = """
                # CONTEXTO Y ROL
                Eres un diseñador instruccional especializado en aprendizaje activo para educación superior chilena.

                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion

                # INSTRUCCIONES
                1. **Si hay PDF adjunto**: Analiza su contenido completo y basa las actividades en él
                2. **Confirma el contexto**: Identifica tema y nivel
                3. **Asume rol de pedagogo**: Experto en metodologías activas
                4. **Diseña 4 actividades**: Variadas y con diferente nivel de complejidad

                # FORMATO DE RESPUESTA

                🎓 **CONTEXTO DETECTADO**
                • Tema: [tema identificado del PDF o descripción]
                • Enfoque sugerido: [ej: Práctico, Teórico-aplicado, Reflexivo]

                🎯 **ACTIVIDADES DISEÑADAS**

                **ACTIVIDAD 1: [Nombre descriptivo]**
                • **Objetivo**: [Qué se busca lograr]
                • **Duración**: [X minutos]
                • **Tipo**: [Individual/Grupal/Plenaria]
                • **Materiales**: [Recursos necesarios]
                • **Cómo ejecutarla**: [Pasos breves]

                **ACTIVIDAD 2: [Nombre descriptivo]**
                • **Objetivo**: [Qué se busca lograr]
                • **Duración**: [X minutos]
                • **Tipo**: [Individual/Grupal/Plenaria]
                • **Materiales**: [Recursos necesarios]
                • **Cómo ejecutarla**: [Pasos breves]

                **ACTIVIDAD 3: [Nombre descriptivo]**
                • **Objetivo**: [Qué se busca lograr]
                • **Duración**: [X minutos]
                • **Tipo**: [Individual/Grupal/Plenaria]
                • **Materiales**: [Recursos necesarios]
                • **Cómo ejecutarla**: [Pasos breves]

                **ACTIVIDAD 4: [Nombre descriptivo]**
                • **Objetivo**: [Qué se busca lograr]
                • **Duración**: [X minutos]
                • **Tipo**: [Individual/Grupal/Plenaria]
                • **Materiales**: [Recursos necesarios]
                • **Cómo ejecutarla**: [Pasos breves]

                ⏱️ **TIEMPO TOTAL**: [Suma de duraciones]

                💡 **TIP DE IMPLEMENTACIÓN**
                [Consejo para que las actividades fluyan mejor]

                # ESTILO
                - Español chileno profesional
                - Instrucciones claras y ejecutables
                - Actividades variadas (no repetitivas)
                - Máximo 500 palabras
            """.trimIndent()

            // ✅ USAR MÉTODO HÍBRIDO si hay PDF
            val resultado = if (!pdfUrl.isNullOrEmpty()) {
                Log.d(
                    TAG,
                    "🎯 [ACTIVIDADES] PDF detectado, usando método híbrido Firebase AI/PDFBox..."
                )
                analizarPDFInteligente(pdfUrl, prompt)
            } else {
                Log.d(TAG, "🎯 [ACTIVIDADES] Sin PDF, llamada Gemini estándar")
                llamarGemini(prompt)
            }

            "$resultado\n\n🚬😶‍🌫️ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            Log.e(TAG, "🎯 [ACTIVIDADES] Error: ${e.message}", e)
            "⚠️ Error al conectar con Gemini AI\n\n🎯 ACTIVIDADES GENERALES para $nombreClase\n\nPara actividades detalladas, verifica tu conexión.\n\nError: ${
                e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    /**
     * ⏱️ Estructura clase por tiempo
     */
    suspend fun estructurarClasePorTiempo(
        nombreClase: String,
        descripcion: String,
        duracionMinutos: String,
        pdfUrl: String? = null
    ): String {
        return try {
            val contextoPdf = if (!pdfUrl.isNullOrEmpty()) {
                "\n📎 **Material disponible**: PDF de apoyo para complementar la estructura"
            } else ""
            val prompt = """
                # CONTEXTO Y ROL
                Eres un planificador educativo experto en gestión del tiempo para clases universitarias chilenas.

                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion
                ⏱️ Duración total: $duracionMinutos$contextoPdf                # INSTRUCCIONES
                1. **Confirma el tema**: Identifica área y complejidad
                2. **Asume rol de planificador**: Experto en timing educativo
                3. **Estructura la clase**: Divide el tiempo de forma óptima

                # FORMATO DE RESPUESTA

                🎓 **ANÁLISIS INICIAL**
                • Tema: [tema identificado]
                • Complejidad: [Baja/Media/Alta]
                • Enfoque recomendado: [Teórico/Práctico/Mixto]

                ⏱️ **ESTRUCTURA TEMPORAL ($duracionMinutos)**

                **🟢 INICIO (__ min | 0:00 - __:__)**
                • **Saludo y contextualización** (__ min)
                  → [Qué hacer específicamente]
                • **Objetivos de la clase** (__ min)
                  → [Presentar metas claras]
                • **Activación de conocimientos previos** (__ min)
                  → [Pregunta o actividad breve]

                **🔵 DESARROLLO (__ min | __:__ - __:__)**
                • **Presentación del contenido principal** (__ min)
                  → [Explicación o demostración]
                • **Actividad práctica / Ejercicio** (__ min)
                  → [Trabajo individual o grupal]
                • **Retroalimentación grupal** (__ min)
                  → [Compartir resultados]
                • **Profundización** (__ min)
                  → [Ejemplos adicionales o dudas]

                **🟡 CIERRE (__ min | __:__ - __:__)**
                • **Síntesis de aprendizajes** (__ min)
                  → [Resumen colaborativo]
                • **Evaluación formativa** (__ min)
                  → [Quiz, pregunta, ticket de salida]
                • **Asignación de tareas** (__ min)
                  → [Si aplica, explicar homework]

                ⚠️ **TIEMPO DE BUFFER**: __ min (para imprevistos)

                💡 **TIPS DE GESTIÓN DEL TIEMPO**
                • [Consejo 1 para mantener el ritmo]
                • [Consejo 2 para no excederse]

                # ESTILO
                - Español chileno profesional
                - Tiempos precisos y realistas
                - Actividades claras en cada bloque
                - Máximo 450 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n⏱️ ESTRUCTURA BÁSICA ($duracionMinutos) para $nombreClase\n\nPara planificación detallada, verifica tu conexión.\n\nError: ${
                e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    /**
     * 📊 Analiza PDF con IA
     */
    suspend fun analizarPdfConIA(nombreClase: String, pdfUrl: String? = null): String {
        return try {
            if (pdfUrl.isNullOrEmpty()) {
                return "⚠️ No se proporcionó URL del PDF"
            }

            val prompt = """
                # CONTEXTO Y ROL
                Eres un analista de materiales educativos para docentes universitarios chilenos.

                # SITUACIÓN
                Analiza el PDF adjunto para la clase "$nombreClase".

                # INSTRUCCIONES
                1. **Lee TODO el PDF completo**
                2. **Identifica**: Tema principal, conceptos clave, estructura
                3. **Evalúa**: Calidad, profundidad, utilidad pedagógica
                4. **Sugiere**: Cómo aprovechar mejor este material

                # FORMATO DE RESPUESTA

                📊 **ANÁLISIS DEL PDF**

                **Tema Principal**: [Tema identificado del PDF]

                **Conceptos Clave**:
                • [Concepto 1 del PDF]
                • [Concepto 2 del PDF]
                • [Concepto 3 del PDF]

                **Estructura Detectada**:
                [Breve descripción de cómo está organizado el PDF]

                **Evaluación Pedagógica**:
                [Fortalezas y limitaciones del material]

                **Sugerencias de Uso**:
                1. [Cómo usar este PDF en clase]
                2. [Actividad sugerida basada en el contenido]
                3. [Punto importante a enfatizar del PDF]

                # ESTILO
                - Español chileno profesional
                - Análisis basado en contenido REAL del PDF
                - Máximo 400 palabras
            """.trimIndent()

            // ✅ USAR MÉTODO HÍBRIDO
            Log.d(TAG, "📊 [ANÁLISIS] Analizando PDF con método híbrido...")
            val resultado = analizarPDFInteligente(pdfUrl, prompt)
            "$resultado\n\n🚬😶‍🌫️ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            Log.e(TAG, "📊 [ANÁLISIS] Error: ${e.message}", e)
            "⚠️ Error al analizar el PDF\n\nError: ${e.message?.take(100) ?: "Desconocido"}"
        }
    }

    /**
     * 📝 Resume contenido del PDF
     */
    suspend fun resumirContenidoPdf(
        nombreClase: String,
        descripcion: String,
        nombrePdf: String
    ): String {
        return try {
            val prompt = """
                # CONTEXTO Y ROL
                Eres un sintetizador de contenido educativo para docentes chilenos.

                # DATOS DEL MATERIAL
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion
                📄 Material PDF: $nombrePdf

                # INSTRUCCIONES
                1. **Confirma el tema**: Identifica el contenido principal
                2. **Asume rol apropiado**: Experto en esa área
                3. **Genera resumen estructurado**: Útil para preparar la clase

                # FORMATO DE RESPUESTA

                📄 **IDENTIFICACIÓN DEL MATERIAL**
                • Clase: $nombreClase
                • Tema central detectado: [tema principal]
                • Tipo de contenido: [Teórico/Práctico/Mixto]

                ## TEMA PRINCIPAL
                [Descripción clara del tema central en 2-3 líneas]

                ## CONCEPTOS CLAVE
                • **Concepto 1**: [Explicación breve]
                • **Concepto 2**: [Explicación breve]
                • **Concepto 3**: [Explicación breve]
                • **Concepto 4**: [Explicación breve]
                • **Concepto 5**: [Si aplica]

                ## CONCLUSIONES PRINCIPALES
                [Resumen de los aprendizajes fundamentales que el docente debe transmitir]

                💡 **SUGERENCIA DIDÁCTICA**
                [Cómo el docente puede presentar este contenido de forma efectiva]

                # ESTILO
                - Español chileno profesional
                - Formato Markdown estructurado
                - Enfoque en lo esencial para el docente
                - Máximo 350 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n📝 RESUMEN BÁSICO de $nombreClase\n\nPara resumen detallado, verifica tu conexión.\n\nError: ${
                e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    /**
     * 🌟 Reordena temas para presentar (Guía de presentación)
     */
    suspend fun generarGuiaPresentacion(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String? = null
    ): String {
        return try {
            val contextoPdf = if (!pdfUrl.isNullOrEmpty()) {
                "\n📎 **Material de apoyo**: PDF disponible para integrar en la presentación"
            } else ""
            val prompt = """
                # CONTEXTO Y ROL
                Eres un coach de oratoria y presentación para docentes universitarios chilenos.

                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Contenido: $descripcion$contextoPdf                # INSTRUCCIONES
                1. **Confirma el tema**: Identifica área y complejidad
                2. **Asume rol de coach**: Experto en presentación efectiva
                3. **Crea guía detallada**: Secuencia óptima para presentar el contenido

                # FORMATO DE RESPUESTA

                🎓 **ANÁLISIS DEL CONTENIDO**
                • Tema: $nombreClase
                • Complejidad: [Baja/Media/Alta]
                • Tipo de clase: [Magistral/Participativa/Práctica]

                ## 1. INTRODUCCIÓN SUGERIDA (2-3 minutos)

                **🎯 Gancho inicial**
                [Frase, pregunta o dato impactante para captar atención]

                **📌 Contextualización**
                [Por qué este tema es relevante para los estudiantes]

                **🎓 Objetivos de la clase**
                [Lo que aprenderán hoy, en lenguaje simple]

                ---

                ## 2. PUNTOS CLAVE A ENFATIZAR

                **Punto 1: [Concepto fundamental]**
                → Por qué es importante: [Relevancia]
                → Cómo explicarlo: [Estrategia]

                **Punto 2: [Concepto fundamental]**
                → Por qué es importante: [Relevancia]
                → Cómo explicarlo: [Estrategia]

                **Punto 3: [Concepto fundamental]**
                → Por qué es importante: [Relevancia]
                → Cómo explicarlo: [Estrategia]

                ---

                ## 3. EJEMPLOS PRÁCTICOS RECOMENDADOS

                • **Ejemplo 1**: [Caso real o analogía simple]
                • **Ejemplo 2**: [Ejercicio concreto]
                • **Ejemplo 3**: [Comparación útil]

                ---

                ## 4. PREGUNTAS PARA GENERAR PARTICIPACIÓN

                **Preguntas de inicio** (despertar interés):
                • [Pregunta abierta 1]
                • [Pregunta abierta 2]

                **Preguntas de desarrollo** (profundizar):
                • [Pregunta desafiante 1]
                • [Pregunta desafiante 2]

                **Pregunta de cierre** (sintetizar):
                • [Pregunta reflexiva final]

                ---

                💡 **TIPS DE PRESENTACIÓN**
                • [Consejo 1 para mantener la atención]
                • [Consejo 2 para manejar dudas]
                • [Consejo 3 para cerrar con impacto]

                # ESTILO
                - Español chileno profesional
                - Formato Markdown estructurado
                - Consejos prácticos y aplicables
                - Máximo 600 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n🎤 GUÍA BÁSICA DE PRESENTACIÓN para $nombreClase\n\nPara guía detallada, verifica tu conexión.\n\nError: ${
                e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    /**
     * 💭 Ideas para dictar la clase (Actividades interactivas)
     */
    suspend fun generarActividadesInteractivas(
        nombreClase: String,
        descripcion: String,
        nombrePdf: String?
    ): String {
        return try {
            val contextoPdf =
                if (!nombrePdf.isNullOrEmpty()) "\n📎 Material de apoyo: $nombrePdf" else ""
            val prompt = """
                # CONTEXTO Y ROL
                Eres un diseñador instruccional especializado en transformar contenido en experiencias de aprendizaje activo.

                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion$contextoPdf

                # INSTRUCCIONES
                1. **Confirma el tema**: Identifica área y nivel
                2. **Asume rol de diseñador**: Experto en aprendizaje activo
                3. **Transforma en clase interactiva**: Múltiples actividades variadas

                # FORMATO DE RESPUESTA

                🎓 **CONTEXTO PEDAGÓGICO**
                • Tema: $nombreClase
                • Enfoque: [Constructivista/Colaborativo/Experiencial]
                • Nivel de interactividad: [Alto - múltiples dinámicas]

                ## 1. ACTIVIDADES PRÁCTICAS (3-5 propuestas)

                **📝 Actividad Individual: [Nombre]**
                • **Qué hacer**: [Descripción de la actividad]
                • **Tiempo**: [X minutos]
                • **Recursos**: [Materiales necesarios]
                • **Objetivo**: [Qué desarrolla en el estudiante]

                **👥 Actividad Grupal: [Nombre]**
                • **Qué hacer**: [Descripción de la actividad]
                • **Tiempo**: [X minutos]
                • **Recursos**: [Materiales necesarios]
                • **Objetivo**: [Qué desarrolla en el estudiante]

                **💡 Actividad Creativa: [Nombre]**
                • **Qué hacer**: [Descripción de la actividad]
                • **Tiempo**: [X minutos]
                • **Recursos**: [Materiales necesarios]
                • **Objetivo**: [Qué desarrolla en el estudiante]

                [Continuar con 2-3 actividades más]

                ---

                ## 2. PREGUNTAS DE REFLEXIÓN

                **Nivel inicial** (accesibles):
                1. [Pregunta simple sobre conceptos básicos]
                2. [Pregunta de experiencia personal]

                **Nivel intermedio** (analíticas):
                3. [Pregunta que requiere análisis]
                4. [Pregunta de relación entre conceptos]

                **Nivel avanzado** (críticas):
                5. [Pregunta que desafía suposiciones]
                6. [Pregunta de aplicación creativa]
                7. [Pregunta de evaluación]

                ---

                ## 3. EJERCICIOS GRUPALES

                **Dinámica 1: [Nombre]**
                • **Instrucciones paso a paso**:
                  1. [Paso 1]
                  2. [Paso 2]
                  3. [Paso 3]
                • **Tiempo**: [X minutos]
                • **Resultado esperado**: [Qué producen los grupos]

                **Dinámica 2: [Nombre]**
                • **Instrucciones paso a paso**:
                  1. [Paso 1]
                  2. [Paso 2]
                  3. [Paso 3]
                • **Tiempo**: [X minutos]
                • **Resultado esperado**: [Qué producen los grupos]

                ---

                ## 4. RECURSOS COMPLEMENTARIOS

                **Videos sugeridos**:
                • [Tema relacionado 1 - buscar en YouTube]
                • [Tema relacionado 2 - buscar en YouTube]

                **Artículos/Lecturas**:
                • [Tema para profundizar 1]
                • [Tema para profundizar 2]

                **Herramientas online**:
                • [Herramienta digital 1 - para qué sirve]
                • [Herramienta digital 2 - para qué sirve]

                ---

                💡 **CONSEJOS DE IMPLEMENTACIÓN**
                • [Tip 1 para que las actividades fluyan]
                • [Tip 2 para mantener la energía]
                • [Tip 3 para evaluar el aprendizaje]

                # ESTILO
                - Español chileno neutral y profesional
                - Formato Markdown estructurado
                - Actividades creativas pero prácticas
                - Máximo 700 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n🎮 ACTIVIDADES BÁSICAS para $nombreClase\n\nPara actividades detalladas, verifica tu conexión.\n\nError: ${
                e.message?.take(
                    100
                ) ?: "Desconocido"
            }"
        }
    }

    // ========================================
    // 🎓 FUNCIONES IA PARA ALUMNOS
    // ========================================

    /**
     * 📚 Explica conceptos claves para que el alumno APRENDA
     */
    suspend fun explicarConceptosParaAlumno(
        nombreClase: String,
        descripcion: String,
        nombrePdf: String?
    ): String {
        return try {
            val contextoPdf = if (!nombrePdf.isNullOrEmpty()) {
                "\n📄 Material de estudio: $nombrePdf"
            } else ""
            val prompt = """
                # CONTEXTO Y ROL
                Eres un tutor personal que ayuda a estudiantes a COMPRENDER contenidos.

                # DATOS DE LA CLASE
                📚 Clase que el alumno está estudiando: $nombreClase
                📝 Descripción: $descripcion$contextoPdf

                # OBJETIVO
                Explicar los conceptos de forma simple y clara para que el ESTUDIANTE entienda.
                NO estás ayudando a un profesor a enseñar, estás ayudando a un ALUMNO a APRENDER.

                # FORMATO DE RESPUESTA

                🎓 **¿QUÉ VAS A APRENDER HOY?**
                Tema: $nombreClase
                [Explicación en 2-3 líneas de qué trata esta clase]

                ## 📖 CONCEPTOS PRINCIPALES EXPLICADOS

                **1. [Concepto clave]**
                → **¿Qué es?**: [Definición simple]
                → **¿Por qué importa?**: [Aplicación práctica]
                → **Ejemplo**: [Caso concreto fácil de entender]

                **2. [Concepto clave]**
                → **¿Qué es?**: [Definición simple]
                → **¿Por qué importa?**: [Aplicación práctica]
                → **Ejemplo**: [Caso concreto fácil de entender]

                **3. [Concepto clave]**
                → **¿Qué es?**: [Definición simple]
                → **¿Por qué importa?**: [Aplicación práctica]
                → **Ejemplo**: [Caso concreto fácil de entender]

                ## 💡 RESUMEN EN PALABRAS SIMPLES
                [Explicación global que conecta todos los conceptos, como si se lo explicaras a un amigo]

                ## 🎯 CÓMO ESTUDIAR ESTO
                1. [Consejo de estudio 1]
                2. [Consejo de estudio 2]
                3. [Consejo de estudio 3]

                # ESTILO
                - Tutear al estudiante (usa "tú")
                - Lenguaje amigable y motivador
                - Ejemplos cercanos a la vida real
                - Máximo 500 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n📚 CONCEPTOS BÁSICOS de $nombreClase\n\nPara explicación detallada, verifica tu conexión.\n\nError: ${
                e.message?.take(100) ?: "Desconocido"
            }"
        }
    }

    /**
     * ✍️ Genera ejercicios prácticos para que el alumno PRACTIQUE
     */
    suspend fun generarEjerciciosParaAlumno(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        return try {
            val contextoPdf = if (!pdfUrl.isNullOrEmpty()) {
                "\n📄 Basado en el material: PDF de la clase"
            } else ""
            val prompt = """
                # CONTEXTO Y ROL
                Eres un tutor que crea ejercicios prácticos para que los ESTUDIANTES practiquen y refuercen lo aprendido.

                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Contenido: $descripcion$contextoPdf

                # OBJETIVO
                Crear ejercicios variados para que el ALUMNO practique activamente.
                Ejercicios deben ser: claros, progresivos (fácil → difícil), y con orientación.

                # FORMATO DE RESPUESTA

                🎯 **EJERCICIOS PRÁCTICOS**
                Clase: $nombreClase

                ## 📝 NIVEL BÁSICO (Para empezar)

                **Ejercicio 1: [Título]**
                🎯 Objetivo: [Qué practicas con esto]
                📋 Instrucciones:
                [Paso 1]
                [Paso 2]
                [Paso 3]
                💡 Pista: [Ayuda si se traba]

                **Ejercicio 2: [Título]**
                🎯 Objetivo: [Qué practicas con esto]
                📋 Instrucciones:
                [Paso 1]
                [Paso 2]
                [Paso 3]
                💡 Pista: [Ayuda si se traba]

                ---

                ## 🚀 NIVEL INTERMEDIO (Más desafío)

                **Ejercicio 3: [Título]**
                🎯 Objetivo: [Qué practicas con esto]
                📋 Instrucciones:
                [Descripción del ejercicio]
                💡 Pista: [Ayuda si se traba]

                **Ejercicio 4: [Título]**
                🎯 Objetivo: [Qué practicas con esto]
                📋 Instrucciones:
                [Descripción del ejercicio]
                💡 Pista: [Ayuda si se traba]

                ---

                ## ⭐ EJERCICIO DESAFÍO (Opcional)

                **Ejercicio 5: [Título creativo]**
                🎯 Objetivo: [Integrar todo lo aprendido]
                📋 Instrucciones:
                [Descripción del ejercicio más complejo]
                💡 Pista: [Ayuda estratégica]

                ---

                ## ✅ AUTOEVALUACIÓN
                Después de hacer los ejercicios, pregúntate:
                1. [Pregunta de reflexión 1]
                2. [Pregunta de reflexión 2]
                3. [Pregunta de reflexión 3]

                # ESTILO
                - Tutear al estudiante
                - Instrucciones claras y paso a paso
                - Motivador y positivo
                - Máximo 600 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n✍️ EJERCICIOS BÁSICOS de $nombreClase\n\nPara ejercicios detallados, verifica tu conexión.\n\nError: ${
                e.message?.take(100) ?: "Desconocido"
            }"
        }
    }

    /**
     * 📖 Crea resumen de estudio para el alumno
     */
    suspend fun crearResumenEstudioParaAlumno(
        nombreClase: String,
        descripcion: String,
        nombrePdf: String?
    ): String {
        return try {
            val contextoPdf = if (!nombrePdf.isNullOrEmpty()) {
                "\n📄 Material: $nombrePdf"
            } else ""
            val prompt = """
                # CONTEXTO Y ROL
                Eres un tutor que ayuda a estudiantes a crear RESÚMENES efectivos para estudiar.

                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion$contextoPdf

                # OBJETIVO
                Crear un resumen estructurado que el ALUMNO pueda usar para estudiar y repasar.
                Debe ser: conciso, visual (emojis/listas), fácil de memorizar.

                # FORMATO DE RESPUESTA

                📖 **RESUMEN DE ESTUDIO**
                Clase: $nombreClase

                ## 🎯 LO MÁS IMPORTANTE (En 3 puntos)
                1. [Idea central 1]
                2. [Idea central 2]
                3. [Idea central 3]

                ---

                ## 📚 CONCEPTOS CLAVE

                | Concepto | Definición Simple | ¿Para qué sirve? |
                |----------|-------------------|------------------|
                | [Término 1] | [Explicación breve] | [Aplicación] |
                | [Término 2] | [Explicación breve] | [Aplicación] |
                | [Término 3] | [Explicación breve] | [Aplicación] |
                | [Término 4] | [Explicación breve] | [Aplicación] |

                ---

                ## 🔗 CONEXIONES IMPORTANTES
                [Cómo se relacionan los conceptos entre sí, explicado de forma simple]

                ---

                ## 💡 REGLAS/FÓRMULAS A RECORDAR
                • **[Regla 1]**: [Explicación]
                • **[Regla 2]**: [Explicación]
                • **[Regla 3]**: [Explicación]

                ---

                ## ⚠️ ERRORES COMUNES A EVITAR
                1. ❌ [Error típico 1] → ✅ [Forma correcta]
                2. ❌ [Error típico 2] → ✅ [Forma correcta]
                3. ❌ [Error típico 3] → ✅ [Forma correcta]

                ---

                ## 🎓 TIPS PARA MEMORIZAR
                • [Truco mnemotécnico o consejo 1]
                • [Truco mnemotécnico o consejo 2]
                • [Truco mnemotécnico o consejo 3]

                ---

                ## ✅ CHECKLIST DE ESTUDIO
                Marca lo que ya dominas:
                - (  ) Habilidad 1
                - (  ) Habilidad 2
                - (  ) Habilidad 3
                - (  ) Habilidad 4

                # ESTILO
                - Tutear al estudiante
                - Visual y organizado
                - Fácil de escanear rápidamente
                - Máximo 500 palabras
            """.trimIndent()

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\n📖 RESUMEN BÁSICO de $nombreClase\n\nPara resumen detallado, verifica tu conexión.\n\nError: ${
                e.message?.take(100) ?: "Desconocido"
            }"
        }
    }

    /**
     * 🚀 INICIALIZA EL CHAT CON MEMORIA (Stateful)
     *
     * Carga el PDF una sola vez y establece el historial inicial.
     * Esto permite que la IA "recuerde" el documento en mensajes futuros.
     */
    suspend fun iniciarChatConContexto(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?,
        respuestaInicial: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "💬 [CHAT] Iniciando sesión de chat stateful...")
                val historial = mutableListOf<com.google.ai.client.generativeai.type.Content>()

                // 1. Crear mensaje inicial del usuario (Prompt + PDF si existe)
                val promptInicial = """
                    CONTEXTO DE LA CLASE:
                    📚 Clase: $nombreClase
                    📝 Descripción: $descripcion

                    Analiza el material adjunto y responde a la consulta inicial.
                """.trimIndent()

                val contenidoUsuario = if (!pdfUrl.isNullOrEmpty()) {
                    Log.d(TAG, "💬 [CHAT] Descargando PDF para inyectar en memoria (streaming)...")
                    // Descargar a archivo temporal para decidir estrategia
                    val pdfFile = descargarPDFATempFile(pdfUrl)
                    if (pdfFile.length() <= 20_000_000L) {
                        val pdfBytes = pdfFile.readBytes()
                        content("user") {
                            text(promptInicial)
                            blob("application/pdf", pdfBytes)
                        }
                    } else {
                        // Si es >20MB, comprimir localmente y enviar texto representativo
                        val chunks = extractTextChunksFromPdf(pdfFile)
                        val compressed = compressChunksForSingleCall(chunks)
                        content("user") {
                            text(promptInicial + "\n\nCONTENIDO_COMPRIMIDO:\n" + compressed)
                        }
                    }
                } else {
                    content("user") { text(promptInicial) }
                }
                historial.add(contenidoUsuario)

                // 2. Crear mensaje inicial del modelo (lo que ya respondió en la pantalla anterior)
                // Esto le da continuidad a la conversación
                historial.add(content("model") { text(respuestaInicial) })

                // 3. Iniciar chat con el historial cargado
                chatSession = googleAiModel.startChat(history = historial)
                Log.d(
                    TAG,
                    "✅ [CHAT] Sesión iniciada correctamente con historial (${historial.size} mensajes)"
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ [CHAT] Error al iniciar sesión: ${e.message}")
                chatSession = null
            }
        }
    }

    /**
     * ✨ Envía un mensaje al chat activo
     *
     * Usa la sesión persistente para mantener el contexto del PDF y mensajes anteriores.
     */
    suspend fun enviarMensajeChat(mensaje: String): String {
        return withContext(Dispatchers.IO) {
            if (chatSession != null) {
                try {
                    Log.d(TAG, "💬 [CHAT] Enviando mensaje a sesión existente...")
                    val response = chatSession!!.sendMessage(mensaje)
                    response.text ?: "Sin respuesta de la IA"
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [CHAT] Error en sesión: ${e.message}")
                    throw e
                }
            } else {
                // Fallback: Si no hay sesión (ej: falló la inicialización), intentar llamada simple
                // Nota: Aquí se pierde el contexto del PDF si no se reinicia, pero evita crash
                Log.w(TAG, "⚠️ [CHAT] No hay sesión activa, usando fallback stateless...")
                llamarGemini(mensaje)
            }
        }
    }
}
