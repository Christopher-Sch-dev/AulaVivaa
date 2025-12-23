package cl.duocuc.aulaviva.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

@Composable
fun AulaVivaCard(
    modifier: Modifier = Modifier,
    title: String = "",
    hasGradient: Boolean = false,
    content: @Composable () -> Unit
) {
    val gradient = if (hasGradient) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.05f)
            )
        )
    } else {
        SolidColor(MaterialTheme.colorScheme.surface)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient), // Note: applying background to modifier here might be shadowed by Card containerColor. It's better to use containerColor or box inside.
             // However, Card containerColor doesn't take a brush.
             // To support gradient, we need to put the gradient INSIDE the card content or wrap the content.
             // Let's stick to the user provided code structure but maybe improve it if I see a bug.
             // The user provided code does: Card(modifier...) { Column... }
             // The .background(gradient) on Card modifier MIGHT work if Card color is transparent, but standard Card opaquely renders containerColor.
             // Let's follow the user's snippet logic but fix the likely issue:
             // Card defaults to opaque surface. To see the gradient behind (if applied to Box) or ON TOP, we need specific handling.
             // For "Professional" look, usually a subtle gradient ON the card surface.
             // Correct approach: Use a Box inside the Card with the gradient.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // We apply the gradient purely as a visual overlay/background for the content column
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                content()
            }
        }
    }
}
