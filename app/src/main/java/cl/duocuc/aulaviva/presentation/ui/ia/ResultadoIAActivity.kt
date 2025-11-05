package cl.duocuc.aulaviva.presentation.ui.ia

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import io.noties.markwon.Markwon

/**
 * Muestra resultados largos de IA renderizando Markdown con Markwon.
 * ✅ TAREA 4: Ahora renderiza Markdown con formato visual (negrita, bullets, etc.)
 * ✅ TAREA 5: Incluye botones para copiar y compartir el contenido.
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
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setLineSpacing(4f, 1f)
        }

        // ✅ Usar Markwon para renderizar Markdown con formato
        try {
            val markwon = Markwon.create(this)
            markwon.setMarkdown(textView, contenido)
        } catch (e: Exception) {
            // Fallback: mostrar texto plano si Markwon falla
            textView.text = contenido
            Toast.makeText(this, "Error al renderizar Markdown: ${e.message}", Toast.LENGTH_LONG).show()
        }

        scrollView.addView(textView)
        root.addView(toolbar)
        root.addView(scrollView)

        // Contenedor para botones de acción
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * resources.displayMetrics.density).toInt() }
            gravity = android.view.Gravity.CENTER
        }

        // Botón Copiar
        val btnCopiar = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, // Ancho variable
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f // Peso para distribuir espacio
            ).apply { marginEnd = (8 * resources.displayMetrics.density).toInt() }
            text = "Copiar"
            setOnClickListener { copiarAlPortapapeles(contenido) }
        }
        buttonContainer.addView(btnCopiar)

        // Botón Compartir
        val btnCompartir = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, // Ancho variable
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f // Peso para distribuir espacio
            ).apply { marginStart = (8 * resources.displayMetrics.density).toInt() }
            text = "Compartir"
            setOnClickListener { compartirTexto(contenido) }
        }
        buttonContainer.addView(btnCompartir)

        root.addView(buttonContainer)

        setContentView(root)
    }

    private fun copiarAlPortapapeles(texto: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Resultado IA", texto)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Texto copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun compartirTexto(texto: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir resultado IA"))
    }
}
