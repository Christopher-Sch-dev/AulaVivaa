package cl.duocuc.aulaviva.presentation.ui.clases

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duocuc.aulaviva.databinding.ActivityListaClasesBinding
import cl.duocuc.aulaviva.presentation.adapter.ClaseAdapter
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel

class ListaClasesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListaClasesBinding
    private val viewModel: ClaseViewModel by viewModels()
    private lateinit var adapter: ClaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaClasesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Cargar clases al inicio
        viewModel.cargarClases()
    }

    private fun setupRecyclerView() {
        adapter = ClaseAdapter(
            clases = emptyList(),
            onClaseClick = { clase ->
                Toast.makeText(this, "Clase: ${clase.nombre}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerViewClases.apply {
            layoutManager = LinearLayoutManager(this@ListaClasesActivity)
            adapter = this@ListaClasesActivity.adapter
        }
    }

    private fun setupObservers() {
        // Observer para lista de clases
        viewModel.clases.observe(this, Observer { clases ->
            adapter.updateList(clases)
        })

        // Observer para loading (sin progressBar por ahora)
        viewModel.isLoading.observe(this, Observer { isLoading ->
            // Aquí podrías mostrar un loading si agregas un ProgressBar al layout
        })

        // Observer para errores
        viewModel.error.observe(this, Observer { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        })

        // Observer para operaciones exitosas
        viewModel.operationSuccess.observe(this, Observer { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        })
    }

    private fun setupListeners() {
        // Botón para crear nueva clase
        binding.crearClaseButton.setOnClickListener {
            // Aquí podrías abrir un diálogo o nueva actividad para crear clase
            Toast.makeText(this, "Función para agregar clase", Toast.LENGTH_SHORT).show()
        }
    }
}