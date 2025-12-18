package cl.duocuc.aulaviva.presentation.activity.compose

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.presentation.ui.clases.compose.DetalleClaseActivityCompose
import cl.duocuc.aulaviva.presentation.ui.common.AulaVivaScreenFrame
import cl.duocuc.aulaviva.presentation.ui.common.PullToRefreshContainer
import cl.duocuc.aulaviva.presentation.ui.common.ScreenEffectMode
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel
import cl.duocuc.aulaviva.utils.Constants
import cl.duocuc.aulaviva.R

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

    LaunchedEffect(Unit) {
        if (asignaturaId.isNotEmpty()) {
            viewModel.sincronizarClasesPorAsignatura(asignaturaId)
        }
    }

    // Manejar pull-to-refresh
    val onRefresh: () -> Unit = {
        if (asignaturaId.isNotEmpty()) {
            viewModel.sincronizarClasesPorAsignatura(asignaturaId)
        }
        scope.launch {
            delay(Constants.REFRESH_DELAY_MS)
            snackbarHostState.showSnackbar(context.getString(R.string.msg_datos_actualizados))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(asignaturaNombre)
                        Text(context.getString(R.string.clases), style = MaterialTheme.typography.bodySmall)
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        // Aplicar efectos visuales - Modo LIST: solo scanLines (performance primero en listas)
        AulaVivaScreenFrame(mode = ScreenEffectMode.LIST) {
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
                PullToRefreshContainer(
                    isRefreshing = isLoading,
                    onRefresh = onRefresh
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Constants.PADDING_STANDARD_DP.dp),
                        verticalArrangement = Arrangement.spacedBy(Constants.SPACING_ITEMS_DP.dp)
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
}

@Composable
fun EmptyStateClasesAlumno(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📖", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(Constants.PADDING_STANDARD_DP.dp))
        Text(
            text = context.getString(R.string.no_hay_clases_disponibles),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Constants.PADDING_SMALL_DP.dp))
        Text(
            text = context.getString(R.string.docente_no_creo_clases),
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
    val context = LocalContext.current
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
            Spacer(modifier = Modifier.height(Constants.PADDING_SMALL_DP.dp))
            Text(
                text = context.getString(R.string.lbl_fecha, clase.fecha),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (clase.archivoPdfUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📄 ${clase.archivoPdfNombre.ifEmpty { context.getString(R.string.material_pdf_disponible) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

