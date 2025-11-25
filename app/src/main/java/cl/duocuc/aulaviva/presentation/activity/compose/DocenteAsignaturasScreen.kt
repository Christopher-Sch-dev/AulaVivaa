package cl.duocuc.aulaviva.presentation.activity.compose

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.aulaviva.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.presentation.activity.compose.DocenteClasesActivityCompose
import cl.duocuc.aulaviva.presentation.activity.compose.InscritosActivityCompose
import cl.duocuc.aulaviva.presentation.ui.common.PullToRefreshContainer
import cl.duocuc.aulaviva.presentation.viewmodel.AsignaturasViewModel
import cl.duocuc.aulaviva.utils.ClipboardHelper
import cl.duocuc.aulaviva.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocenteAsignaturasScreen(
    viewModel: AsignaturasViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTablet = screenWidth >= 600
    val scope = rememberCoroutineScope()
    val asignaturas: List<Asignatura> by viewModel.asignaturas.observeAsState(emptyList())
    val isLoading: Boolean by viewModel.isLoading.observeAsState(false)
    val operationSuccess: String? by viewModel.operationSuccess.observeAsState()
    val codigoGenerado: String? by viewModel.codigoGenerado.observeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCrearDialog by remember { mutableStateOf(false) }
    var showEditarDialog by remember { mutableStateOf(false) }
    var asignaturaAEditar by remember { mutableStateOf<Asignatura?>(null) }
    var nombreAsignatura by remember { mutableStateOf("") }
    var descripcionAsignatura by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.sincronizarAsignaturas()
    }

    // Mostrar mensaje de éxito con código generado cuando se crea una asignatura
    // Se ejecuta inmediatamente sin esperar, para dar feedback rápido al usuario
    LaunchedEffect(operationSuccess, codigoGenerado) {
        operationSuccess?.let { mensaje ->
            scope.launch {
                // Construir mensaje completo incluyendo el código si está disponible
                val mensajeCompleto = if (codigoGenerado != null) {
                    "✅ $mensaje\n📋 Código: $codigoGenerado"
                } else {
                    "✅ $mensaje"
                }
                // Mostrar snackbar inmediatamente para dar feedback al usuario
                snackbarHostState.showSnackbar(
                    message = mensajeCompleto,
                    duration = SnackbarDuration.Long
                )
                viewModel.limpiarCodigoGenerado()
            }
        }
    }

    // Manejar pull-to-refresh
    val onRefresh: () -> Unit = {
        viewModel.sincronizarAsignaturas()
        scope.launch {
            delay(Constants.REFRESH_DELAY_MS)
            snackbarHostState.showSnackbar(context.getString(R.string.msg_datos_actualizados))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.mis_asignaturas)) },
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
                PullToRefreshContainer(
                    isRefreshing = isLoading,
                    onRefresh = onRefresh
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = if (isTablet) (Constants.PADDING_STANDARD_DP * 2).dp else Constants.PADDING_STANDARD_DP.dp,
                            vertical = Constants.PADDING_STANDARD_DP.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(Constants.SPACING_ITEMS_DP.dp)
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
                                    val codigo = viewModel.obtenerCodigoParaCopiar(asignatura.id)
                                    if (codigo != null && ClipboardHelper.copyToClipboard(
                                            context,
                                            Constants.CLIPBOARD_LABEL_CODIGO,
                                            codigo
                                        )
                                    ) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.msg_codigo_copiado, codigo)
                                            )
                                        }
                                    }
                                },
                                onEditar = { asignatura ->
                                    asignaturaAEditar = asignatura
                                    nombreAsignatura = asignatura.nombre
                                    descripcionAsignatura = asignatura.descripcion
                                    showEditarDialog = true
                                },
                                onEliminar = {
                                    viewModel.verificarTieneClases(asignatura.id) { tieneClases ->
                                        if (tieneClases) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.msg_error_eliminar_con_clases),
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        } else {
                                            viewModel.eliminarAsignatura(asignatura.id)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.msg_asignatura_eliminada)
                                                )
                                            }
                                        }
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

    // Dialog crear asignatura
    if (showCrearDialog) {
        AlertDialog(
            onDismissRequest = { showCrearDialog = false },
            title = { Text(context.getString(R.string.crear_asignatura)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreAsignatura,
                        onValueChange = { nombreAsignatura = it },
                        label = { Text(context.getString(R.string.nombre_asignatura)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(Constants.PADDING_SMALL_DP.dp))
                    OutlinedTextField(
                        value = descripcionAsignatura,
                        onValueChange = { descripcionAsignatura = it },
                        label = { Text(context.getString(R.string.descripcion_asignatura)) },
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
                    Text(context.getString(R.string.crear_asignatura))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCrearDialog = false }) {
                    Text(context.getString(R.string.cancelar))
                }
            }
        )
    }

    // Diálogo para editar el nombre y descripción de una asignatura existente
    if (showEditarDialog && asignaturaAEditar != null) {
        AlertDialog(
            onDismissRequest = {
                showEditarDialog = false
                asignaturaAEditar = null
                nombreAsignatura = ""
                descripcionAsignatura = ""
            },
            title = { Text("Editar Asignatura") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreAsignatura,
                        onValueChange = { nombreAsignatura = it },
                        label = { Text(context.getString(R.string.nombre_asignatura)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(Constants.PADDING_SMALL_DP.dp))
                    OutlinedTextField(
                        value = descripcionAsignatura,
                        onValueChange = { descripcionAsignatura = it },
                        label = { Text(context.getString(R.string.descripcion_asignatura)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombreAsignatura.isNotBlank() && asignaturaAEditar != null) {
                            val asignaturaActualizada = asignaturaAEditar!!.copy(
                                nombre = nombreAsignatura.trim(),
                                descripcion = descripcionAsignatura.trim()
                                // El código de acceso no se actualiza al editar, se mantiene igual
                                // Solo se pueden modificar el nombre y la descripción
                            )
                            viewModel.actualizarAsignatura(asignaturaActualizada)
                            showEditarDialog = false
                            asignaturaAEditar = null
                            nombreAsignatura = ""
                            descripcionAsignatura = ""
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditarDialog = false
                    asignaturaAEditar = null
                    nombreAsignatura = ""
                    descripcionAsignatura = ""
                }) {
                    Text(context.getString(R.string.cancelar))
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
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📚", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = context.getString(R.string.no_tienes_asignaturas),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Constants.PADDING_SMALL_DP.dp))
        Text(
            text = context.getString(R.string.crea_primera_asignatura),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(Constants.PADDING_SMALL_DP.dp))
            Text(context.getString(R.string.crear_asignatura))
        }
    }
}

@Composable
fun AsignaturaDocenteCard(
    asignatura: Asignatura,
    onVerClases: () -> Unit,
    onVerInscritos: () -> Unit,
    onCopiarCodigo: () -> Unit,
    onEliminar: () -> Unit,
    onEditar: (Asignatura) -> Unit // Callback ejecutado cuando el usuario selecciona editar una asignatura
) {
    val context = LocalContext.current
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
                    Spacer(modifier = Modifier.height(Constants.PADDING_SMALL_DP.dp))
                    Text(
                        text = context.getString(R.string.codigo_acceso, asignatura.codigoAcceso),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(Icons.Default.MoreVert, "Opciones")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar asignatura") },
                            onClick = {
                                onEditar(asignatura)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.copiar_codigo)) },
                            onClick = {
                                onCopiarCodigo()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.eliminar), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onEliminar()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL_DP.dp)
            ) {
                OutlinedButton(
                    onClick = onVerClases,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(context.getString(R.string.ver_clases))
                }
                OutlinedButton(
                    onClick = onVerInscritos,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.People, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(context.getString(R.string.ver_inscritos))
                }
            }
        }
    }
}

