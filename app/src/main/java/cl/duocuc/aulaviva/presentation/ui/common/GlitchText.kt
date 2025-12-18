package cl.duocuc.aulaviva.presentation.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import cl.duocuc.aulaviva.presentation.ui.theme.GlitchBlue
import cl.duocuc.aulaviva.presentation.ui.theme.GlitchRed

@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GlitchTransition")
    
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlitchOffset"
    )
    
    val alphaRandom by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlitchAlpha"
    )

    Box(modifier = modifier) {
        // Red Channel (Offset Left)
        Text(
            text = text,
            style = style.copy(fontFamily = FontFamily.Monospace),
            color = GlitchRed.copy(alpha = 0.7f),
            modifier = Modifier
                .offset(x = (offsetX * 2).dp, y = 0.dp)
                .graphicsLayer { alpha = alphaRandom }
        )

        // Blue Channel (Offset Right)
        Text(
            text = text,
            style = style.copy(fontFamily = FontFamily.Monospace),
            color = GlitchBlue.copy(alpha = 0.7f),
            modifier = Modifier
                .offset(x = (-offsetX * 2).dp, y = 0.dp)
                .graphicsLayer { alpha = alphaRandom }
        )

        // Main Text (Center)
        Text(
            text = text,
            style = style.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
