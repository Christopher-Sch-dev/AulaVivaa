package cl.duocuc.aulaviva.presentation.ui.pdf

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import cl.duocuc.aulaviva.R

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var progressDialog: ProgressDialog? = null

    // Constante como propiedad normal para evitar error de compilación
    private val PDF_LOADING_DELAY_MS = 3000L

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI minimalista
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Toolbar
        val toolbar = Toolbar(this).apply {
            title = getString(R.string.pdf_viewer_title)
            setTitleTextColor(
                ContextCompat.getColor(
                    this@PdfViewerActivity,
                    R.color.colorOnPrimaryToolbar
                )
            )
            setBackgroundColor(
                ContextCompat.getColor(
                    this@PdfViewerActivity,
                    R.color.colorPrimaryToolbar
                )
            )
            setNavigationOnClickListener { finish() }
            setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel)
        }
        root.addView(
            toolbar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // WebView
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportZoom(true)
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true

            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }
        root.addView(
            webView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply { weight = 1f })

        setContentView(root)

        // Obtener URL
        val pdfUrl = intent.getStringExtra("PDF_URL") ?: ""

        if (pdfUrl.isEmpty()) {
            Toast.makeText(this, "URL del PDF no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("PdfViewer", "📥 Abriendo PDF: $pdfUrl")

        // Mostrar progreso
        progressDialog = ProgressDialog(this).apply {
            setTitle("Cargando PDF")
            setMessage("Espera un momento...")
            setCancelable(false)
            show()
        }

        // WebViewClient personalizado para detectar cuando termina de cargar
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressDialog?.dismiss()
                Log.d("PdfViewer", "✅ PDF cargado correctamente")
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                progressDialog?.dismiss()
                Log.e("PdfViewer", "❌ Error cargando PDF: $description")
                Toast.makeText(
                    this@PdfViewerActivity,
                    "Error al cargar el PDF. Verifica tu conexión a internet.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Cargar PDF usando Google Docs Viewer
        // WebView no puede mostrar PDFs directamente, usamos Google Docs
        try {
            val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=$pdfUrl"
            Log.d("PdfViewer", "🔗 URL Google Docs: $googleDocsUrl")
            webView.loadUrl(googleDocsUrl)

        } catch (e: Exception) {
            progressDialog?.dismiss()
            Log.e("PdfViewer", "❌ Error al intentar cargar PDF", e)
            Toast.makeText(
                this,
                "Error inesperado: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
