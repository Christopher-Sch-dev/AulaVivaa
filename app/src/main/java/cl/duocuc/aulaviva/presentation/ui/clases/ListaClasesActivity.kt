package cl.duocuc.aulaviva.presentation.ui.clases

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListaClasesActivity : AppCompatActivity() {

    private val viewModel: ClaseViewModel by viewModels()
    private lateinit var adapter: ClaseAdapter

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

        val pdfNombreSeleccionado = ""
        val pdfUrlSeleccionada = ""

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSeleccionarPdf.setOnClickListener {
            textPdfSeleccionado.text = "Función de subir PDF próximamente"
            Toast.makeText(this, "Función de subir PDF próximamente", Toast.LENGTH_SHORT).show()
        }

        dialogView.findViewById<Button>(R.id.btnCancelar).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<Button>(R.id.btnGuardarClase).setOnClickListener {
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
                val nuevaClase = cl.duocuc.aulaviva.data.model.Clase(
                    id = "",
                    nombre = nombre,
                    descripcion = descripcion,
                    fecha = fecha,
                    archivoPdfUrl = pdfUrlSeleccionada,
                    archivoPdfNombre = pdfNombreSeleccionado,
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
                                "✅ Clase creada exitosamente",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
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
                        }
                    }
                )
            }
        }

        dialog.show()
    }
}
