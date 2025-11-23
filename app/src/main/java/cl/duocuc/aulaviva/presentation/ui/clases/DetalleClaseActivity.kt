package cl.duocuc.aulaviva.presentation.ui.clases

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import cl.duocuc.aulaviva.presentation.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.viewModels
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.repository.IARepository
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetalleClaseActivity : BaseActivity() {

    private val iaViewModel: cl.duocuc.aulaviva.presentation.viewmodel.IAViewModel by viewModels()
    private val claseViewModel: ClaseViewModel by viewModels()
    private var claseActual: Clase? = null
    private var esAlumno: Boolean = false  // Flag para determinar si es alumno

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_clase)

        // IARepository now accessed via IAViewModel (constructed with applicationContext internally)

        // Obtener flag ES_ALUMNO del intent
        esAlumno = intent.getBooleanExtra("ES_ALUMNO", false)

        val claseId = intent.getStringExtra("CLASE_ID")
        if (claseId.isNullOrEmpty()) {
            Toast.makeText(this, "No se pudo cargar la clase", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarDatosClase(claseId)
        setupListeners()

        // Edge-to-edge: aplicado automáticamente por BaseActivity
    }

    private fun cargarDatosClase(claseId: String) {
        lifecycleScope.launch {
            val clase = claseViewModel.obtenerClasePorId(claseId)
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
            // Ocultar botón "Analizar PDF" si es alumno
            btnAnalizarPdf?.visibility = if (esAlumno) View.GONE else View.VISIBLE
        } else {
            cardPdf?.visibility = View.GONE
            btnAnalizarPdf?.visibility = View.GONE
        }

        // Ocultar botones de IA exclusivos del docente si es alumno
        if (esAlumno) {
            findViewById<View>(R.id.btnGenerarIdeas)?.visibility = View.GONE
            findViewById<View>(R.id.btnSugerirActividades)?.visibility = View.GONE
            findViewById<View>(R.id.btnEstructurarClase)?.visibility = View.GONE
            findViewById<View>(R.id.btnResumirPdf)?.visibility = View.GONE
            findViewById<View>(R.id.btnReordenarTemas)?.visibility = View.GONE
            findViewById<View>(R.id.btnIdeasDocentePdf)?.visibility = View.GONE

            // Mostrar botones específicos para alumno
            findViewById<View>(R.id.btnExplicarConceptos)?.visibility = View.VISIBLE
            findViewById<View>(R.id.btnGenerarEjercicios)?.visibility = View.VISIBLE
            findViewById<View>(R.id.btnResumenEstudio)?.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.btnVolver)?.setOnClickListener { finish() }
        findViewById<Button>(R.id.btnVerPdf)?.setOnClickListener { intentarAbrirPdfFijo() }

        // Botones para DOCENTE
        findViewById<Button>(R.id.btnGenerarIdeas)?.setOnClickListener { generarIdeasParaClase() }
        findViewById<Button>(R.id.btnSugerirActividades)?.setOnClickListener { sugerirActividades() }
        findViewById<Button>(R.id.btnEstructurarClase)?.setOnClickListener { estructurarClasePorTiempo() }
        findViewById<Button>(R.id.btnAnalizarPdf)?.setOnClickListener { analizarPdfConIA() }
        findViewById<Button>(R.id.btnResumirPdf)?.setOnClickListener { resumirContenidoPdf() }
        findViewById<Button>(R.id.btnReordenarTemas)?.setOnClickListener { reordenarTemasParaClase() }
        findViewById<Button>(R.id.btnIdeasDocentePdf)?.setOnClickListener { ideasDocenteBasadasEnPdf() }

        // Botones para ALUMNO
        findViewById<Button>(R.id.btnExplicarConceptos)?.setOnClickListener { explicarConceptosAlumno() }
        findViewById<Button>(R.id.btnGenerarEjercicios)?.setOnClickListener { generarEjerciciosAlumno() }
        findViewById<Button>(R.id.btnResumenEstudio)?.setOnClickListener { crearResumenEstudioAlumno() }
    }

    /**
     * Abre PDF con el visor nativo del sistema usando PdfUtils.
     * Solución centralizada y profesional.
     */
    private fun abrirPdf(pdfUrl: String) {
        cl.duocuc.aulaviva.utils.PdfUtils.abrirPdfExterno(this, pdfUrl)
    }

    /**
     * Fallback: Abre PDF en navegador (DEPRECADO - ahora maneja PdfUtils).
     * Mantenido por compatibilidad pero ya no se usa directamente.
     */
    private fun abrirEnNavegador(pdfUrl: String) {
        cl.duocuc.aulaviva.utils.PdfUtils.abrirPdfExterno(this, pdfUrl)
    }

    private fun mostrarDialogoCarga(titulo: String, mensaje: String): AlertDialog {
        return AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun mostrarResultadoIA(
        titulo: String,
        contenido: String,
        tipoConsulta: String = "",
        nombreClase: String = "",
        descripcionClase: String = "",
        pdfUrl: String = ""
    ) {
        val intent =
            Intent(this, cl.duocuc.aulaviva.presentation.ui.ia.ResultadoIAActivity::class.java)
        intent.putExtra("TITULO", titulo)
        intent.putExtra("CONTENIDO", contenido)
        intent.putExtra("TIPO_CONSULTA", tipoConsulta)
        intent.putExtra("NOMBRE_CLASE", nombreClase)
        intent.putExtra("DESCRIPCION_CLASE", descripcionClase)
        intent.putExtra("PDF_URL", pdfUrl)
        startActivity(intent)
    }

    // ---- IA Docente ----
    private fun generarIdeasParaClase() {
        val clase = claseActual ?: return
        val loading =
            mostrarDialogoCarga("💡 Generando ideas...", "La IA está trabajando, por favor espera")
        // Delegar a IAViewModel y observar resultado
        val live = iaViewModel.generarIdeasParaClase(clase.nombre, clase.descripcion, clase.archivoPdfUrl)
        live.observe(this) { result ->
            loading.dismiss()
            if (result.isSuccess) {
                val contenido = result.getOrNull() ?: ""
                mostrarResultadoIA(
                    titulo = "💡 Ideas para tu clase",
                    contenido = contenido,
                    tipoConsulta = "IDEAS",
                    nombreClase = clase.nombre,
                    descripcionClase = clase.descripcion,
                    pdfUrl = clase.archivoPdfUrl
                )
            } else {
                Toast.makeText(this@DetalleClaseActivity, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
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
                val live = iaViewModel.sugerirActividades(clase.nombre, clase.descripcion, clase.archivoPdfUrl)
                live.observe(this@DetalleClaseActivity) { result ->
                    loading.dismiss()
                    if (result.isSuccess) {
                        mostrarResultadoIA(
                            titulo = "🎯 Actividades sugeridas",
                            contenido = result.getOrNull() ?: "",
                            tipoConsulta = "IDEAS",
                            nombreClase = clase.nombre,
                            descripcionClase = clase.descripcion,
                            pdfUrl = clase.archivoPdfUrl
                        )
                    } else {
                        Toast.makeText(this@DetalleClaseActivity, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    Toast.makeText(this@DetalleClaseActivity, e.message, Toast.LENGTH_SHORT).show()
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
                        val live = iaViewModel.estructurarClasePorTiempo(clase.nombre, clase.descripcion, duracion, clase.archivoPdfUrl)
                        live.observe(this@DetalleClaseActivity) { result ->
                            loading.dismiss()
                            if (result.isSuccess) {
                                mostrarResultadoIA("⏱️ Estructura de $duracion", result.getOrNull() ?: "")
                            } else {
                                Toast.makeText(this@DetalleClaseActivity, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            loading.dismiss()
                            Toast.makeText(this@DetalleClaseActivity, e.message, Toast.LENGTH_SHORT)
                                .show()
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
                val live = iaViewModel.analizarPdfConIA(clase.nombre, clase.archivoPdfUrl)
                live.observe(this@DetalleClaseActivity) { result ->
                    loading.dismiss()
                    if (result.isSuccess) {
                        mostrarResultadoIA("📄 Análisis del material PDF", result.getOrNull() ?: "")
                    } else {
                        Toast.makeText(this@DetalleClaseActivity, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    Toast.makeText(this@DetalleClaseActivity, e.message, Toast.LENGTH_SHORT).show()
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
                val live = iaViewModel.resumirContenidoPdf(clase.nombre, clase.descripcion, clase.archivoPdfNombre ?: "Material educativo")
                live.observe(this@DetalleClaseActivity) { result ->
                    loading.dismiss()
                    if (result.isSuccess) {
                        mostrarResultadoIA("📝 Resumen del contenido", result.getOrNull() ?: "")
                    } else {
                        Toast.makeText(this@DetalleClaseActivity, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    Toast.makeText(this@DetalleClaseActivity, e.message, Toast.LENGTH_SHORT).show()
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
                val live = iaViewModel.generarGuiaPresentacion(clase.nombre, clase.descripcion, clase.archivoPdfUrl)
                live.observe(this@DetalleClaseActivity) { result ->
                    loading.dismiss()
                    if (result.isSuccess) {
                        mostrarResultadoIA("🎤 Guía de Presentación", result.getOrNull() ?: "")
                    } else {
                        Toast.makeText(this@DetalleClaseActivity, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    Toast.makeText(this@DetalleClaseActivity, e.message, Toast.LENGTH_SHORT).show()
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
                val live = iaViewModel.generarActividadesInteractivas(clase.nombre, clase.descripcion, clase.archivoPdfNombre)
                live.observe(this@DetalleClaseActivity) { result ->
                    loading.dismiss()
                    if (result.isSuccess) {
                        mostrarResultadoIA("🎮 Actividades Interactivas", result.getOrNull() ?: "")
                    } else {
                        Toast.makeText(this@DetalleClaseActivity, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    Toast.makeText(this@DetalleClaseActivity, e.message, Toast.LENGTH_SHORT).show()
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

    // ========================================
    // 🎓 FUNCIONES IA PARA ALUMNO
    // ========================================

    private fun explicarConceptosAlumno() {
        val clase = claseActual ?: return
        val loading = mostrarDialogoCarga(
            "📚 Explicando conceptos...",
            "Tu tutor IA está preparando la explicación"
        )
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val live = iaViewModel.explicarConceptosParaAlumno(clase.nombre, clase.descripcion, clase.archivoPdfNombre)
                live.observe(this@DetalleClaseActivity) { result ->
                    loading.dismiss()
                    if (result.isSuccess) {
                        mostrarResultadoIA(
                            titulo = "📚 Conceptos explicados para ti",
                            contenido = result.getOrNull() ?: "",
                            tipoConsulta = "ALUMNO_CONCEPTOS",
                            nombreClase = clase.nombre,
                            descripcionClase = clase.descripcion,
                            pdfUrl = clase.archivoPdfUrl
                        )
                    } else {
                        Toast.makeText(this@DetalleClaseActivity, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    Toast.makeText(this@DetalleClaseActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generarEjerciciosAlumno() {
        val clase = claseActual ?: return
        val loading = mostrarDialogoCarga(
            "✍️ Creando ejercicios...",
            "Tu tutor IA está diseñando ejercicios de práctica"
        )
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val live = iaViewModel.generarEjerciciosParaAlumno(clase.nombre, clase.descripcion, clase.archivoPdfUrl)
                live.observe(this@DetalleClaseActivity) { result ->
                    loading.dismiss()
                    if (result.isSuccess) {
                        mostrarResultadoIA(
                            titulo = "✍️ Ejercicios de práctica",
                            contenido = result.getOrNull() ?: "",
                            tipoConsulta = "ALUMNO_EJERCICIOS",
                            nombreClase = clase.nombre,
                            descripcionClase = clase.descripcion,
                            pdfUrl = clase.archivoPdfUrl
                        )
                    } else {
                        Toast.makeText(this@DetalleClaseActivity, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    Toast.makeText(this@DetalleClaseActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun crearResumenEstudioAlumno() {
        val clase = claseActual ?: return
        val loading = mostrarDialogoCarga(
            "📖 Creando resumen...",
            "Tu tutor IA está organizando el contenido para ti"
        )
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val live = iaViewModel.crearResumenEstudioParaAlumno(clase.nombre, clase.descripcion, clase.archivoPdfNombre)
                live.observe(this@DetalleClaseActivity) { result ->
                    loading.dismiss()
                    if (result.isSuccess) {
                        mostrarResultadoIA(
                            titulo = "📖 Tu resumen de estudio",
                            contenido = result.getOrNull() ?: "",
                            tipoConsulta = "ALUMNO_RESUMEN",
                            nombreClase = clase.nombre,
                            descripcionClase = clase.descripcion,
                            pdfUrl = clase.archivoPdfUrl
                        )
                    } else {
                        Toast.makeText(this@DetalleClaseActivity, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    Toast.makeText(this@DetalleClaseActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
