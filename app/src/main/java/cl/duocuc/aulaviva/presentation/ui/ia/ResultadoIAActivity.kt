package cl.duocuc.aulaviva.presentation.ui.ia

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import io.noties.markwon.Markwon

/**
 * Muestra resultados largos de IA renderizando Markdown con Markwon.
 * ✅ TAREA 4: Ahora renderiza Markdown con formato visual (negrita, bullets, etc.)
 */
class ResultadoIAActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val titulo = intent.getStringExtra("TITULO") ?: "Resultado IA"
        val contenido = intent.getStringExtra("CONTENIDO") ?: ""

        // Root vertical con Toolbar + ScrollView + TextView
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

        // ScrollView para hacer scroll del contenido
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // TextView para mostrar el Markdown renderizado
        val textView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            textSize = 15f
            setTextColor(0xFF2C2C2C.toInt())
            setLineSpacing(4f, 1f)
        }

        // ✅ Usar Markwon para renderizar Markdown con formato
        try {
            val markwon = Markwon.create(this)
            markwon.setMarkdown(textView, contenido)
        } catch (_: Exception) {
            // Fallback: mostrar texto plano si Markwon falla
            textView.text = contenido
        }

        scrollView.addView(textView)
        root.addView(toolbar)
        root.addView(scrollView)
        setContentView(root)
    }
}
