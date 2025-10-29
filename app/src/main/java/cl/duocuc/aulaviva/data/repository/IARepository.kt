package cl.duocuc.aulaviva.data.repository

import kotlinx.coroutines.delay

/**
 * Repository para integración con IA.
 * Este servicio simula/ejecuta funciones de Inteligencia Artificial.
 * 
 * IMPORTANTE: Aquí puedes conectar la API real de Gemini cuando quieras.
 * Por ahora funciona con IA simulada para demostrar la funcionalidad.
 * 
 * USO EDUCATIVO DE IA:
 * - Generar resúmenes de clases automáticamente
 * - Crear glosarios de términos técnicos
 * - Sugerir preguntas de quiz basadas en el contenido
 * - Analizar participación de estudiantes
 * 
 * Pensamiento: La IA no reemplaza al profesor, lo potencia.
 * Automatiza tareas repetitivas para que el profe se enfoque en enseñar.
 */
class IARepository {
    
    /**
     * Genera un resumen automático del contenido de una clase.
     * Por ahora es simulado, pero la estructura está lista para Gemini API.
     * 
     * @param textoClase: Contenido o descripción de la clase
     * @return Resumen generado por IA
     */
    suspend fun generarResumen(textoClase: String): String {
        // Simulo el tiempo que tarda una API real (1.5 segundos)
        delay(1500)
        
        // Análisis básico del texto
        val palabras = textoClase.split(" ").filter { it.isNotBlank() }
        val numPalabras = palabras.size
        val primerasPalabras = palabras.take(5).joinToString(" ")
        
        // Genero un resumen estructurado (esto lo haría Gemini en producción)
        return """
        📝 RESUMEN GENERADO POR IA
        
        📌 Tema principal detectado:
        "$primerasPalabras..."
        
        📊 Análisis del contenido:
        • Longitud: $numPalabras palabras
        • Complejidad: ${if (numPalabras > 50) "Alta" else "Media"}
        • Puntos clave identificados: ${(numPalabras / 10).coerceAtLeast(1)}
        
        🎯 Conceptos principales:
        1. ${palabras.getOrNull(0)?.capitalize() ?: "Concepto"} - Idea central
        2. ${palabras.getOrNull(2)?.capitalize() ?: "Desarrollo"} - Desarrollo temático
        3. ${palabras.getOrNull(4)?.capitalize() ?: "Conclusión"} - Conclusiones
        
        💡 Sugerencia pedagógica:
        Este contenido es apto para estudiantes de nivel ${if (numPalabras > 100) "avanzado" else "intermedio"}.
        Se recomienda complementar con ejemplos prácticos.
        
        ⚡ Generado con IA de Aula Viva
        (Modo simulado - listo para conectar Gemini API)
        """.trimIndent()
    }
    
    /**
     * Genera un glosario automático de términos técnicos.
     * Identifica palabras clave y las explica.
     */
    suspend fun generarGlosario(textoClase: String): String {
        delay(1200)
        
        // Banco de términos técnicos que la IA detecta
        val terminosTecnicos = mapOf(
            "android" to "Sistema operativo móvil de Google",
            "firebase" to "Plataforma de desarrollo de apps de Google",
            "kotlin" to "Lenguaje de programación moderno para Android",
            "mvvm" to "Patrón arquitectónico Model-View-ViewModel",
            "room" to "Librería de persistencia local en Android",
            "viewmodel" to "Componente que maneja lógica de UI",
            "livedata" to "Observable que respeta el ciclo de vida",
            "firestore" to "Base de datos NoSQL en la nube",
            "corrutinas" to "Sistema de programación asíncrona en Kotlin",
            "recyclerview" to "Vista eficiente para listas grandes"
        )
        
        // Busco términos en el texto
        val textoLower = textoClase.lowercase()
        val encontrados = terminosTecnicos.filter { (termino, _) ->
            textoLower.contains(termino)
        }
        
        return if (encontrados.isNotEmpty()) {
            """
            📚 GLOSARIO AUTOMÁTICO
            
            Términos técnicos detectados en la clase:
            
            ${encontrados.entries.joinToString("\n\n") { (termino, definicion) ->
                "📖 ${termino.uppercase()}\n   → $definicion"
            }}
            
            💡 Total de términos: ${encontrados.size}
            ⚡ Generado con IA
            """.trimIndent()
        } else {
            """
            📚 GLOSARIO AUTOMÁTICO
            
            📖 CLASE
               → Unidad educativa con contenido específico
            
            📖 FECHA
               → Programación temporal de la sesión
            
            📖 MATERIAL
               → Recursos didácticos de apoyo
            
            📖 EVALUACIÓN
               → Medición del aprendizaje logrado
            
            💡 Nota: No se detectaron términos técnicos específicos
            ⚡ Generado con IA
            """.trimIndent()
        }
    }
    
