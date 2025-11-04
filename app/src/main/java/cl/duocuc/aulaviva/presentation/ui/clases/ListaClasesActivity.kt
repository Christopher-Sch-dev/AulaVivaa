package cl.duocuc.aulaviva.presentation.ui.clases

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.presentation.adapter.ClaseAdapter
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ListaClasesActivity : AppCompatActivity() {

    private val viewModel: ClaseViewModel by viewModels()
    private lateinit var adapter: ClaseAdapter

    // Estado temporal para selección de PDF en el diálogo
    private var tempPdfUri: String = ""
    private var tempPdfName: String = ""
    private var isSaving: Boolean = false

    private val pickPdfLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                tempPdfUri = it.toString()
                // Obtener nombre del archivo
                var displayName = "archivo.pdf"
                contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
                tempPdfName = displayName
                // Actualizo texto si el diálogo está visible
                currentPdfTextView?.text = displayName
            }
        }

    private var currentPdfTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_clases)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.sincronizarConFirestore()
    }

    private fun setupRecyclerView() {
        adapter = ClaseAdapter(
            clases = emptyList(),
            onClaseClick = { clase ->
                val intent = Intent(this, DetalleClaseActivity::class.java)
                intent.putExtra("CLASE_ID", clase.id)
                startActivity(intent)
            }
        )

        val rv = findViewById<RecyclerView>(R.id.recyclerViewClases)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.clases.observe(this, Observer { clases ->
            adapter.updateList(clases)
            val empty = findViewById<TextView>(R.id.textEmptyState)
            empty?.visibility =
                if (clases.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        })

        viewModel.isLoading.observe(this, Observer { _ -> })

        viewModel.error.observe(this, Observer { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        })

        viewModel.operationSuccess.observe(this, Observer { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        })
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.crearClaseButton)?.setOnClickListener {
            mostrarDialogoCrearClase()
        }
    }

    private fun mostrarDialogoCrearClase() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_crear_clase, null)

        val inputNombre =
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputNombreClase)
        val inputDescripcion =
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputDescripcionClase)
        val inputFecha =
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputFechaClase)
        val btnSeleccionarPdf = dialogView.findViewById<Button>(R.id.btnSeleccionarPdf)
        val textPdfSeleccionado = dialogView.findViewById<TextView>(R.id.textPdfSeleccionado)
        val layoutNombre =
            dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layoutNombreClase)
        val layoutDescripcion =
            dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layoutDescripcionClase)
        val layoutFecha =
            dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layoutFechaClase)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardarClase)

        // Reset estado PDF al abrir diálogo
        tempPdfUri = ""
        tempPdfName = ""
        currentPdfTextView = textPdfSeleccionado

        // Fecha: abrir selector y formatear dd/MM/yyyy
        inputFecha?.isFocusable = false
        inputFecha?.isClickable = true
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona la fecha")
            .build()
        inputFecha?.setOnClickListener {
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
        datePicker.addOnPositiveButtonClickListener { selection ->
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.timeInMillis = selection
                inputFecha?.setText(sdf.format(cal.time))
            } catch (_: Exception) {
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSeleccionarPdf.setOnClickListener {
            currentPdfTextView = textPdfSeleccionado
            pickPdfLauncher.launch(arrayOf("application/pdf"))
        }

        dialogView.findViewById<Button>(R.id.btnCancelar).setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {
            if (isSaving) return@setOnClickListener

            val nombre = inputNombre?.text?.toString()?.trim() ?: ""
            val descripcion = inputDescripcion?.text?.toString()?.trim() ?: ""
            val fecha = inputFecha?.text?.toString()?.trim() ?: ""

            var valid = true
            if (nombre.isEmpty()) {
                layoutNombre?.error = "El título es obligatorio"; valid = false
            } else layoutNombre?.error = null
            if (descripcion.isEmpty()) {
                layoutDescripcion?.error = "La descripción es obligatoria"; valid = false
            } else layoutDescripcion?.error = null
            if (fecha.isEmpty()) {
                layoutFecha?.error = "La fecha es obligatoria"; valid = false
            } else layoutFecha?.error = null

            if (valid) {
                isSaving = true
                btnGuardar.isEnabled = false

                val nuevaClase = cl.duocuc.aulaviva.data.model.Clase(
                    id = "",
                    nombre = nombre,
                    descripcion = descripcion,
                    fecha = fecha,
                    archivoPdfUrl = tempPdfUri,
                    archivoPdfNombre = tempPdfName,
                    creador = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        ?: ""
                )

                val repository = cl.duocuc.aulaviva.data.repository.ClaseRepository(this)
                repository.crearClaseAsync(
                    clase = nuevaClase,
                    onSuccess = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                this@ListaClasesActivity,
                                "✅ Clase creada",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                            isSaving = false
                            btnGuardar.isEnabled = true
                            viewModel.sincronizarConFirestore()
                        }
                    },
                    onError = { error ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                this@ListaClasesActivity,
                                "Error: $error",
                                Toast.LENGTH_SHORT
                            ).show()
                            isSaving = false
                            btnGuardar.isEnabled = true
                        }
                    }
                )
            }
        }

        dialog.show()
    }
}
