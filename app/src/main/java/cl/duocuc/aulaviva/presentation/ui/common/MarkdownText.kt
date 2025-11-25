package cl.duocuc.aulaviva.presentation.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Componente para renderizar texto Markdown básico en Compose
 * Soporta: **negrita**, *cursiva*, listas, encabezados, código
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val annotatedString = parseMarkdown(text, colorScheme)
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.fillMaxWidth()
    )
}

private fun parseMarkdown(
    text: String,
    colorScheme: androidx.compose.material3.ColorScheme
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Encabezados ##
                text.startsWith("##", i) -> {
                    val endLine = text.indexOf('\n', i)
                    val headerText = if (endLine != -1) {
                        text.substring(i + 2, endLine).trim()
                    } else {
                        text.substring(i + 2).trim()
                    }
                    withStyle(
                        style = SpanStyle(
                            fontSize = 22.sp, // ✅ Más grande
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                            letterSpacing = 0.5.sp // ✅ Mejor espaciado
                        )
                    ) {
                        append(headerText)
                    }
                    if (endLine != -1) {
                        append("\n")
                        i = endLine + 1
                    } else {
                        i = text.length
                    }
                }
                // Encabezados ###
                text.startsWith("###", i) -> {
                    val endLine = text.indexOf('\n', i)
                    val headerText = if (endLine != -1) {
                        text.substring(i + 3, endLine).trim()
                    } else {
                        text.substring(i + 3).trim()
                    }
                    withStyle(
                        style = SpanStyle(
                            fontSize = 20.sp, // ✅ Más grande
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                            letterSpacing = 0.3.sp
                        )
                    ) {
                        append(headerText)
                    }
                    if (endLine != -1) {
                        append("\n")
                        i = endLine + 1
                    } else {
                        i = text.length
                    }
                }
                // Negrita **texto**
                text.startsWith("**", i) && i + 2 < text.length -> {
                    val endBold = text.indexOf("**", i + 2)
                    if (endBold != -1) {
                        val boldText = text.substring(i + 2, endBold)
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface,
                                fontSize = 15.sp // ✅ Más grande para negritas
                            )
                        ) {
                            append(boldText)
                        }
                        i = endBold + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Cursiva *texto*
                text[i] == '*' && i + 1 < text.length && text[i + 1] != '*' -> {
                    val endItalic = text.indexOf('*', i + 1)
                    if (endItalic != -1) {
                        val italicText = text.substring(i + 1, endItalic)
                        withStyle(
                            style = SpanStyle(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        ) {
                            append(italicText)
                        }
                        i = endItalic + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Lista con bullet •
                text.startsWith("•", i) || text.startsWith("-", i) -> {
                    withStyle(
                        style = SpanStyle(
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("  • ")
                    }
                    i++
                    // Saltar espacios después del bullet
                    while (i < text.length && text[i] == ' ') i++
                }
                // Código `texto`
                text[i] == '`' && i + 1 < text.length -> {
                    val endCode = text.indexOf('`', i + 1)
                    if (endCode != -1) {
                        val codeText = text.substring(i + 1, endCode)
                        withStyle(
                            style = SpanStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                background = colorScheme.secondaryContainer, // ✅ Mejor contraste
                                color = colorScheme.onSecondaryContainer,
                                fontSize = 14.sp, // ✅ Tamaño adecuado
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(codeText)
                        }
                        i = endCode + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Separador ---
                text.startsWith("---", i) -> {
                    append("\n")
                    withStyle(
                        style = SpanStyle(
                            color = colorScheme.outline
                        )
                    ) {
                        append("─────────────────────────")
                    }
                    append("\n")
                    i += 3
                    // Saltar hasta el siguiente salto de línea
                    while (i < text.length && text[i] != '\n') i++
                    if (i < text.length) i++
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

