package cl.duocuc.aulaviva.presentation.ui.clases

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duocuc.aulaviva.databinding.ActivityListaClasesBinding
import cl.duocuc.aulaviva.data.repository.IARepository
import cl.duocuc.aulaviva.presentation.adapter.ClaseAdapter
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // Sincronizar clases desde Firestore al abrir la pantalla
        // Room mostrará datos inmediatamente, y Firestore actualizará si hay conexión
        viewModel.sincronizarConFirestore()
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
        viewModel.isLoading.observe(this, Observer { _ ->
            // Aquí podrías mostrar un loading si agregas un ProgressBar al layout
            // Por ahora se mantiene sin UI para simplificar
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
        
        // INTEGRACIÓN IA: Botón para generar resumen con IA
        binding.btnGenerarResumenIA.setOnClickListener {
            generarResumenConIA()
        }
    }
    
    /**
     * Genera un resumen automático usando IA.
     * Esta función demuestra la integración de IA en la app.
     */
    private fun generarResumenConIA() {
        val iaRepo = IARepository()
        
        // Texto de ejemplo (en producción vendría del contenido de las clases)
        val textoClase = """
            Aula Viva es una aplicación Android desarrollada con Kotlin que implementa
            arquitectura MVVM, Room Database, Firebase Firestore, notificaciones push,
            y funciones de IA para potenciar la educación presencial.
        """.trimIndent()
        
        // Muestro diálogo de carga mientras la IA procesa
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("🤖 IA procesando...")
            .setMessage("La Inteligencia Artificial está analizando el contenido")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        // Ejecuto la IA en background (corrutina)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Llamo al repositorio de IA
                val resumen = iaRepo.generarResumen(textoClase)
                
                // Vuelvo al hilo principal para mostrar resultado
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    
                    // Muestro el resumen en un diálogo
                    AlertDialog.Builder(this@ListaClasesActivity)
                        .setTitle("📝 Resumen generado por IA")
                        .setMessage(resumen)
                        .setPositiveButton("Entendido", null)
                        .setNeutralButton("Generar Glosario") { _, _ ->
                            generarGlosarioConIA(textoClase)
                        }
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@ListaClasesActivity,
                        "Error al generar resumen: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    /**
     * Genera un glosario automático de términos técnicos.
     */
    private fun generarGlosarioConIA(textoClase: String) {
        val iaRepo = IARepository()
        
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("🤖 IA analizando términos...")
            .setMessage("Identificando conceptos clave")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val glosario = iaRepo.generarGlosario(textoClase)
                
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    
                    AlertDialog.Builder(this@ListaClasesActivity)
                        .setTitle("📚 Glosario generado por IA")
                        .setMessage(glosario)
                        .setPositiveButton("Entendido", null)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@ListaClasesActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}