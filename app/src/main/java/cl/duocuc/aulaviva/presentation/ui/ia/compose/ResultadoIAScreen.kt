package cl.duocuc.aulaviva.presentation.ui.ia.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import cl.duocuc.aulaviva.presentation.viewmodel.IAViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoIAScreen(
    titulo: String,
    contenidoInicial: String,
    tipoConsulta: String = "",
    nombreClase: String = "",
    descripcionClase: String = "",
    pdfUrl: String = "",
    iaViewModel: IAViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var mensajesRestantes by remember { mutableStateOf(3) }
    var mensajeTexto by remember { mutableStateOf("") }
    var mensajes by remember { mutableStateOf<List<MensajeChat>>(listOf()) }
    var isLoading by remember { mutableStateOf(false) }

    // Agregar mensaje inicial de IA
    LaunchedEffect(Unit) {
        mensajes = listOf(MensajeChat(esUsuario = false, contenido = contenidoInicial))

        // Inicializar sesión de chat
        iaViewModel.iniciarChatConContexto(
            nombreClase = nombreClase,
            descripcion = descripcionClase,
            pdfUrl = if (pdfUrl.isEmpty()) null else pdfUrl,
            respuestaInicial = contenidoInicial
        ).observe(lifecycleOwner) { result ->
            if (result.isFailure) {
                scope.launch {
                    snackbarHostState.showSnackbar("ERROR INICIANDO CORE IA: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { cl.duocuc.aulaviva.presentation.ui.common.CyberSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(titulo.uppercase(), fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }, // Uppercase & Spacing
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? android.app.Activity)?.finish()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // Cyber Gradient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                             MaterialTheme.colorScheme.background,
                             MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Chat messages
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp) // More space between bubbles
                ) {
                    mensajes.forEach { mensaje ->
                        MensajeChatCard(mensaje = mensaje)
                    }
                    if (isLoading) {
                        cl.duocuc.aulaviva.presentation.ui.common.CyberLoading(
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(16.dp),
                            size = 32.dp
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))

                // Contador de mensajes
                Text(
                    text = if (mensajesRestantes > 0) {
                        "// SYSTEM STATUS: $mensajesRestantes MENSAJES DISPONIBLES"
                    } else {
                        "// SYSTEM WARNING: LÍMITE DE PROCESAMIENTO ALCANZADO"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mensajesRestantes > 0) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                // Input de mensaje
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = mensajeTexto,
                        onValueChange = { mensajeTexto = it },
                        modifier = Modifier.weight(1f),
                        enabled = mensajesRestantes > 0 && !isLoading,
                        placeholder = { Text("INGRESAR COMANDO DE CONSULTA...", style = MaterialTheme.typography.bodySmall) },
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    )
                    IconButton(
                        onClick = {
                            if (mensajeTexto.isNotBlank() && mensajesRestantes > 0 && !isLoading) {
                                val mensaje = mensajeTexto
                                mensajeTexto = ""
                                isLoading = true
                                mensajesRestantes--

                                // Agregar mensaje del usuario
                                mensajes = mensajes + MensajeChat(esUsuario = true, contenido = mensaje)

                                // Enviar a IA
                                val live = iaViewModel.enviarMensajeChat(mensaje)
                                var observer: androidx.lifecycle.Observer<Result<String>>? = null
                                observer = androidx.lifecycle.Observer<Result<String>> { result ->
                                    isLoading = false
                                    if (result.isSuccess) {
                                        val respuesta = result.getOrNull() ?: ""
                                        mensajes = mensajes + MensajeChat(esUsuario = false, contenido = respuesta)
                                    } else {
                                        mensajesRestantes++
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "ERROR: ${result.exceptionOrNull()?.message}"
                                            )
                                        }
                                    }
                                    observer?.let { live.removeObserver(it) }
                                }
                                live.observe(lifecycleOwner, observer)
                            }
                        },
                        enabled = mensajeTexto.isNotBlank() && mensajesRestantes > 0 && isLoading == false
                    ) {
                        Icon(Icons.Default.Send, "ENVIAR", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

data class MensajeChat(
    val esUsuario: Boolean,
    val contenido: String
)

@Composable
fun MensajeChatCard(mensaje: MensajeChat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mensaje.esUsuario) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = if (mensaje.esUsuario) 280.dp else 320.dp) 
                .fillMaxWidth(0.85f), 
            shape = if (mensaje.esUsuario) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            } else {
                RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            },
            colors = CardDefaults.cardColors(
                containerColor = if (mensaje.esUsuario) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.8f) 
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f) 
                }
            ),
            border = if (mensaje.esUsuario) {
                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.5f))
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))
            },
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) 
        ) {
            Column(
                modifier = Modifier.padding(16.dp) 
            ) {
                Text(
                    text = if (mensaje.esUsuario) "USUARIO // AUTHENTICATED" else "AULA VIVA // CORE AI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (mensaje.esUsuario) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                // ✅ Renderizar Markdown para respuestas de IA
                if (mensaje.esUsuario) {
                    Text(
                        text = mensaje.contenido,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    // ✅ Usar Markwon para renderizar Markdown (como antes)
                     cl.duocuc.aulaviva.presentation.ui.common.MarkdownText( // Keep full path to ensure resolution if imports missing
                        text = mensaje.contenido,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

