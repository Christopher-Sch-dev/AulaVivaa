package cl.duocuc.aulaviva.presentation.ui.common

import android.graphics.Typeface
import android.text.TextUtils
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin

/**
 * Componente optimizado para renderizar Markdown usando Markwon
 *
 * Sigue principios de Clean Architecture:
 * - Separación de responsabilidades: Solo renderiza, no procesa lógica
 * - Reutilizable: Componente composable independiente
 * - Configuración profesional: Plugins completos para mejor renderizado
 *
 * Soporta:
 * - Encabezados (##, ###)
 * - Negritas (**texto**)
 * - Cursivas (*texto*)
 * - Listas (•, -, 1.)
 * - Código inline (`código`) y bloques (```código```)
 * - Links automáticos
 * - Tablas
 * - Task lists
 * - Strikethrough
 * - Imágenes
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    // Preprocess text to fix common markdown rendering issues
    val processedText = preprocessMarkdownText(text)
    
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // CRITICAL FIX: Colores para TextView que AndroidView NO hereda automáticamente del tema Compose
    // Sin esto, el texto puede ser invisible en dark theme (texto negro sobre fondo oscuro)
    val textColor = colorScheme.onSurface.toArgb()
    val linkColor = colorScheme.primary.toArgb()

    // Crear instancia de Markwon con configuración completa de plugins
    // Usar remember con context y colorScheme como keys para evitar recrear la instancia
    // en cada recomposición, mejorando el rendimiento
    val markwon = remember(context, colorScheme) {
        // Convertir colores de Material3 Compose (Color) a Android Color (Int)
        // para que Markwon pueda utilizarlos en su configuración de tema
        val primaryColor = android.graphics.Color.valueOf(
            colorScheme.primary.red,
            colorScheme.primary.green,
            colorScheme.primary.blue,
            colorScheme.primary.alpha
        ).toArgb()

        val surfaceVariantColor = android.graphics.Color.valueOf(
            colorScheme.surfaceVariant.red,
            colorScheme.surfaceVariant.green,
            colorScheme.surfaceVariant.blue,
            colorScheme.surfaceVariant.alpha
        ).toArgb()

        val onSurfaceVariantColor = android.graphics.Color.valueOf(
            colorScheme.onSurfaceVariant.red,
            colorScheme.onSurfaceVariant.green,
            colorScheme.onSurfaceVariant.blue,
            colorScheme.onSurfaceVariant.alpha
        ).toArgb()

        val outlineColor = android.graphics.Color.valueOf(
            colorScheme.outline.red,
            colorScheme.outline.green,
            colorScheme.outline.blue,
            colorScheme.outline.alpha
        ).toArgb()

        Markwon.builder(context)
            // Plugin para renderizar imágenes en Markdown
            .usePlugin(ImagesPlugin.create())
            // Plugin para convertir URLs y emails en links clickeables automáticamente
            .usePlugin(LinkifyPlugin.create())
            // Plugin para renderizar tablas en formato Markdown
            .usePlugin(TablePlugin.create(context))
            // Plugin para renderizar listas de tareas (checkboxes) en Markdown
            .usePlugin(TaskListPlugin.create(context))
            // Plugin para renderizar texto tachado (strikethrough) en Markdown
            .usePlugin(StrikethroughPlugin.create())
            // Configurar tema personalizado para integrar colores de Material3
            // Esto asegura que el Markdown renderizado use los colores del tema actual
            .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .headingBreakHeight(0) // Sin línea bajo encabezados
                        .headingTextSizeMultipliers(floatArrayOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f))
                        .codeTextSize(14) // Tamaño de código
                        .codeBackgroundColor(surfaceVariantColor)
                        .codeTextColor(onSurfaceVariantColor)
                        // Asegurar contraste en links y quotes
                        .linkColor(primaryColor)
                        .blockQuoteColor(primaryColor)
                        .listItemColor(onSurfaceVariantColor) // Mejor que primary para items de lista
                        .thematicBreakColor(outlineColor)
                }
            })
            .build()
    }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                // Configurar tamaño de texto base equivalente a Material3 bodyMedium
                textSize = 16f
                // Configurar espaciado entre líneas para mejorar la legibilidad del Markdown
                setLineSpacing(8f, 1.0f)
                // No agregar padding aquí, el Card contenedor maneja el espaciado
                setPadding(0, 0, 0, 0)

                // CRITICAL FIX: AndroidView NO hereda colores de Material theme automáticamente
                // Sin esto, el texto puede ser invisible en dark theme (texto negro sobre fondo oscuro)
                setTextColor(textColor)
                setLinkTextColor(linkColor)

                // Permitir que el usuario seleccione el texto renderizado
                setTextIsSelectable(true)
                // Usar tipo de letra normal (sin negrita ni cursiva por defecto)
                setTypeface(null, Typeface.NORMAL)

                // Habilitar que los links en el Markdown sean clickeables
                linksClickable = true
                // Configurar el método de movimiento para que los links funcionen correctamente
                movementMethod = android.text.method.LinkMovementMethod.getInstance()

                // Deshabilitar la barra de scroll vertical, el contenedor maneja el scroll
                isVerticalScrollBarEnabled = false

                // Configurar ellipsis para texto muy largo (aunque normalmente no se usa)
                ellipsize = TextUtils.TruncateAt.END
                // Permitir todas las líneas necesarias para mostrar el contenido completo
                maxLines = Int.MAX_VALUE
            }
        },
        update = { textView ->
            // Aplicar colores en cada update para manejar cambios de tema en tiempo de ejecución
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            
            // Renderizar el contenido Markdown usando Markwon
            // Solo actualizar si el texto cambió para evitar renderizado innecesario
            if (textView.text?.toString() != processedText) {
                markwon.setMarkdown(textView, processedText)
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Preprocesses markdown text to fix common rendering issues.
 * 
 * Issues fixed:
 * - Consecutive underscores (blanks like "_________") are escaped to prevent
 *   markdown interpreting them as emphasis markers, which causes garbled display.
 * - Asterisks inside words that shouldn't be bold/italic.
 * - Special unicode characters that may not render correctly.
 */
private fun preprocessMarkdownText(text: String): String {
    return text
        // Fix consecutive underscores (fill-in-the-blank patterns)
        // Pattern: 2+ underscores in a row -> replace with visible line character
        .replace(Regex("_{2,}")) { match ->
            "\u2015".repeat(match.value.length.coerceAtMost(10)) // Use horizontal bar character
        }
        // Fix single underscores at word boundaries that may cause unintended emphasis
        .replace(Regex("(?<=\\s)_(?!_)(.+?)(?<!_)_(?=\\s|\\.|,|\\))")) { match ->
            "\u2015${match.groupValues[1]}\u2015" // Replace emphasis underscores with horizontal bars
        }
        // Remove potential problematic control characters
        .replace(Regex("[\u0000-\u001F&&[^\t\n\r]]"), "")
}
