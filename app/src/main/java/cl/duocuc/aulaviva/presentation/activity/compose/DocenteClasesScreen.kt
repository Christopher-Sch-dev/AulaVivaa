package cl.duocuc.aulaviva.presentation.activity.compose

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.SwipeRefresh
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
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
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.presentation.ui.clases.compose.CrearEditarClaseActivityCompose
import cl.duocuc.aulaviva.presentation.ui.clases.compose.DetalleClaseActivityCompose
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocenteClasesScreen(
    asignaturaId: String,
    asignaturaNombre: String,
    viewModel: ClaseViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clases: List<Clase> by viewModel.obtenerClasesPorAsignatura(asignaturaId).observeAsState(emptyList())
    val isLoading: Boolean by viewModel.isLoading.observeAsState(false)
    val snackbarHostState = remember { SnackbarHostState() }
    val isRefreshing = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.sincronizarClasesPorAsignatura(asignaturaId)
    }

    // Manejar pull-to-refresh
    fun onRefresh() {
        isRefreshing.value = true
        viewModel.sincronizarClasesPorAsignatura(asignaturaId)
        scope.launch {
            kotlinx.coroutines.delay(500)
            isRefreshing.value = false
            snackbarHostState.showSnackbar("✓ Datos actualizados")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(asignaturaNombre)
                        Text("Gestionar Clases", style = MaterialTheme.typography.bodySmall)
                    }
                },
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
                onClick = {
                    val intent = Intent(context, CrearEditarClaseActivityCompose::class.java)
                    intent.putExtra("ASIGNATURA_ID", asignaturaId)
                    intent.putExtra("ASIGNATURA_NOMBRE", asignaturaNombre)
                    context.startActivity(intent)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Crear clase")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && clases.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (clases.isEmpty()) {
                EmptyStateClases(
                    onCreateClick = {
                        val intent = Intent(context, CrearEditarClaseActivityCompose::class.java)
                        intent.putExtra("ASIGNATURA_ID", asignaturaId)
                        intent.putExtra("ASIGNATURA_NOMBRE", asignaturaNombre)
                        context.startActivity(intent)
                    },
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
                    items(clases) { clase ->
                        ClaseCard(
                            clase = clase,
                            onClaseClick = {
                                val intent = Intent(context, DetalleClaseActivityCompose::class.java)
                                intent.putExtra("CLASE_ID", clase.id)
                                intent.putExtra("ES_ALUMNO", false)
                                context.startActivity(intent)
                            },
                            onEditar = {
                                val intent = Intent(context, CrearEditarClaseActivityCompose::class.java)
                                intent.putExtra("CLASE_ID", clase.id)
                                intent.putExtra("ASIGNATURA_ID", asignaturaId)
                                intent.putExtra("ASIGNATURA_NOMBRE", asignaturaNombre)
                                context.startActivity(intent)
                            },
                            onEliminar = {
                                viewModel.eliminarClase(clase.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("✓ Clase eliminada")
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
}

@Composable
fun EmptyStateClases(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📖", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No hay clases",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Crea tu primera clase para comenzar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear Clase")
        }
    }
}

@Composable
fun ClaseCard(
    clase: Clase,
    onClaseClick: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClaseClick,
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
                    text = clase.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (clase.descripcion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = clase.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fecha: ${clase.fecha}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
            text = { Text("Editar") },
            onClick = {
                onEditar()
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Default.Edit, null) }
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
}

