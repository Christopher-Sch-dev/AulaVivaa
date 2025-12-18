package cl.duocuc.aulaviva.presentation.ui.main.compose

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import cl.duocuc.aulaviva.utils.Constants
import cl.duocuc.aulaviva.presentation.ui.common.PullToRefreshContainer
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.presentation.activity.compose.AlumnoClasesActivityCompose
import cl.duocuc.aulaviva.presentation.ui.auth.compose.LoginActivityCompose
import cl.duocuc.aulaviva.presentation.viewmodel.AlumnoViewModel
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaColors
import cl.duocuc.aulaviva.presentation.ui.components.CyberButton
import cl.duocuc.aulaviva.presentation.ui.components.CyberButtonVariant
import cl.duocuc.aulaviva.presentation.ui.components.CyberCard
import cl.duocuc.aulaviva.presentation.ui.components.CyberTextField
import cl.duocuc.aulaviva.presentation.ui.effects.CyberParticleBackground
import cl.duocuc.aulaviva.presentation.ui.effects.breakcoreGlitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelAlumnoScreen(
    viewModel: AlumnoViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val asignaturas: List<Asignatura> by viewModel.asignaturasInscritas.observeAsState(emptyList())
    val isLoading: Boolean by viewModel.isLoading.observeAsState(false)
    val error: String? by viewModel.error.observeAsState()
    val inscripcionExitosa: Asignatura? by viewModel.inscripcionExitosa.observeAsState()
    val logoutEvent: Boolean by viewModel.logoutEvent.observeAsState(false)
    val userEmail: String? by viewModel.userEmail.observeAsState()

    var showInscripcionDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var codigoInscripcion by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Manejar logout
    LaunchedEffect(logoutEvent) {
        if (logoutEvent) {
            val intent = Intent(context, LoginActivityCompose::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    // Mostrar errores
    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
            viewModel.limpiarError()
        }
    }

    // Mostrar éxito de inscripción
    LaunchedEffect(inscripcionExitosa) {
        inscripcionExitosa?.let { asignatura ->
            scope.launch {
                snackbarHostState.showSnackbar("✓ ACCESO RED CONCEDIDO: ${asignatura.nombre}")
            }
            showInscripcionDialog = false
            codigoInscripcion = ""
        }
    }

    // Sincronizar al inicio
    LaunchedEffect(Unit) {
        viewModel.sincronizarAsignaturasInscritas()
    }

    // Callback para manejar el gesto de pull-to-refresh
    val onRefresh: () -> Unit = {
        viewModel.sincronizarAsignaturasInscritas()
        scope.launch {
            delay(Constants.REFRESH_DELAY_MS)
            snackbarHostState.showSnackbar("ENLACE NEURONAL REESTABLECIDO")
        }
    }

    Scaffold(
        snackbarHost = { cl.duocuc.aulaviva.presentation.ui.common.CyberSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    cl.duocuc.aulaviva.presentation.ui.common.GlitchText(
                        text = "PANEL ESTUDIANTE", 
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AulaVivaColors.CyberBlack.copy(alpha = 0.9f),
                    titleContentColor = AulaVivaColors.PrimaryCyan
                )
            )
        },
        containerColor = AulaVivaColors.CyberBlack
    ) { paddingValues ->
        
        // Cyber Particle Layout
        Box(
             modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AulaVivaColors.CyberBlack)
        ) {
            CyberParticleBackground()
            
            if (isLoading && asignaturas.isEmpty()) {
                cl.duocuc.aulaviva.presentation.ui.common.FullScreenLoading()
            } else {
                PullToRefreshContainer(
                    isRefreshing = isLoading,
                    onRefresh = onRefresh
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card de bienvenida
                        item {
                            CyberCard(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Icono de bienvenida con glitch
                                    Box(modifier = Modifier.breakcoreGlitch()) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.School,
                                            contentDescription = "Student",
                                            modifier = Modifier.size(64.dp),
                                            tint = AulaVivaColors.PrimaryCyan
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Mensaje de bienvenida
                                    Text(
                                        text = "ESTUDIANTE ${userEmail?.substringBefore("@")?.uppercase() ?: "ALUMNO"}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AulaVivaColors.TextPrimary,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "REGISTRO ACADÉMICO GLOBAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AulaVivaColors.TextSecondary,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        color = AulaVivaColors.PrimaryCyan.copy(alpha = 0.3f)
                                    )

                                    // Información y botón de inscripción
                                    Text(
                                        text = "GESTIÓN DE MATRÍCULA",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AulaVivaColors.SecondaryAccent
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Ingrese token de acceso para desbloquear módulo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AulaVivaColors.TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    // Botón de inscripción
                                    CyberButton(
                                        text = "INSCRIBIR ASIGNATURA >",
                                        onClick = { showInscripcionDialog = true }
                                    )
                                }
                            }
                        }

                        // Título de sección
                        if (asignaturas.isNotEmpty()) {
                            item {
                                Text(
                                    text = "MIS ASIGNATURAS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AulaVivaColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Lista de asignaturas
                        if (asignaturas.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = AulaVivaColors.TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "NO HAY DATA",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AulaVivaColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = asignaturas,
                                key = { it.id }
                            ) { asignatura ->
                                AsignaturaCard(
                                    asignatura = asignatura,
                                    onClick = {
                                        val intent = Intent(context, AlumnoClasesActivityCompose::class.java)
                                        intent.putExtra("ASIGNATURA_ID", asignatura.id)
                                        intent.putExtra("ASIGNATURA_NOMBRE", asignatura.nombre)
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                )
                            }
                        }

                        // Botón de cerrar sesión
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            CyberButton(
                                text = "DESCONECTAR",
                                onClick = { showLogoutDialog = true },
                                variant = CyberButtonVariant.SECONDARY,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    // Dialog de inscripción
    if (showInscripcionDialog) {
        AlertDialog(
            onDismissRequest = { showInscripcionDialog = false },
            title = { Text("TOKEN DE ACCESO", style = MaterialTheme.typography.titleLarge, color = AulaVivaColors.TextPrimary) },
            text = {
                CyberTextField(
                    value = codigoInscripcion,
                    onValueChange = { codigoInscripcion = it.uppercase() },
                    label = "INGRESE CÓDIGO"
                )
            },
            containerColor = AulaVivaColors.SurfaceDark,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (codigoInscripcion.isNotBlank()) {
                            viewModel.inscribirConCodigo(codigoInscripcion.trim())
                        }
                    }
                ) {
                    Text("AUTORIZAR", color = AulaVivaColors.PrimaryCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInscripcionDialog = false }) {
                    Text("ABORTAR", color = AulaVivaColors.TextSecondary)
                }
            }
        )
    }

    // Dialog de confirmación de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Terminar Sesión", color = AulaVivaColors.TextPrimary) },
            text = { Text("¿Confirmar desconexión del sistema?", color = AulaVivaColors.TextSecondary) },
            containerColor = AulaVivaColors.SurfaceDark,
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    }
                ) {
                    Text("SALIR", color = AulaVivaColors.ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCELAR", color = AulaVivaColors.TextSecondary)
                }
            }
        )
    }
}


@Composable
fun AsignaturaCard(
    asignatura: Asignatura,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CyberCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = AulaVivaColors.SecondaryAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = asignatura.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AulaVivaColors.TextPrimary
                )
            }
            
            if (asignatura.descripcion.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = asignatura.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AulaVivaColors.TextSecondary,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .background(AulaVivaColors.SurfaceLight.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ID: ${asignatura.codigoAcceso}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AulaVivaColors.PrimaryCyan,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}
