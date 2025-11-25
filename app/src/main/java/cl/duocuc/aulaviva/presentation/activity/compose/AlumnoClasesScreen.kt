package cl.duocuc.aulaviva.presentation.activity.compose

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.SwipeRefresh
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.presentation.ui.clases.compose.DetalleClaseActivityCompose
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumnoClasesScreen(
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
        if (asignaturaId.isNotEmpty()) {
            viewModel.sincronizarClasesPorAsignatura(asignaturaId)
        }
    }

    // Manejar pull-to-refresh
    fun onRefresh() {
        isRefreshing.value = true
        if (asignaturaId.isNotEmpty()) {
            viewModel.sincronizarClasesPorAsignatura(asignaturaId)
        }
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
                        Text("Clases", style = MaterialTheme.typography.bodySmall)
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
                EmptyStateClasesAlumno(
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
                        ClaseAlumnoCard(
                            clase = clase,
                            onClick = {
                                val intent = Intent(context, DetalleClaseActivityCompose::class.java)
                                intent.putExtra("CLASE_ID", clase.id)
                                intent.putExtra("ES_ALUMNO", true)
                                context.startActivity(intent)
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
fun EmptyStateClasesAlumno(
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
            "No hay clases disponibles",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "El docente aún no ha creado clases para esta asignatura",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ClaseAlumnoCard(
    clase: Clase,
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
            if (clase.archivoPdfUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📄 ${clase.archivoPdfNombre.ifEmpty { "Material PDF disponible" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

