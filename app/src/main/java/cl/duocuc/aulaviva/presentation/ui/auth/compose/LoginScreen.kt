package cl.duocuc.aulaviva.presentation.ui.auth.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.aulaviva.presentation.ui.components.CyberButton
import cl.duocuc.aulaviva.presentation.ui.components.CyberCard
import cl.duocuc.aulaviva.presentation.ui.components.CyberTextField
import cl.duocuc.aulaviva.presentation.ui.effects.CyberParticleBackground
import cl.duocuc.aulaviva.presentation.ui.effects.aggressiveScanLines
import cl.duocuc.aulaviva.presentation.ui.effects.breakcoreGlitch
import cl.duocuc.aulaviva.presentation.ui.effects.cyberGrid
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaColors
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateBack: () -> Unit = {} // Kept for compatibility, though not used in new design
) {
    val loginSuccess by viewModel.loginSuccess.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Observar login exitoso
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            onLoginSuccess()
        }
    }
    
    // Mostrar errores
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError() // Asumiendo que existe, sino ignora
        }
    }
    
    LoginScreenContent(
        isLoading = isLoading,
        onLogin = { email, password -> viewModel.login(email, password) },
        onNavigateToRegister = { /* Implementar navegación a registro si necesario */ }
    )
    
    // Snackbar Host
    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun LoginScreenContent(
    isLoading: Boolean,
    onLogin: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AulaVivaColors.CyberBlack)
            .cyberGrid() // Grid infinito de fondo
            .aggressiveScanLines() // Scan lines evidentes
    ) {
        // Particle system
        CyberParticleBackground(maxParticles = 30)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header con glitch BREAKCORE (más agresivo)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.breakcoreGlitch() // Glitch cada 3-5s
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = AulaVivaColors.PrimaryCyan
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Texto con flicker sutil
                var alpha by remember { mutableFloatStateOf(1f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        alpha = if (Random.nextFloat() > 0.97f) 0.7f else 1f
                        delay(50)
                        alpha = 1f
                        delay(Random.nextLong(2000, 5000))
                    }
                }
                
                Text(
                    text = "AULA VIVA",
                    style = MaterialTheme.typography.displayLarge,
                    color = AulaVivaColors.PrimaryCyan.copy(alpha = alpha)
                )
                
                Text(
                    text = "// SISTEMA ACADÉMICO DIGITAL",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = AulaVivaColors.TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Card sin glitch interno (solo contenido limpio)
            CyberCard {
                Column {
                    CyberTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "CORREO_ELECTRONICO",
                        enabled = !isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    CyberTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "PASSWORD_HASH",
                        enabled = !isLoading
                    ) // TODO: Add isPassword parameter to CyberTextField if needed
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    CyberButton(
                        text = ">> INICIAR_SESION",
                        onClick = { onLogin(email, password) },
                        enabled = !isLoading,
                        loading = isLoading
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onNavigateToRegister) {
                Text(
                    text = "> ¿Sin cuenta? Registrarse",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = AulaVivaColors.TextSecondary
                )
            }
        }
    }
}
