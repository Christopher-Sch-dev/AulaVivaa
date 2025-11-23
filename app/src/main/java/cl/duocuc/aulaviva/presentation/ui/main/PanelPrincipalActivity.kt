package cl.duocuc.aulaviva.presentation.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import cl.duocuc.aulaviva.presentation.base.BaseActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
import cl.duocuc.aulaviva.databinding.ActivityPanelPrincipalBinding
import cl.duocuc.aulaviva.presentation.ui.auth.LoginActivity
import cl.duocuc.aulaviva.utils.NotificationHelper
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import cl.duocuc.aulaviva.presentation.viewmodel.PanelPrincipalViewModel

class PanelPrincipalActivity : BaseActivity() {

    private lateinit var binding: ActivityPanelPrincipalBinding
    private lateinit var notificationHelper: NotificationHelper
    private var rolActual: String = ""
    private val viewModel: PanelPrincipalViewModel by viewModels()

    // Launcher para pedir permiso de notificaciones (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, envío notificación de bienvenida
            enviarNotificacionBienvenida()
        }
        // Si no lo concede, la app sigue funcionando normal
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanelPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observadores del ViewModel
        viewModel.toastMessage.observe(this, Observer { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        })
        viewModel.demoCodigo.observe(this, Observer { codigo ->
            // Si necesitas hacer algo extra con el código, hacerlo aquí
        })

        // Observadores para info del usuario y logout
        viewModel.userEmail.observe(this, Observer { email ->
            val displayName = email?.substringBefore("@") ?: "Usuario"
            val emoji = "👨‍🏫"
            binding.bienvenidaTextView.text = "$emoji Bienvenido Profesor $displayName"
        })

        viewModel.logoutEvent.observe(this, Observer { loggedOut ->
            if (loggedOut == true) {
                val intent = Intent(this@PanelPrincipalActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        })

        // Inicializo el helper de notificaciones (RECURSO NATIVO #1)
        notificationHelper = NotificationHelper(this)

        setupListeners()
        pedirPermisoNotificaciones()
        cargarDatosUsuario()  // Cargo info del usuario desde Supabase
    }

    /**
     * Carga los datos del usuario desde Supabase y personaliza la UI.
     * Muestra el rol (docente/alumno) y adapta las opciones disponibles.
     */
    private fun cargarDatosUsuario() {
        // Pedimos al ViewModel que cargue/prepare la info si corresponde
        // (PanelPrincipalViewModel inicializa user info en su init)
        // Por defecto, rol actual seguirá como "docente" hasta que se implemente fetch de rol.
        rolActual = "docente"
    }

    /**
     * Pide permiso para notificaciones solo en Android 13+ (Tiramisu).
     * En versiones anteriores no es necesario pedir permiso.
     */
    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya tengo el permiso, envío notificación
                    enviarNotificacionBienvenida()
                }

                else -> {
                    // Pido el permiso
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // En Android 12 o inferior no necesito pedir permiso
            enviarNotificacionBienvenida()
        }
    }

    /**
     * Envía una notificación de bienvenida personalizada
     */
    private fun enviarNotificacionBienvenida() {
        val email = SupabaseAuthManager.getCurrentUserEmail() ?: "Usuario"
        val nombre = email.substringBefore("@")  // Extraigo nombre del email
        notificationHelper.enviarNotificacionBienvenida(nombre)
    }

    private fun setupListeners() {
        // 📚 Botón: Mis Asignaturas (Docente)
        binding.misAsignaturasButton.setOnClickListener {
            try {
                if (rolActual.isEmpty()) {
                    Toast.makeText(
                        this,
                        "⏳ Cargando tu perfil... Intenta de nuevo en un momento",
                        Toast.LENGTH_SHORT
                    ).show()
                    cargarDatosUsuario()
                    return@setOnClickListener
                }

                Log.d("PanelPrincipal", "✅ Abriendo Mis Asignaturas (Docente)")
                startActivity(
                    Intent(
                        this,
                        cl.duocuc.aulaviva.presentation.activity.DocenteAsignaturasActivity::class.java
                    )
                )
            } catch (ex: Exception) {
                Log.e("PanelPrincipal", "❌ Error abriendo asignaturas: ${ex.message}", ex)
                Toast.makeText(this, "No se pudo abrir asignaturas", Toast.LENGTH_SHORT).show()
            }
        }

        // 🧪 Botón: Modo Demostración
        binding.crearClasePruebaButton.setOnClickListener {
            crearAsignaturaYClaseDemo()
        }

        // Botón para cerrar sesión con confirmación
        binding.logoutButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro que quieres salir de Aula Viva?")
                .setPositiveButton("Sí, salir") { _, _ ->
                    lifecycleScope.launch {
                        SupabaseAuthManager.logout()
                        val intent = Intent(this@PanelPrincipalActivity, LoginActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    /**
     * 🎓 Crea una asignatura DEMO y luego una clase de demostración dentro de ella
     * Flujo: Asignatura → Clase con contenido rico
     *
     * ✅ ACTUALIZADO: Ahora crea primero asignatura, luego clase
     */
    private fun crearAsignaturaYClaseDemo() {
        if (rolActual.isEmpty()) {
            Toast.makeText(
                this,
                "⏳ Espera a que se cargue tu perfil...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("🎓 Crear Asignatura y Clase Demo")
            .setMessage("Crea una asignatura completa con:\n\n✅ Asignatura: 'Programación Móvil DEMO'\n✅ Código de acceso único\n✅ Clase: 'Introducción a Kotlin'\n✅ Contenido educativo completo\n\nPerfecto para probar todas las funcionalidades.")
            .setPositiveButton("Crear Demo") { _, _ ->
                Log.d("PanelPrincipal", "🔄 Creando asignatura y clase demo...")
                Toast.makeText(this, "⏳ Creando demostración...", Toast.LENGTH_SHORT).show()

                // Delegar la creación de demo al ViewModel
                viewModel.crearAsignaturaYClaseDemo()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
