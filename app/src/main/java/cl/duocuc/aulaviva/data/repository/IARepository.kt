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

    // Persistencia local de sesiones de chat
    private val db by lazy { AppDatabase.getDatabase(context) }
    private val chatDao by lazy { db.chatDao() }
    private var currentSessionId: Long? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
                            val text =
                                response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (!text.isNullOrBlank()) return@withTimeout text
                            throw Exception("Respuesta vacía de Gemini")
                        } else {
                            throw Exception("HTTP ${'$'}{response.code()}: ${'$'}{response.message()}")
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
                                Thread.sleep(800L * intento)
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

    override suspend fun analizarPdfConIA(nombreClase: String, pdfUrl: String?): String {
        return try {
            if (pdfUrl.isNullOrEmpty()) return "⚠️ No se proporcionó URL del PDF"
            analizarPDFInteligente(
                pdfUrl,
                "Analiza el PDF y entrega un informe pedagógico para la clase: $nombreClase"
            )
        } catch (e: Exception) {
            "⚠️ Error analizando PDF: ${e.message ?: "Desconocido"}"
        }
    }

    private suspend fun analizarPDFInteligente(pdfUrl: String, prompt: String): String =
        withContext(Dispatchers.IO) {
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
            val response =
                withTimeout(90_000) { googleAiModel.generateContent(contenidoMultimodal) }
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
                try {
                    summaries.add(llamarGemini(chunkPrompt))
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "⚠️ Error resumiendo chunk ${index + 1}: ${e.message}"
                    ); summaries.add("[Error resumiendo chunk ${index + 1}: ${e.message}]")
                }
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
        val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS).build()
        val request = okhttp3.Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("HTTP ${'$'}{response.code}")
        val body = response.body ?: throw java.io.IOException("Body vacío")
        val tempFile = File.createTempFile("aulaviva_pdf_", ".pdf", context.cacheDir)
        FileOutputStream(tempFile).use { out ->
            body.byteStream().use { input -> input.copyTo(out) }
        }
        tempFile
    }

    private fun extractTextChunksFromPdf(file: File, pagesPerChunk: Int = 5): List<String> {
        val out = mutableListOf<String>()
        try {
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(
                file,
                MemoryUsageSetting.setupTempFileOnly()
            )
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
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [PDFBox] Error extrayendo: ${e.message}")
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
        var i = 0
        while (b.length < maxChars && i < chunks.size) {
            b.append("-- MUESTRA ${i + 1} (tail) --\n")
            b.append(chunks[i].takeLast(500))
            b.append("\n\n")
            i++
        }
        return if (b.length > maxChars) b.toString().take(maxChars) else b.toString()
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
                            val analysis = analizarPDFInteligente(
                                pdfUrl,
                                "Analiza el PDF y entrega un informe pedagógico para la clase: $nombreClase"
                            )
                            chatDao.getLatestSessionForClass(nombreClase)?.copy().apply {
                                // safe update: getLatestSessionForClass should return the same inserted session
                            }
                            // actualizar la sesión con el análisis
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
                            // Persistir el análisis como mensaje AI para contexto histórico
                            chatDao.insertMessage(
                                ChatMessageEntity(
                                    sessionId = sessionId,
                                    sender = "ai",
                                    message = analysis
                                )
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Error analizando PDF al iniciar chat: ${e.message}")
                        }
                    }

                    // Persistir el mensaje inicial de contexto del usuario
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
