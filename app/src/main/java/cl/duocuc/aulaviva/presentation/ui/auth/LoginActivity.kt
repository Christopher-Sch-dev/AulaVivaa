package cl.duocuc.aulaviva.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.databinding.ActivityLoginBinding
import cl.duocuc.aulaviva.presentation.viewmodel.AuthViewModel
import cl.duocuc.aulaviva.presentation.ui.main.PanelPrincipalActivity

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
        viewModel.isLoading.observe(this, Observer { isLoading ->
            // Mostrar/ocultar elementos según el estado de carga
            binding.loginButton.isEnabled = !isLoading
            binding.goToRegisterButton.isEnabled = !isLoading
        })

        // Observer para errores
        viewModel.error.observe(this, Observer { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })

        // Observer para login exitoso
        viewModel.loginSuccess.observe(this, Observer { success ->
            if (success) {
                Toast.makeText(this, "Bienvenido!", Toast.LENGTH_SHORT).show()
                // Ir al Panel Principal (Dashboard)
                val intent = Intent(this, PanelPrincipalActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                // ANIMACIÓN FUNCIONAL: Transición suave entre pantallas (API 34+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left)
                } else {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        })
    }

    private fun setupListeners() {
        // Botón para ir a registro
        binding.goToRegisterButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Botón de login
        binding.loginButton.setOnClickListener {
            // ANIMACIÓN FUNCIONAL: Feedback al tocar el botón
            it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_scale))
            
            val email = binding.emailInput.text?.toString()?.trim() ?: ""
            val password = binding.passwordInput.text?.toString() ?: ""

            // Validación básica y sencilla
            var valid = true
            if (!viewModel.isValidEmail(email)) {
                binding.emailLayout.error = "Correo inválido"
                valid = false
            } else {
                binding.emailLayout.error = null
            }

            if (!viewModel.isValidPassword(password)) {
                binding.passwordLayout.error = "La contraseña debe tener al menos 6 caracteres"
                valid = false
            } else {
                binding.passwordLayout.error = null
            }

            // Si los datos están bien, intenta login
            if (valid) {
                viewModel.login(email, password)
            }
        }
    }
}
