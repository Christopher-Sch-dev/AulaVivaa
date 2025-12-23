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
        private const val MAX_PDF_SIZE = 100_000_000L // 100 MB (actualizado según requerimiento)
        private const val PDF_CHUNK_SIZE = 8_000 // Aumentado para mejor contexto
    }

    // ✅ NUEVO: Caché de PDFs descargados y texto extraído
    private val pdfCache = mutableMapOf<String, File>()
    private val textCache = mutableMapOf<String, String>()

    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY

    // Modelos Gemini con fallback: primario gemini-3-flash-preview, fallback gemini-2.5-flash
    private val primaryModelName = "gemini-3-flash-preview"
    private val fallbackModelName = "gemini-2.5-flash"
    
    private val googleAiModelPrimary by lazy {
        GenerativeModel(
            modelName = primaryModelName,
            apiKey = GEMINI_API_KEY,
            generationConfig = com.google.ai.client.generativeai.type.generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 8192
            }
        )
    }
    
    private val googleAiModelFallback by lazy {
        GenerativeModel(
            modelName = fallbackModelName,
            apiKey = GEMINI_API_KEY,
            generationConfig = com.google.ai.client.generativeai.type.generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 8192
            }
        )
    }
    
    // Modelo activo (se usa el primario, fallback si falla)
    private var googleAiModel = googleAiModelPrimary

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
        Log.d(TAG, "💬 [GEMINI] Iniciando llamada a Gemini (prompt: ${prompt.length} caracteres)")
        return withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            // Timeout global para toda la operación (reintentos incluidos)
            try {
                withTimeout(180_000L) {
                    var intento = 0
                    var ultimoError: Exception? = null
                    val maxIntentos = 3
                    var resultado: String? = null
                    var usandoFallback = false

                    val baseDelay = 1000L // 1s

                    while (intento < maxIntentos && resultado == null) {
                        try {
                            val request = GeminiRequest(
                                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                                generationConfig = GenerationConfig(
                                    temperature = 0.6f, topP = 0.9f, maxOutputTokens = 4096
                                )
                            )
                            
                            // Usar modelo primario o fallback según estado
                            val modelName = if (usandoFallback) fallbackModelName else primaryModelName
                            Log.d(TAG, "🔁 [GEMINI] Intento ${intento + 1}/$maxIntentos — usando modelo: $modelName")
                            
                            val response = if (usandoFallback) {
                                geminiService.generateContentFallback(GEMINI_API_KEY, request)
                            } else {
                                geminiService.generateContentPrimary(GEMINI_API_KEY, request)
                            }
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
                                        "✅ [GEMINI] Respuesta recibida en ${elapsed}ms (intento ${intento + 1}): ${text.length} caracteres"
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
                                
                                // Si es 404 y no estamos usando fallback, activar fallback
                                if (code == 404 && !usandoFallback) {
                                    Log.w(TAG, "⚠️ Modelo primario ($primaryModelName) no disponible, activando fallback a $fallbackModelName")
                                    usandoFallback = true
                                    continue // Reintentar inmediatamente con fallback
                                }
                                
                                // decidir retry para 429 o 5xx
                                val shouldRetryHttp = (code == 429) || (code >= 500)
                                intento++
                                if (!shouldRetryHttp || intento >= maxIntentos) {
                                    // Si aún no probamos fallback, activarlo antes de rendirse
                                    if (!usandoFallback && code != 404) {
                                        Log.w(TAG, "⚠️ Modelo primario falló, intentando con fallback $fallbackModelName")
                                        usandoFallback = true
                                        intento = 0 // Reset intentos para fallback
                                        continue
                                    }
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

                    val finalResult = resultado
                        ?: run {
                            val errMsg = ultimoError?.message ?: "Desconocido"
                            Log.e(TAG, "❌ [GEMINI] Error después de todos los intentos: $errMsg")
                            throw Exception("Error Gemini: $errMsg")
                        }

                    Log.d(TAG, "✅ [GEMINI] Llamada completada exitosamente: ${finalResult.length} caracteres")
                    finalResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [GEMINI] Error en llamarGemini: ${e.message}", e)
                // Propagar el error para manejo superior
                throw e
            }
        }
    }

    override suspend fun analizarPdfConIA(nombreClase: String, pdfUrl: String?): String {
        Log.d(TAG, "🚀 [IA] Iniciando analizarPdfConIA - Clase: $nombreClase")
        if (pdfUrl.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ [IA] No se proporcionó URL del PDF")
            return "⚠️ No se proporcionó URL del PDF"
        }
        return try {
            Log.d(TAG, "⏱️ [IA] Timeout configurado: 180 segundos")
            withTimeout(180_000L) {
                withContext(Dispatchers.IO) {
                    Log.d(TAG, "📝 [IA] Construyendo prompt de análisis...")
                    Log.d(TAG, "📚 [IA] Preparando contexto del PDF para análisis...")
                    val contextoPdf = prepararContextoPdf(pdfUrl)
                    Log.d(TAG, "✅ [IA] Contexto preparado: ${contextoPdf.length} caracteres")

                    val prompt = """
                        $contextoPdf

                        ROL: Eres un analista de contenido educativo con experiencia en evaluación de materiales pedagógicos.

                        TAREA:
                        Analiza exhaustivamente el PDF proporcionado arriba para la clase "$nombreClase" y genera un informe pedagógico completo.

                        IMPORTANTE:
                        - Primero identifica y menciona DATOS REALES del PDF (título, autor si está disponible, temas principales específicos)
                        - Luego analiza el contenido de forma estructurada
                        - Usa información ESPECÍFICA del PDF, NO genérica

                        FORMATO REQUERIDO:

                        **📊 Informe Pedagógico Completo**

                        **📄 Información del Documento:**
                        • **Título/Tema identificado:** [Nombre real del documento o tema principal extraído del PDF]
                        • **Contenido principal:** [2-3 líneas con información específica del PDF, NO genérica]
                        • **Alcance:** [Qué cubre específicamente este documento según su contenido]

                        **1. VISIÓN GENERAL**
                        • Tema central del documento: [Información REAL del PDF]
                        • Alcance y profundidad del contenido: [Basado en el contenido real]
                        • Público objetivo identificado: [Según el contenido del PDF]

                        **2. ESTRUCTURA Y ORGANIZACIÓN**
                        • Cómo está organizado el documento: [Mencionar secciones REALES encontradas]
                        • Secciones principales y su secuencia lógica: [Listar secciones específicas del PDF]
                        • Calidad de la progresión de contenidos: [Evaluación basada en el contenido real]

                        **3. CONTENIDOS CLAVE**
                        [Para cada concepto importante:]
                        • **[Concepto]:** Explicación y ubicación en el PDF
                        • **[Concepto]:** Explicación y ubicación en el PDF
                        • **[Concepto]:** Explicación y ubicación en el PDF
                        [Mínimo 5 conceptos]

                        **4. FORTALEZAS PEDAGÓGICAS**
                        • [Aspecto positivo 1]
                        • [Aspecto positivo 2]
                        • [Aspecto positivo 3]

                        **5. OPORTUNIDADES DE MEJORA/COMPLEMENTO**
                        • [Qué podría añadir el docente]
                        • [Qué requiere explicación adicional]

                        **6. APLICACIÓN EN CLASE**
                        • **Para nivel básico:** [Recomendaciones]
                        • **Para nivel intermedio:** [Recomendaciones]
                        • **Para nivel avanzado:** [Recomendaciones]

                        **7. RECURSOS COMPLEMENTARIOS SUGERIDOS**
                        • [Tipo de material que complementaría este PDF]

                        **8. EVALUACIÓN DEL MATERIAL**
                        • Complejidad: [Básico/Intermedio/Avanzado]
                        • Extensión: [Apropiada/Excesiva/Insuficiente]
                        • Claridad: [Alta/Media/Baja]
                        • Aplicabilidad: [Inmediata/Requiere adaptación]

                        CRITERIOS:
                        - Análisis objetivo y fundamentado
                        - Referencias específicas al contenido del PDF
                        - Enfoque práctico para uso docente
                        - Identificación de valor pedagógico real
                    """.trimIndent()

                    Log.d(TAG, "📤 [IA] Enviando análisis a Gemini...")
                    val resultado = analizarPDFInteligente(pdfUrl, prompt)
                    Log.d(TAG, "✅ [IA] Análisis recibido: ${resultado.length} caracteres")
                    Log.d(TAG, "🎉 [IA] analizarPdfConIA completado exitosamente")
                    resultado
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ [IA] Error en analizarPdfConIA: ${e.message}", e)
            "⚠️ Error analizando PDF: ${e.message ?: "Desconocido"}"
        }
    }

    // ✅ OPTIMIZADO: Extracción rápida de texto con optimizaciones de rendimiento
    private suspend fun extractTextFromPdf(pdfUrl: String): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "📄 [PDF] Iniciando extracción de texto desde: $pdfUrl")

        // Verificar caché primero
        textCache[pdfUrl]?.let { cached ->
            Log.d(TAG, "✅ [PDF] Texto encontrado en caché: ${cached.length} caracteres")
            return@withContext cached
        }

        Log.d(TAG, "⬇️ [PDF] Descargando archivo PDF...")
        val pdfFile = descargarPDFATempFile(pdfUrl)
        val fileSizeMB = pdfFile.length() / 1024 / 1024
        Log.d(TAG, "✅ [PDF] Archivo descargado: ${pdfFile.length()} bytes (${fileSizeMB} MB)")

        Log.d(TAG, "🔍 [PDF] Abriendo documento con PDFBox (optimizado para velocidad)...")

        // ✅ OPTIMIZACIÓN: Para PDFs grandes (>10MB), usar setupTempFileOnly (más rápido)
        // Para PDFs pequeños, usar setupMixed (más eficiente en memoria)
        val memorySetting = if (fileSizeMB > 10) {
            Log.d(TAG, "⚡ [PDF] PDF grande, usando setupTempFileOnly para máxima velocidad")
            MemoryUsageSetting.setupTempFileOnly()
        } else {
            Log.d(TAG, "⚡ [PDF] PDF pequeño, usando setupMixed para eficiencia")
            MemoryUsageSetting.setupMixed(1024 * 1024 * 20) // 20MB en memoria
        }

        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdfFile, memorySetting)

        try {
            val pageCount = document.numberOfPages
            Log.d(TAG, "📄 [PDF] Documento tiene $pageCount páginas")

            Log.d(TAG, "📝 [PDF] Extrayendo texto con PDFTextStripper (optimizado)...")
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()

            // ✅ OPTIMIZACIONES DE RENDIMIENTO:
            stripper.sortByPosition = true // Más rápido que false
            stripper.setStartPage(1)
            stripper.setEndPage(pageCount)
            // Suprimir texto duplicado para mayor velocidad
            stripper.setSuppressDuplicateOverlappingText(true)

            val startTime = System.currentTimeMillis()
            val fullText = stripper.getText(document)
            val extractionTime = System.currentTimeMillis() - startTime

            Log.d(TAG, "⚡ [PDF] Extracción completada en ${extractionTime}ms (${fullText.length} caracteres)")

            // ✅ OPTIMIZACIÓN: Limpiar texto extraído (reducir tamaño sin perder contenido)
            val cleanedText = limpiarTextoExtraido(fullText)
            val reduction = if (fullText.isNotEmpty()) {
                ((fullText.length - cleanedText.length) * 100 / fullText.length)
            } else 0
            Log.d(TAG, "🧹 [PDF] Texto limpiado: ${cleanedText.length} chars (reducción: $reduction%)")

            // ✅ FIX: Guardar en caché
            textCache[pdfUrl] = cleanedText

            Log.d(TAG, "✅ [PDF] Texto extraído y optimizado exitosamente: ${cleanedText.length} caracteres")
            cleanedText
        } finally {
            try {
                document.close()
                Log.d(TAG, "🔒 [PDF] Documento cerrado")
            } catch (_: Exception) {
            }
        }
    }

    // ✅ NUEVO: Limpia el texto extraído eliminando redundancias sin perder contenido
    private fun limpiarTextoExtraido(texto: String): String {
        return texto
            // Eliminar múltiples espacios en blanco
            .replace(Regex(" +"), " ")
            // Eliminar múltiples saltos de línea (máximo 2 consecutivos)
            .replace(Regex("\n{3,}"), "\n\n")
            // Eliminar espacios al inicio/final de líneas
            .lines().joinToString("\n") { it.trim() }
            // Eliminar líneas vacías múltiples
            .replace(Regex("\n\n\n+"), "\n\n")
            .trim()
    }

    // ✅ OPTIMIZADO: Prepara contexto completo del PDF optimizado para Gemini
    private suspend fun prepararContextoPdf(pdfUrl: String?): String {
        if (pdfUrl.isNullOrEmpty()) {
            Log.d(TAG, "⚠️ [CONTEXTO] No hay URL de PDF, retornando contexto vacío")
            return ""
        }

        Log.d(TAG, "📚 [CONTEXTO] Preparando contexto del PDF: $pdfUrl")
        try {
            val textoPdf = extractTextFromPdf(pdfUrl)
            Log.d(TAG, "✅ [CONTEXTO] Texto extraído y optimizado: ${textoPdf.length} caracteres")

            // ✅ ESTRATEGIA: Enviar PDF completo pero optimizado
            // Gemini 2.5 Flash Lite tiene límite de ~1M tokens, pero para evitar 429 usamos estrategia inteligente
            // Si el texto es razonable (<100K chars), enviarlo completo
            // Si es muy grande, usar muestreo inteligente que cubra TODO el documento

            return if (textoPdf.length <= 100_000) {
                // PDF pequeño/mediano: enviar completo
                Log.d(TAG, "📦 [CONTEXTO] PDF completo (${textoPdf.length} chars), enviando todo el contenido")
                """
                📚 CONTENIDO COMPLETO DEL PDF (${textoPdf.length} caracteres):
                ---
                $textoPdf
                ---
                """.trimIndent()
            } else {
                // PDF muy grande: usar muestreo inteligente que cubra todo el documento
                // Estrategia: dividir en N secciones y tomar muestra de cada una
                Log.d(TAG, "📦 [CONTEXTO] PDF grande (${textoPdf.length} chars), usando muestreo inteligente completo")

                val numSecciones = 10 // Dividir en 10 secciones
                val charsPorSeccion = textoPdf.length / numSecciones
                val muestraPorSeccion = 8_000 // 8K chars por sección = 80K total

                val muestras = mutableListOf<String>()
                for (i in 0 until numSecciones) {
                    val inicioSeccion = i * charsPorSeccion
                    val finSeccion = ((i + 1) * charsPorSeccion).coerceAtMost(textoPdf.length)
                    val seccion = textoPdf.substring(inicioSeccion, finSeccion)

                    // Tomar muestra representativa de cada sección (inicio + medio + final de la sección)
                    val muestra = when {
                        seccion.length <= muestraPorSeccion -> seccion
                        else -> {
                            val inicio = seccion.take(muestraPorSeccion / 3)
                            val medio = seccion.substring(seccion.length / 2 - muestraPorSeccion / 6, seccion.length / 2 + muestraPorSeccion / 6)
                            val fin = seccion.takeLast(muestraPorSeccion / 3)
                            "$inicio\n[... contenido medio ...]\n$medio\n[... contenido final ...]\n$fin"
                        }
                    }
                    muestras.add("=== SECCIÓN ${i + 1}/$numSecciones ===\n$muestra")
                }

                val contextoCompleto = muestras.joinToString("\n\n")
                Log.d(TAG, "📊 [CONTEXTO] Muestreo completo preparado: ${contextoCompleto.length} chars (de ${textoPdf.length} originales)")

                """
                📚 CONTENIDO COMPLETO DEL PDF (${textoPdf.length} caracteres totales, muestreo representativo de todas las secciones):
                ---
                $contextoCompleto
                ---
                NOTA: Este es un muestreo inteligente que cubre todo el documento. El PDF original tiene ${textoPdf.length} caracteres distribuidos en $numSecciones secciones.
                """.trimIndent()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ [CONTEXTO] Error preparando contexto PDF: ${e.message}", e)
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
        Log.d(TAG, "⬇️ [PDF] Iniciando descarga desde: $url")

        // ✅ FIX: Verificar caché primero
        pdfCache[url]?.let { cachedFile ->
            if (cachedFile.exists()) {
                Log.d(TAG, "✅ [PDF] Usando PDF desde caché: ${cachedFile.length()} bytes (${cachedFile.length() / 1024 / 1024} MB)")
                return@withContext cachedFile
            } else {
                Log.d(TAG, "🗑️ [PDF] Archivo en caché no existe, limpiando entrada...")
                pdfCache.remove(url) // Limpiar entrada inválida
            }
        }

        Log.d(TAG, "🌐 [PDF] Realizando petición HTTP...")
        try {
            return@withContext withTimeout(60_000L) {
                val request = okhttp3.Request.Builder().url(url).get().build()
                val response = okHttpClient.newCall(request).execute()
                try {
                    Log.d(TAG, "📡 [PDF] Respuesta HTTP recibida: código ${response.code}")
                    if (!response.isSuccessful) {
                        throw java.io.IOException("HTTP error descargando PDF: código ${response.code}")
                    }
                    val body = response.body ?: throw java.io.IOException("Body vacío")

                    // ✅ FIX: Validar tamaño antes de descargar
                    val contentLength = body.contentLength()
                    Log.d(TAG, "📊 [PDF] Tamaño del archivo: ${contentLength} bytes (${contentLength / 1024 / 1024} MB)")
                    if (contentLength > MAX_PDF_SIZE) {
                        throw java.io.IOException("PDF demasiado grande: ${contentLength / 1024 / 1024} MB (máximo ${MAX_PDF_SIZE / 1024 / 1024} MB)")
                    }

                    Log.d(TAG, "💾 [PDF] Guardando archivo temporal...")
                    val tempFile = File.createTempFile("aulaviva_pdf_", ".pdf", context.cacheDir)
                    FileOutputStream(tempFile).use { out ->
                        body.byteStream().use { input -> input.copyTo(out) }
                    }

                    Log.d(TAG, "✅ [PDF] PDF descargado exitosamente: ${tempFile.length()} bytes (${tempFile.length() / 1024 / 1024} MB)")

                    // ✅ FIX: Guardar en caché
                    pdfCache[url] = tempFile
                    Log.d(TAG, "💾 [PDF] PDF guardado en caché")

                    tempFile
                } finally {
                    response.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ [PDF] Error descargando PDF: ${e.message}", e)
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
        Log.d(TAG, "🚀 [IA] Iniciando generarIdeasParaClase - Clase: $nombreClase")
        return try {
            Log.d(TAG, "📚 [IA] Preparando contexto del PDF...")
            val contextoPdf = prepararContextoPdf(pdfUrl)
            Log.d(TAG, "✅ [IA] Contexto preparado: ${contextoPdf.length} caracteres")

            Log.d(TAG, "💬 [IA] Construyendo prompt para Gemini...")
            val prompt = """
                $contextoPdf

                ROL: Eres un consultor pedagógico especializado en innovación educativa, con experiencia en el sistema educativo chileno.

                CONTEXTO:
                • Clase: $nombreClase
                • Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "• Material de referencia: PDF completo proporcionado arriba" else ""}

                TAREA:
                Analiza el PDF proporcionado arriba y genera 4 ideas pedagógicas innovadoras y aplicables para esta clase.

                IMPORTANTE:
                - Primero identifica y menciona DATOS REALES del PDF (título, temas principales específicos)
                - Luego genera ideas basadas en contenido ESPECÍFICO del PDF, NO genéricas
                - Cada idea debe aplicar directamente conceptos específicos encontrados en el documento

                FORMATO REQUERIDO:

                **📄 Información del Documento:**
                • **Título/Tema identificado:** [Nombre real del documento o tema principal extraído del PDF]
                • **Contenido principal:** [2-3 líneas con información específica del PDF, NO genérica]

                **💡 IDEAS PEDAGÓGICAS:**

                FORMATO para cada idea:
                **Idea [N]: [Título descriptivo]**
                • **Objetivo pedagógico:** [Qué aprenderán los estudiantes]
                • **Contenido del PDF aplicado:** [Concepto/sección específica del material]
                • **Implementación:** [3-4 pasos concretos para ejecutar en clase]
                • **Recursos necesarios:** [Materiales o herramientas]
                • **Tiempo estimado:** [Duración aproximada]

                CRITERIOS:
                - Ideas prácticas y realistas para el contexto escolar/universitario chileno
                - Enfoque en participación activa de estudiantes
                - Alineación directa con el contenido del material
                - Lenguaje profesional pero accesible para docentes
            """.trimIndent()

            Log.d(TAG, "📤 [IA] Enviando petición a Gemini (prompt: ${prompt.length} chars)...")
            val resultado = llamarGemini(prompt)
            Log.d(TAG, "✅ [IA] Respuesta recibida de Gemini: ${resultado.length} caracteres")
            Log.d(TAG, "🎉 [IA] generarIdeasParaClase completado exitosamente")
            resultado
        } catch (e: Exception) {
            Log.e(TAG, "❌ [IA] Error en generarIdeasParaClase: ${e.message}", e)
            "⚠️ Error generando ideas: ${e.message ?: "Desconocido"}"
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
        Log.d(TAG, "🚀 [IA] Iniciando estructurarClasePorTiempo - Clase: $nombreClase, Duración: $duracion")
        return try {
            Log.d(TAG, "📚 [IA] Preparando contexto del PDF...")
            val contextoPdf = prepararContextoPdf(pdfUrl)
            Log.d(TAG, "✅ [IA] Contexto preparado: ${contextoPdf.length} caracteres")

            val prompt = """
                $contextoPdf

                ROL: Eres un planificador pedagógico experto en diseño de sesiones de clase efectivas.

                CONTEXTO:
                • Clase: $nombreClase
                • Descripción: $descripcion
                • Duración total: $duracion
                ${if (contextoPdf.isNotBlank()) "• Material base: PDF completo proporcionado arriba" else ""}

                TAREA:
                Analiza el PDF proporcionado arriba y diseña una estructura temporal completa para esta clase, distribuyendo actividades de forma pedagógicamente efectiva.

                IMPORTANTE:
                - Primero identifica y menciona DATOS REALES del PDF (título, temas principales específicos)
                - Luego integra directamente el contenido ESPECÍFICO del PDF en cada segmento
                - Usa información REAL del documento, NO genérica

                FORMATO REQUERIDO:

                **📄 Información del Documento:**
                • **Título/Tema identificado:** [Nombre real del documento o tema principal extraído del PDF]
                • **Contenido principal:** [2-3 líneas con información específica del PDF, NO genérica]

                **📋 Plan de Clase - $nombreClase ($duracion)**

                **1. INICIO (X min)**
                • Actividad: [Descripción]
                • Objetivo: [Propósito específico]
                • Contenido PDF: [Sección/concepto aplicado]

                **2. DESARROLLO (X min)**
                • Actividad: [Descripción]
                • Objetivo: [Propósito específico]
                • Contenido PDF: [Sección/concepto aplicado]

                **3. PRÁCTICA (X min)**
                • Actividad: [Descripción]
                • Objetivo: [Propósito específico]
                • Contenido PDF: [Sección/concepto aplicado]

                **4. CIERRE (X min)**
                • Actividad: [Descripción]
                • Objetivo: [Propósito específico]
                • Evaluación: [Método de verificación]

                **📌 Recursos necesarios:**
                [Lista de materiales]

                **💡 Recomendaciones:**
                [2-3 sugerencias para optimizar la sesión]

                CRITERIOS:
                - Tiempos realistas y flexibles
                - Transiciones fluidas entre actividades
                - Balance entre teoría y práctica
                - Adaptable al contexto chileno
            """.trimIndent()

            Log.d(TAG, "📤 [IA] Enviando petición a Gemini...")
            val resultado = llamarGemini(prompt)
            Log.d(TAG, "✅ [IA] Respuesta recibida: ${resultado.length} caracteres")
            Log.d(TAG, "🎉 [IA] estructurarClasePorTiempo completado exitosamente")
            resultado
        } catch (e: Exception) {
            Log.e(TAG, "❌ [IA] Error en estructurarClasePorTiempo: ${e.message}", e)
            "⚠️ Error estructurando clase: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun resumirContenidoPdf(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        Log.d(TAG, "🚀 [IA] Iniciando resumirContenidoPdf - Clase: $nombre")
        if (pdfUrl.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ [IA] No se proporcionó URL del PDF")
            return "⚠️ No se proporcionó URL del PDF"
        }
        return try {
            Log.d(TAG, "📚 [IA] Preparando contexto del PDF...")
            val contextoPdf = prepararContextoPdf(pdfUrl)
            Log.d(TAG, "✅ [IA] Contexto preparado: ${contextoPdf.length} caracteres")

            val prompt = """
                $contextoPdf

                ROL: Eres un sintetizador de contenido académico especializado en extraer información clave.

                TAREA:
                Analiza el PDF proporcionado arriba y genera un resumen CONCISO Y EJECUTIVO para la clase "$nombre".

                IMPORTANTE:
                - Primero identifica y menciona DATOS REALES del PDF (título, autor si está disponible, temas principales específicos)
                - Luego sintetiza el contenido de forma estructurada
                - Este resumen será usado por docentes para toma rápida de decisiones

                FORMATO REQUERIDO (MÁXIMO 400 PALABRAS):

                **📚 Resumen Ejecutivo**

                **📄 Información del Documento:**
                • **Título/Tema identificado:** [Nombre real del documento o tema principal extraído del PDF]
                • **Contenido principal:** [2-3 líneas con información específica del PDF, NO genérica]
                • **Alcance:** [Qué cubre específicamente este documento según su contenido]

                **Tema principal:**
                [1-2 líneas con información REAL del PDF, mencionando conceptos específicos encontrados]

                **Conceptos clave:**
                • [Concepto 1 REAL del PDF + breve explicación basada en el contenido]
                • [Concepto 2 REAL del PDF + breve explicación basada en el contenido]
                • [Concepto 3 REAL del PDF + breve explicación basada en el contenido]
                • [Concepto 4 REAL del PDF + breve explicación basada en el contenido]

                **Estructura del documento:**
                [2-3 líneas describiendo cómo está organizado REALMENTE el PDF, mencionando secciones específicas si las hay]

                **Aplicabilidad pedagógica:**
                [2-3 líneas sobre cómo usar este material específico en clase, basado en el contenido real]

                **Nivel de complejidad:**
                [Básico/Intermedio/Avanzado + justificación en 1 línea basada en el contenido real del PDF]

                CRITERIOS CRÍTICOS:
                - BREVEDAD: Máximo 400 palabras total
                - PRECISIÓN: Información EXACTA y ESPECÍFICA del PDF (NO genérica)
                - DATOS REALES: Menciona información concreta encontrada en el documento
                - UTILIDAD: Enfocado en qué puede hacer el docente con este material específico
                - SIN REDUNDANCIA: Cada palabra debe aportar valor
                - ESTRUCTURA: Sigue EXACTAMENTE el formato proporcionado con todas las secciones
            """.trimIndent()

            Log.d(TAG, "📤 [IA] Enviando petición a Gemini...")
            val resultado = llamarGemini(prompt)
            Log.d(TAG, "✅ [IA] Resumen recibido: ${resultado.length} caracteres")
            Log.d(TAG, "🎉 [IA] resumirContenidoPdf completado exitosamente")
            resultado
        } catch (e: Exception) {
            Log.e(TAG, "❌ [IA] Error en resumirContenidoPdf: ${e.message}", e)
            "⚠️ Error generando resumen: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun generarGuiaPresentacion(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        Log.d(TAG, "🚀 [IA] Iniciando generarGuiaPresentacion - Clase: $nombre")
        return try {
            Log.d(TAG, "📚 [IA] Preparando contexto del PDF...")
            val contextoPdf = prepararContextoPdf(pdfUrl)
            Log.d(TAG, "✅ [IA] Contexto preparado: ${contextoPdf.length} caracteres")

            val prompt = """
                $contextoPdf

                ROL: Eres un especialista en comunicación pedagógica y diseño de presentaciones efectivas.

                CONTEXTO:
                • Clase: $nombre
                • Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "• Material base: PDF completo proporcionado arriba" else ""}

                TAREA:
                Analiza el PDF proporcionado arriba y diseña una guía profesional para presentar/dictar esta clase de forma estructurada y atractiva.

                IMPORTANTE:
                - Primero identifica y menciona DATOS REALES del PDF (título, temas principales específicos)
                - Luego utiliza el contenido ESPECÍFICO del PDF para construir cada sección
                - Usa información REAL del documento, NO genérica

                FORMATO REQUERIDO:

                **📄 Información del Documento:**
                • **Título/Tema identificado:** [Nombre real del documento o tema principal extraído del PDF]
                • **Contenido principal:** [2-3 líneas con información específica del PDF, NO genérica]

                **🎯 Guía de Presentación: $nombre**

                **1. APERTURA (3-5 minutos)**
                • **Gancho inicial:** [Pregunta/dato/situación que capte atención]
                • **Objetivo de la clase:** [Qué aprenderán hoy]
                • **Conexión con conocimientos previos:** [Qué ya saben los estudiantes]

                **2. DESARROLLO DE CONTENIDO**

                **Tema 1: [Nombre del tema]**
                • Concepto principal: [Del PDF]
                • Explicación clave: [2-3 puntos fundamentales]
                • Ejemplo práctico: [Situación real o caso]
                • Apoyo visual sugerido: [Diagrama/imagen/pizarra]

                **Tema 2: [Nombre del tema]**
                [Misma estructura]

                **Tema 3: [Nombre del tema]**
                [Misma estructura]

                **3. ACTIVIDAD PRÁCTICA**
                • Tipo: [Individual/Grupal/Colaborativa]
                • Instrucciones: [Paso a paso]
                • Tiempo: [Duración]
                • Objetivo: [Qué reforzar]

                **4. SÍNTESIS Y CIERRE**
                • Puntos clave para recapitular: [Lista breve]
                • Preguntas de reflexión: [2-3 preguntas abiertas]
                • Conexión con próxima clase: [Puente al siguiente tema]

                **📝 NOTAS PARA EL DOCENTE:**
                • Conceptos que pueden generar dudas: [Lista]
                • Tiempos flexibles por sección: [Estimaciones]
                • Material de apoyo recomendado: [Recursos]

                CRITERIOS:
                - Flujo narrativo lógico y progresivo
                - Balance entre teoría y práctica
                - Lenguaje accesible pero preciso
                - Anticipación de posibles dudas
            """.trimIndent()

            Log.d(TAG, "📤 [IA] Enviando petición a Gemini...")
            val resultado = llamarGemini(prompt)
            Log.d(TAG, "✅ [IA] Guía recibida: ${resultado.length} caracteres")
            Log.d(TAG, "🎉 [IA] generarGuiaPresentacion completado exitosamente")
            resultado
        } catch (e: Exception) {
            Log.e(TAG, "❌ [IA] Error en generarGuiaPresentacion: ${e.message}", e)
            "⚠️ Error generando guía: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun generarEjerciciosParaAlumno(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        Log.d(TAG, "🚀 [IA] Iniciando generarEjerciciosParaAlumno - Clase: $nombre")
        return try {
            Log.d(TAG, "📚 [IA] Preparando contexto del PDF...")
            val contextoPdf = prepararContextoPdf(pdfUrl)
            Log.d(TAG, "✅ [IA] Contexto preparado: ${contextoPdf.length} caracteres")

            val prompt = """
                $contextoPdf

                ROL: Eres un creador de ejercicios didácticos que ayudan a estudiantes a practicar y consolidar aprendizajes.

                CONTEXTO:
                • Tema: $nombre
                • Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "• Material de referencia: PDF completo proporcionado arriba" else ""}

                AUDIENCIA: Estudiantes que necesitan practicar lo aprendido de forma autónoma.

                TAREA:
                Analiza el PDF proporcionado arriba y crea ejercicios prácticos variados con diferentes niveles de dificultad.

                IMPORTANTE:
                - Primero identifica y menciona DATOS REALES del PDF (título, temas principales específicos)
                - Luego crea ejercicios basados DIRECTAMENTE en el contenido ESPECÍFICO del PDF
                - Todos los ejercicios deben usar información REAL del documento, NO genérica

                FORMATO REQUERIDO:

                **📄 Información del Documento:**
                • **Título/Tema identificado:** [Nombre real del documento o tema principal extraído del PDF]
                • **Contenido principal:** [2-3 líneas con información específica del PDF, NO genérica]

                **✍️ Ejercicios de Práctica - $nombre**

                **📝 NIVEL BÁSICO (Comprensión)**

                **Ejercicio 1: Completar**
                [Texto con espacios en blanco basado en el PDF]

                Palabras clave: [lista de palabras]

                **Ejercicio 2: Verdadero o Falso**
                1. [Afirmación basada en el PDF] (  )
                2. [Afirmación basada en el PDF] (  )
                3. [Afirmación basada en el PDF] (  )
                4. [Afirmación basada en el PDF] (  )
                5. [Afirmación basada en el PDF] (  )

                ---

                **🧩 NIVEL INTERMEDIO (Aplicación)**

                **Ejercicio 3: Relacionar Conceptos**
                Columna A → Columna B
                [Concepto] → [Opciones]
                [Concepto] → [Opciones]
                [Concepto] → [Opciones]

                **Ejercicio 4: Situación Práctica**
                [Plantear un problema/situación real donde se apliquen los conceptos del PDF]

                Preguntas:
                a) [Pregunta de análisis]
                b) [Pregunta de aplicación]
                c) [Pregunta de solución]

                ---

                **🚀 NIVEL AVANZADO (Análisis y Síntesis)**

                **Ejercicio 5: Caso de Estudio**
                [Descripción de un caso complejo que requiera integrar varios conceptos del PDF]

                Tareas:
                1. [Analizar aspecto X]
                2. [Proponer solución para Y]
                3. [Justificar decisión Z]

                **Ejercicio 6: Pregunta Abierta**
                [Pregunta reflexiva que requiera pensamiento crítico basado en el contenido]

                ---

                **✅ RESPUESTAS**
                [Al final, incluir las respuestas correctas de TODOS los ejercicios]

                **Ejercicio 1:** [respuestas]
                **Ejercicio 2:** [V/F con justificación]
                **Ejercicio 3:** [pares correctos]
                **Ejercicio 4:** [orientaciones de respuesta]
                **Ejercicio 5:** [rúbrica de evaluación]
                **Ejercicio 6:** [criterios de una buena respuesta]

                **💡 CONSEJOS PARA RESOLVER:**
                • [Tip 1 sobre cómo abordar los ejercicios]
                • [Tip 2 sobre qué repasar si tienes dudas]
                • [Tip 3 sobre cómo autoevaluarte]

                CRITERIOS:
                - Ejercicios progresivos en dificultad
                - Instrucciones claras y sin ambigüedad
                - Variedad de formatos para mantener interés
                - Aplicables de forma autónoma
                - Contenido 100% basado en el material del PDF
            """.trimIndent()

            Log.d(TAG, "📤 [IA] Enviando petición a Gemini...")
            val resultado = llamarGemini(prompt)
            Log.d(TAG, "✅ [IA] Ejercicios recibidos: ${resultado.length} caracteres")
            Log.d(TAG, "🎉 [IA] generarEjerciciosParaAlumno completado exitosamente")
            resultado
        } catch (e: Exception) {
            Log.e(TAG, "❌ [IA] Error en generarEjerciciosParaAlumno: ${e.message}", e)
            "⚠️ Error generando ejercicios: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun crearResumenEstudioParaAlumno(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        Log.d(TAG, "🚀 [IA] Iniciando crearResumenEstudioParaAlumno - Clase: $nombre")
        return try {
            Log.d(TAG, "📚 [IA] Preparando contexto del PDF...")
            val contextoPdf = prepararContextoPdf(pdfUrl)
            Log.d(TAG, "✅ [IA] Contexto preparado: ${contextoPdf.length} caracteres")

            val prompt = """
                $contextoPdf

                ROL: Eres un compañero de estudios experto que ayuda a crear resúmenes efectivos para preparar evaluaciones.

                CONTEXTO:
                • Tema: $nombre
                • Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "• Material a resumir: PDF completo proporcionado arriba" else ""}

                AUDIENCIA: Estudiantes que necesitan un resumen organizado para estudiar y repasar antes de evaluaciones.

                TAREA:
                Analiza el PDF proporcionado arriba y crea un resumen de estudio completo, organizado y fácil de repasar.

                IMPORTANTE:
                - Primero identifica y menciona DATOS REALES del PDF (título, temas principales específicos)
                - Luego extrae y sintetiza el contenido clave del PDF de forma estructurada
                - Usa información ESPECÍFICA del documento, NO genérica

                FORMATO REQUERIDO:

                **📄 Información del Documento:**
                • **Título/Tema identificado:** [Nombre real del documento o tema principal extraído del PDF]
                • **Contenido principal:** [2-3 líneas con información específica del PDF, NO genérica]

                **📖 Resumen de Estudio - $nombre**

                **🎯 Objetivo del Tema:**
                [En 2-3 líneas: ¿Qué voy a aprender con este material?]

                ---

                **📌 CONCEPTOS FUNDAMENTALES**

                **1. [Concepto Clave #1]**
                • **Definición:** [Breve y clara]
                • **Características:**
                  - [Punto 1]
                  - [Punto 2]
                  - [Punto 3]
                • **Ejemplo:** [Ejemplo concreto del PDF]
                • **⚡ Memoriza:** [Frase/regla mnemotécnica]

                **2. [Concepto Clave #2]**
                [Misma estructura]

                **3. [Concepto Clave #3]**
                [Misma estructura]

                [Incluir mínimo 5 conceptos fundamentales]

                ---

                **🔗 RELACIONES Y CONEXIONES**
                • [Concepto A] se relaciona con [Concepto B] porque...
                • [Concepto C] es consecuencia de...
                • La diferencia entre [X] y [Y] es...

                ---

                **📊 ESQUEMA VISUAL SUGERIDO**
                [Describir cómo organizar la información en un mapa mental o diagrama]
                ```
                [Representación simple en texto de cómo estructurar visualmente]
                ```

                ---

                **⚠️ ERRORES COMUNES A EVITAR**
                1. ❌ [Error típico] → ✅ [Forma correcta]
                2. ❌ [Error típico] → ✅ [Forma correcta]
                3. ❌ [Error típico] → ✅ [Forma correcta]

                ---

                **❓ PREGUNTAS DE AUTOEVALUACIÓN**
                Antes de la prueba, asegúrate de poder responder:
                1. [Pregunta esencial de comprensión]
                2. [Pregunta de aplicación práctica]
                3. [Pregunta de análisis/síntesis]
                4. [Pregunta integradora]

                ---

                **📝 TÉRMINOS CLAVE PARA MEMORIZAR**
                | Término | Definición Breve |
                |---------|------------------|
                | [Término 1] | [Definición] |
                | [Término 2] | [Definición] |
                | [Término 3] | [Definición] |
                [Mínimo 8-10 términos]

                ---

                **🎯 CHECKLIST DE ESTUDIO**
                Para considerar que dominas el tema, verifica:
                - [ ] Puedo explicar cada concepto con mis propias palabras
                - [ ] Entiendo cómo se relacionan los conceptos entre sí
                - [ ] Puedo dar ejemplos propios (no solo los del PDF)
                - [ ] Identifico aplicaciones prácticas del tema
                - [ ] Resuelvo ejercicios sin consultar apuntes

                ---

                **💡 TIPS DE ESTUDIO**
                • **Mejor momento:** [Sugerencia de cuándo estudiar esto]
                • **Técnica recomendada:** [Método específico para este contenido]
                • **Tiempo sugerido:** [Cuánto dedicar al estudio activo]
                • **Material de apoyo:** [Qué más revisar si necesitas profundizar]

                CRITERIOS:
                - Información condensada pero completa
                - Organización lógica y visual
                - Lenguaje accesible para estudiantes
                - Herramientas prácticas de memorización
                - Balance entre teoría y aplicación
                - 100% basado en el contenido del PDF
            """.trimIndent()

            Log.d(TAG, "📤 [IA] Enviando petición a Gemini...")
            val resultado = llamarGemini(prompt)
            Log.d(TAG, "✅ [IA] Resumen recibido: ${resultado.length} caracteres")
            Log.d(TAG, "🎉 [IA] crearResumenEstudioParaAlumno completado exitosamente")
            resultado
        } catch (e: Exception) {
            Log.e(TAG, "❌ [IA] Error en crearResumenEstudioParaAlumno: ${e.message}", e)
            "⚠️ Error creando resumen: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun generarActividadesInteractivas(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        Log.d(TAG, "🚀 [IA] Iniciando generarActividadesInteractivas - Clase: $nombreClase")
        return try {
            Log.d(TAG, "📚 [IA] Preparando contexto del PDF...")
            val contextoPdf = prepararContextoPdf(pdfUrl)
            Log.d(TAG, "✅ [IA] Contexto preparado: ${contextoPdf.length} caracteres")

            val prompt = """
                $contextoPdf

                ROL: Eres un diseñador de experiencias de aprendizaje activo, especializado en metodologías participativas.

                CONTEXTO:
                • Clase: $nombreClase
                • Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "• Material fuente: PDF completo proporcionado arriba" else ""}

                TAREA:
                Analiza el PDF proporcionado arriba y diseña 3 actividades interactivas innovadoras que promuevan participación activa y aprendizaje significativo.

                IMPORTANTE:
                - Primero identifica y menciona DATOS REALES del PDF (título, temas principales específicos)
                - Luego diseña actividades que apliquen directamente contenido ESPECÍFICO del PDF
                - Cada actividad debe usar información REAL del documento, NO genérica

                FORMATO REQUERIDO:

                **📄 Información del Documento:**
                • **Título/Tema identificado:** [Nombre real del documento o tema principal extraído del PDF]
                • **Contenido principal:** [2-3 líneas con información específica del PDF, NO genérica]

                **🎮 ACTIVIDADES INTERACTIVAS:**

                FORMATO para cada actividad:

                **🎮 Actividad [N]: [Nombre atractivo]**

                **Tipo:** [Individual/Parejas/Grupos/Plenaria]

                **Objetivos de aprendizaje:**
                • [Objetivo cognitivo]
                • [Objetivo procedimental]

                **Contenido del PDF aplicado:**
                [Concepto/sección específica que se trabaja]

                **Dinámica paso a paso:**
                1. [Instrucción clara para estudiantes]
                2. [Siguiente paso]
                3. [Siguiente paso]
                4. [Cierre/socialización]

                **Materiales necesarios:**
                • [Lista específica y realista]

                **Tiempo:** [X minutos]

                **Variante/Adaptación:**
                [Cómo modificar si el grupo es más grande/pequeño o tiene otro nivel]

                **Evaluación formativa:**
                [Cómo verificar que se logró el aprendizaje]

                ---

                **💡 NOTAS DE IMPLEMENTACIÓN:**
                • Recomendaciones para gestión del tiempo
                • Posibles desafíos y cómo abordarlos
                • Conexión entre las 3 actividades (progresión)

                CRITERIOS:
                - Actividades factibles con recursos típicos de aula chilena
                - Progresión de simple a complejo
                - Fomento de pensamiento crítico y colaboración
                - Instrucciones claras y ejecutables
            """.trimIndent()

            Log.d(TAG, "📤 [IA] Enviando petición a Gemini...")
            val resultado = llamarGemini(prompt)
            Log.d(TAG, "✅ [IA] Actividades recibidas: ${resultado.length} caracteres")
            Log.d(TAG, "🎉 [IA] generarActividadesInteractivas completado exitosamente")
            resultado
        } catch (e: Exception) {
            Log.e(TAG, "❌ [IA] Error en generarActividadesInteractivas: ${e.message}", e)
            "⚠️ Error generando actividades: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun explicarConceptosParaAlumno(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String {
        Log.d(TAG, "🚀 [IA] Iniciando explicarConceptosParaAlumno - Clase: $nombreClase")
        return try {
            Log.d(TAG, "📚 [IA] Preparando contexto del PDF...")
            val contextoPdf = prepararContextoPdf(pdfUrl)
            Log.d(TAG, "✅ [IA] Contexto preparado: ${contextoPdf.length} caracteres")

            val prompt = """
                $contextoPdf

                ROL: Eres un tutor paciente y didáctico que explica conceptos complejos de forma simple y amigable.

                CONTEXTO:
                • Tema: $nombreClase
                • Descripción: $descripcion
                ${if (contextoPdf.isNotBlank()) "• Material de estudio: PDF completo proporcionado arriba" else ""}

                AUDIENCIA: Estudiantes que están aprendiendo este tema por primera vez o reforzando conocimientos.

                TAREA:
                Analiza el PDF proporcionado arriba y explica los conceptos principales de este tema de forma clara, usando ejemplos cotidianos y lenguaje accesible.

                IMPORTANTE:
                - Primero identifica y menciona DATOS REALES del PDF (título, temas principales específicos)
                - Luego basa cada explicación en el contenido ESPECÍFICO del PDF
                - Usa información REAL del documento, NO genérica

                FORMATO REQUERIDO:

                **📄 Información del Documento:**
                • **Título/Tema identificado:** [Nombre real del documento o tema principal extraído del PDF]
                • **Contenido principal:** [2-3 líneas con información específica del PDF, NO genérica]

                **🎓 Explicación de Conceptos - $nombreClase**

                **¿De qué trata este tema?**
                [Introducción general en 2-3 líneas, como si hablaras con un amigo]

                ---

                **Conceptos Principales:**

                **1️⃣ [Nombre del Concepto]**

                **¿Qué es?**
                [Definición simple, sin tecnicismos innecesarios]

                **¿Cómo funciona?**
                [Explicación paso a paso o proceso]

                **Ejemplo del día a día:**
                [Situación cotidiana que ilustre el concepto]

                **💡 Para recordar:**
                [Tip o frase clave para memorizar]

                ---

                **2️⃣ [Nombre del Concepto]**
                [Misma estructura]

                ---

                **3️⃣ [Nombre del Concepto]**
                [Misma estructura]

                ---

                **🔗 ¿Cómo se relacionan estos conceptos?**
                [Explicar las conexiones entre los conceptos de forma simple]

                **❓ Preguntas para verificar que entendiste:**
                1. [Pregunta de comprensión]
                2. [Pregunta de aplicación]
                3. [Pregunta de análisis]

                **📚 ¿Quieres profundizar?**
                [Sugerencias de qué estudiar después o cómo practicar más]

                CRITERIOS:
                - Lenguaje simple y directo (como hablarías con un amigo)
                - Evitar jerga técnica o explicarla cuando sea necesaria
                - Ejemplos concretos y relacion ables
                - Tono motivador y positivo
                - Explicaciones que construyan comprensión progresiva
            """.trimIndent()

            Log.d(TAG, "📤 [IA] Enviando petición a Gemini...")
            val resultado = llamarGemini(prompt)
            Log.d(TAG, "✅ [IA] Explicación recibida: ${resultado.length} caracteres")
            Log.d(TAG, "🎉 [IA] explicarConceptosParaAlumno completado exitosamente")
            resultado
        } catch (e: Exception) {
            Log.e(TAG, "❌ [IA] Error en explicarConceptosParaAlumno: ${e.message}", e)
            "⚠️ Error explicando conceptos: ${e.message ?: "Desconocido"}"
        }
    }

    override suspend fun iniciarChatConContexto(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?,
        respuestaInicial: String
    ) {
        Log.d(TAG, "🆕 [CHAT] Iniciando chat con contexto - Clase: $nombreClase")
        withContext(Dispatchers.IO) {
            try {
                // ✅ FIX: NO restaurar sesiones antiguas, SIEMPRE crear sesión TEMPORAL nueva
                // El chat es momentáneo mientras el usuario está en la pantalla ResultadoIAActivity
                Log.d(TAG, "🆕 [CHAT] Creando sesión TEMPORAL nueva (no se restaura historial)")

                val historial = mutableListOf<com.google.ai.client.generativeai.type.Content>()

                // ✅ FIX: Incluir el texto completo del PDF en el contexto inicial
                Log.d(TAG, "📚 [CHAT] Preparando contexto del PDF para el chat...")
                val contextoPdf = prepararContextoPdf(pdfUrl)
                Log.d(TAG, "✅ [CHAT] Contexto PDF preparado: ${contextoPdf.length} caracteres")

                // ✅ El contexto ya viene optimizado de prepararContextoPdf, no necesita truncar
                val contextoLimitado = contextoPdf

                val promptInicial = """
                    CONTEXTO DE LA CLASE:
                    Clase: $nombreClase
                    Descripción: $descripcion

                    $contextoLimitado

                    ${if (contextoLimitado.isNotBlank()) "Por favor, mantén el contexto del PDF en todas tus respuestas." else ""}
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
                Log.d(TAG, "💬 [CHAT] Iniciando sesión de chat con Gemini (historial: ${historial.size} mensajes)...")
                chatSession = googleAiModel.startChat(history = historial)
                Log.d(TAG, "✅ [CHAT] Sesión TEMPORAL creada exitosamente:")
                Log.d(TAG, "   - Session ID: $sessionId")
                Log.d(TAG, "   - Contexto PDF: ${contextoLimitado.length} caracteres (de ${contextoPdf.length} originales)")
                Log.d(TAG, "   - Historial inicial: ${historial.size} mensajes")
                Log.d(TAG, "🎉 [CHAT] Chat listo para recibir mensajes")
            } catch (e: Exception) {
                Log.e(TAG, "❌ [CHAT] Error iniciando chat: ${e.message}", e)
                chatSession = null
            }
        }
    }

    override suspend fun enviarMensajeChat(mensaje: String): String {
        Log.d(TAG, "💬 [CHAT] Enviando mensaje al chat: ${mensaje.take(50)}...")
        return withContext(Dispatchers.IO) {
            if (chatSession != null) {
                try {
                    // Persistir mensaje del usuario en BD si existe sesión
                    currentSessionId?.let { sid ->
                        Log.d(TAG, "💾 [CHAT] Guardando mensaje del usuario en BD (sessionId: $sid)")
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                sessionId = sid,
                                sender = "user",
                                message = mensaje
                            )
                        )
                    }
                    Log.d(TAG, "📤 [CHAT] Enviando mensaje a Gemini...")
                    val response = chatSession!!.sendMessage(mensaje)
                    val texto = response.text ?: "Sin respuesta de la IA"
                    Log.d(TAG, "✅ [CHAT] Respuesta recibida de Gemini: ${texto.length} caracteres")

                    // Persistir respuesta de la IA
                    currentSessionId?.let { sid ->
                        Log.d(TAG, "💾 [CHAT] Guardando respuesta de la IA en BD...")
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
                    Log.d(TAG, "🎉 [CHAT] Mensaje procesado exitosamente")
                    return@withContext texto
                } catch (e: Exception) {
                    val errorMsg = "⚠️ Error de conexión con IA: ${e.message?.take(50)}..."
                    Log.e(TAG, "❌ [CHAT] Error enviando mensaje: ${e.message}", e)
                    
                    // Agregar mensaje de error localmente para que el usuario sepa que falló
                    currentSessionId?.let { sid ->
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                sessionId = sid,
                                sender = "ai",
                                message = errorMsg
                            )
                        )
                    }
                    return@withContext errorMsg
                }
            } else {
                Log.w(TAG, "⚠️ [CHAT] No hay sesión activa, usando fallback stateless")
                try {
                    return@withContext llamarGemini(mensaje)
                } catch (e: Exception) {
                    val errorMsg = "⚠️ Error (stateless): ${e.message?.take(50)}..."
                    Log.e(TAG, "❌ [CHAT] Error fallback: ${e.message}", e)
                    return@withContext errorMsg
                }
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
