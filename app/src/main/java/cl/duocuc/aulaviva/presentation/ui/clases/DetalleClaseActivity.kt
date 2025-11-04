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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc -> rolActual = doc.getString("rol") ?: "alumno" }
            .addOnFailureListener { rolActual = "alumno" }
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
                    Eres un asistente docente. Resume los puntos clave del material de esta clase.
                    Título: ${clase.nombre}
                    Descripción: ${clase.descripcion}
                    Si hay PDF: ${clase.archivoPdfNombre}. Resume en 5-7 bullets claros.
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
            mostrarDialogoCarga("🧩 Reordenando temas...", "La IA está trabajando, por favor espera")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Eres experto en didáctica. Reordena los temas del contenido para presentarlos en clase de forma progresiva.
                    Título: ${clase.nombre}
                    Descripción: ${clase.descripcion}
                    Lista los bloques en orden lógico con breve objetivo.
                """.trimIndent()
                val resultado = iaRepository.generarRespuestaPersonalizada(prompt)
                withContext(Dispatchers.Main) {
                    loading.dismiss(); mostrarResultadoIA(
                    "🧩 Secuencia didáctica (orden sugerido)",
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
            mostrarDialogoCarga("🎓 Generando ideas...", "La IA está trabajando, por favor espera")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Eres docente con foco en aprendizaje activo. Usando el contenido de la clase y el PDF (${clase.archivoPdfNombre}),
                    genera ideas de: 1) cómo iniciar la clase, 2) actividades interactivas, 3) cierre/reflexión.
                    Da 3 ideas por sección, en bullets.
                """.trimIndent()
                val resultado = iaRepository.generarRespuestaPersonalizada(prompt)
                withContext(Dispatchers.Main) {
                    loading.dismiss(); mostrarResultadoIA(
                    "🎓 Ideas para dictar la clase (con PDF)",
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

        // 2) Si no hay URL, intentar cargar desde Firestore
        val loading = mostrarDialogoCarga("📄 Cargando PDF...", "Obteniendo documento...")
        FirebaseFirestore.getInstance().collection("clases").document(clase.id).get()
            .addOnSuccessListener { doc ->
                loading.dismiss()
                val urlFs = doc.getString("archivoPdfUrl").orEmpty()
                if (urlFs.isNotEmpty() &&
                    (urlFs.startsWith("content://") ||
                            urlFs.startsWith("http://") ||
                            urlFs.startsWith("https://"))
                ) {
                    claseActual = clase.copy(archivoPdfUrl = urlFs)
                    abrirPdf(urlFs)
                } else {
                    // No hay PDF disponible
                    Toast.makeText(
                        this@DetalleClaseActivity,
                        "❌ Esta clase no tiene un PDF asociado",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                loading.dismiss()
                Toast.makeText(
                    this@DetalleClaseActivity,
                    "❌ Error al cargar PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
