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
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * 🤖 GEMINI AI con Retrofit - PROFESIONAL Y RÁPIDO
 * Modelo: gemini-2.5-flash (vigente octubre 2025)
 *
 * ✅ API Key cargada desde BuildConfig (portable entre PCs)
 */
class IARepository {

    // ✅ API Key cargada desde local.properties vía BuildConfig
    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY

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
                                temperature = 0.6f, topK = 40, topP = 0.9f, maxOutputTokens = 4096
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
     * 📄 Descarga y extrae texto de un PDF desde URL
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
                // Descargar PDF usando OkHttp
                val request = okhttp3.Request.Builder()
                    .url(pdfUrl)
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext "[No se pudo descargar el PDF. Código: ${response.code}]"
                }

                val pdfBytes = response.body?.bytes()
                if (pdfBytes == null || pdfBytes.isEmpty()) {
                    return@withContext "[PDF descargado está vacío]"
                }

                // Extraer texto usando PDFBox Android
                val pdfDocument = com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdfBytes)
                val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()

                // ✅ EXTRAER TODAS LAS PÁGINAS (sin límites)
                stripper.startPage = 1
                stripper.endPage = pdfDocument.numberOfPages

                val textoCompleto = stripper.getText(pdfDocument)
                pdfDocument.close()

                // ✅ RETORNAR TODO EL TEXTO (sin límites de caracteres)
                // Gemini 2.0 Flash puede manejar contextos largos
                return@withContext textoCompleto.trim()

            } catch (e: Exception) {
                return@withContext "[Error al extraer texto del PDF: ${e.message?.take(100)}]"
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
            // ✅ LEER CONTENIDO DEL PDF si existe
            val contextoPdf = if (!pdfUrl.isNullOrEmpty()) {
                val textoPdf = extraerTextoDePdf(pdfUrl)
                "\n\n📎 CONTENIDO DEL PDF ADJUNTO:\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n$textoPdf\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            } else {
                ""
            }

            val prompt = """
                # CONTEXTO Y ROL
                Eres un consultor en innovación educativa para docentes de educación superior chilena.

                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion$contextoPdf

                # INSTRUCCIONES
                1. **Confirma el tema**: Identifica el área disciplinar y nivel
                2. **Asume rol apropiado**: Actúa como especialista en esa área
                3. **Genera ideas innovadoras**: Propuestas creativas pero aplicables

                # FORMATO DE RESPUESTA

                🎯 **ANÁLISIS RÁPIDO**
                • Tema central: [tema detectado]
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

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
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
    suspend fun sugerirActividades(nombreClase: String, descripcion: String, pdfUrl: String? = null): String {
        return try {
            val contextoPdf = if (!pdfUrl.isNullOrEmpty()) {
                "\n📎 **Material disponible**: PDF adjunto con contenido de apoyo"
            } else ""
            val prompt = """
                # CONTEXTO Y ROL
                Eres un diseñador instruccional especializado en aprendizaje activo para educación superior chilena.

                # DATOS DE LA CLASE
                📚 Clase: $nombreClase
                📝 Descripción: $descripcion$contextoPdf

                # INSTRUCCIONES
                1. **Confirma el contexto**: Identifica tema y nivel
                2. **Asume rol de pedagogo**: Experto en metodologías activas
                3. **Diseña 4 actividades**: Variadas y con diferente nivel de complejidad

                # FORMATO DE RESPUESTA

                🎓 **CONTEXTO DETECTADO**
                • Tema: [tema identificado]
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

            val resultado = llamarGemini(prompt)
            "$resultado\n\n🚬😶‍ ESTE FUE GEMINI REAL BRO"
        } catch (e: Exception) {
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
            val contextoPdf = if (!pdfUrl.isNullOrEmpty()) {
                "\n✅ **PDF confirmado**: $pdfUrl"
            } else {
                "\n⚠️ **Sin PDF**: Se generarán estrategias generales"
            }
            val prompt = """
                # CONTEXTO Y ROL
                Eres un analista de materiales educativos para docentes universitarios chilenos.

                # SITUACIÓN
                Un docente tiene un PDF para la clase: "$nombreClase"$contextoPdf
                Necesita ideas concretas para aprovechar ese material en su clase.                # INSTRUCCIONES
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
            "⚠️ Error al conectar con Gemini AI\n\n📊 ESTRATEGIAS GENERALES PARA PDF de $nombreClase\n\nPara estrategias personalizadas, verifica tu conexión.\n\nError: ${
                e.message?.take(
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
    suspend fun generarGuiaPresentacion(nombreClase: String, descripcion: String, pdfUrl: String? = null): String {
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
                - [ ] [Habilidad 1]
                - [ ] [Habilidad 2]
                - [ ] [Habilidad 3]
                - [ ] [Habilidad 4]

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
            // ✅ NUEVO: Si hay PDF, EXTRAER SU CONTENIDO REAL
            val promptConPdf = if (!pdfUrl.isNullOrEmpty()) {
                // Extraer texto del PDF
                val textoPdf = extraerTextoDePdf(pdfUrl)

                """
                $promptCompleto

                📎 CONTENIDO DEL PDF ADJUNTO:
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                $textoPdf
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                IMPORTANTE: El texto anterior ES EL CONTENIDO REAL del PDF.
                Cuando el usuario pida "analizar el PDF", "revisar el material", "explicar el documento", etc.,
                debes usar ESTE CONTENIDO para generar respuestas específicas y precisas.

                No inventes información. Basa tus respuestas en lo que realmente está escrito arriba.
                """.trimIndent()
            } else {
                promptCompleto
            }

            val resultado = llamarGemini(promptConPdf)
            resultado
        } catch (e: Exception) {
            "⚠️ Error al conectar con Gemini AI\n\nNo pude procesar tu mensaje. Verifica tu conexión.\n\nError: ${
                e.message?.take(100) ?: "Desconocido"
            }"
        }
    }
}
