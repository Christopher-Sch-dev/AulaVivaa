package cl.duocuc.aulaviva.presentation.ui.clases.compose

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearEditarClaseScreen(
    asignaturaId: String,
    asignaturaNombre: String,
    claseId: String? = null,
    fecha: String = "",
    onFechaChange: (String) -> Unit = {},
    onMostrarDatePicker: () -> Unit = {},
    viewModel: ClaseViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfNombre by remember { mutableStateOf("") }
    var existingPdfUrl by remember { mutableStateOf("") }

    var nombreError by remember { mutableStateOf<String?>(null) }
    var descripcionError by remember { mutableStateOf<String?>(null) }
    var fechaError by remember { mutableStateOf<String?>(null) }

    val isLoading: Boolean by viewModel.isLoading.observeAsState(false)
    val operationSuccess: String? by viewModel.operationSuccess.observeAsState()
    val error: String? by viewModel.error.observeAsState()

    // PDF picker
    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pdfUri = it
            pdfNombre = getFileName(context, it)
        }
    }

    // Cargar datos si es edición
    LaunchedEffect(claseId) {
        claseId?.let { id ->
            scope.launch {
                viewModel.obtenerClasePorId(id)?.let { clase ->
                    nombre = clase.nombre
                    descripcion = clase.descripcion
                    onFechaChange(clase.fecha)
                    existingPdfUrl = clase.archivoPdfUrl
                    pdfNombre = clase.archivoPdfNombre
                }
            }
        }
    }

    // Observar resultados y cerrar automáticamente al editar
    LaunchedEffect(operationSuccess) {
        operationSuccess?.let { mensaje ->
            scope.launch {
                snackbarHostState.showSnackbar(mensaje)
                // Si es edición, cerrar automáticamente después de mostrar el mensaje
                if (claseId != null) {
                    kotlinx.coroutines.delay(800) // Esperar para que se vea el mensaje
                    (context as? android.app.Activity)?.finish()
                }
                viewModel.clearMessages()
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (claseId == null) "Nueva Clase" else "Editar Clase")
                        Text(asignaturaNombre, style = MaterialTheme.typography.bodySmall)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo Nombre
            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    nombreError = null
                },
                label = { Text("Nombre de la clase") },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                isError = nombreError != null,
                supportingText = nombreError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Campo Descripción
            OutlinedTextField(
                value = descripcion,
                onValueChange = {
                    descripcion = it
                    descripcionError = null
                },
                label = { Text("Descripción de actividades") },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                isError = descripcionError != null,
                supportingText = descripcionError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            // Campo Fecha
            OutlinedTextField(
                value = fecha,
                onValueChange = { },
                readOnly = true,
                label = { Text("Fecha y Hora (dd/MM/yyyy HH:mm)") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                trailingIcon = {
                    IconButton(onClick = {
                        onMostrarDatePicker()
                        fechaError = null
                    }) {
                        Icon(Icons.Default.DateRange, null)
                    }
                },
                isError = fechaError != null,
                supportingText = fechaError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // PDF selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📄 Material PDF (Opcional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (pdfNombre.isNotEmpty()) {
                        Text(
                            text = pdfNombre,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = { pickPdfLauncher.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (pdfNombre.isNotEmpty()) "Cambiar PDF" else "Seleccionar PDF")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { (context as? android.app.Activity)?.finish() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        // Validar
                        var valid = true
                        if (nombre.isBlank()) {
                            nombreError = "El nombre es obligatorio"
                            valid = false
                        }
                        if (descripcion.isBlank()) {
                            descripcionError = "La descripción es obligatoria"
                            valid = false
                        }
                        if (fecha.isBlank()) {
                            fechaError = "La fecha es obligatoria"
                            valid = false
                        }

                        if (valid) {
                            val fechaFinal = fecha
                            if (claseId != null) {
                                // Editar
                                if (pdfUri != null) {
                                    viewModel.subirPdfYActualizarClase(
                                        uri = pdfUri!!,
                                        nombreArchivo = pdfNombre,
                                        claseId = claseId,
                                        nombre = nombre,
                                        descripcion = descripcion,
                                        fecha = fechaFinal
                                    )
                                } else {
                                    viewModel.actualizarClase(
                                        claseId = claseId,
                                        nombre = nombre,
                                        descripcion = descripcion,
                                        fecha = fechaFinal,
                                        archivoPdfUrl = existingPdfUrl,
                                        archivoPdfNombre = pdfNombre
                                    )
                                }
                            } else {
                                // Crear
                                if (pdfUri != null) {
                                    viewModel.subirPdfYCrearClase(
                                        uri = pdfUri!!,
                                        nombreArchivo = pdfNombre,
                                        nombre = nombre,
                                        descripcion = descripcion,
                                        fecha = fechaFinal,
                                        asignaturaId = asignaturaId
                                    )
                                } else {
                                    viewModel.crearClase(
                                        nombre = nombre,
                                        descripcion = descripcion,
                                        fecha = fechaFinal,
                                        archivoPdfUrl = "",
                                        archivoPdfNombre = "",
                                        asignaturaId = asignaturaId
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isLoading == false
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (claseId != null) "Actualizar" else "Crear Clase")
                    }
                }
            }
        }
    }
}

fun getFileName(context: android.content.Context, uri: Uri): String {
    var name = "documento.pdf"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
    }
    return name
}


