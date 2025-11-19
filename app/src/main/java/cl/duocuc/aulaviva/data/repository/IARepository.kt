package cl.duocuc.aulaviva.data.repository

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * 🤖 GEMINI AI con Firebase AI Logic (Gemini Developer API)
 * Modelo: gemini-2.5-flash-latest (vigente noviembre 2025)
 *
 * ✅ Firebase AI Logic con Gemini Developer API (15 req/min gratis)
 * ✅ PDFBox como fallback (offline, límite API)
 * ✅ Retrofit para funciones sin PDF
 */
class IARepository(private val context: Context) {

    companion object {
        private const val TAG = "AulaViva_IA"
    }

    // ✅ API Key cargada desde local.properties vía BuildConfig
    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY

    // ✅ Google AI SDK (Gemini Developer API - compatible con Supabase Ktor 2.3.12)
    // Modelo con soporte multimodal (PDF, imágenes, etc.)
    private val googleAiModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash-latest",
            apiKey = GEMINI_API_KEY
        )
    }

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
            PDFBoxResourceLoader.init(context)
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

        // 1. Descargar PDF
        val pdfBytes = descargarPDF(pdfUrl)
        Log.d(
            TAG,
            "📦 [Google AI] PDF descargado: ${pdfBytes.size} bytes (${pdfBytes.size / 1024} KB)"
        )

        // 2. Verificar límite 20MB (Gemini Developer API)
        if (pdfBytes.size > 20_000_000) {
            throw java.io.IOException("PDF muy grande (>20MB)")
        }

        // 3. Contenido multimodal (Google AI SDK DSL) - usa blob() para bytes
        val contenidoMultimodal = content {
            text(prompt)
            blob("application/pdf", pdfBytes)
        }

        Log.d(TAG, "🤖 [Google AI] Enviando a Gemini 1.5 Flash (Developer API)...")

        // 5. Generar respuesta con timeout
        val response = withTimeout(90_000) { // 90 segundos
            googleAiModel.generateContent(contenidoMultimodal)
        }

        val textoRespuesta = response.text ?: throw Exception("Respuesta vacía de Google AI SDK")

        Log.d(TAG, "✅ [Google AI] Respuesta recibida: ${textoRespuesta.length} caracteres")
        Log.d(TAG, "✅ [Google AI] Primeros 150 chars: ${textoRespuesta.take(150)}...")

        return textoRespuesta
    }

    /**
     * 📄 PDFBox: Extrae texto local y envía a Gemini (FALLBACK)
     *
     * Este método se usa cuando Firebase falla (red, timeout, límite API).
     * Extrae texto localmente y lo envía a Gemini vía Retrofit.
     *
     * Limitaciones:
     * - No lee PDFs escaneados (requiere texto extraíble)
     * - No parsea tablas complejas ni gráficos
     * - Funciona OFFLINE (ventaja para fallback)
     *
     * @param pdfUrl URL del PDF
     * @param prompt Instrucción para la IA
     * @return Respuesta de Gemini basada en texto extraído
     */
    private suspend fun analizarConPDFBox(pdfUrl: String, prompt: String): String {
        Log.d(TAG, "📄 [PDFBox] Iniciando extracción de texto local...")

        // 1. Descargar PDF
        val pdfBytes = descargarPDF(pdfUrl)
        Log.d(TAG, "📄 [PDFBox] PDF descargado: ${pdfBytes.size} bytes")

        // 2. Extraer texto con PDFBox Android
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdfBytes)
        val totalPaginas = document.numberOfPages
        Log.d(TAG, "📄 [PDFBox] Total páginas: $totalPaginas")

        val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
        stripper.startPage = 1
        stripper.endPage = totalPaginas

        val textoPdf = stripper.getText(document)
        document.close()

        Log.d(TAG, "📄 [PDFBox] Texto extraído: ${textoPdf.length} caracteres")
        Log.d(TAG, "📄 [PDFBox] Primeros 200 chars: ${textoPdf.take(200)}")

        // 3. Construir prompt completo con metadata
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

        // 4. Enviar a Gemini usando el método Retrofit existente
        val respuesta = llamarGemini(promptCompleto)

        Log.d(TAG, "✅ [PDFBox] Respuesta recibida: ${respuesta.length} caracteres")
        return respuesta
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
     * 📄 Descarga y extrae texto de un PDF desde URL (DEPRECADO - Usar analizarPDFInteligente)
     *
     * IMPORTANTE: Esta función descarga el PDF completo y extrae su contenido textual.
     * Extrae TODAS las páginas disponibles para que la IA tenga el contexto completo.
     *
     * @param pdfUrl URL del PDF a descargar
     * @return Texto extraído del PDF completo
     */
    private suspend fun extraerTextoDePdf(pdfUrl: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📄 [PDF] Descargando desde: $pdfUrl")

                // Descargar PDF usando OkHttp
                val request = okhttp3.Request.Builder()
                    .url(pdfUrl)
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val error = "[❌ No se pudo descargar el PDF. Código HTTP: ${response.code}]"
                    Log.e(TAG, "📄 [PDF] $error")
                    return@withContext error
                }

                val pdfBytes = response.body?.bytes()
                if (pdfBytes == null || pdfBytes.isEmpty()) {
                    val error = "[❌ PDF descargado está vacío]"
                    Log.e(TAG, "📄 [PDF] $error")
                    return@withContext error
                }

                Log.d(TAG, "📄 [PDF] Descargado exitosamente: ${pdfBytes.size} bytes")

                // Extraer texto usando PDFBox Android
                val pdfDocument = com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdfBytes)
                val totalPaginas = pdfDocument.numberOfPages
                Log.d(TAG, "📄 [PDF] Total páginas: $totalPaginas")

                val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()

                // ✅ EXTRAER TODAS LAS PÁGINAS (sin límites)
                stripper.startPage = 1
                stripper.endPage = totalPaginas

                val textoCompleto = stripper.getText(pdfDocument)
                pdfDocument.close()

                Log.d(TAG, "📄 [PDF] Texto extraído: ${textoCompleto.length} caracteres")
                Log.d(
                    TAG,
                    "📄 [PDF] Primeros 200 chars: ${textoCompleto.take(200)}"
                )                // ✅ RETORNAR TODO EL TEXTO con metadata
                val textoConMetadata = """
                    📊 METADATA DEL PDF:
                    - Total de páginas: $totalPaginas
                    - Caracteres extraídos: ${textoCompleto.length}

                    📄 CONTENIDO COMPLETO:
                    $textoCompleto
                """.trimIndent()

                return@withContext textoConMetadata.trim()

            } catch (e: Exception) {
                val error = "[❌ Error al extraer texto del PDF: ${e.message}]"
                Log.e(TAG, "📄 [PDF] $error", e)
                return@withContext error
            }
        }
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
     * ✨ NUEVO: Procesa un prompt con contexto completo para el sistema de chat
     *
     * Este método se usa cuando el usuario envía mensajes adicionales
     * en ResultadoIAActivity para refinar o modificar resultados anteriores.
     *
     * @param promptCompleto El prompt con todo el contexto de la conversación
     * @param pdfUrl URL del PDF si hay uno disponible (para extraer y leer su contenido REAL)
     */
    suspend fun procesarPromptConContexto(promptCompleto: String, pdfUrl: String? = null): String {
        return try {
            // ✅ USAR MÉTODO HÍBRIDO si hay PDF
            val resultado = if (!pdfUrl.isNullOrEmpty()) {
                Log.d(TAG, "💬 [CHAT] PDF detectado, usando método híbrido Firebase AI/PDFBox...")
                analizarPDFInteligente(pdfUrl, promptCompleto)
            } else {
                Log.d(TAG, "💬 [CHAT] Sin PDF, llamada Gemini estándar")
                llamarGemini(promptCompleto)
            }

            resultado
        } catch (e: Exception) {
            Log.e(TAG, "💬 [CHAT] Error: ${e.message}", e)
            "⚠️ Error al conectar con Gemini AI\n\nNo pude procesar tu mensaje. Verifica tu conexión.\n\nError: ${
                e.message?.take(100) ?: "Desconocido"
            }"
        }
    }
}
