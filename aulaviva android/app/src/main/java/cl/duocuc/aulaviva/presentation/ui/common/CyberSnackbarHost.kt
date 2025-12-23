package cl.duocuc.aulaviva.presentation.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaColors

@Composable
fun CyberSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        if (data.visuals.message.isNotBlank()) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                contentColor = AulaVivaColors.PrimaryCyan,
                containerColor = AulaVivaColors.SurfaceDark,
                shape = RoundedCornerShape(4.dp),
                // Border effect simulated via container color for now, or could use a specific Surface
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AulaVivaColors.TextPrimary
                )
            }
        }
    }
}
