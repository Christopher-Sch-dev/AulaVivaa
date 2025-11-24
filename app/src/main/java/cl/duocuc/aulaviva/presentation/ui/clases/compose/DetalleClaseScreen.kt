package cl.duocuc.aulaviva.presentation.ui.clases.compose

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.presentation.ui.ia.compose.ResultadoIAActivityCompose
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel
import cl.duocuc.aulaviva.presentation.viewmodel.IAViewModel
import cl.duocuc.aulaviva.utils.PdfUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleClaseScreen(
    claseId: String,
    esAlumno: Boolean = false,
    claseViewModel: ClaseViewModel = viewModel(),
    iaViewModel: IAViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var claseActual by remember { mutableStateOf<Clase?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showDuracionDialog by remember { mutableStateOf(false) }

    // Cargar clase
    LaunchedEffect(claseId) {
        scope.launch {
            claseViewModel.obtenerClasePorId(claseId)?.let {
                claseActual = it
            } ?: run {
                snackbarHostState.showSnackbar("No se pudo cargar la clase")
            }
        }
    }

    val clase = claseActual ?: return

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Clase") },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Título
            Text(
                text = clase.nombre,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Fecha
            Text(
                text = "Fecha: ${clase.fecha}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Card descripción
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📝 Descripción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = clase.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card PDF
            if (clase.archivoPdfUrl.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📄 Material PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = clase.archivoPdfNombre.ifEmpty { "Material disponible" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    PdfUtils.abrirPdfExterno(context, clase.archivoPdfUrl)
                                }
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ver PDF")
                            }
                            if (!esAlumno) {
                                Button(
                                    onClick = {
                                        ejecutarIA(
                                            iaViewModel = iaViewModel,
                                            lifecycleOwner = lifecycleOwner,
                                            isLoading = { isLoading = it },
                                            onSuccess = { contenido ->
                                                mostrarResultadoIA(
                                                    context,
                                                    "📄 Análisis del material PDF",
                                                    contenido,
                                                    clase
                                                )
                                            },
                                            onError = { error ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(error)
                                                }
                                            }
                                        ) {
                                            iaViewModel.analizarPdfConIA(clase.nombre, clase.archivoPdfUrl)
                                        }
                                    },
                                    enabled = !isLoading
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Analytics, null)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analizar PDF")
                                }
                            }
                        }
                    }
                }
            }

            // Botones IA para DOCENTE
            if (!esAlumno) {
                Text(
                    text = "🤖 Funciones IA para Docente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IAButton(
                        icon = Icons.Default.Lightbulb,
                        text = "💡 Generar Ideas",
                        onClick = {
                            ejecutarIA(
                                iaViewModel = iaViewModel,
                                lifecycleOwner = lifecycleOwner,
                                isLoading = { isLoading = it },
                                onSuccess = { contenido ->
                                    mostrarResultadoIA(
                                        context,
                                        "💡 Ideas para tu clase",
                                        contenido,
                                        clase,
                                        "IDEAS"
                                    )
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            ) {
                                iaViewModel.generarIdeasParaClase(
                                    clase.nombre,
                                    clase.descripcion,
                                    clase.archivoPdfUrl
                                )
                            }
                        },
                        enabled = !isLoading
                    )

                    IAButton(
                        icon = Icons.Default.Event,
                        text = "🎯 Sugerir Actividades",
                        onClick = {
                            ejecutarIA(
                                iaViewModel = iaViewModel,
                                lifecycleOwner = lifecycleOwner,
                                isLoading = { isLoading = it },
                                onSuccess = { contenido ->
                                    mostrarResultadoIA(
                                        context,
                                        "🎯 Actividades sugeridas",
                                        contenido,
                                        clase,
                                        "IDEAS"
                                    )
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            ) {
                                iaViewModel.sugerirActividades(
                                    clase.nombre,
                                    clase.descripcion,
                                    clase.archivoPdfUrl
                                )
                            }
                        },
                        enabled = !isLoading
                    )

                    IAButton(
                        icon = Icons.Default.Schedule,
                        text = "⏱️ Estructurar por Tiempo",
                        onClick = { showDuracionDialog = true },
                        enabled = !isLoading
                    )

                    IAButton(
                        icon = Icons.Default.Summarize,
                        text = "📝 Resumir PDF",
                        onClick = {
                            ejecutarIA(
                                iaViewModel = iaViewModel,
                                lifecycleOwner = lifecycleOwner,
                                isLoading = { isLoading = it },
                                onSuccess = { contenido ->
                                    mostrarResultadoIA(
                                        context,
                                        "📝 Resumen del contenido",
                                        contenido,
                                        clase
                                    )
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            ) {
                                iaViewModel.resumirContenidoPdf(
                                    clase.nombre,
                                    clase.descripcion,
                                    clase.archivoPdfUrl
                                )
                            }
                        },
                        enabled = !isLoading
                    )

                    IAButton(
                        icon = Icons.Default.PresentationChart,
                        text = "🎤 Guía de Presentación",
                        onClick = {
                            ejecutarIA(
                                iaViewModel = iaViewModel,
                                lifecycleOwner = lifecycleOwner,
                                isLoading = { isLoading = it },
                                onSuccess = { contenido ->
                                    mostrarResultadoIA(
                                        context,
                                        "🎤 Guía de Presentación",
                                        contenido,
                                        clase
                                    )
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            ) {
                                iaViewModel.generarGuiaPresentacion(
                                    clase.nombre,
                                    clase.descripcion,
                                    clase.archivoPdfUrl
                                )
                            }
                        },
                        enabled = !isLoading
                    )

                    IAButton(
                        icon = Icons.Default.Games,
                        text = "🎮 Actividades Interactivas",
                        onClick = {
                            ejecutarIA(
                                iaViewModel = iaViewModel,
                                lifecycleOwner = lifecycleOwner,
                                isLoading = { isLoading = it },
                                onSuccess = { contenido ->
                                    mostrarResultadoIA(
                                        context,
                                        "🎮 Actividades Interactivas",
                                        contenido,
                                        clase
                                    )
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            ) {
                                iaViewModel.generarActividadesInteractivas(
                                    clase.nombre,
                                    clase.descripcion,
                                    clase.archivoPdfUrl
                                )
                            }
                        },
                        enabled = !isLoading
                    )
                }
            } else {
                // Botones IA para ALUMNO
                Text(
                    text = "🤖 Funciones IA para Alumno",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IAButton(
                        icon = Icons.Default.School,
                        text = "📚 Explicar Conceptos",
                        onClick = {
                            ejecutarIA(
                                iaViewModel = iaViewModel,
                                lifecycleOwner = lifecycleOwner,
                                isLoading = { isLoading = it },
                                onSuccess = { contenido ->
                                    mostrarResultadoIA(
                                        context,
                                        "📚 Explicación de Conceptos",
                                        contenido,
                                        clase
                                    )
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            ) {
                                iaViewModel.explicarConceptosParaAlumno(
                                    clase.nombre,
                                    clase.descripcion,
                                    clase.archivoPdfUrl
                                )
                            }
                        },
                        enabled = !isLoading
                    )

                    IAButton(
                        icon = Icons.Default.Assignment,
                        text = "✏️ Generar Ejercicios",
                        onClick = {
                            ejecutarIA(
                                iaViewModel = iaViewModel,
                                lifecycleOwner = lifecycleOwner,
                                isLoading = { isLoading = it },
                                onSuccess = { contenido ->
                                    mostrarResultadoIA(
                                        context,
                                        "✏️ Ejercicios Generados",
                                        contenido,
                                        clase
                                    )
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            ) {
                                iaViewModel.generarEjerciciosParaAlumno(
                                    clase.nombre,
                                    clase.descripcion,
                                    clase.archivoPdfUrl
                                )
                            }
                        },
                        enabled = !isLoading
                    )

                    IAButton(
                        icon = Icons.Default.Book,
                        text = "📖 Crear Resumen de Estudio",
                        onClick = {
                            ejecutarIA(
                                iaViewModel = iaViewModel,
                                lifecycleOwner = lifecycleOwner,
                                isLoading = { isLoading = it },
                                onSuccess = { contenido ->
                                    mostrarResultadoIA(
                                        context,
                                        "📖 Resumen de Estudio",
                                        contenido,
                                        clase
                                    )
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            ) {
                                iaViewModel.crearResumenEstudioParaAlumno(
                                    clase.nombre,
                                    clase.descripcion,
                                    clase.archivoPdfUrl
                                )
                            }
                        },
                        enabled = !isLoading
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Dialog duración
    if (showDuracionDialog) {
        AlertDialog(
            onDismissRequest = { showDuracionDialog = false },
            title = { Text("⏱️ Duración de la clase") },
            text = {
                Column {
                    listOf("45 minutos", "60 minutos", "90 minutos", "120 minutos").forEach { duracion ->
                        TextButton(
                            onClick = {
                                showDuracionDialog = false
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(
                                            context,
                                            "⏱️ Estructura de $duracion",
                                            contenido,
                                            clase
                                        )
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(error)
                                        }
                                    }
                                ) {
                                    iaViewModel.estructurarClasePorTiempo(
                                        clase.nombre,
                                        clase.descripcion,
                                        duracion,
                                        clase.archivoPdfUrl
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(duracion)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDuracionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun IAButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(icon, null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ejecutarIA(
    iaViewModel: IAViewModel,
    lifecycleOwner: LifecycleOwner,
    isLoading: (Boolean) -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
    llamadaIA: () -> androidx.lifecycle.LiveData<Result<String>>
) {
    DisposableEffect(Unit) {
        isLoading(true)
        val live = llamadaIA()
        val observer = Observer<Result<String>> { result ->
            isLoading(false)
            if (result.isSuccess) {
                onSuccess(result.getOrNull() ?: "")
            } else {
                onError(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
        live.observe(lifecycleOwner, observer)
        onDispose {
            live.removeObserver(observer)
        }
    }
}

fun mostrarResultadoIA(
    context: android.content.Context,
    titulo: String,
    contenido: String,
    clase: Clase,
    tipoConsulta: String = ""
) {
    val intent = Intent(context, ResultadoIAActivityCompose::class.java)
    intent.putExtra("TITULO", titulo)
    intent.putExtra("CONTENIDO", contenido)
    intent.putExtra("TIPO_CONSULTA", tipoConsulta)
    intent.putExtra("NOMBRE_CLASE", clase.nombre)
    intent.putExtra("DESCRIPCION_CLASE", clase.descripcion)
    intent.putExtra("PDF_URL", clase.archivoPdfUrl)
    context.startActivity(intent)
}
