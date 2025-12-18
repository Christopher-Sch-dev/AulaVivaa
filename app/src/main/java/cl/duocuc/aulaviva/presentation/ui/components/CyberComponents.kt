package cl.duocuc.aulaviva.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaColors
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTypography

/**
 * CyberCard: Tarjeta con borde cyan y fondo oscuro.
 * Diseño "Tech" con bordes ligeramente cortados o redondeados.
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SolidColor(AulaVivaColors.PrimaryCyan.copy(alpha = 0.5f))),
            colors = CardDefaults.cardColors(
                containerColor = AulaVivaColors.SurfaceDark.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SolidColor(AulaVivaColors.PrimaryCyan.copy(alpha = 0.5f))),
            colors = CardDefaults.cardColors(
                containerColor = AulaVivaColors.SurfaceDark.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * Variantes de botón Cyber
 */
enum class CyberButtonVariant {
    PRIMARY,
    SECONDARY,
    DANGER
}

/**
 * CyberButton: Botón rectangular, fuente monospace.
 */
@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: CyberButtonVariant = CyberButtonVariant.PRIMARY,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val containerColor = when (variant) {
        CyberButtonVariant.PRIMARY -> AulaVivaColors.PrimaryCyan
        CyberButtonVariant.SECONDARY -> AulaVivaColors.SurfaceLight
        CyberButtonVariant.DANGER -> AulaVivaColors.ErrorRed
    }
    
    val contentColor = when (variant) {
        CyberButtonVariant.PRIMARY -> Color.Black
        CyberButtonVariant.SECONDARY -> AulaVivaColors.PrimaryCyan
        CyberButtonVariant.DANGER -> Color.White
    }

    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled && !loading,
        shape = CutCornerShape(4.dp), // Estilo "Cyber" agresivo
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = AulaVivaColors.SurfaceDark,
            disabledContentColor = AulaVivaColors.TextSecondary
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = 8.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        }
        if (icon != null && !loading) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp).padding(end = 8.dp),
                tint = contentColor
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

/**
 * CyberTextField: Input con borde cyan y texto monospace.
 * 
 * @param value Valor actual del campo
 * @param onValueChange Callback cuando el valor cambia
 * @param label Etiqueta del campo (estilo terminal)
 * @param modifier Modificador de Compose
 * @param enabled Si el campo está habilitado
 * @param isPassword Si es true, oculta el texto con bullets y muestra toggle de visibilidad.
 *                   También configura el teclado para password input.
 */
@Composable
fun CyberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false
) {
    // Estado local para toggle de visibilidad - NO afecta ViewModel externo
    // Mantiene encapsulación: el estado de visibilidad es UI-only
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Text(
                text = label, 
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            ) 
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        // Transformación visual: bullets cuando es password y no está visible
        // PasswordVisualTransformation usa el character de enmascaramiento estándar de Android
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        // Teclado optimizado para password: no autocomplete, no suggestions
        keyboardOptions = if (isPassword) {
            KeyboardOptions(keyboardType = KeyboardType.Password)
        } else {
            KeyboardOptions.Default
        },
        // Toggle de visibilidad: solo se muestra para campos password
        // Icono cambia entre ojo abierto/cerrado según estado
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        },
                        tint = AulaVivaColors.PrimaryCyan
                    )
                }
            }
        },
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            color = AulaVivaColors.TextPrimary
        ),
        shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AulaVivaColors.PrimaryCyan,
            unfocusedBorderColor = AulaVivaColors.PrimaryCyan.copy(alpha = 0.3f),
            focusedLabelColor = AulaVivaColors.PrimaryCyan,
            unfocusedLabelColor = AulaVivaColors.TextSecondary,
            cursorColor = AulaVivaColors.PrimaryCyan
        )
    )
}
