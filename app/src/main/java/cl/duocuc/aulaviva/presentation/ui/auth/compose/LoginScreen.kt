package cl.duocuc.aulaviva.presentation.ui.auth.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTheme
import cl.duocuc.aulaviva.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Pantalla de Login en Jetpack Compose con Material Design 3
 *
 * Incluye:
 * - Campos de email y contraseña con validación
 * - Toggle para mostrar/ocultar contraseña
 * - Manejo de errores con Snackbar
 * - Integración con AuthViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Estados locales
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Estados del ViewModel
    val isLoading: Boolean by viewModel.isLoading.observeAsState(false)
    val error: String? by viewModel.error.observeAsState()
    val loginSuccess: Boolean by viewModel.loginSuccess.observeAsState(false)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observar login exitoso
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            val nombre = email.substringBefore("@").takeIf { it.isNotEmpty() } ?: "Usuario"
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Acceso Concedido: $nombre", // Cyber style text
                    duration = SnackbarDuration.Short
                )
                onLoginSuccess()
            }
        }
    }

    // Gestionar errores visuales
    LaunchedEffect(email) { if (emailError != null && email.isNotEmpty()) emailError = null }
    LaunchedEffect(password) { if (passwordError != null && password.isNotEmpty()) passwordError = null }

    LaunchedEffect(error) {
        error?.let { errorMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(message = errorMessage, duration = SnackbarDuration.Long)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background // Ensure dark background
    ) { paddingValues ->
        
        // Background with subtle Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main Login Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp), // Slightly softer for tech look
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon / Logo
                        Icon(
                            imageVector = Icons.Default.Lock, // Generic robust icon or specific 'School' if available
                            contentDescription = "Security Lock",
                            modifier = Modifier
                                .size(64.dp)
                                .padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        // Title
                        Text(
                            text = "AULA VIVA",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "FUTURO EDUCATIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary, // Use secondary/tertiary for subtitle
                            modifier = Modifier.padding(bottom = 32.dp),
                            letterSpacing = 2.sp
                        )

                        // Spinner if loading (centered in card)
                        if (isLoading) {
                            cl.duocuc.aulaviva.presentation.ui.common.CyberLoading(
                                modifier = Modifier
                                    .padding(bottom = 16.dp),
                                size = 48.dp
                            )
                        }

                        // Inputs
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = null
                            },
                            label = { Text("ID DE USUARIO / CORREO") },
                            placeholder = { Text("usuario@duocuc.cl") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            isError = emailError != null,
                            supportingText = emailError?.let { { Text(it) } },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            }
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                            },
                            label = { Text("CLAVE DE ACCESO") }, // Uppercase for tech feel
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                            isError = passwordError != null,
                            supportingText = passwordError?.let { { Text(it) } },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Password"
                                    )
                                }
                            }
                        )

                        // Action Buttons
                        Button(
                            onClick = {
                                var isValid = true
                                if (!viewModel.isValidEmail(email)) {
                                    emailError = "Formato de correo inválido"
                                    isValid = false
                                }
                                if (!viewModel.isValidPassword(password)) {
                                    passwordError = "Mínimo 6 caracteres requeridos"
                                    isValid = false
                                }
                                if (isValid) viewModel.login(email, password)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isLoading
                        ) {
                           Text("INICIAR SESIÓN")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CANCELAR OPERACIÓN")
                        }
                    }
                }
                
                // Footer (outside card)
                Spacer(modifier = Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info, // Minimal info icon
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SISTEMA SEGURO v2.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

