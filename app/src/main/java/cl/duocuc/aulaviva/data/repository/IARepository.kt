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
        private const val MAX_PDF_SIZE = 50_000_000L // 50 MB
        private const val PDF_CHUNK_SIZE = 8_000 // Aumentado para mejor contexto
    }
    
    // ✅ NUEVO: Caché de PDFs descargados y texto extraído
    private val pdfCache = mutableMapOf<String, File>()
    private val textCache = mutableMapOf<String, String>()

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
            // ✅ FIX: Cambiar a BASIC para evitar logging masivo de PDFs
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
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

    // ✅ FIX: Helpers mejorados para extracción y caché de texto
    private suspend fun extractTextFromPdf(pdfUrl: String): String = withContext(Dispatchers.IO) {
        // Verificar caché primero
        textCache[pdfUrl]?.let { cached ->
            Log.d(TAG, "✅ Usando texto desde caché: ${cached.length} chars")
            return@withContext cached
        }
        
        val pdfFile = descargarPDFATempFile(pdfUrl)
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(
            pdfFile,
            MemoryUsageSetting.setupTempFileOnly()
        )
        
        try {
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            val fullText = stripper.getText(document)
            
            // ✅ FIX: Guardar en caché
            textCache[pdfUrl] = fullText
            
            Log.d(TAG, "✅ Texto extraído del PDF: ${fullText.length} caracteres")
            fullText
        } finally {
            try {
                document.close()
            } catch (_: Exception) {
            }
        }
    }
    
    // ✅ NUEVO: Helper para preparar contexto de PDF inteligentemente
    private suspend fun prepararContextoPdf(pdfUrl: String?): String {
        if (pdfUrl.isNullOrEmpty()) return ""
        
        try {
            val textoPdf = extractTextFromPdf(pdfUrl)
            
            // Estrategia inteligente según tamaño
            return if (textoPdf.length <= 20_000) {
                """
                📚 CONTENIDO COMPLETO DEL PDF (${textoPdf.length} caracteres):
                ---
                $textoPdf
                ---
                """.trimIndent()
            } else {
                // Tomar secciones clave: inicio + medio + final
                val inicio = textoPdf.take(10_000)
                val medio = textoPdf.substring(
                    textoPdf.length / 2, 
                    (textoPdf.length / 2) + 5_000
                ).coerceAtMost(textoPdf)
                val fin = textoPdf.takeLast(5_000)
                """
                📚 CONTENIDO DEL PDF (${textoPdf.length} caracteres totales, mostrando secciones clave):
                ---
                === INICIO DEL PDF ===
                $inicio
                
                === SECCIÓN MEDIA DEL PDF ===
                $medio
                
                === FINAL DEL PDF ===
                $fin
                ---
                """.trimIndent()
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error preparando contexto PDF: ${e.message}")
            return ""
        }
    }
    
    private fun extractTextChunksFromPdf(pdfFile: File, chunkSize: Int = PDF_CHUNK_SIZE): List<String> {
        return try {
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(
                pdfFile,
                MemoryUsageSetting.setupTempFileOnly()
            )
            val stripper = try {
                com.tom_roush.pdfbox.text.PDFTextStripper()
            } catch (t: Throwable) {
                Log.w(TAG, "⚠️ PDFTextStripper no disponible: ${t.message}")
                null
            }
            val fullText = stripper?.getText(document) ?: ""
            try {
            } finally {
                try {
                    document.close()
                } catch (_: Exception) {
                }
            }
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

    // ✅ FIX: NUNCA enviar PDF binario, siempre extraer texto primero
    private suspend fun analizarConGoogleAI(pdfUrl: String, prompt: String): String {
        Log.d(TAG, "📄 Iniciando análisis de PDF con Google AI")
        
        try {
            // ✅ FIX: SIEMPRE extraer texto, NUNCA enviar binario
            val fullText = extractTextFromPdf(pdfUrl)
            
            if (fullText.isBlank()) {
                throw Exception("No se pudo extraer texto del PDF")
            }
            
            Log.d(TAG, "✅ Texto extraído: ${fullText.length} caracteres")
            
            // ✅ FIX: Si el texto es razonable, enviarlo completo
            if (fullText.length <= 25_000) {
                val promptCompleto = """
                    $prompt
                    
                    📎 CONTENIDO COMPLETO DEL PDF:
                    ---
                    $fullText
                    ---
                    
                    IMPORTANTE: Analiza TODO el contenido del PDF proporcionado. Responde en español.
                """.trimIndent()
                return llamarGemini(promptCompleto)
            }
            
            // ✅ FIX: Si es muy largo, tomar secciones clave
            Log.d(TAG, "⚠️ Texto largo (${fullText.length} chars), usando estrategia inteligente")
            
            // Tomar: inicio (10K) + medio (5K) + final (5K) = 20K chars total
            val inicio = fullText.take(10_000)
            val medio = if (fullText.length > 15_000) {
                fullText.substring(fullText.length / 2, (fullText.length / 2) + 5_000)
            } else ""
            val fin = if (fullText.length > 20_000) {
                fullText.takeLast(5_000)
            } else ""
            
            val contenidoInteligente = buildString {
                append("=== INICIO DEL DOCUMENTO ===\n")
                append(inicio)
                if (medio.isNotBlank()) {
                    append("\n\n=== SECCIÓN MEDIA DEL DOCUMENTO ===\n")
                    append(medio)
                }
                if (fin.isNotBlank()) {
                    append("\n\n=== FINAL DEL DOCUMENTO ===\n")
                    append(fin)
                }
            }
            
            val promptCompleto = """
                $prompt
                
                📎 CONTENIDO CLAVE DEL PDF (${fullText.length} caracteres totales):
                ---
                $contenidoInteligente
                ---
                
                INSTRUCCIÓN: Este es un documento de ${fullText.length} caracteres. He incluido las secciones clave (inicio, medio y final) para que puedas hacer un análisis completo y coherente. Responde en español.
            """.trimIndent()
            
            return llamarGemini(promptCompleto)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en analizarConGoogleAI: ${e.message}")
            throw e
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
        // ✅ FIX: Verificar caché primero
        pdfCache[url]?.let { cachedFile ->
            if (cachedFile.exists()) {
                Log.d(TAG, "✅ Usando PDF desde caché: ${cachedFile.length()} bytes")
                return@withContext cachedFile
            } else {
                pdfCache.remove(url) // Limpiar entrada inválida
            }
        }
        
        Log.d(TAG, "⬇️ Descargando PDF desde: $url")
        try {
            return@withContext withTimeout(60_000L) {
                val request = okhttp3.Request.Builder().url(url).get().build()
                val response = okHttpClient.newCall(request).execute()
                try {
                    if (!response.isSuccessful) throw java.io.IOException("HTTP error descargando PDF: código ${response.code}")
                    val body = response.body ?: throw java.io.IOException("Body vacío")
                    
                    // ✅ FIX: Validar tamaño antes de descargar
                    val contentLength = body.contentLength()
                    if (contentLength > MAX_PDF_SIZE) {
                        throw java.io.IOException("PDF demasiado grande: ${contentLength / 1024 / 1024} MB (máximo ${MAX_PDF_SIZE / 1024 / 1024} MB)")
                    }
                    
                    val tempFile = File.createTempFile("aulaviva_pdf_", ".pdf", context.cacheDir)
                    FileOutputStream(tempFile).use { out ->
                        body.byteStream().use { input -> input.copyTo(out) }
                    }
                    
                    Log.d(TAG, "✅ PDF descargado: ${tempFile.length()} bytes")
                    
                    // ✅ FIX: Guardar en caché
                    pdfCache[url] = tempFile
                    
                    tempFile
                } finally {
                    response.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error descargando PDF: ${e.message}")
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

        // 2) si no hay sesión pero hay pdfUrl, extraer metadata y hacer análisis COMPLETO
        if (!pdfUrl.isNullOrEmpty()) {
            val meta = extractPdfMetadata(pdfUrl)
            title = meta.first
            author = meta.second
            try {
                // ✅ FIX: Análisis COMPLETO en vez de 3 líneas
                val fullPrompt = """
                    Analiza este PDF académico y genera un resumen pedagógico completo que incluya:
                    1. Tema principal y subtemas
                    2. Conceptos clave explicados
                    3. Estructura del documento
                    4. Puntos importantes para enseñanza
                    
                    El resumen debe ser detallado (mínimo 200 palabras) para mantener contexto útil.
                """.trimIndent()
                
                val resumen = try {
                    withTimeout(45_000L) { analizarPDFInteligente(pdfUrl, fullPrompt) }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ [ANALYSIS] Análisis timeout: ${e.message}"); null
                }
                
                if (!resumen.isNullOrBlank()) {
                    analysis = resumen
                    // crear sesión para almacenar análisis completo
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
                        Log.d(TAG, "✅ Análisis completo guardado: ${analysis.length} chars")
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
            // ✅ FIX: Preparar contexto completo del PDF
            val contextoPdf = prepararContextoPdf(pdfUrl)
            
            val prompt = """
                $contextoPdf
                
                Eres un consultor en innovación educativa.
                Clase: $nombreClase
                Descripción: $descripcion

                ${if (contextoPdf.isNotBlank()) "Basándote EN EL CONTENIDO DEL PDF mostrado arriba, g" else "G"}enera 4 ideas prácticas para trabajo en clase, con objetivo pedagógico y pasos. Responde en español.
            """.trimIndent()
            
            val resultado = llamarGemini(prompt)
            "$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en generarIdeasParaClase: ${e.message}")
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
            val contextoPdf = prepararContextoPdf(pdfUrl)
            val prompt = """
                $contextoPdf
                
                Genera una guía de presentación clara y ordenada para la clase $nombre.
                Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "\nBasándote en el contenido del PDF mostrado arriba." else ""}
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            "$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
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
            val contextoPdf = prepararContextoPdf(pdfUrl)
            val prompt = """
                $contextoPdf
                
                Genera ejercicios para alumnos relacionados a $nombre.
                Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "\nBasa los ejercicios en el contenido del PDF mostrado arriba." else ""}
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            "$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
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
            val contextoPdf = prepararContextoPdf(pdfUrl)
            val prompt = """
                $contextoPdf
                
                Crea un resumen de estudio para alumnos basado en el material de la clase $nombre.
                Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "\nEl resumen debe estar basado en el contenido del PDF mostrado arriba." else ""}
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            "$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
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
            val contextoPdf = prepararContextoPdf(pdfUrl)
            val prompt = """
                $contextoPdf
                
                Diseña 3 actividades interactivas para la clase $nombreClase.
                Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "\nLas actividades deben estar basadas en el contenido del PDF mostrado arriba." else ""}
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            "$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
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
            val contextoPdf = prepararContextoPdf(pdfUrl)
            val prompt = """
                $contextoPdf
                
                Eres un tutor. Explica en lenguaje simple los conceptos principales de $nombreClase.
                Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "\nBasa tu explicación en el contenido del PDF mostrado arriba." else ""}
            """.trimIndent()
            val resultado = llamarGemini(prompt)
            "$resultado\n\nEste fue gemini real bro 🚬😶‍🌫️"
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
                // ✅ FIX: NO restaurar sesiones antiguas, SIEMPRE crear sesión TEMPORAL nueva
                // El chat es momentáneo mientras el usuario está en la pantalla ResultadoIAActivity
                Log.d(TAG, "🆕 [CHAT] Creando sesión TEMPORAL nueva (no se restaura historial)")
                
                val historial = mutableListOf<com.google.ai.client.generativeai.type.Content>()
                
                // ✅ FIX: Incluir el texto completo del PDF en el contexto inicial
                val contextoPdf = prepararContextoPdf(pdfUrl)
                
                val promptInicial = """
                    CONTEXTO DE LA CLASE:
                    Clase: $nombreClase
                    Descripción: $descripcion
                    
                    $contextoPdf
                    
                    ${if (contextoPdf.isNotBlank()) "Por favor, mantén el contexto del PDF en todas tus respuestas." else ""}
                """.trimIndent()
                
                // Mensaje inicial del usuario con contexto
                val contenidoUsuario = content("user") { text(promptInicial) }
                historial.add(contenidoUsuario)
                
                // Respuesta inicial de la IA (la que ya se generó antes)
                historial.add(content("model") { text(respuestaInicial) })
                
                // Guardar sesión TEMPORAL en BD
                val sessionId = chatDao.insertSession(
                    ChatSessionEntity(
                        nombreClase = nombreClase,
                        descripcion = descripcion,
                        pdfUrl = pdfUrl,
                        analysisText = contextoPdf // Guardar el contexto del PDF
                    )
                )
                currentSessionId = sessionId
                
                // Persistir mensajes iniciales
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        sender = "user",
                        message = promptInicial
                    )
                )
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        sender = "ai",
                        message = respuestaInicial
                    )
                )
                
                // Crear sesión de chat con historial inicial
                chatSession = googleAiModel.startChat(history = historial)
                Log.d(TAG, "✅ [CHAT] Sesión TEMPORAL creada con id=$sessionId, contexto PDF incluido")
            } catch (e: Exception) {
                Log.e(TAG, "❌ [CHAT] Error iniciando: ${e.message}")
                chatSession = null
            }
        }
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
