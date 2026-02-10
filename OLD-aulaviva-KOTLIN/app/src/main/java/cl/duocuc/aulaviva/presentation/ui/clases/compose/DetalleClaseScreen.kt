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
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaColors
import cl.duocuc.aulaviva.presentation.ui.components.CyberButton
import cl.duocuc.aulaviva.presentation.ui.components.CyberButtonVariant
import cl.duocuc.aulaviva.presentation.ui.components.CyberCard
import cl.duocuc.aulaviva.presentation.ui.common.CyberSnackbarHost
import cl.duocuc.aulaviva.presentation.ui.common.GlitchText
import cl.duocuc.aulaviva.presentation.ui.effects.aggressiveScanLines
import cl.duocuc.aulaviva.presentation.ui.effects.breakcoreGlitch
import cl.duocuc.aulaviva.presentation.ui.effects.cyberGrid

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
                snackbarHostState.showSnackbar("ERROR SISTEMA: DATA NO ENCONTRADA")
            }
        }
    }

    val clase = claseActual ?: return

    Scaffold(
        snackbarHost = { CyberSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                     GlitchText(
                        text = "DETALLE DE CLASE", 
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? android.app.Activity)?.finish()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = AulaVivaColors.PrimaryCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AulaVivaColors.CyberBlack.copy(alpha = 0.9f),
                    titleContentColor = AulaVivaColors.PrimaryCyan
                )
            )
        },
        containerColor = AulaVivaColors.CyberBlack
    ) { paddingValues ->
        
        // Breakcore Grid Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AulaVivaColors.CyberBlack)
                .cyberGrid()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .aggressiveScanLines() // Effect overlay on content
                    .padding(20.dp)
            ) {
                // Título
                Text(
                    text = clase.nombre.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AulaVivaColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Fecha
                Text(
                    text = "FECHA: ${clase.fecha}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AulaVivaColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 20.dp),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                // Card descripción
                CyberCard(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = AulaVivaColors.SecondaryAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DESCRIPCIÓN",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp),
                                color = AulaVivaColors.SecondaryAccent
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AulaVivaColors.PrimaryCyan.copy(alpha = 0.3f))
                        Text(
                            text = clase.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AulaVivaColors.TextPrimary
                        )
                    }
                }

                // Card PDF
                if (clase.archivoPdfUrl.isNotEmpty()) {
                    CyberCard(
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Column {
                            Text(
                                text = "MATERIAL DE ESTUDIO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp),
                                color = AulaVivaColors.PrimaryCyan
                            )
                            Text(
                                text = clase.archivoPdfNombre.ifEmpty { "DOCUMENTO PDF DISPONIBLE" },
                                style = MaterialTheme.typography.bodySmall,
                                color = AulaVivaColors.TextSecondary,
                                modifier = Modifier.padding(bottom = 12.dp),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            CyberButton(
                                text = "VISUALIZAR DOCUMENTO",
                                onClick = {
                                    PdfUtils.abrirPdfExterno(context, clase.archivoPdfUrl)
                                },
                                icon = Icons.Default.PictureAsPdf
                            )
                        }
                    }
                }

                // Botones IA para DOCENTE
                if (!esAlumno) {
                    Text(
                        text = "CORE INTELLIGENCE // DOCENTE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AulaVivaColors.BitcoinGold,
                        modifier = Modifier.padding(vertical = 16.dp),
                        letterSpacing = 1.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberButton(
                            text = "GENERAR IDEAS",
                            icon = Icons.Default.Lightbulb,
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(context, "IDEAS PRELIMINARES", contenido, clase, "IDEAS")
                                    },
                                    onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } },
                                    llamadaIA = { iaViewModel.generarIdeasParaClase(clase.nombre, clase.descripcion, clase.archivoPdfUrl) }
                                )
                            },
                            enabled = !isLoading,
                            loading = isLoading,
                            variant = CyberButtonVariant.SECONDARY
                        )

                        CyberButton(
                            text = "SUGERIR ACTIVIDADES",
                            icon = Icons.Default.Event,
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(context, "ACTIVIDADES SUGERIDAS", contenido, clase, "IDEAS")
                                    },
                                    onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } },
                                    llamadaIA = { iaViewModel.sugerirActividades(clase.nombre, clase.descripcion, clase.archivoPdfUrl) }
                                )
                            },
                            enabled = !isLoading,
                            loading = isLoading,
                            variant = CyberButtonVariant.SECONDARY
                        )

                        CyberButton(
                            text = "ESTRUCTURAR TIEMPO",
                            icon = Icons.Default.Schedule,
                            onClick = { showDuracionDialog = true },
                            enabled = !isLoading,
                            variant = CyberButtonVariant.SECONDARY
                        )

                        CyberButton(
                            text = "RESUMIR PDF",
                            icon = Icons.Default.Summarize,
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(context, "RESUMEN DE CONTENIDO", contenido, clase)
                                    },
                                    onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } },
                                    llamadaIA = { iaViewModel.resumirContenidoPdf(clase.nombre, clase.descripcion, clase.archivoPdfUrl) }
                                )
                            },
                            enabled = !isLoading,
                            loading = isLoading,
                            variant = CyberButtonVariant.SECONDARY
                        )

                        CyberButton(
                            text = "GUÍA DE PRESENTACIÓN",
                            icon = Icons.Default.PresentToAll,
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(context, "GUÍA DE PRESENTACIÓN", contenido, clase)
                                    },
                                    onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } },
                                    llamadaIA = { iaViewModel.generarGuiaPresentacion(clase.nombre, clase.descripcion, clase.archivoPdfUrl) }
                                )
                            },
                            enabled = !isLoading,
                            loading = isLoading,
                            variant = CyberButtonVariant.SECONDARY
                        )
                        
                        CyberButton(
                            text = "ACTIVIDADES INTERACTIVAS",
                            icon = Icons.Default.Games,
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(context, "ACTIVIDADES INTERACTIVAS", contenido, clase)
                                    },
                                    onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } },
                                    llamadaIA = { iaViewModel.generarActividadesInteractivas(clase.nombre, clase.descripcion, clase.archivoPdfUrl) }
                                )
                            },
                            enabled = !isLoading,
                            loading = isLoading,
                            variant = CyberButtonVariant.SECONDARY
                        )
                    }
                } else {
                    // Botones IA para ALUMNO
                    Text(
                        text = "CORE INTELLIGENCE // ALUMNO",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AulaVivaColors.BitcoinGold,
                        modifier = Modifier.padding(vertical = 16.dp),
                        letterSpacing = 1.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberButton(
                            text = "EXPLICAR CONCEPTOS",
                            icon = Icons.Default.School,
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(context, "EXPLICACIÓN DE CONCEPTOS", contenido, clase)
                                    },
                                    onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } },
                                    llamadaIA = { iaViewModel.explicarConceptosParaAlumno(clase.nombre, clase.descripcion, clase.archivoPdfUrl) }
                                )
                            },
                            enabled = !isLoading,
                            loading = isLoading,
                            variant = CyberButtonVariant.SECONDARY
                        )

                        CyberButton(
                            text = "GENERAR EJERCICIOS",
                            icon = Icons.Default.Assignment,
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(context, "EJERCICIOS GENERADOS", contenido, clase)
                                    },
                                    onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } },
                                    llamadaIA = { iaViewModel.generarEjerciciosParaAlumno(clase.nombre, clase.descripcion, clase.archivoPdfUrl) }
                                )
                            },
                            enabled = !isLoading,
                            loading = isLoading,
                            variant = CyberButtonVariant.SECONDARY
                        )

                        CyberButton(
                            text = "CREAR RESUMEN ESTUDIO",
                            icon = Icons.Default.Book,
                            onClick = {
                                ejecutarIA(
                                    iaViewModel = iaViewModel,
                                    lifecycleOwner = lifecycleOwner,
                                    isLoading = { isLoading = it },
                                    onSuccess = { contenido ->
                                        mostrarResultadoIA(context, "RESUMEN DE ESTUDIO", contenido, clase)
                                    },
                                    onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } },
                                    llamadaIA = { iaViewModel.crearResumenEstudioParaAlumno(clase.nombre, clase.descripcion, clase.archivoPdfUrl) }
                                )
                            },
                            enabled = !isLoading,
                            loading = isLoading,
                            variant = CyberButtonVariant.SECONDARY
                        )
                    }
                }
            }
        }
    }

    // Dialog duración
    if (showDuracionDialog) {
        AlertDialog(
            onDismissRequest = { showDuracionDialog = false },
            title = { Text("DURACIÓN ESTIMADA", style = MaterialTheme.typography.titleMedium, color = AulaVivaColors.TextPrimary) },
            containerColor = AulaVivaColors.SurfaceDark,
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
                                        mostrarResultadoIA(context, "ESTRUCTURA DE $duracion", contenido, clase)
                                    },
                                    onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } },
                                    llamadaIA = { iaViewModel.estructurarClasePorTiempo(clase.nombre, clase.descripcion, duracion, clase.archivoPdfUrl) }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(duracion, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = AulaVivaColors.PrimaryCyan)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDuracionDialog = false }) {
                    Text("CANCELAR", color = AulaVivaColors.TextSecondary)
                }
            }
        )
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
    // Generic logic preserved, observing LiveData
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
