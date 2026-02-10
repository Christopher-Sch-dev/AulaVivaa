package cl.duocuc.aulaviva.presentation.ui.utils

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale

/**
 * Image loader con disk cache + memory cache
 * Fuente: https://coil-kt.github.io/coil/compose/
 */
@Composable
fun OptimizedImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .scale(Scale.FIT) // OPTIMIZACIÓN: No cargar full res
            .memoryCacheKey(url) // Cache en memoria
            .diskCacheKey(url) // Cache en disco
            .build()
    )
    
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
