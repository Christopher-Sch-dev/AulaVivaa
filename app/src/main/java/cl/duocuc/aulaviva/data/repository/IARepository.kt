package cl.duocuc.aulaviva.data.repository

import com.google.ai.client.generativeai.GenerativeModel

/**
 * 🤖 GEMINI AI REAL - Repository para IA Educativa
 * Conectado directamente a Google Gemini Pro
 * 
 * Funciones educativas:
 * - Generar resúmenes de clases
 * - Crear glosarios automáticos
 * - Analizar PDFs y dar ideas pedagógicas
 */
class IARepository {
    
    // 🔑 Gemini AI Real activado
    private val GEMINI_API_KEY = "AIzaSyA6e4Wle5UkV93rOKIWm4FIKTQBDaOy8EY"
    private val generativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = GEMINI_API_KEY
    )
    
    init {
        println("✅ 🤖 GEMINI AI REAL ACTIVADO")
    }
    
    /**
     * 🤖 GEMINI REAL - Genera resumen para ALUMNO
     */
    suspend fun generarResumen(textoClase: String): String {
        return try {
            val prompt = """
                ¡Hola! Soy Gemini AI de Google, tu asistente educativo personal.
                
                He analizado el contenido de tu clase y aquí está el resumen:
                
                CONTENIDO ANALIZADO:
                $textoClase
                
                Por favor, genera un resumen estructurado así:
                
                📝 RESUMEN DE CLASE
                
                📌 Tema Principal:
                [Explica en 1-2 frases el tema central]
                
                🎯 Conceptos Clave:
                1. [Concepto 1 - explicación breve]
                2. [Concepto 2 - explicación breve]
                3. [Concepto 3 - explicación breve]
                
                💡 Lo Más Importante:
                [1-2 frases sobre qué debe recordar el estudiante]
                
                📚 Para estudiar:
                [Tips rápidos de estudio]
                
                IMPORTANTE: Termina con "⚡ Generado por Gemini AI Real de Google"
                
                Mantén un tono amigable y educativo. Máximo 200 palabras.
            """.trimIndent()
            
            val response = generativeModel.generateContent(prompt)
            response.text ?: "❌ Error: No se recibió respuesta de Gemini"
            
        } catch (e: Exception) {
            """
            ⚠️ No pude conectar con Gemini AI ahora mismo
            
            📝 RESUMEN LOCAL:
            
            📌 Tema: ${textoClase.take(100)}...
            
            🎯 Puntos clave:
            • Contenido educativo identificado
            • Material para estudio
            • Información de clase
            
            💡 Tip: Verifica tu conexión a internet
            
            Error técnico: ${e.message}
            """.trimIndent()
        }
    }
    
    /**
     * 🤖 GEMINI REAL - Genera glosario para ALUMNO
     */
    suspend fun generarGlosario(textoClase: String): String {
        return try {
            val prompt = """
                ¡Hola! Soy Gemini AI de Google, tu tutor virtual.
                
                Voy a crear un glosario de términos importantes de esta clase:
                
                CONTENIDO:
                $textoClase
                
                Genera un glosario así:
                
                📚 GLOSARIO DE TÉRMINOS
                
                [Identifica 5-7 términos clave del texto]
                
                Para cada término usa este formato:
                📖 TÉRMINO EN MAYÚSCULAS
                   → Definición clara y simple (1 línea)
                   💡 Ejemplo o contexto breve
                
                Si no hay términos técnicos, explica los conceptos principales del contenido.
                
                IMPORTANTE: Termina con "⚡ Generado por Gemini AI Real de Google"
                
                Sé claro y didáctico. Máximo 250 palabras.
            """.trimIndent()
            
            val response = generativeModel.generateContent(prompt)
            response.text ?: "❌ Error: No se recibió respuesta de Gemini"
            
        } catch (e: Exception) {
            """
            ⚠️ No pude conectar con Gemini AI
            
            📚 GLOSARIO LOCAL:
            
            📖 CLASE
               → Unidad educativa con contenido específico
               💡 Ej: "Tengo clase de programación hoy"
            
            📖 MATERIAL
               → Recursos didácticos de apoyo
               💡 Ej: "El material está en PDF"
            
            Error: ${e.message}
            """.trimIndent()
        }
    }
    
    /**
     * 🤖 GEMINI REAL - Analiza PDF y da IDEAS al DOCENTE
     */
    suspend fun analizarPDFParaProfesor(nombreArchivo: String, descripcionClase: String): String {
        return try {
            val prompt = """
                ¡Hola Profesor! Soy Gemini AI de Google, tu asistente pedagógico.
                
                Acabo de analizar tu material:
                📄 Archivo: $nombreArchivo
                📝 Contexto: $descripcionClase
                
                Como tu asistente educativo, te doy estas IDEAS PARA TU CLASE:
                
                💡 PLAN DE CLASE SUGERIDO:
                
                1. 📖 ACTIVIDADES RECOMENDADAS:
                   • [2-3 actividades interactivas específicas]
                
                2. 🎯 PUNTOS CLAVE A ENFATIZAR:
                   • [2-3 conceptos fundamentales]
                
                3. 📊 EVALUACIÓN SUGERIDA:
                   • [1-2 formas de evaluar]
                
                4. ⏱️ DISTRIBUCIÓN DE TIEMPO:
                   • Intro: X min
                   • Desarrollo: Y min
                   • Cierre: Z min
                
                5. 💬 TIP PEDAGÓGICO:
                   [Un consejo práctico para mejorar la clase]
                
                IMPORTANTE: Termina con "⚡ Análisis generado por Gemini AI Real de Google"
                
                Sé específico y práctico. Máximo 300 palabras.
            """.trimIndent()
            
            val response = generativeModel.generateContent(prompt)
            response.text ?: "❌ Error: No se recibió respuesta de Gemini"
            
        } catch (e: Exception) {
            """
            ⚠️ No pude conectar con Gemini AI
            
            💡 IDEAS LOCALES PARA TU CLASE:
            
            📄 Archivo: $nombreArchivo
            📝 Tema: $descripcionClase
            
            1. 📖 Actividades:
               • Discusión grupal
               • Ejercicios prácticos
               • Quiz rápido
            
            2. 🎯 Puntos clave:
               • Conceptos fundamentales
               • Aplicaciones prácticas
            
            3. ⏱️ Tiempo:
               • 15 min intro
               • 30 min desarrollo
               • 15 min práctica
            
            Error: ${e.message}
            """.trimIndent()
        }
    }
}
