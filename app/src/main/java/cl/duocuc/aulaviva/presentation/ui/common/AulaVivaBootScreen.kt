package cl.duocuc.aulaviva.presentation.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaColors
import kotlinx.coroutines.delay

/**
 * AulaVivaBootScreen: Pantalla de bienvenida animada al iniciar la app.
 * 
 * Diseño estilo terminal/cyberpunk con:
 * - Texto de bienvenida con efecto typewriter
 * - Animaciones de entrada suaves
 * - Barra de progreso estilizada
 * - Mensaje de estado cambiante
 * 
 * USOS:
 * - Como splash screen animado al iniciar la app
 * - Como preloader entre login y panel principal
 * 
 * @param onLoadComplete Callback cuando la animación termina
 * @param loadingMessages Lista de mensajes que se mostrarán secuencialmente
 * @param title Título principal (default: "AULA VIVA")
 * @param subtitle Subtítulo (default: "SISTEMA EDUCATIVO v2.0")
 */
@Composable
fun AulaVivaBootScreen(
    onLoadComplete: () -> Unit,
    loadingMessages: List<String> = listOf(
        "Inicializando sistema...",
        "Conectando con servidor...",
        "Cargando configuración...",
        "Verificando credenciales...",
        "Preparando interfaz...",
        "Sistema listo."
    ),
    title: String = "AULA VIVA",
    subtitle: String = "SISTEMA EDUCATIVO v2.0"
) {
    var currentMessageIndex by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var showTitle by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }
    
    // Animación de entrada del título
    val titleAlpha by animateFloatAsState(
        targetValue = if (showTitle) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )
    
    // Animación de entrada del subtítulo
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (showSubtitle) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "subtitleAlpha"
    )
    
    // Animación de la barra de progreso
    val progressAlpha by animateFloatAsState(
        targetValue = if (showProgress) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "progressAlpha"
    )
    
    // Animación de parpadeo del cursor
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )
    
    // Efecto de carga secuencial
    LaunchedEffect(Unit) {
        // Mostrar título
        delay(200)
        showTitle = true
        
        // Mostrar subtítulo
        delay(400)
        showSubtitle = true
        
        // Mostrar barra de progreso
        delay(300)
        showProgress = true
        
        // Progreso y mensajes
        val messageDelay = 400L
        val progressStep = 1f / loadingMessages.size
        
        for (i in loadingMessages.indices) {
            currentMessageIndex = i
            delay(messageDelay)
            progress = (i + 1) * progressStep
        }
        
        // Pequeña pausa antes de completar
        delay(300)
        onLoadComplete()
    }
    
    // UI
    AulaVivaScreenFrame(mode = ScreenEffectMode.SPLASH) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Título principal con efecto typewriter
            Text(
                text = "[ $title ]",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AulaVivaColors.PrimaryCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .padding(bottom = 8.dp)
            )
            
            // Subtítulo
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                color = AulaVivaColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(subtitleAlpha)
                    .padding(bottom = 48.dp)
            )
            
            // Barra de progreso estilizada
            Column(
                modifier = Modifier
                    .alpha(progressAlpha)
                    .fillMaxWidth(0.8f),
                horizontalAlignment = Alignment.Start
            ) {
                // Mensaje de estado actual
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "> ",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AulaVivaColors.PrimaryCyan
                    )
                    Text(
                        text = if (currentMessageIndex < loadingMessages.size) {
                            loadingMessages[currentMessageIndex]
                        } else {
                            "Listo."
                        },
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AulaVivaColors.TextPrimary
                    )
                    // Cursor parpadeante
                    Text(
                        text = "_",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AulaVivaColors.PrimaryCyan,
                        modifier = Modifier.alpha(cursorAlpha)
                    )
                }
                
                // Barra de progreso visual
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(AulaVivaColors.SurfaceDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(AulaVivaColors.PrimaryCyan)
                    )
                }
                
                // Porcentaje
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AulaVivaColors.TextSecondary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1.5f))
            
            // Footer
            Text(
                text = "DEV-CHRIS.sch | DUOC UC 2025",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = AulaVivaColors.TextSecondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * AulaVivaLoadingOverlay: Overlay de carga para operaciones async.
 * 
 * Diseño semi-transparente que se superpone al contenido existente.
 * Útil para:
 * - Carga de datos del servidor
 * - Procesamiento de IA
 * - Generación de PDFs
 * 
 * @param message Mensaje a mostrar durante la carga
 * @param isVisible Si el overlay debe mostrarse
 */
@Composable
fun AulaVivaLoadingOverlay(
    message: String = "Cargando...",
    isVisible: Boolean = true
) {
    if (!isVisible) return
    
    // Animación de parpadeo
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AulaVivaColors.BackgroundDark.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicador de carga estilo terminal
            Text(
                text = "[ PROCESANDO ]",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AulaVivaColors.PrimaryCyan,
                modifier = Modifier.alpha(alpha)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mensaje
            Text(
                text = "> $message",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = AulaVivaColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Spinner animado con caracteres
            AnimatedLoadingSpinner()
        }
    }
}

/**
 * AnimatedLoadingSpinner: Spinner estilo terminal con caracteres rotativos.
 */
@Composable
private fun AnimatedLoadingSpinner() {
    val spinnerChars = listOf("|", "/", "-", "\\")
    var currentIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            currentIndex = (currentIndex + 1) % spinnerChars.size
        }
    }
    
    Text(
        text = "[${spinnerChars[currentIndex]}]",
        fontSize = 24.sp,
        fontFamily = FontFamily.Monospace,
        color = AulaVivaColors.PrimaryCyan
    )
}
