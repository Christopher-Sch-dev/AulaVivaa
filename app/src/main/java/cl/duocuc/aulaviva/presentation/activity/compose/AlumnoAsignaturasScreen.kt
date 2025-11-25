package cl.duocuc.aulaviva.presentation.activity.compose

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.SwipeRefresh
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.presentation.activity.compose.AlumnoClasesActivityCompose
import cl.duocuc.aulaviva.presentation.viewmodel.AlumnoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumnoAsignaturasScreen(
    viewModel: AlumnoViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val asignaturas: List<Asignatura> by viewModel.asignaturasInscritas.observeAsState(emptyList())
    val isLoading: Boolean by viewModel.isLoading.observeAsState(false)
    val snackbarHostState = remember { SnackbarHostState() }
    val isRefreshing = remember { mutableStateOf(false) }

    var showCodigoDialog by remember { mutableStateOf(false) }
    var codigoIngresado by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.sincronizarAsignaturasInscritas()
    }

    // Manejar pull-to-refresh
    fun onRefresh() {
        isRefreshing.value = true
        viewModel.sincronizarAsignaturasInscritas()
        scope.launch {
            kotlinx.coroutines.delay(500)
            isRefreshing.value = false
            snackbarHostState.showSnackbar("✓ Datos actualizados")
        }
    }

    val error: String? by viewModel.error.observeAsState()
    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
            viewModel.limpiarError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mis Asignaturas") },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCodigoDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Agregar con código")
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
                EmptyStateAlumno(
                    onAgregarClick = { showCodigoDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                SwipeRefresh(
                    onRefresh = { onRefresh() },
                    refreshing = isRefreshing.value
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    items(asignaturas) { asignatura ->
                        AsignaturaAlumnoCard(
                            asignatura = asignatura,
                            onVerClases = {
                                val intent = Intent(context, AlumnoClasesActivityCompose::class.java)
                                intent.putExtra("ASIGNATURA_ID", asignatura.id)
                                intent.putExtra("ASIGNATURA_NOMBRE", asignatura.nombre)
                                context.startActivity(intent)
                            },
                            onDarDeBaja = {
                                viewModel.darDeBaja(asignatura.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("✓ Te has dado de baja de ${asignatura.nombre}")
                                }
                            }
                        )
                    }
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

    // Dialog código
    if (showCodigoDialog) {
        AlertDialog(
            onDismissRequest = { showCodigoDialog = false },
            title = { Text("Inscribirse con código") },
            text = {
                OutlinedTextField(
                    value = codigoIngresado,
                    onValueChange = { codigoIngresado = it },
                    label = { Text("Código de acceso") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (codigoIngresado.isNotBlank()) {
                            viewModel.inscribirConCodigo(codigoIngresado.trim())
                            showCodigoDialog = false
                            codigoIngresado = ""
                        }
                    }
                ) {
                    Text("Inscribirse")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodigoDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EmptyStateAlumno(
    onAgregarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
            "Agrega una asignatura con un código de acceso",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAgregarClick) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar con código")
        }
    }
}

@Composable
fun AsignaturaAlumnoCard(
    asignatura: Asignatura,
    onVerClases: () -> Unit,
    onDarDeBaja: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onVerClases,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "Opciones")
            }
        }
    }

    Box {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
        DropdownMenuItem(
            text = { Text("Ver clases") },
            onClick = {
                onVerClases()
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Default.MenuBook, null) }
        )
        DropdownMenuItem(
            text = { Text("Dar de baja", color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDarDeBaja()
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        )
        }
    }
}

