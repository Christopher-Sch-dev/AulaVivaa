package cl.duocuc.aulaviva.presentation.ui.clases.compose

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
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
                snackbarHostState.showSnackbar("ERROR DE CARGA: CLASE NO ENCONTRADA")
            }
        }
    }

    val clase = claseActual ?: return

    Scaffold(
        snackbarHost = { cl.duocuc.aulaviva.presentation.ui.common.CyberSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("DETALLE DE CLASE", fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
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
                    Brush.verticalGradient(
                        colors = listOf(
                             MaterialTheme.colorScheme.background,
                             MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Título
                Text(
                    text = clase.nombre.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Fecha
                Text(
                    text = "FECHA: ${clase.fecha}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 20.dp),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                // Card descripción
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DESCRIPCIÓN",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = clase.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "MATERIAL DE ESTUDIO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = clase.archivoPdfNombre.ifEmpty { "DOCUMENTO PDF DISPONIBLE" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Button(
                                onClick = {
                                    PdfUtils.abrirPdfExterno(context, clase.archivoPdfUrl)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("VISUALIZAR DOCUMENTO")
                            }
                        }
                    }
                }

                // Botones IA para DOCENTE
                if (!esAlumno) {
                    Text(
                        text = "CORE INTELLIGENCE // DOCENTE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(vertical = 16.dp),
                        letterSpacing = 1.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IAButton(
                            icon = Icons.Default.Lightbulb,
                            text = "GENERAR IDEAS",
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(
                                            context,
                                            "IDEAS PRELIMINARES",
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
                            enabled = isLoading == false
                        )

                        IAButton(
                            icon = Icons.Default.Event,
                            text = "SUGERIR ACTIVIDADES",
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(
                                            context,
                                            "ACTIVIDADES SUGERIDAS",
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
                            enabled = isLoading == false
                        )

                        IAButton(
                            icon = Icons.Default.Schedule,
                            text = "ESTRUCTURAR TIEMPO",
                            onClick = { showDuracionDialog = true },
                            enabled = isLoading == false
                        )

                        IAButton(
                            icon = Icons.Default.Summarize,
                            text = "RESUMIR PDF",
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(
                                            context,
                                            "RESUMEN DE CONTENIDO",
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
                            enabled = isLoading == false
                        )

                        IAButton(
                            icon = Icons.Default.PresentToAll,
                            text = "GUÍA DE PRESENTACIÓN",
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(
                                            context,
                                            "GUÍA DE PRESENTACIÓN",
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
                            enabled = isLoading == false
                        )

                        IAButton(
                            icon = Icons.Default.Games,
                            text = "ACTIVIDADES INTERACTIVAS",
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(
                                            context,
                                            "ACTIVIDADES INTERACTIVAS",
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
                            enabled = isLoading == false
                        )
                    }
                } else {
                    // Botones IA para ALUMNO
                    Text(
                        text = "CORE INTELLIGENCE // ALUMNO",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(vertical = 16.dp),
                        letterSpacing = 1.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IAButton(
                            icon = Icons.Default.School,
                            text = "EXPLICAR CONCEPTOS",
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(
                                            context,
                                            "EXPLICACIÓN DE CONCEPTOS",
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
                            enabled = isLoading == false
                        )

                        IAButton(
                            icon = Icons.Default.Assignment,
                            text = "GENERAR EJERCICIOS",
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(
                                            context,
                                            "EJERCICIOS GENERADOS",
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
                            enabled = isLoading == false
                        )

                        IAButton(
                            icon = Icons.Default.Book,
                            text = "CREAR RESUMEN ESTUDIO",
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(
                                            context,
                                            "RESUMEN DE ESTUDIO",
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
                            enabled = isLoading == false
                        )
                    }
                }

                if (isLoading) {
                    cl.duocuc.aulaviva.presentation.ui.common.CyberLoading(
                         modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Dialog duración
    if (showDuracionDialog) {
        AlertDialog(
            onDismissRequest = { showDuracionDialog = false },
            title = { Text("DURACIÓN ESTIMADA", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    listOf("45 MINUTOS", "60 MINUTOS", "90 MINUTOS", "120 MINUTOS").forEach { duracion ->
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
                                            "ESTRUCTURA DE $duracion",
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
                            Text(duracion, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDuracionDialog = false }) {
                    Text("CANCELAR")
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
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.4f),
            contentColor = MaterialTheme.colorScheme.primary
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.5f))
    ) {
        Icon(icon, null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

fun ejecutarIA(
    iaViewModel: IAViewModel,
    lifecycleOwner: LifecycleOwner,
    isLoading: (Boolean) -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
    llamadaIA: () -> androidx.lifecycle.LiveData<Result<String>>
) {
    isLoading(true)
    val live = llamadaIA()
    var observer: androidx.lifecycle.Observer<Result<String>>? = null
    observer = androidx.lifecycle.Observer<Result<String>> { result ->
        isLoading(false)
        if (result.isSuccess) {
            onSuccess(result.getOrNull() ?: "")
        } else {
            onError(result.exceptionOrNull()?.message ?: "ERROR DESCONOCIDO")
        }
        observer?.let { live.removeObserver(it) }
    }
    live.observe(lifecycleOwner, observer)
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
