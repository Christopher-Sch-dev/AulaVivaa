package cl.duocuc.aulaviva.presentation.ui.ia

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import cl.duocuc.aulaviva.data.repository.IARepository

/**
 * Muestra resultados largos de IA en una WebView, convirtiendo Markdown a HTML simple.
 */
class ResultadoIAActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val titulo = intent.getStringExtra("TITULO") ?: "Resultado IA"
        val contenido = intent.getStringExtra("CONTENIDO") ?: ""
        title = titulo

        val web = WebView(this)
        web.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        web.settings.javaScriptEnabled = false
        val html = IARepository().markdownToHtml(contenido)
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)

        val root = FrameLayout(this)
        root.addView(web)
        setContentView(root)
    }
}
