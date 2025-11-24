package cl.duocuc.aulaviva.data.repository

import android.content.Context
import android.util.Log
import cl.duocuc.aulaviva.BuildConfig
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.local.ChatMessageEntity
import cl.duocuc.aulaviva.data.local.ChatSessionEntity
import cl.duocuc.aulaviva.data.remote.Content
import cl.duocuc.aulaviva.data.remote.GeminiApiService
import cl.duocuc.aulaviva.data.remote.GeminiRequest
import cl.duocuc.aulaviva.data.remote.GenerationConfig
import cl.duocuc.aulaviva.data.remote.Part
import cl.duocuc.aulaviva.domain.repository.IIARepository
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
import java.util.concurrent.TimeUnit
import kotlin.random.Random

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

    // Persistencia local de sesiones de chat
    private val db by lazy { AppDatabase.getDatabase(context) }
    private val chatDao by lazy { db.chatDao() }
    private var currentSessionId: Long? = null

    private val okHttpClient = OkHttpClient.Builder()
        // Aumentar read/write timeout para llamadas a Gemini que pueden tardar (modelos grandes)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
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
            val start = System.currentTimeMillis()
            // Timeout global para toda la operación (reintentos incluidos)
            try {
                withTimeout(180_000L) {
                    var intento = 0
                    var ultimoError: Exception? = null
                    val maxIntentos = 3
                    var resultado: String? = null

                    val baseDelay = 1000L // 1s

                    while (intento < maxIntentos && resultado == null) {
                        try {
                            val request = GeminiRequest(
                                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                                generationConfig = GenerationConfig(
                                    temperature = 0.6f, topP = 0.9f, maxOutputTokens = 4096
                                )
                            )
                            Log.d(TAG, "🔁 Gemini attempt ${intento + 1} — enviando petición")
                            val response = geminiService.generateContent(GEMINI_API_KEY, request)
                            if (response.isSuccessful) {
                                val text = response.body()
                                    ?.candidates
                                    ?.firstOrNull()
                                    ?.content
                                    ?.parts
                                    ?.firstOrNull()
                                    ?.text
                                if (!text.isNullOrBlank()) {
                                    val elapsed = System.currentTimeMillis() - start
                                    Log.d(
                                        TAG,
                                        "✅ Gemini responded in ${elapsed} ms (attempt ${intento + 1})"
                                    )
                                    resultado = text
                                    break
                                } else {
                                    ultimoError = Exception("Respuesta vacía de Gemini")
                                    // marcar como retryable y aumentar intento
                                    intento++
                                    val jitter = Random.nextLong(0, 500)
                                    val delayMs =
                                        1000L * (1L shl (intento - 1)).coerceAtMost(30L) + jitter
                                    Log.w(
                                        TAG,
                                        "⚠️ Gemini empty response on attempt ${intento}, delaying ${delayMs}ms and retrying"
                                    )
                                    kotlinx.coroutines.delay(delayMs)
                                    continue
                                }
                            } else {
                                val code = response.code()
                                val message = response.message()
                                ultimoError =
                                    Exception("HTTP error en Gemini: respuesta no exitosa (code=${code}, message=${message})")
                                // decidir retry para 429 o 5xx
                                val shouldRetryHttp = (code == 429) || (code >= 500)
                                intento++
                                if (!shouldRetryHttp || intento >= maxIntentos) {
                                    Log.w(
                                        TAG,
                                        "⚠️ Gemini HTTP failure code=${code}, will not retry further (attempt ${intento})"
                                    )
                                    break
                                } else {
                                    val jitter = Random.nextLong(0, 500)
                                    val delayMs =
                                        1000L * (1L shl (intento - 1)).coerceAtMost(30L) + jitter
                                    Log.w(
                                        TAG,
                                        "⚠️ Gemini HTTP ${code} on attempt ${intento}, sleeping ${delayMs}ms before retry"
                                    )
                                    kotlinx.coroutines.delay(delayMs)
                                    continue
                                }
                            }
                        } catch (e: Exception) {
                            ultimoError = e
                            // Sólo reintentar en errores de red/timeout (IO) o SocketTimeout
                            val shouldRetry = when (e) {
                                is java.net.SocketTimeoutException -> true
                                is java.io.IOException -> true
                                else -> false
                            }
                            intento++
                            Log.w(
                                TAG,
                                "⚠️ Gemini attempt ${intento} failed: ${e.message}. willRetry=$shouldRetry"
                            )
                            if (!shouldRetry) break
                            // backoff exponencial con jitter
                            val jitter = Random.nextLong(0, 500)
                            val delayMs =
                                baseDelay * (1L shl (intento - 1)).coerceAtMost(30L) + jitter
                            try {
                                kotlinx.coroutines.delay(delayMs)
                            } catch (_: Exception) {
                                // ignorar cancelaciones de delay
                            }
                        }
                    }

                    resultado
                        ?: run {
                            val errMsg = ultimoError?.message ?: "Desconocido"
                            throw Exception("Error Gemini: $errMsg")
                        }
                }
            } catch (e: Exception) {
                // Propagar el error para manejo superior
                throw e
            }
        }
    }

    override suspend fun analizarPdfConIA(nombreClase: String, pdfUrl: String?): String {
        if (pdfUrl.isNullOrEmpty()) return "⚠️ No se proporcionó URL del PDF"
        return try {
            // Limitar el trabajo total de análisis para evitar que la UI espere demasiado
            try {
                // devolvemos directamente el resultado del bloque con timeout
                withTimeout(180_000L) {
                    withContext(Dispatchers.IO) {
                        analizarPDFInteligente(
                            pdfUrl,
                            "Analiza el PDF y entrega un informe pedagógico para la clase: $nombreClase"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ análisis largo: ${e.message}")
                "⚠️ Tiempo de análisis excedido o error: ${e.message ?: "Desconocido"}"
            }
        } catch (e: Exception) {
            "⚠️ Error analizando PDF: ${e.message ?: "Desconocido"}"
        }
    }

    // Helpers básicos para extracción y compresión de texto (implementación simple y segura)
    private fun extractTextChunksFromPdf(pdfFile: File, chunkSize: Int = 6_000): List<String> {
        return try {
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdfFile)
            val stripper = try {
                // PDFBox Android puede exponer PDFTextStripper bajo este paquete
                com.tom_roush.pdfbox.text.PDFTextStripper()
            } catch (t: Throwable) {
                // Si no está disponible el stripper relocado, no referenciamos org.apache.* (no está en classpath Android)
                Log.w(TAG, "⚠️ PDFTextStripper no disponible en com.tom_roush: ${t.message}")
                null
            }
            val fullText = stripper?.getText(document) ?: ""
            document.close()
            if (fullText.isBlank()) return emptyList()
            val chunks = mutableListOf<String>()
            var idx = 0
            while (idx < fullText.length) {
                val end = (idx + chunkSize).coerceAtMost(fullText.length)
                chunks.add(fullText.substring(idx, end))
                idx = end
            }
            chunks
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [PDF] extractTextChunksFromPdf failed: ${e.message}")
            emptyList()
        }
    }

    private fun compressChunksForSingleCall(chunks: List<String>): String {
        if (chunks.isEmpty()) return ""
        // estrategia simple: tomar primeros N chars de cada chunk hasta límite global
        val globalLimit = 12_000
        val sb = StringBuilder()
        for (c in chunks) {
            if (sb.length >= globalLimit) break
            val toTake = (globalLimit - sb.length).coerceAtMost(c.length)
            sb.append(c.substring(0, toTake))
            if (sb.length < globalLimit) sb.append("\n---\n")
        }
        return sb.toString().take(globalLimit)
    }

    private suspend fun analizarPDFInteligente(pdfUrl: String, prompt: String): String =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔎 iniciar analizarPDFInteligente para url=$pdfUrl")
                analizarConGoogleAI(pdfUrl, prompt)
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Google AI falló: ${e.message}, usando PDFBox fallback")
                try {
                    withTimeout(30_000L) { analizarConPDFBox(pdfUrl, prompt) }
                } catch (e2: Exception) {
                    Log.e(TAG, "❌ Fallback PDFBox también falló: ${e2.message}")
                    throw Exception("No fue posible analizar el PDF: ${e2.message ?: e.message}")
                }
            }
        }

    private suspend fun analizarConGoogleAI(pdfUrl: String, prompt: String): String {
        val pdfFile = descargarPDFATempFile(pdfUrl)
        Log.d(TAG, "📄 PDF descargado size=${pdfFile.length()} bytes")
        // Para evitar múltiples llamadas por chunk que tardan mucho, si el archivo es grande usamos un único resumen comprimido
        if (pdfFile.length() > 6_000_000L) {
            Log.d(
                TAG,
                "⚠️ PDF grande, aplicando compresión de texto y llamada única en vez de múltiples chunks"
            )
            val chunks = extractTextChunksFromPdf(pdfFile)
            if (chunks.isEmpty()) throw Exception("No se pudo extraer texto del PDF")
            val compressed = compressChunksForSingleCall(chunks)
            val finalPrompt = """
                $prompt
                
                CONTENIDO_COMPRIMIDO:
                $compressed
                
                INSTRUCCIÓN: A partir del contenido comprimido, genera un análisis en español.
            """.trimIndent()
            return llamarGemini(finalPrompt)
        }
        if (pdfFile.length() <= 20_000_000L) {
            val pdfBytes = pdfFile.readBytes()
            val contenidoMultimodal = content { text(prompt); blob("application/pdf", pdfBytes) }
            // Limitar la llamada multimodal para que no quede esperando excesivamente
            val response =
                withTimeout(60_000) { googleAiModel.generateContent(contenidoMultimodal) }
            return response.text ?: throw Exception("Respuesta vacía de Google AI SDK")
        } else {
            val chunks = extractTextChunksFromPdf(pdfFile)
            if (chunks.isEmpty()) throw Exception("No se pudo extraer texto del PDF")
            mutableListOf<String>()
            // En vez de llamar por cada chunk (muy lento), hacemos un resumen comprimido y una única llamada
            val compressed = compressChunksForSingleCall(chunks)
            val combinedPrompt = """
                $prompt
                
                HE DIVIDIDO EN PARTES Y COMPRIMIDO EL CONTENIDO:
                $compressed
                
                INSTRUCCIÓN: A partir del contenido comprimido, genera un análisis coherente del PDF completo, mencionando conceptos y detalles específicos. Responde en español.
            """.trimIndent()
            try {
                return llamarGemini(combinedPrompt)
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error en llamarGemini para contenido comprimido: ${e.message}")
                throw e
            }
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
                Caracteres extraídos: ${textoPdf.length}

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
        Log.d(TAG, "⬇️ Descargando PDF desde: $url")
        try {
            return@withContext withTimeout(60_000L) {
                val request = okhttp3.Request.Builder().url(url).get().build()
                // Ejecutar la llamada de forma síncrona en el contexto IO (evita callbacks y problemas de parsing)
                val response = okHttpClient.newCall(request).execute()
                try {
                    if (!response.isSuccessful) throw java.io.IOException("HTTP error descargando PDF: respuesta no exitosa")
                    val body = response.body ?: throw java.io.IOException("Body vacío")
                    val tempFile = File.createTempFile("aulaviva_pdf_", ".pdf", context.cacheDir)
                    FileOutputStream(tempFile).use { out ->
                        body.byteStream().use { input -> input.copyTo(out) }
                    }
                    Log.d(
                        TAG,
                        "⬇️ PDF guardado en temp: ${tempFile.absolutePath} size=${tempFile.length()}"
                    )
                    tempFile
                } finally {
                    response.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error descargando PDF o timeout: ${e.message}")
            throw e
        }
    }

    // --- Helpers para contexto de PDF y metadata ---
    private suspend fun extractPdfMetadata(pdfUrl: String?): Pair<String, String> {
        if (pdfUrl.isNullOrEmpty()) return Pair("", "")
        return try {
            val pdfFile = descargarPDFATempFile(pdfUrl)
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(
                pdfFile,
                MemoryUsageSetting.setupTempFileOnly()
            )
            val info = document.documentInformation
            val title = info.title ?: ""
            val author = info.author ?: ""
            document.close()
            Pair(title, author)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [PDF] No metadata: ${e.message}")
            Pair("", "")
        }
    }

    private suspend fun ensureAnalysisAndMetadata(
        nombreClase: String,
        pdfUrl: String?
    ): Triple<String, String, String?> {
        // devuelve (title, author, analysisSummary)
        var title = ""
        var author = ""
        var analysis: String? = null

        // 1) intentar obtener sesión existente
        try {
            val existing =
                if (nombreClase.isNotBlank()) chatDao.getLatestSessionForClass(nombreClase) else null
            if (existing != null) {
                title = ""
                author = ""
                analysis = existing.analysisText
                // si no hay metadata pero existe pdfUrl en la sesión, extraer metadata
                if (existing.pdfUrl != null) {
                    val meta = extractPdfMetadata(existing.pdfUrl)
                    title = meta.first
                    author = meta.second
                }
                return Triple(title, author, analysis)
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [CHAT] Error consultando sesión: ${e.message}")
        }

        // 2) si no hay sesión pero hay pdfUrl, extraer metadata y (opcional) hacer un análisis rápido
        if (!pdfUrl.isNullOrEmpty()) {
            val meta = extractPdfMetadata(pdfUrl)
            title = meta.first
            author = meta.second
            try {
                // análisis rápido y breve (timeout corto)
                val shortPrompt =
                    "Resume brevemente los puntos clave del PDF para uso pedagógico (máx 3 líneas)."
                val resumen = try {
                    withTimeout(30_000L) { analizarPDFInteligente(pdfUrl, shortPrompt) }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ [ANALYSIS] No quick analysis: ${e.message}"); null
                }
                if (!resumen.isNullOrBlank()) {
                    analysis = resumen
                    // crear sesión temporal para almacenar análisis
                    try {
                        val sessionId = chatDao.insertSession(
                            ChatSessionEntity(
                                nombreClase = nombreClase,
                                descripcion = "",
                                pdfUrl = pdfUrl,
                                analysisText = analysis
                            )
                        )
                        currentSessionId = sessionId
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                sessionId = sessionId,
                                sender = "ai",
                                message = analysis
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ [CHAT] Error guardando análisis: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ [ANALYSIS] Error: ${e.message}")
            }
        }

        return Triple(title, author, analysis)
    }

    override suspend fun generarIdeasParaClase(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        return try {
            val (title, author, analysis) = ensureAnalysisAndMetadata(nombreClase, pdfUrl)
            val contextHeader = buildString {
                append("DETALLES DEL PDF ANALIZADO:\n")
                append("Título: ${if (title.isNotBlank()) title else "Desconocido"}\n")
                append("Autor: ${if (author.isNotBlank()) author else "Desconocido"}\n")
                append("Resumen: ${analysis?.take(500) ?: "No disponible"}\n\n")
            }
            val prompt = """
                $contextHeader
                Eres un consultor en innovación educativa.
                Clase: $nombreClase
                Descripción: $descripcion

                Genera 4 ideas prácticas para trabajo en clase, con objetivo pedagógico y pasos.
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            // persistir respuesta si hay sesión
            currentSessionId?.let { sid ->
                try {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sid,
                            sender = "ai",
                            message = resultado
                        )
                    )
                } catch (_: Exception) {
                }
            }
            "$contextHeader$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
        } catch (e: Exception) {
            "⚠️ Error: ${e.message ?: "Desconocido"}"
        }
    }

    // ----- Compatibility wrappers expected by callers (IAViewModel, Activities)
    override suspend fun sugerirActividades(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        return generarActividadesInteractivas(nombreClase, descripcion, pdfUrl)
    }

    override suspend fun estructurarClasePorTiempo(
        nombreClase: String,
        descripcion: String,
        duracion: String,
        pdfUrl: String?
    ): String {
        val promptExtra = "Duración: $duracion"
        return generarIdeasParaClase(nombreClase, "$descripcion\n$promptExtra", pdfUrl)
    }

    override suspend fun resumirContenidoPdf(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        return try {
            analizarPdfConIA(nombre, pdfUrl)
        } catch (e: Exception) {
            "⚠️ Resumen no disponible: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun generarGuiaPresentacion(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        return try {
            val (title, author, analysis) = ensureAnalysisAndMetadata(nombre, pdfUrl)
            val contextHeader = buildString {
                append("DETALLES DEL PDF ANALIZADO:\n")
                append("Título: ${if (title.isNotBlank()) title else "Desconocido"}\n")
                append("Autor: ${if (author.isNotBlank()) author else "Desconocido"}\n")
                append("Resumen: ${analysis?.take(500) ?: "No disponible"}\n\n")
            }
            val prompt = """
                $contextHeader
                Genera una guía de presentación clara y ordenada para la clase $nombre.
                Descripción: $descripcion
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            currentSessionId?.let { sid ->
                try {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sid,
                            sender = "ai",
                            message = resultado
                        )
                    )
                } catch (_: Exception) {
                }
            }
            "$contextHeader$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
        } catch (e: Exception) {
            "⚠️ Error: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun generarEjerciciosParaAlumno(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        return try {
            val (title, author, analysis) = ensureAnalysisAndMetadata(nombre, pdfUrl)
            val contextHeader = buildString {
                append("DETALLES DEL PDF ANALIZADO:\n")
                append("Título: ${if (title.isNotBlank()) title else "Desconocido"}\n")
                append("Autor: ${if (author.isNotBlank()) author else "Desconocido"}\n")
                append("Resumen: ${analysis?.take(500) ?: "No disponible"}\n\n")
            }
            val prompt = """
                $contextHeader
                Genera ejercicios para alumnos relacionados a $nombre.
                Descripción: $descripcion
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            currentSessionId?.let { sid ->
                try {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sid,
                            sender = "ai",
                            message = resultado
                        )
                    )
                } catch (_: Exception) {
                }
            }
            "$contextHeader$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
        } catch (e: Exception) {
            "⚠️ Error: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun crearResumenEstudioParaAlumno(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        return try {
            val (title, author, analysis) = ensureAnalysisAndMetadata(nombre, pdfUrl)
            val contextHeader = buildString {
                append("DETALLES DEL PDF ANALIZADO:\n")
                append("Título: ${if (title.isNotBlank()) title else "Desconocido"}\n")
                append("Autor: ${if (author.isNotBlank()) author else "Desconocido"}\n")
                append("Resumen: ${analysis?.take(500) ?: "No disponible"}\n\n")
            }
            val prompt = """
                $contextHeader
                Crea un resumen de estudio para alumnos basado en el material de la clase $nombre.
                Descripción: $descripcion
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            currentSessionId?.let { sid ->
                try {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sid,
                            sender = "ai",
                            message = resultado
                        )
                    )
                } catch (_: Exception) {
                }
            }
            "$contextHeader$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
        } catch (e: Exception) {
            "⚠️ Error: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun generarActividadesInteractivas(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        return try {
            val (title, author, analysis) = ensureAnalysisAndMetadata(nombreClase, pdfUrl)
            val contextHeader = buildString {
                append("DETALLES DEL PDF ANALIZADO:\n")
                append("Título: ${if (title.isNotBlank()) title else "Desconocido"}\n")
                append("Autor: ${if (author.isNotBlank()) author else "Desconocido"}\n")
                append("Resumen: ${analysis?.take(500) ?: "No disponible"}\n\n")
            }
            val prompt = """
                $contextHeader
                Diseña 3 actividades interactivas para la clase $nombreClase.
                Descripción: $descripcion
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            currentSessionId?.let { sid ->
                try {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sid,
                            sender = "ai",
                            message = resultado
                        )
                    )
                } catch (_: Exception) {
                }
            }
            "$contextHeader$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
        } catch (e: Exception) {
            "⚠️ Error: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun explicarConceptosParaAlumno(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        return try {
            val (title, author, analysis) = ensureAnalysisAndMetadata(nombreClase, pdfUrl)
            val contextHeader = buildString {
                append("DETALLES DEL PDF ANALIZADO:\n")
                append("Título: ${if (title.isNotBlank()) title else "Desconocido"}\n")
                append("Autor: ${if (author.isNotBlank()) author else "Desconocido"}\n")
                append("Resumen: ${analysis?.take(500) ?: "No disponible"}\n\n")
            }
            val prompt = """
                $contextHeader
                Eres un tutor. Explica en lenguaje simple los conceptos principales de $nombreClase.
                Descripción: $descripcion
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            currentSessionId?.let { sid ->
                try {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sid,
                            sender = "ai",
                            message = resultado
                        )
                    )
                } catch (_: Exception) {
                }
            }
            "$contextHeader$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
        } catch (e: Exception) {
            "⚠️ Error: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun iniciarChatConContexto(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?,
        respuestaInicial: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Intentar restaurar sesión existente para la misma clase
                val existing = chatDao.getLatestSessionForClass(nombreClase)
                if (existing != null) {
                    currentSessionId = existing.id
                    val historial = mutableListOf<com.google.ai.client.generativeai.type.Content>()
                    // Si ya existe un análisis guardado, lo añadimos como mensaje de sistema para dar contexto
                    existing.analysisText?.let { anal -> historial.add(content("system") { text("ANALYSIS:\n" + anal) }) }
                    val messages = chatDao.getMessagesForSession(existing.id)
                    for (m in messages) {
                        if (m.sender == "user") historial.add(content("user") { text(m.message) })
                        else historial.add(content("model") { text(m.message) })
                    }
                    chatSession = googleAiModel.startChat(history = historial)
                    Log.d(TAG, "✅ [CHAT] Sesión restaurada desde BD con ${messages.size} mensajes")
                } else {
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
                            content("user") {
                                text(promptInicial); blob(
                                "application/pdf",
                                pdfBytes
                            )
                            }
                        } else {
                            val chunks = extractTextChunksFromPdf(pdfFile)
                            val compressed = compressChunksForSingleCall(chunks)
                            content("user") { text(promptInicial + "\n\nCONTENIDO_COMPRIMIDO:\n" + compressed) }
                        }
                    } else content("user") { text(promptInicial) }

                    // Guardar nueva sesión en BD
                    val sessionId = chatDao.insertSession(
                        ChatSessionEntity(
                            nombreClase = nombreClase,
                            descripcion = descripcion,
                            pdfUrl = pdfUrl
                        )
                    )
                    currentSessionId = sessionId

                    // Si hay PDF, intentar analizar y guardar el análisis en la sesión
                    if (!pdfUrl.isNullOrEmpty()) {
                        try {
                            // Intentar análisis breve y con timeout corto para no bloquear el inicio del chat
                            val analysis = try {
                                withTimeout(25_000L) {
                                    analizarPDFInteligente(
                                        pdfUrl,
                                        "Analiza el PDF y entrega un informe pedagógico para la clase: $nombreClase"
                                    )
                                }
                            } catch (e: Exception) {
                                Log.w(
                                    TAG,
                                    "⚠️ Análisis inicial rápido falló o timeout: ${e.message}"
                                ); null
                            }
                            if (!analysis.isNullOrBlank()) {
                                // actualizar la sesión con el análisis (si obtenemos uno)
                                val sessionToUpdate = ChatSessionEntity(
                                    id = sessionId,
                                    nombreClase = nombreClase,
                                    descripcion = descripcion,
                                    pdfUrl = pdfUrl,
                                    analysisText = analysis,
                                    startedAt = System.currentTimeMillis(),
                                    lastActivityAt = System.currentTimeMillis()
                                )
                                chatDao.updateSession(sessionToUpdate)
                                chatDao.insertMessage(
                                    ChatMessageEntity(
                                        sessionId = sessionId,
                                        sender = "ai",
                                        message = analysis
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(
                                TAG,
                                "⚠️ Error analizando PDF al iniciar chat (no crítico): ${e.message}"
                            )
                        }
                    }

                    // Persistir mensaje inicial de contexto del usuario
                    historial.add(contenidoUsuario)
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            sender = "user",
                            message = promptInicial
                        )
                    )
                    // Añadir la respuesta inicial provista por la app como mensaje del modelo
                    historial.add(content("model") { text(respuestaInicial) })
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            sender = "ai",
                            message = respuestaInicial
                        )
                    )

                    chatSession = googleAiModel.startChat(history = historial)
                    Log.d(TAG, "✅ [CHAT] Nueva sesión iniciada y persistida con id=$sessionId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [CHAT] Error iniciando: ${e.message}"); chatSession = null
            }
        }
    }

    override suspend fun enviarMensajeChat(mensaje: String): String {
        return withContext(Dispatchers.IO) {
            if (chatSession != null) {
                try {
                    // Persistir mensaje del usuario en BD si existe sesión
                    currentSessionId?.let { sid ->
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                sessionId = sid,
                                sender = "user",
                                message = mensaje
                            )
                        )
                    }
                    val response = chatSession!!.sendMessage(mensaje)
                    val texto = response.text ?: "Sin respuesta de la IA"
                    // Persistir respuesta de la IA
                    currentSessionId?.let { sid ->
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                sessionId = sid,
                                sender = "ai",
                                message = texto
                            )
                        )
                        // actualizar lastActivityAt en la sesión
                        val sess = chatDao.getSessionById(sid)
                        if (sess != null) {
                            val updated = sess.copy(lastActivityAt = System.currentTimeMillis())
                            chatDao.updateSession(updated)
                        }
                    }
                    return@withContext texto
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [CHAT] Error: ${e.message}"); throw e
                }
            } else {
                Log.w(
                    TAG,
                    "⚠️ [CHAT] No hay sesión activa, fallback stateless"
                ); return@withContext llamarGemini(mensaje)
            }
        }
    }

    // --- Implementación de métodos de lectura/reanálisis definidos en IIARepository
    override suspend fun obtenerUltimaSesionParaClase(nombreClase: String): cl.duocuc.aulaviva.domain.model.ChatSession? {
        val s = chatDao.getLatestSessionForClass(nombreClase) ?: return null
        return cl.duocuc.aulaviva.domain.model.ChatSession(
            id = s.id,
            nombreClase = s.nombreClase,
            descripcion = s.descripcion,
            pdfUrl = s.pdfUrl,
            analysisText = s.analysisText,
            startedAt = s.startedAt,
            lastActivityAt = s.lastActivityAt
        )
    }

    override suspend fun obtenerMensajesDeSesion(sessionId: Long): List<cl.duocuc.aulaviva.domain.model.ChatMessage> {
        val msgs = chatDao.getMessagesForSession(sessionId)
        return msgs.map { m ->
            cl.duocuc.aulaviva.domain.model.ChatMessage(
                id = m.id,
                sessionId = m.sessionId,
                sender = m.sender,
                message = m.message,
                createdAt = m.createdAt
            )
        }
    }

    override suspend fun reanalizarPdfParaSesion(sessionId: Long, pdfUrl: String?): String {
        if (pdfUrl.isNullOrEmpty()) return "⚠️ No hay PDF para reanalizar"
        val analysis =
            analizarPDFInteligente(pdfUrl, "Reanálisis solicitado para la sesión $sessionId")
        // actualizar sesión con nuevo análisis
        val sess = chatDao.getSessionById(sessionId)
        if (sess != null) {
            val updated =
                sess.copy(analysisText = analysis, lastActivityAt = System.currentTimeMillis())
            chatDao.updateSession(updated)
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    sender = "ai",
                    message = analysis
                )
            )
        }
        return analysis
    }

    override suspend fun cerrarSesion(sessionId: Long) {
        withContext(Dispatchers.IO) {
            try {
                // borrar mensajes asociados y la sesión
                chatDao.clearMessagesForSession(sessionId)
                chatDao.deleteSessionById(sessionId)
                // si la sesión en memoria coincide, limpiarla
                if (currentSessionId == sessionId) {
                    chatSession = null
                    currentSessionId = null
                }
                Log.d(TAG, "✅ [CHAT] Sesión $sessionId cerrada y limpiada")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ [CHAT] Error cerrando sesión $sessionId: ${e.message}")
            }
        }
    }

}
