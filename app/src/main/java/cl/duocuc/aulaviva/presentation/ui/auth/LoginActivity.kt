package cl.duocuc.aulaviva.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import cl.duocuc.aulaviva.presentation.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.data.repository.AuthRepository
import cl.duocuc.aulaviva.databinding.ActivityLoginBinding
import cl.duocuc.aulaviva.presentation.ui.main.PanelAlumnoActivity
import cl.duocuc.aulaviva.presentation.ui.main.PanelPrincipalActivity
import cl.duocuc.aulaviva.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    // ViewBinding para acceso seguro a las vistas
    private lateinit var binding: ActivityLoginBinding

    // ViewModel usando delegado by viewModels()
    private val viewModel: AuthViewModel by viewModels()

    // Repository para consultar rol
    private val authRepository = cl.duocuc.aulaviva.data.repository.RepositoryProvider.provideAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar observers para el ViewModel
        setupObservers()

        // Configurar listeners de los botones
        setupListeners()
    }

    private fun setupObservers() {
        // Observer para loading
        viewModel.isLoading.observe(this) { isLoading ->
            updateUIState(isLoading)
        }

        // Observer para errores
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
                // Asegurar que la UI vuelva al estado normal después de un error
                updateUIState(false)
            }
        }

        // Observer para login exitoso
        viewModel.loginSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "✓ Bienvenido!", Toast.LENGTH_SHORT).show()

                // Verificar rol y redirigir según corresponda
                lifecycleScope.launch {
                    try {
                        val resultRol = authRepository.obtenerRolUsuario()

                        val rol = resultRol.getOrNull() ?: "docente" // Default a docente si falla

                        // Pequeño delay para feedback visual
                        binding.root.postDelayed({
                            val intent = when (rol.lowercase()) {
                                "docente" -> Intent(
                                    this@LoginActivity,
                                    PanelPrincipalActivity::class.java
                                )

                                "alumno" -> Intent(
                                    this@LoginActivity,
                                    PanelAlumnoActivity::class.java
                                )

                                else -> Intent(
                                    this@LoginActivity,
                                    PanelPrincipalActivity::class.java
                                ) // Default docente
                            }

                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            @Suppress("DEPRECATION")
                            overridePendingTransition(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )
                            finish()
                        }, 400)

                    } catch (e: Exception) {
                        // Error obteniendo rol - Permitir entrada con rol por defecto
                        Toast.makeText(
                            this@LoginActivity,
                            "Accediendo como docente...",
                            Toast.LENGTH_SHORT
                        ).show()

                        binding.root.postDelayed({
                            val intent =
                                Intent(this@LoginActivity, PanelPrincipalActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }, 400)
                    }
                }
            }
        }
    }

    private fun updateUIState(isLoading: Boolean) {
        // Actualizar botones
        binding.loginButton.isEnabled = !isLoading
        binding.goToRegisterButton.isEnabled = !isLoading

        // Actualizar texto del botón
        binding.loginButton.text = if (isLoading) "Iniciando sesión..." else "Iniciar Sesión"

        // Actualizar campos de entrada
        binding.emailInput.isEnabled = !isLoading
        binding.passwordInput.isEnabled = !isLoading
    }

    private fun setupListeners() {
        // ✅ FIX: Botón para volver a la pantalla de bienvenida
        // Abre WelcomeActivity explícitamente en lugar de hacer finish()
        // Esto evita que la app se cierre si no hay activity en el stack
        binding.goToRegisterButton.setOnClickListener {
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Botón de login (iniciar sesión)
        binding.loginButton.setOnClickListener {
            // Animación de feedback visual cuando se toca el botón
            it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_scale))

            // Obtener los datos ingresados por el usuario
            val email = binding.emailInput.text?.toString()?.trim() ?: ""
            val password = binding.passwordInput.text?.toString() ?: ""

            // Validar que los datos sean correctos antes de enviar
            var valid = true

            // Validar formato de email
            if (!viewModel.isValidEmail(email)) {
                binding.emailLayout.error = "Correo inválido"
                valid = false
            } else {
                binding.emailLayout.error = null // Limpiar error previo
            }

            // Validar que la contraseña tenga al menos 6 caracteres
            if (!viewModel.isValidPassword(password)) {
                binding.passwordLayout.error = "La contraseña debe tener al menos 6 caracteres"
                valid = false
            } else {
                binding.passwordLayout.error = null // Limpiar error previo
            }

            // Si los datos son válidos, intentar hacer login
            if (valid) {
                viewModel.login(email, password) // Llama a Supabase para autenticar
            }
        }
    }
}
