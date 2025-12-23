package cl.duocuc.aulaviva.presentation.ui.main.compose

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import cl.duocuc.aulaviva.presentation.activity.compose.DocenteAsignaturasActivityCompose
import cl.duocuc.aulaviva.presentation.ui.auth.compose.LoginActivityCompose
import cl.duocuc.aulaviva.presentation.viewmodel.PanelPrincipalViewModel
import cl.duocuc.aulaviva.utils.NotificationHelper
import cl.duocuc.aulaviva.presentation.ui.components.CyberButton
import cl.duocuc.aulaviva.presentation.ui.components.CyberCard
import cl.duocuc.aulaviva.presentation.ui.effects.BiomechanicalVeins
import cl.duocuc.aulaviva.presentation.ui.effects.CyberParticleBackground
import cl.duocuc.aulaviva.presentation.ui.effects.aggressiveScanLines
import cl.duocuc.aulaviva.presentation.ui.effects.breakcoreGlitch
import cl.duocuc.aulaviva.presentation.ui.effects.cyberGrid
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelPrincipalScreen(
    viewModel: PanelPrincipalViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados del ViewModel
    val userEmail: String? by viewModel.userEmail.observeAsState()
    val toastMessage: String? by viewModel.toastMessage.observeAsState()
    val logoutEvent: Boolean by viewModel.logoutEvent.observeAsState(false)

    // Notificación helper
    val notificationHelper = remember { NotificationHelper(context) }

    // Launcher para permisos de notificaciones
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val email = userEmail ?: "Usuario"
            val nombre = email.substringBefore("@")
            notificationHelper.enviarNotificacionBienvenida(nombre)
        }
    }

    // Pedir permiso de notificaciones al iniciar
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // Ya tiene permiso
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var isCreatingDemo by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Mostrar toast
    LaunchedEffect(toastMessage) {
        toastMessage?.let { mensaje ->
            scope.launch {
                snackbarHostState.showSnackbar(mensaje)
                viewModel.limpiarToastMessage()
            }
        }
    }

    // Observar cuando se completa la creación de demo
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            isCreatingDemo = false
        }
    }

    // Manejar logout
    LaunchedEffect(logoutEvent) {
        if (logoutEvent) {
            val intent = Intent(context, LoginActivityCompose::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    Scaffold(
        snackbarHost = { cl.duocuc.aulaviva.presentation.ui.common.CyberSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    cl.duocuc.aulaviva.presentation.ui.common.GlitchText(
                        text = "PANEL DOCENTE", 
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AulaVivaColors.CyberBlack.copy(alpha = 0.9f), // Cyber Black
                    titleContentColor = AulaVivaColors.PrimaryCyan
                )
            )
        },
        containerColor = AulaVivaColors.CyberBlack // Cyber background
    ) { paddingValues ->
        
        // BREAKCORE BACKGROUND STACK
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AulaVivaColors.CyberBlack)
                .cyberGrid()          // Capa 1: Grid Infinito
        ) {
            BiomechanicalVeins()      // Capa 2: Venas orgánicas
            CyberParticleBackground() // Capa 3: Partículas flotantes
            
            // Capa 4: Contenido UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .aggressiveScanLines() // Scanlines sobre el contenido
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Avatar con Glitch
                Box(
                    modifier = Modifier.breakcoreGlitch()
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle, 
                        contentDescription = "Profile",
                        modifier = Modifier.size(64.dp),
                        tint = AulaVivaColors.PrimaryCyan
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mensaje de bienvenida
                Text(
                    text = "PROFESOR ${userEmail?.substringBefore("@")?.uppercase() ?: "USUARIO"}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AulaVivaColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "CONTROL DE MANDO ACADÉMICO",
                    style = MaterialTheme.typography.labelSmall,
                    color = AulaVivaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)
                )

                // Card: Mis Asignaturas
                CyberCard(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = AulaVivaColors.SecondaryAccent,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "MIS ASIGNATURAS",
                            style = MaterialTheme.typography.titleLarge,
                            color = AulaVivaColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        CyberButton(
                            text = "ACCEDER A CLASES >>",
                            onClick = {
                                val intent = Intent(context, DocenteAsignaturasActivityCompose::class.java)
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // Card: Modo Demostración
                CyberCard(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = AulaVivaColors.BitcoinGold, // Tech Gold contrast
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "MODO DEMOSTRACIÓN",
                            style = MaterialTheme.typography.titleLarge,
                            color = AulaVivaColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        CyberButton(
                            text = "PERTURBAR REALIDAD (GEN DATA)",
                            onClick = {
                                if (!isCreatingDemo) {
                                    showConfirmDialog = true
                                }
                            },
                            enabled = !isCreatingDemo,
                            loading = isCreatingDemo,
                            variant = cl.duocuc.aulaviva.presentation.ui.components.CyberButtonVariant.SECONDARY
                        )
                    }
                }

                // Dialogs
                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text("Generar Entorno Demo", color = AulaVivaColors.TextPrimary) },
                        text = { Text("Se generará una asignatura y clase de prueba de forma instantánea.", color = AulaVivaColors.TextSecondary) },
                        containerColor = AulaVivaColors.SurfaceDark,
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showConfirmDialog = false
                                    isCreatingDemo = true
                                    viewModel.crearAsignaturaYClaseDemo()
                                }
                            ) {
                                Text("CONFIRMAR", color = AulaVivaColors.PrimaryCyan)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text("CANCELAR", color = AulaVivaColors.TextSecondary)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón cerrar sesión (Estilo Cyber Outlined manual o custom button con variante danger si existiera, usaremos Secondary por ahora)
                CyberButton(
                    text = "DESCONECTAR SISTEMA",
                    onClick = { showLogoutDialog = true },
                    variant = cl.duocuc.aulaviva.presentation.ui.components.CyberButtonVariant.SECONDARY
                ) 

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialog de confirmación de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Terminar Sesión", color = AulaVivaColors.TextPrimary) },
            text = { Text("¿Desea desconectarse del nodo?", color = AulaVivaColors.TextSecondary) },
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
