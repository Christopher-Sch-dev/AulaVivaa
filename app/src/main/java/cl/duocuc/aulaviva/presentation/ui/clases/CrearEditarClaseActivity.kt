package cl.duocuc.aulaviva.presentation.ui.clases

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import cl.duocuc.aulaviva.databinding.ActivityCrearEditarClaseBinding
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Activity simple para crear o editar una clase.
 * Recibe asignaturaId obligatorio del intent.
 */
class CrearEditarClaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearEditarClaseBinding
    private val viewModel: ClaseViewModel by viewModels()
    private var asignaturaId: String = ""
    private var asignaturaNombre: String = ""
    private var claseId: String? = null  // null = crear, valor = editar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearEditarClaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener datos del intent
        asignaturaId = intent.getStringExtra("ASIGNATURA_ID") ?: ""
        asignaturaNombre = intent.getStringExtra("ASIGNATURA_NOMBRE") ?: "Asignatura"
        claseId = intent.getStringExtra("CLASE_ID")  // null si es crear

        if (asignaturaId.isEmpty()) {
            Toast.makeText(this, "Error: No se especificó asignatura", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupDatePicker()
        setupButtons()

        // Si es edición, cargar datos
        claseId?.let { cargarDatosClase(it) }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = if (claseId == null) "Nueva Clase" else "Editar Clase"
            subtitle = asignaturaNombre
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupDatePicker() {
        binding.inputFechaClase.isFocusable = false
        binding.inputFechaClase.isClickable = true

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona la fecha")
            .build()

        binding.inputFechaClase.setOnClickListener {
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        datePicker.addOnPositiveButtonClickListener { selection ->
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.timeInMillis = selection
                binding.inputFechaClase.setText(sdf.format(cal.time))
            } catch (_: Exception) {
                Toast.makeText(this, "Error al seleccionar fecha", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupButtons() {
        binding.buttonGuardar.setOnClickListener {
            guardarClase()
        }

        binding.buttonCancelar.setOnClickListener {
            finish()
        }
    }

    private fun cargarDatosClase(id: String) {
        // TODO: Cargar datos de la clase existente desde ViewModel
        // Por ahora dejamos vacío, se puede implementar después
        Toast.makeText(this, "Modo edición (implementar carga de datos)", Toast.LENGTH_SHORT).show()
    }

    private fun guardarClase() {
        val nombre = binding.inputNombreClase.text.toString().trim()
        val descripcion = binding.inputDescripcionClase.text.toString().trim()
        val fecha = binding.inputFechaClase.text.toString().trim()

        // Validaciones simples
        if (nombre.isEmpty()) {
            binding.layoutNombreClase.error = "El nombre es obligatorio"
            return
        }
        if (descripcion.isEmpty()) {
            binding.layoutDescripcionClase.error = "La descripción es obligatoria"
            return
        }
        if (fecha.isEmpty()) {
            binding.layoutFechaClase.error = "La fecha es obligatoria"
            return
        }

        // Limpiar errores
        binding.layoutNombreClase.error = null
        binding.layoutDescripcionClase.error = null
        binding.layoutFechaClase.error = null

        // Crear objeto Clase
        val clase = Clase(
            id = claseId ?: "",  // Vacío si es nueva, se genera en el repository
            nombre = nombre,
            descripcion = descripcion,
            fecha = fecha,
            archivoPdfUrl = "",  // Por ahora sin PDF (se puede agregar después)
            archivoPdfNombre = "",
            creador = SupabaseAuthManager.getCurrentUserId() ?: "",
            asignaturaId = asignaturaId  // OBLIGATORIO
        )

        // Guardar en ViewModel
        viewModel.crearClase(
            clase = clase,
            onSuccess = {
                Toast.makeText(this, "✅ Clase guardada", Toast.LENGTH_SHORT).show()
                finish()
            },
            onError = { error ->
                Toast.makeText(this, "❌ Error: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