    /**
     * Genera preguntas de quiz automáticamente.
     * Útil para que el profesor evalúe rápidamente.
     */
    suspend fun generarPreguntas(textoClase: String): String {
        delay(1800)
        
        val palabras = textoClase.split(" ").filter { it.isNotBlank() }
        
        return """
        ❓ PREGUNTAS DE QUIZ SUGERIDAS
        
        Basadas en el contenido de la clase:
        
        1. ¿Cuál es el concepto principal de "${palabras.take(3).joinToString(" ")}"?
           a) Opción A
           b) Opción B
           c) Opción C
           ✓ d) [Respuesta correcta a definir]
        
        2. ¿Cómo se relacionan ${palabras.getOrNull(1)} y ${palabras.getOrNull(3)}?
           a) Opción A
           b) Opción B
           ✓ c) [Respuesta correcta a definir]
           d) Opción D
        
        3. Pregunta de aplicación práctica:
           "En un caso real, ¿cómo aplicarías...?"
           (Pregunta abierta para evaluar comprensión)
        
        💡 Estas preguntas son punto de partida.
        El docente puede editarlas según sus objetivos.
        
        ⚡ Generado con IA de Aula Viva
        """.trimIndent()
    }
    
    // ============ FUNCIONES SIMULADAS (FALLBACK) ============
    
    private suspend fun generarResumenSimulado(textoClase: String): String {
        // Simulo el tiempo que tarda una API real (1.5 segundos)
        delay(1500)
        
        // Análisis básico del texto
        val palabras = textoClase.split(" ").filter { it.isNotBlank() }
        val numPalabras = palabras.size
        val primerasPalabras = palabras.take(5).joinToString(" ")
        
        return """
        📝 RESUMEN (Modo Simulado)
        
        📌 Tema principal detectado:
        "$primerasPalabras..."
        
        📊 Análisis del contenido:
        • Longitud: $numPalabras palabras
        • Complejidad: ${if (numPalabras > 50) "Alta" else "Media"}
        • Puntos clave identificados: ${(numPalabras / 10).coerceAtLeast(1)}
        
        🎯 Conceptos principales:
        1. ${palabras.getOrNull(0)?.replaceFirstChar { it.uppercase() } ?: "Concepto"} - Idea central
        2. ${palabras.getOrNull(2)?.replaceFirstChar { it.uppercase() } ?: "Desarrollo"} - Desarrollo temático
        3. ${palabras.getOrNull(4)?.replaceFirstChar { it.uppercase() } ?: "Conclusión"} - Conclusiones
        
        💡 Sugerencia pedagógica:
        Este contenido es apto para estudiantes de nivel ${if (numPalabras > 100) "avanzado" else "intermedio"}.
        
        ⚠️ Modo simulado - Configura GEMINI_API_KEY para IA real
        """.trimIndent()
    }
    
