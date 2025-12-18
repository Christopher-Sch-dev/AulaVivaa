package cl.duocuc.aulaviva.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
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
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp), // Un poco más suave que cut corner para "Professional"
        border = BorderStroke(1.dp, SolidColor(AulaVivaColors.PrimaryCyan.copy(alpha = 0.5f))),
        colors = CardDefaults.cardColors(
            containerColor = AulaVivaColors.SurfaceDark.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // Wrapper interno para padding coherente
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
            content()
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
    variant: CyberButtonVariant = CyberButtonVariant.PRIMARY
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
        ),
        singleLine = true
    )
}
