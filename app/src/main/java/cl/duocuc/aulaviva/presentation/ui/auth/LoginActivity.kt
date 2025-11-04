package cl.duocuc.aulaviva.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.databinding.ActivityLoginBinding
import cl.duocuc.aulaviva.presentation.ui.main.PanelPrincipalActivity
import cl.duocuc.aulaviva.presentation.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    // ViewBinding para acceso seguro a las vistas
    private lateinit var binding: ActivityLoginBinding

    // ViewModel usando delegado by viewModels()
    private val viewModel: AuthViewModel by viewModels()

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

                // Pequeño delay para feedback visual
                binding.root.postDelayed({
                    val intent = Intent(this, PanelPrincipalActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }, 400)
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
        // Botón para volver a la pantalla de bienvenida
        binding.goToRegisterButton.setOnClickListener {
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
                viewModel.login(email, password) // Llama a Firebase para autenticar
            }
        }
    }
}