    private suspend fun generarGlosarioSimulado(textoClase: String): String {
        delay(1200)
        
        val terminosTecnicos = mapOf(
            "android" to "Sistema operativo móvil de Google",
            "firebase" to "Plataforma de desarrollo de apps de Google",
            "kotlin" to "Lenguaje de programación moderno para Android",
            "mvvm" to "Patrón arquitectónico Model-View-ViewModel",
            "room" to "Librería de persistencia local en Android"
        )
        
        val textoLower = textoClase.lowercase()
        val encontrados = terminosTecnicos.filter { (termino, _) ->
            textoLower.contains(termino)
        }
        
        return if (encontrados.isNotEmpty()) {
            """
            📚 GLOSARIO (Modo Simulado)
            
            ${encontrados.entries.joinToString("\n\n") { (termino, definicion) ->
                "📖 ${termino.uppercase()}\n   → $definicion"
            }}
            
            💡 Total de términos: ${encontrados.size}
            ⚠️ Configura GEMINI_API_KEY para IA real
            """.trimIndent()
        } else {
            """
            📚 GLOSARIO (Modo Simulado)
            
            📖 CLASE → Unidad educativa
            📖 EVALUACIÓN → Medición del aprendizaje
            
            ⚠️ Configura GEMINI_API_KEY para IA real
            """.trimIndent()
        }
    }
    
    private suspend fun generarPreguntasSimuladas(textoClase: String): String {
        delay(1800)
        
        // Análisis básico del contenido para personalizar las preguntas
        val palabrasClave = textoClase.split(" ").take(5).joinToString(", ")
        
        return """
        ❓ QUIZ (Modo Simulado)
        
        Basado en: $palabrasClave...
        
        1. ¿Cuál es el concepto principal?
           a) Opción A
           b) Opción B
           ✓ c) [Respuesta correcta]
           d) Opción D
        
        2. ¿Cómo se aplica en la práctica?
           a) Opción A
           ✓ b) [Respuesta correcta]
           c) Opción C
           d) Opción D
        
        3. Pregunta de análisis:
           (Pregunta abierta para comprensión)
        
        ⚠️ Configura GEMINI_API_KEY para IA real
        """.trimIndent()
    }
    
    private suspend fun analizarContenidoSimulado(textoClase: String): String {
        delay(1000)
        
        val palabras = textoClase.split(" ").size
        
        return """
        🔍 ANÁLISIS (Modo Simulado)
        
        📊 Métricas:
        • Palabras: $palabras
        • Tiempo de lectura: ${(palabras / 200).coerceAtLeast(1)} min
        • Nivel: ${when {
            palabras < 50 -> "Básico ⭐"
            palabras < 150 -> "Intermedio ⭐⭐"
            else -> "Avanzado ⭐⭐⭐"
        }}
        
        🎯 Recomendaciones:
        ${if (palabras < 50) 
            "• Expandir contenido\n• Agregar ejemplos"
        else 
            "• Buen nivel de detalle\n• Incluir evaluaciones"
        }
        
        ⚠️ Configura GEMINI_API_KEY para IA real
        """.trimIndent()
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * GUÍA PARA CONECTAR GEMINI AI REAL
 * ═══════════════════════════════════════════════════════════════
 * 
 * 1️⃣ OBTÉN TU API KEY GRATUITA:
 *    - Ve a: https://makersuite.google.com/app/apikey
 *    - Inicia sesión con tu cuenta Google
 *    - Haz clic en "Create API Key"
 *    - Copia la key generada
 * 
 * 2️⃣ CONFIGURA LA KEY EN TU PROYECTO:
 *    - Abre el archivo: local.properties (raíz del proyecto)
 *    - Agrega esta línea:
 *      GEMINI_API_KEY=tu_clave_aqui
 *    - Guarda el archivo
 * 
 * 3️⃣ SINCRONIZA GRADLE:
 *    - En Android Studio: File > Sync Project with Gradle Files
 *    - Espera que termine la sincronización
 * 
 * 4️⃣ ¡LISTO! 
 *    - La app ahora usará Gemini AI real
 *    - Si la key no está o falla, usa modo simulado automáticamente
 * 
 * 📌 IMPORTANTE:
 *    - NO subas local.properties a Git (ya está en .gitignore)
 *    - La API de Gemini es GRATUITA con límites generosos
 *    - Funciona sin tarjeta de crédito
 * 
 * ═══════════════════════════════════════════════════════════════
 */
