package cl.duocuc.aulaviva.presentation.ui.clases

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.databinding.ActivityCrearEditarClaseBinding
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
import cl.duocuc.aulaviva.data.supabase.SupabaseClientProvider
import com.google.android.material.datepicker.MaterialDatePicker
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

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
    private var tempPdfUri: Uri? = null
    private var tempPdfName: String = ""

    private val pickPdfLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(uri)
            tempPdfUri = uri
            tempPdfName = fileName
            binding.textViewPdfSelected.text = "📄 $fileName"
            binding.buttonSelectPdf.text = "Cambiar PDF"
        }
    }

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
        setupPdfPicker()
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

        binding.inputFechaClase.setOnClickListener {
            // Mostrar selector de fecha y hora
            mostrarSelectorFechaHora()
        }
    }

    /**
     * Muestra selector de fecha y luego de hora (formato chileno).
     */
    private fun mostrarSelectorFechaHora() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona la fecha")
            .build()

        datePicker.show(supportFragmentManager, "DATE_PICKER")

        datePicker.addOnPositiveButtonClickListener { selection ->
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Santiago"))
                cal.timeInMillis = selection

                // Ahora pedir hora
                val horaActual = Calendar.getInstance(TimeZone.getTimeZone("America/Santiago"))
                val hora = horaActual.get(Calendar.HOUR_OF_DAY)
                val minuto = horaActual.get(Calendar.MINUTE)

                // TimePickerDialog simple
                android.app.TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val fechaFormateada = sdf.format(cal.time)
                        val horaFormateada = String.format("%02d:%02d", hourOfDay, minute)
                        binding.inputFechaClase.setText("$fechaFormateada $horaFormateada")
                    },
                    hora,
                    minuto,
                    true  // Formato 24 horas
                ).show()

            } catch (e: Exception) {
                Toast.makeText(this, "Error al seleccionar fecha/hora", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupPdfPicker() {
        binding.buttonSelectPdf.setOnClickListener {
            pickPdfLauncher.launch("application/pdf")
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

    private fun getFileName(uri: Uri): String {
        var name = "documento.pdf"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        return name
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

        // Si hay PDF, subir primero
        if (tempPdfUri != null) {
            subirPdfYCrearClase(nombre, descripcion, fecha)
        } else {
            // Crear clase sin PDF
            crearClaseSinPdf(nombre, descripcion, fecha)
        }
    }

    private fun subirPdfYCrearClase(nombre: String, descripcion: String, fecha: String) {
        lifecycleScope.launch {
            try {
                binding.buttonGuardar.isEnabled = false
                binding.buttonGuardar.text = "Subiendo PDF..."

                val pdfUrl = subirPdfASupabase(tempPdfUri!!, tempPdfName)

                if (pdfUrl.isNotEmpty()) {
                    // Crear clase con PDF
                    viewModel.crearClase(
                        nombre = nombre,
                        descripcion = descripcion,
                        fecha = fecha,
                        archivoPdfUrl = pdfUrl,
                        archivoPdfNombre = tempPdfName,
                        asignaturaId = asignaturaId
                    )

                    observarResultado()
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CrearEditarClaseActivity,
                            "Error al subir PDF",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.buttonGuardar.isEnabled = true
                        binding.buttonGuardar.text = "Guardar Clase"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CrearEditarClaseActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.buttonGuardar.isEnabled = true
                    binding.buttonGuardar.text = "Guardar Clase"
                }
            }
        }
    }

    private fun crearClaseSinPdf(nombre: String, descripcion: String, fecha: String) {
        viewModel.crearClase(
            nombre = nombre,
            descripcion = descripcion,
            fecha = fecha,
            archivoPdfUrl = "",
            archivoPdfNombre = "",
            asignaturaId = asignaturaId
        )
        observarResultado()
    }

    private fun observarResultado() {
        viewModel.operationSuccess.observe(this) { mensaje ->
            mensaje?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                binding.buttonGuardar.isEnabled = true
                binding.buttonGuardar.text = "Guardar Clase"
            }
        }
    }

    private suspend fun subirPdfASupabase(uri: Uri, nombreArchivo: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val supabase = SupabaseClientProvider.getClient()
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@withContext ""

                val uniqueName = "${UUID.randomUUID()}_$nombreArchivo"
                val bucket = supabase.storage["clases"]

                bucket.upload(uniqueName, bytes)

                val publicUrl = bucket.publicUrl(uniqueName)
                publicUrl
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
