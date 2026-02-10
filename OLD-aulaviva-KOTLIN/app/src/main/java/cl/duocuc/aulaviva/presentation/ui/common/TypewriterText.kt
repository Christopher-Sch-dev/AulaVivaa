package cl.duocuc.aulaviva.presentation.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    speedMs: Long = 30
) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        displayedText = ""
        text.forEach { char ->
            displayedText += char
            delay(speedMs)
        }
    }

    Text(
        text = displayedText,
        modifier = modifier,
        style = style.copy(fontFamily = FontFamily.Monospace),
        color = color
    )
}
