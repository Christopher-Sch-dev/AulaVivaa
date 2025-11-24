package cl.duocuc.aulaviva.presentation.activity.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.presentation.activity.compose.DocenteClasesActivityCompose
import cl.duocuc.aulaviva.presentation.activity.compose.InscritosActivityCompose
import cl.duocuc.aulaviva.presentation.viewmodel.AsignaturasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocenteAsignaturasScreen(
    viewModel: AsignaturasViewModel = viewModel()
) {
    val context = LocalContext.current
    val asignaturas by viewModel.asignaturas.collectAsStateWithLifecycle(initialValue = emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCrearDialog by remember { mutableStateOf(false) }
    var nombreAsignatura by remember { mutableStateOf("") }
    var descripcionAsignatura by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.sincronizarAsignaturas()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mis Asignaturas") },
                navigationIcon = {
                    IconButton(onClick = { /* finish */ }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
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
                onClick = { showCrearDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Crear asignatura")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && asignaturas.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (asignaturas.isEmpty()) {
                EmptyState(
                    onCreateClick = { showCrearDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(asignaturas) { asignatura ->
                        AsignaturaDocenteCard(
                            asignatura = asignatura,
                            onVerClases = {
                                val intent = Intent(context, DocenteClasesActivityCompose::class.java)
                                intent.putExtra("ASIGNATURA_ID", asignatura.id)
                                intent.putExtra("ASIGNATURA_NOMBRE", asignatura.nombre)
                                context.startActivity(intent)
                            },
                            onVerInscritos = {
                                val intent = Intent(context, InscritosActivityCompose::class.java)
                                intent.putExtra("ASIGNATURA_ID", asignatura.id)
                                intent.putExtra("ASIGNATURA_NOMBRE", asignatura.nombre)
                                context.startActivity(intent)
                            },
                            onCopiarCodigo = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Código", asignatura.codigoAcceso)
                                clipboard.setPrimaryClip(clip)
                                snackbarHostState.showSnackbar("✓ Código copiado: ${asignatura.codigoAcceso}")
                            },
                            onEliminar = {
                                viewModel.eliminarAsignatura(asignatura.id)
                                snackbarHostState.showSnackbar("✓ Asignatura eliminada")
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

    // Dialog crear asignatura
    if (showCrearDialog) {
        AlertDialog(
            onDismissRequest = { showCrearDialog = false },
            title = { Text("Crear Asignatura") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreAsignatura,
                        onValueChange = { nombreAsignatura = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = descripcionAsignatura,
                        onValueChange = { descripcionAsignatura = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombreAsignatura.isNotBlank()) {
                            viewModel.crearAsignatura(nombreAsignatura, descripcionAsignatura)
                            showCrearDialog = false
                            nombreAsignatura = ""
                            descripcionAsignatura = ""
                        }
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCrearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EmptyState(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(32.dp)
    ) {
        Text("📚", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No tienes asignaturas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Crea tu primera asignatura para comenzar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear Asignatura")
        }
    }
}

@Composable
fun AsignaturaDocenteCard(
    asignatura: Asignatura,
    onVerClases: () -> Unit,
    onVerInscritos: () -> Unit,
    onCopiarCodigo: () -> Unit,
    onEliminar: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Opciones")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onVerClases,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Book, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clases")
                }
                OutlinedButton(
                    onClick = onVerInscritos,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.People, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inscritos")
                }
            }
        }
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Copiar código") },
            onClick = {
                onCopiarCodigo()
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
        )
        DropdownMenuItem(
            text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
            onClick = {
                onEliminar()
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        )
    }
}

