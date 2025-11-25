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
                    snackbarHostState.showSnackbar("Error iniciando chat: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? android.app.Activity)?.finish()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat messages
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mensajes.forEach { mensaje ->
                    MensajeChatCard(mensaje = mensaje)
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(16.dp)
                    )
                }
            }

            Divider()

            // Contador de mensajes
            Text(
                text = if (mensajesRestantes > 0) {
                    "💬 Puedes enviar $mensajesRestantes mensajes más"
                } else {
                    "⚠️ Has alcanzado el límite de mensajes"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (mensajesRestantes > 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    placeholder = { Text("Escribe tu mensaje a la IA...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
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
                                            "Error: ${result.exceptionOrNull()?.message}"
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
                    Icon(Icons.Default.Send, "Enviar")
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
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (mensaje.esUsuario) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (mensaje.esUsuario) "👤 Tú:" else "🤖 AulaViva IA:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (mensaje.esUsuario) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                // ✅ Renderizar Markdown para respuestas de IA
                if (mensaje.esUsuario) {
                    Text(
                        text = mensaje.contenido,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    cl.duocuc.aulaviva.presentation.ui.common.MarkdownText(
                        text = mensaje.contenido,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

