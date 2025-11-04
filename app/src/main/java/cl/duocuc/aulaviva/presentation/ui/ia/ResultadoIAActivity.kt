package cl.duocuc.aulaviva.presentation.ui.ia

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import cl.duocuc.aulaviva.data.repository.IARepository
import com.google.android.material.appbar.MaterialToolbar

/**
 * Muestra resultados largos de IA en una WebView, convirtiendo Markdown a HTML simple.
 */
class ResultadoIAActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val titulo = intent.getStringExtra("TITULO") ?: "Resultado IA"
        val contenido = intent.getStringExtra("CONTENIDO") ?: ""

        // Root vertical con Toolbar + WebView
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val toolbar = MaterialToolbar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (56 * resources.displayMetrics.density).toInt()
            )
            title = titulo
            setNavigationIcon(android.R.drawable.ic_menu_revert)
            setNavigationOnClickListener { finish() }
        }

        val web = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            settings.javaScriptEnabled = false
            val html = IARepository().markdownToHtml(contenido)
            loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        }

        root.addView(toolbar)
        root.addView(web)
        setContentView(root)
    }
}
