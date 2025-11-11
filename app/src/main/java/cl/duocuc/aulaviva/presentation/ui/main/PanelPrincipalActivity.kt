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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
import cl.duocuc.aulaviva.databinding.ActivityPanelPrincipalBinding
import cl.duocuc.aulaviva.presentation.ui.auth.LoginActivity
import cl.duocuc.aulaviva.utils.NotificationHelper
import kotlinx.coroutines.launch

class PanelPrincipalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanelPrincipalBinding
    private lateinit var notificationHelper: NotificationHelper
    private var rolActual: String = ""

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
        val uid = SupabaseAuthManager.getCurrentUserId()
        val email = SupabaseAuthManager.getCurrentUserEmail()

        if (uid == null || email == null) {
            Log.e("PanelPrincipal", "❌ Usuario no autenticado")
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_LONG).show()
            // Redirigir al login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d("PanelPrincipal", "✅ Usuario cargado: $email")

        // Por defecto, todos los usuarios registrados son docentes
        rolActual = "docente"

        val emoji = "👨‍🏫"
        val nombreCorto = email.substringBefore("@")
        val mensaje = "Bienvenido Profesor $nombreCorto"

        binding.bienvenidaTextView.text = "$emoji $mensaje"
        binding.irAClasesButton.text = "📊 Gestionar clases"
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
        binding.irAClasesButton.setOnClickListener {
            try {
                if (rolActual.isEmpty()) {
                    Log.w("PanelPrincipal", "⚠️ Rol vacío al hacer clic - Esperando carga...")
                    Toast.makeText(
                        this,
                        "⏳ Cargando tu perfil... Intenta de nuevo en un momento",
                        Toast.LENGTH_LONG
                    ).show()
                    // Intentar recargar datos
                    cargarDatosUsuario()
                    return@setOnClickListener
                }

                Log.d("PanelPrincipal", "✅ Navegando con rol: $rolActual")

                if (rolActual == "docente") {
                    startActivity(
                        Intent(
                            this,
                            cl.duocuc.aulaviva.presentation.ui.clases.ListaClasesActivity::class.java
                        )
                    )
                } else {
                    startActivity(Intent(this, PanelAlumnoActivity::class.java))
                }
            } catch (ex: Exception) {
                Log.e("PanelPrincipal", "❌ Error abriendo módulo: ${ex.message}", ex)
                Toast.makeText(this, "No se pudo abrir el módulo", Toast.LENGTH_SHORT).show()
            }
        }

        // 🎓 Botón NUEVO: Crear clase de prueba
        binding.crearClasePruebaButton.setOnClickListener {
            crearClaseDemostracion()
        }

        // 🎓 Botón NUEVO: Mis Asignaturas
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

                Log.d("PanelPrincipal", "✅ Abriendo Mis Asignaturas")
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
     * 🎓 Crea una clase de demostración con contenido rico
     * Esto sirve para testing y para la defensa EV2
     *
     * ✅ Ahora con mejor logging y manejo de errores
     */
    private fun crearClaseDemostracion() {
        if (rolActual.isEmpty()) {
            Toast.makeText(
                this,
                "⏳ Espera a que se cargue tu perfil...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("🎓 Clase de Demostración")
            .setMessage("¿Quieres crear una clase de prueba con contenido educativo completo?\n\nIncluye:\n• Material educativo rico\n• PDF simulado de Kotlin\n• Links a documentación\n• Listo para probar IA")
            .setPositiveButton("Crear") { _, _ ->
                Log.d("PanelPrincipal", "🔄 Creando clase demo...")
                Toast.makeText(this, "⏳ Creando clase demo...", Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    try {
                        val repository =
                            cl.duocuc.aulaviva.data.repository.ClaseRepository(this@PanelPrincipalActivity)
                        repository.crearClaseDePrueba(
                            onSuccess = {
                                Log.d("PanelPrincipal", "✅ Clase demo creada exitosamente")
                                Toast.makeText(
                                    this@PanelPrincipalActivity,
                                    "✅ Clase de prueba creada! Ve a 'Gestionar clases'",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onError = { error ->
                                Log.e("PanelPrincipal", "❌ Error creando clase demo: $error")
                                Toast.makeText(
                                    this@PanelPrincipalActivity,
                                    "❌ Error: $error",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("PanelPrincipal", "❌ Excepción creando clase demo", e)
                        Toast.makeText(
                            this@PanelPrincipalActivity,
                            "❌ Error inesperado: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
