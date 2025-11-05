package cl.duocuc.aulaviva.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.databinding.ActivityWelcomeBinding

/**
 * WelcomeActivity - Pantalla de bienvenida inicial
 *
 * Esta es la primera pantalla que ve el usuario al abrir la app.
 * Muestra 2 opciones principales:
 * - Iniciar Sesión (para usuarios que ya tienen cuenta)
 * - Crear Cuenta (para nuevos usuarios)
 *
 * Si el usuario ya tiene una sesión activa en Supabase,
 * esta pantalla se salta automáticamente y va directo al Panel Principal.
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NO verificar sesión automáticamente - forzar login manual
        // checkIfUserIsLoggedIn()

        setupListeners()
        setupAnimations()
    }

    /**
     * DESHABILITADO: No auto-login para evitar usuarios sin rol
     * Forzar login manual cada vez para verificar rol en tabla usuarios
     */
    /*
    private fun checkIfUserIsLoggedIn() {
        if (SupabaseAuthManager.isLoggedIn()) {
            val intent = Intent(
                this,
                cl.duocuc.aulaviva.presentation.ui.main.PanelPrincipalActivity::class.java
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    */

    private fun setupListeners() {
        // Botón "Iniciar Sesión" - Va a LoginActivity
        binding.btnSignIn.setOnClickListener {
            // Animación de feedback al tocar
            it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_scale))

            // Navegar a la pantalla de login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // Animación de transición entre pantallas
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        // Botón "Crear Cuenta" - Va a RegisterActivity
        binding.btnSignUp.setOnClickListener {
            // Animación de feedback al tocar
            it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_scale))

            // Navegar a la pantalla de registro
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)

            // Animación de transición entre pantallas
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    /**
     * Animaciones de entrada para los botones.
     * Los botones aparecen con un efecto de fade-in y deslizamiento hacia arriba.
     */
    private fun setupAnimations() {
        // Botón de Iniciar Sesión aparece con animación
        binding.btnSignIn.apply {
            alpha = 0f
            translationY = 50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(200)
                .setDuration(500)
                .start()
        }

        // Botón de Crear Cuenta aparece con animación (un poco después)
        binding.btnSignUp.apply {
            alpha = 0f
            translationY = 50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(300)
                .setDuration(500)
                .start()
        }
    }
}
