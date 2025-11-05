package cl.duocuc.aulaviva.presentation.ui.clases

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.repository.ClaseRepository
import cl.duocuc.aulaviva.data.repository.IARepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetalleClaseActivity : AppCompatActivity() {

    private lateinit var iaRepository: IARepository
    private lateinit var claseRepository: ClaseRepository
    private var claseActual: Clase? = null
    private var rolActual: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_clase)

        iaRepository = IARepository()
        claseRepository = ClaseRepository(this)

        obtenerRolUsuario()

        val claseId = intent.getStringExtra("CLASE_ID")
        if (claseId.isNullOrEmpty()) {
            Toast.makeText(this, "No se pudo cargar la clase", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarDatosClase(claseId)
        setupListeners()
    }

    private fun obtenerRolUsuario() {
        // Por defecto todos los usuarios registrados son docentes
        rolActual = "docente"
    }

    private fun cargarDatosClase(claseId: String) {
        lifecycleScope.launch {
            val clase = claseRepository.obtenerClasePorId(claseId)
            if (clase != null) {
                claseActual = clase
                mostrarDatos(clase)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DetalleClaseActivity,
                        "No se pudo cargar la clase",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun mostrarDatos(clase: Clase) {
        findViewById<TextView>(R.id.textTituloClase)?.text = clase.nombre
        findViewById<TextView>(R.id.textFechaDetalle)?.text = "Fecha: ${clase.fecha}"
        findViewById<TextView>(R.id.textDescripcionDetalle)?.text = clase.descripcion

        val cardPdf = findViewById<View>(R.id.cardPdf)
        val textNombrePdf = findViewById<TextView>(R.id.textNombrePdf)
        val btnAnalizarPdf = findViewById<Button>(R.id.btnAnalizarPdf)

        if (clase.archivoPdfNombre.isNotEmpty()) {
            cardPdf?.visibility = View.VISIBLE
            textNombrePdf?.text = clase.archivoPdfNombre
            btnAnalizarPdf?.visibility = if (rolActual == "docente") View.VISIBLE else View.GONE
        } else {
            cardPdf?.visibility = View.GONE
            btnAnalizarPdf?.visibility = View.GONE
        }

        // Ocultar botones IA si es alumno
        val botonesIa = listOf(
            R.id.btnGenerarIdeas, R.id.btnSugerirActividades, R.id.btnEstructurarClase
        )
        if (rolActual != "docente") {
            botonesIa.forEach { id -> findViewById<View>(id)?.visibility = View.GONE }
        }
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.btnVolver)?.setOnClickListener { finish() }
        findViewById<Button>(R.id.btnVerPdf)?.setOnClickListener { intentarAbrirPdfFijo() }

        findViewById<Button>(R.id.btnGenerarIdeas)?.setOnClickListener { generarIdeasParaClase() }
        findViewById<Button>(R.id.btnSugerirActividades)?.setOnClickListener { sugerirActividades() }
        findViewById<Button>(R.id.btnEstructurarClase)?.setOnClickListener { estructurarClasePorTiempo() }
        findViewById<Button>(R.id.btnAnalizarPdf)?.setOnClickListener { analizarPdfConIA() }
        findViewById<Button>(R.id.btnResumirPdf)?.setOnClickListener { resumirContenidoPdf() }
        findViewById<Button>(R.id.btnReordenarTemas)?.setOnClickListener { reordenarTemasParaClase() }
        findViewById<Button>(R.id.btnIdeasDocentePdf)?.setOnClickListener { ideasDocenteBasadasEnPdf() }
    }

    private fun abrirPdf(url: String) {
        val intent =
            Intent(this, cl.duocuc.aulaviva.presentation.ui.pdf.PdfViewerActivity::class.java)
        intent.putExtra("PDF_URL", url)
        startActivity(intent)
    }

    private fun mostrarDialogoCarga(titulo: String, mensaje: String): AlertDialog {
        return AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun mostrarResultadoIA(titulo: String, contenido: String) {
        val intent =
            Intent(this, cl.duocuc.aulaviva.presentation.ui.ia.ResultadoIAActivity::class.java)
        intent.putExtra("TITULO", titulo)
        intent.putExtra("CONTENIDO", contenido)
        startActivity(intent)
    }

    // ---- IA Docente ----
    private fun generarIdeasParaClase() {
        val clase = claseActual ?: return
        val pdfHint =
            if (!clase.archivoPdfUrl.isNullOrEmpty() && clase.archivoPdfUrl.startsWith("http")) "\nEnlace PDF: ${clase.archivoPdfUrl}" else ""
        val loading =
            mostrarDialogoCarga("💡 Generando ideas...", "La IA está trabajando, por favor espera")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Eres un asistente educativo para docentes. Analiza esta clase y genera ideas creativas:
                    Título: ${clase.nombre}
                    Descripción: ${clase.descripcion}$pdfHint
                    Genera 5 ideas en lista numerada, máximo 2 líneas cada una.
                """.trimIndent()
                val resultado = iaRepository.generarRespuestaPersonalizada(prompt)
                withContext(Dispatchers.Main) {
                    loading.dismiss(); mostrarResultadoIA(
                    "💡 Ideas para tu clase",
                    resultado
                )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss(); Toast.makeText(
                    this@DetalleClaseActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
                }
            }
        }
    }

    private fun sugerirActividades() {
        val clase = claseActual ?: return
        val loading = mostrarDialogoCarga(
            "🎯 Diseñando actividades...",
            "La IA está trabajando, por favor espera"
        )
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Eres experto en pedagogía. Diseña 4 actividades dinámicas:
                    Título: ${clase.nombre}
                    Descripción: ${clase.descripcion}
                    Para cada actividad: nombre, objetivo, duración, materiales.
                """.trimIndent()
                val resultado = iaRepository.generarRespuestaPersonalizada(prompt)
                withContext(Dispatchers.Main) {
                    loading.dismiss(); mostrarResultadoIA(
                    "🎯 Actividades sugeridas",
                    resultado
                )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss(); Toast.makeText(
                    this@DetalleClaseActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
                }
            }
        }
    }

    private fun estructurarClasePorTiempo() {
        val clase = claseActual ?: return
        val opciones = arrayOf("45 minutos", "60 minutos", "90 minutos", "120 minutos")
        AlertDialog.Builder(this)
            .setTitle("⏱️ Duración de la clase")
            .setItems(opciones) { _, which ->
                val duracion = opciones[which]
                val loading = mostrarDialogoCarga(
                    "⏱️ Estructurando...",
                    "La IA está trabajando, por favor espera"
                )
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val prompt = """
                            Estructura esta clase de $duracion:
                            Título: ${clase.nombre}
                            Descripción: ${clase.descripcion}
                            Divide en bloques con minutos exactos y actividades.
                        """.trimIndent()
                        val resultado = iaRepository.generarRespuestaPersonalizada(prompt)
                        withContext(Dispatchers.Main) {
                            loading.dismiss(); mostrarResultadoIA(
                            "⏱️ Estructura de $duracion",
                            resultado
                        )
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            loading.dismiss(); Toast.makeText(
                            this@DetalleClaseActivity,
                            e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun analizarPdfConIA() {
        val clase = claseActual ?: return
        val loading =
            mostrarDialogoCarga("📄 Analizando PDF...", "La IA está trabajando, por favor espera")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    El docente tiene un PDF para: ${clase.nombre}
                    Sugiere 3 formas de aprovechar el material en clase: lectura crítica, debate, práctica.
                """.trimIndent()
                val resultado = iaRepository.generarRespuestaPersonalizada(prompt)
                withContext(Dispatchers.Main) {
                    loading.dismiss(); mostrarResultadoIA(
                    "📄 Análisis del material PDF",
                    resultado
                )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss(); Toast.makeText(
                    this@DetalleClaseActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
                }
            }
        }
    }

    private fun resumirContenidoPdf() {
        val clase = claseActual ?: return
        val loading = mostrarDialogoCarga(
            "📝 Resumiendo contenido...",
            "La IA está trabajando, por favor espera"
        )
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Eres un asistente educativo experto. Resume el contenido de esta clase en español chileno neutro y profesional.
                    
                    Clase: ${clase.nombre}
                    Descripción: ${clase.descripcion}
                    Material PDF: ${clase.archivoPdfNombre}
                    
                    El resumen debe incluir:
                    ## Tema Principal
                    [Descripción en 2-3 líneas]
                    
                    ## Conceptos Clave
                    • [Punto 1]
                    • [Punto 2]
                    • [Punto 3-5]
                    
                    ## Conclusiones
                    [Resumen de aprendizajes principales]
                    
                    Usa formato Markdown claro y estructurado.
                """.trimIndent()
                val resultado = iaRepository.generarRespuestaPersonalizada(prompt)
                withContext(Dispatchers.Main) {
                    loading.dismiss(); mostrarResultadoIA(
                    "📝 Resumen del contenido",
                    resultado
                )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss(); Toast.makeText(
                    this@DetalleClaseActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
                }
            }
        }
    }

    private fun reordenarTemasParaClase() {
        val clase = claseActual ?: return
        val loading =
            mostrarDialogoCarga(
                "🎤 Preparando presentación...",
                "La IA está trabajando, por favor espera"
            )
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Eres un docente experto en pedagogía. Crea una guía detallada para presentar esta clase de forma efectiva.
                    
                    Clase: ${clase.nombre}
                    Contenido: ${clase.descripcion}
                    
                    La guía debe incluir:
                    
                    ## 1. Introducción Sugerida (2-3 minutos)
                    • Gancho inicial para captar atención
                    • Contextualización del tema
                    
                    ## 2. Puntos Clave a Enfatizar
                    • Conceptos fundamentales
                    • Por qué son importantes
                    
                    ## 3. Ejemplos Prácticos Recomendados
                    • Casos reales o ejercicios concretos
                    • Analogías útiles
                    
                    ## 4. Preguntas para Generar Participación
                    • 3-5 preguntas estratégicas
                    • Preguntas abiertas que fomenten discusión
                    
                    Usa Markdown estructurado y lenguaje profesional chileno.
                """.trimIndent()
                val resultado = iaRepository.generarRespuestaPersonalizada(prompt)
                withContext(Dispatchers.Main) {
                    loading.dismiss(); mostrarResultadoIA(
                    "🎤 Guía de Presentación",
                    resultado
                )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss(); Toast.makeText(
                    this@DetalleClaseActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
                }
            }
        }
    }

    private fun ideasDocenteBasadasEnPdf() {
        val clase = claseActual ?: return
        val loading =
            mostrarDialogoCarga(
                "🎮 Creando actividades...",
                "La IA está trabajando, por favor espera"
            )
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Eres un diseñador instruccional especializado en aprendizaje activo. 
                    Transforma este contenido en una clase interactiva para estudiantes universitarios.
                    
                    Clase: ${clase.nombre}
                    Descripción: ${clase.descripcion}
                    Material PDF: ${clase.archivoPdfNombre}
                    
                    Crea:
                    
                    ## 1. Actividades Prácticas (3-5)
                    • Ejercicio individual o grupal
                    • Tiempo estimado
                    • Recursos necesarios
                    
                    ## 2. Preguntas de Reflexión
                    • 5-7 preguntas que promuevan pensamiento crítico
                    • Variar entre simples y complejas
                    
                    ## 3. Ejercicios Grupales
                    • 2-3 dinámicas colaborativas
                    • Instrucciones paso a paso
                    
                    ## 4. Recursos Complementarios
                    • Videos, artículos, herramientas online sugeridos
                    
                    Usa Markdown. Sé creativo pero práctico. Lenguaje chileno neutral.
                """.trimIndent()
                val resultado = iaRepository.generarRespuestaPersonalizada(prompt)
                withContext(Dispatchers.Main) {
                    loading.dismiss(); mostrarResultadoIA(
                    "🎮 Actividades Interactivas",
                    resultado
                )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss(); Toast.makeText(
                    this@DetalleClaseActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
                }
            }
        }
    }

    private fun intentarAbrirPdfFijo() {
        val clase = claseActual ?: return

        // 1) Si tiene URL local válida, abrir directamente
        if (!clase.archivoPdfUrl.isNullOrEmpty() &&
            (clase.archivoPdfUrl.startsWith("content://") ||
                    clase.archivoPdfUrl.startsWith("http://") ||
                    clase.archivoPdfUrl.startsWith("https://"))
        ) {
            abrirPdf(clase.archivoPdfUrl)
            return
        }

        // No hay PDF disponible
        Toast.makeText(
            this@DetalleClaseActivity,
            "❌ Esta clase no tiene un PDF asociado",
            Toast.LENGTH_LONG
        ).show()
    }
}
