package cl.duocuc.aulaviva.presentation.ui.pdf

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import cl.duocuc.aulaviva.R
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 📄 VISUALIZADOR DE PDF NATIVO
 *
 * Usa AndroidPdfViewer para visualizar PDFs:
 * - Descarga el PDF automáticamente
 * - Visualización nativa (sin servicios externos)
 * - Soporta cualquier tamaño de PDF
 * - Zoom, scroll, navegación por páginas
 * - Funciona offline después de descargar
 */
class PdfViewerActivity : AppCompatActivity() {

    private lateinit var pdfView: PDFView
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Visualizador PDF"
        }
        toolbar.setNavigationOnClickListener { finish() }

        // PDFView
        pdfView = findViewById(R.id.pdfView)

        // Obtener URL del PDF
        val pdfUrl = intent.getStringExtra("PDF_URL") ?: ""

        if (pdfUrl.isEmpty()) {
            Toast.makeText(this, "URL del PDF no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("PdfViewer", "📥 Descargando PDF: $pdfUrl")

        // Descargar y mostrar PDF
        descargarYMostrarPdf(pdfUrl)
    }

    /**
     * Descarga el PDF y lo muestra en PDFView
     */
    private fun descargarYMostrarPdf(pdfUrl: String) {
        // Mostrar progreso
        progressDialog = ProgressDialog(this).apply {
            setTitle("Descargando PDF")
            setMessage("Por favor espera...")
            setCancelable(false)
            show()
        }

        // Descargar en background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Crear archivo temporal en cache
                val pdfFile = File(cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")

                Log.d("PdfViewer", "📂 Descargando a: ${pdfFile.absolutePath}")

                // Descargar PDF
                val url = URL(pdfUrl)
                val connection = url.openConnection()
                connection.connect()

                val inputStream = connection.getInputStream()
                val outputStream = FileOutputStream(pdfFile)

                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d("PdfViewer", "✅ PDF descargado: ${pdfFile.length()} bytes")

                // Mostrar PDF en UI thread
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    mostrarPdf(pdfFile)
                }

            } catch (e: Exception) {
                Log.e("PdfViewer", "❌ Error descargando PDF", e)
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    Toast.makeText(
                        this@PdfViewerActivity,
                        "Error al descargar el PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    /**
     * Muestra el PDF descargado en PDFView
     */
    private fun mostrarPdf(pdfFile: File) {
        try {
            pdfView.fromFile(pdfFile)
                .defaultPage(0) // Página inicial
                .enableSwipe(true) // Permitir deslizar para cambiar página
                .swipeHorizontal(false) // Scroll vertical
                .enableDoubletap(true) // Doble tap para zoom
                .enableAnnotationRendering(true) // Renderizar anotaciones
                .scrollHandle(DefaultScrollHandle(this)) // Barra de desplazamiento
                .spacing(10) // Espacio entre páginas (dp)
                .onLoad { numPages ->
                    Log.d("PdfViewer", "✅ PDF cargado: $numPages páginas")
                    Toast.makeText(
                        this,
                        "PDF cargado: $numPages páginas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onPageChange { page, pageCount ->
                    supportActionBar?.title = "Página ${page + 1} de $pageCount"
                }
                .onError { throwable ->
                    Log.e("PdfViewer", "❌ Error mostrando PDF", throwable)
                    Toast.makeText(
                        this,
                        "Error al mostrar el PDF: ${throwable.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                .load()

        } catch (e: Exception) {
            Log.e("PdfViewer", "❌ Error configurando PDFView", e)
            Toast.makeText(
                this,
                "Error inesperado: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar archivos temporales
        try {
            cacheDir.listFiles()?.filter { it.name.startsWith("temp_pdf_") }
                ?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e("PdfViewer", "Error limpiando cache", e)
        }
    }
}
