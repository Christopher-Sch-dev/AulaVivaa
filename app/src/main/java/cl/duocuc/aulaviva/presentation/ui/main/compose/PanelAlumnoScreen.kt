package cl.duocuc.aulaviva.presentation.ui.main.compose

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.presentation.activity.AlumnoClasesActivity
import cl.duocuc.aulaviva.presentation.ui.auth.compose.LoginActivityCompose
import cl.duocuc.aulaviva.presentation.viewmodel.AlumnoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelAlumnoScreen(
    viewModel: AlumnoViewModel = viewModel()
) {
    val context = LocalContext.current
    val asignaturas by viewModel.asignaturasInscritas.collectAsStateWithLifecycle(initialValue = emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val inscripcionExitosa by viewModel.inscripcionExitosa.collectAsStateWithLifecycle()
    val logoutEvent by viewModel.logoutEvent.collectAsStateWithLifecycle()

    var showInscripcionDialog by remember { mutableStateOf(false) }
    var codigoInscripcion by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Manejar logout
    LaunchedEffect(logoutEvent) {
        if (logoutEvent == true) {
            val intent = Intent(context, LoginActivityCompose::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    // Mostrar errores
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Mostrar éxito de inscripción
    LaunchedEffect(inscripcionExitosa) {
        inscripcionExitosa?.let {
            snackbarHostState.showSnackbar("✓ Inscrito exitosamente en ${it.nombre}")
            showInscripcionDialog = false
            codigoInscripcion = ""
        }
    }

    // Sincronizar al inicio
    LaunchedEffect(Unit) {
        viewModel.sincronizarAsignaturasInscritas()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Panel Alumno") },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.ExitToApp, "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showInscripcionDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Inscribirse con código")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && asignaturas.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (asignaturas.isEmpty()) {
                EmptyState(
                    onInscribirseClick = { showInscripcionDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(asignaturas) { asignatura ->
                        AsignaturaCard(
                            asignatura = asignatura,
                            onClick = {
                                val intent = Intent(context, AlumnoClasesActivity::class.java)
                                intent.putExtra("ASIGNATURA_ID", asignatura.id)
                                intent.putExtra("ASIGNATURA_NOMBRE", asignatura.nombre)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }

    // Dialog de inscripción
    if (showInscripcionDialog) {
        AlertDialog(
            onDismissRequest = { showInscripcionDialog = false },
            title = { Text("Inscribirse con código") },
            text = {
                OutlinedTextField(
                    value = codigoInscripcion,
                    onValueChange = { codigoInscripcion = it },
                    label = { Text("Código de acceso") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (codigoInscripcion.isNotBlank()) {
                            viewModel.inscribirseConCodigo(codigoInscripcion.trim())
                        }
                    }
                ) {
                    Text("Inscribirse")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInscripcionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EmptyState(
    onInscribirseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(32.dp)
    ) {
        Text(
            text = "📚",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tienes asignaturas inscritas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Inscríbete con un código de acceso para comenzar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onInscribirseClick) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Inscribirse con código")
        }
    }
}

@Composable
fun AsignaturaCard(
    asignatura: Asignatura,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = asignatura.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (asignatura.descripcion.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = asignatura.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Código: ${asignatura.codigoAcceso}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

