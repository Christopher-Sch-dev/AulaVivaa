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
            Toast.makeText(this, getString(R.string.pdf_url_empty_error), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("PdfViewer", "📥 Abriendo PDF: $pdfUrl")

        // Mostrar progreso
        progressDialog = ProgressDialog(this).apply {
            setTitle(getString(R.string.pdf_loading_title))
            setMessage(getString(R.string.pdf_loading_message))
            setCancelable(false)
            show()
        }

        // Cargar PDF directamente en WebView
        try {
            webView.loadUrl(pdfUrl)

            // Ocultar progress después de 3 segundos
            webView.postDelayed({
                progressDialog?.dismiss()
            }, PDF_LOADING_DELAY_MS)

        } catch (e: Exception) {
            progressDialog?.dismiss()
            Log.e("PdfViewer", "❌ Error cargando PDF", e)
            Toast.makeText(
                this,
                getString(R.string.pdf_unexpected_error, e.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
