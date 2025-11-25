package cl.duocuc.aulaviva.presentation.ui.common

import android.graphics.Typeface
import android.text.TextUtils
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // ✅ Crear instancia de Markwon con configuración profesional
    // Usar remember para evitar recrear la instancia en cada recomposición (optimización de rendimiento)
    val markwon = remember(context, colorScheme) {
        // Convertir colores de Compose a Android Color (int)
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
            // ✅ Plugin de imágenes
            .usePlugin(ImagesPlugin.create())
            // ✅ Plugin de links automáticos
            .usePlugin(LinkifyPlugin.create())
            // ✅ Plugin de tablas
            .usePlugin(TablePlugin.create(context))
            // ✅ Plugin de task lists
            .usePlugin(TaskListPlugin.create(context))
            // ✅ Plugin de strikethrough
            .usePlugin(StrikethroughPlugin.create())
            // ✅ Configurar tema personalizado para mejor integración con Material3
            .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .headingBreakHeight(0) // Sin línea bajo encabezados
                        .headingTextSizeMultipliers(floatArrayOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f))
                        .codeTextSize(14) // Tamaño de código
                        .codeBackgroundColor(surfaceVariantColor)
                        .codeTextColor(onSurfaceVariantColor)
                        .linkColor(primaryColor)
                        .blockQuoteColor(primaryColor)
                        .listItemColor(primaryColor)
                        .thematicBreakColor(outlineColor)
                }
            })
            .build()
    }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                // ✅ Configuración profesional del TextView
                // Usar Material3 typography como base
                textSize = 16f // Tamaño base (bodyMedium)
                setLineSpacing(8f, 1.0f) // Espaciado entre líneas para mejor legibilidad
                setPadding(0, 0, 0, 0) // Padding manejado por el Card contenedor

                // ✅ Configuración de texto
                setTextIsSelectable(true) // Permitir selección de texto
                setTypeface(null, Typeface.NORMAL) // Tipo de letra normal
                // Color de texto se maneja automáticamente por Markwon con el tema configurado

                // ✅ Configuración de links
                linksClickable = true // Links clickeables
                movementMethod = android.text.method.LinkMovementMethod.getInstance()

                // ✅ Configuración de scroll si es necesario
                isVerticalScrollBarEnabled = false // El scroll lo maneja el contenedor

                // ✅ Configuración de ellipsis para texto largo
                ellipsize = TextUtils.TruncateAt.END
                maxLines = Int.MAX_VALUE // Sin límite de líneas
            }
        },
        update = { textView ->
            // ✅ Renderizar Markdown con Markwon
            // Solo actualizar si el texto cambió (optimización)
            if (textView.text?.toString() != text) {
                markwon.setMarkdown(textView, text)
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
