package cl.duocuc.aulaviva

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ListaClasesActivity : AppCompatActivity() {

    // Instancias para conectar con Firestore y obtener el UID del usuario actual
    private val firestore = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Referencias del RecyclerView y su adaptador
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClaseAdapter
    private val listaClases = mutableListOf<Clase>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_clases)

        recyclerView = findViewById(R.id.recyclerViewClases)
        val crearClaseButton = findViewById<Button>(R.id.crearClaseButton)

        // Configuro el RecyclerView para mostrar las clases en lista vertical
        adapter = ClaseAdapter(listaClases)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Cargo las clases desde Firestore al iniciar
        cargarClases()

        // Botón para mostrar el diálogo de crear clase
        crearClaseButton.setOnClickListener {
            mostrarDialogoCrearClase()
        }
    }

    // Trae las clases desde Firestore filtrando solo las del usuario actual
    private fun cargarClases() {
        firestore.collection("clases")
            .whereEqualTo("creador", uid) // Filtro por UID para mostrar solo mis clases
            .get()
            .addOnSuccessListener { result ->
                listaClases.clear() // Limpio la lista antes de agregar los datos actualizados
                for (doc in result) {
                    // Convierto cada documento en un objeto Clase y le asigno su ID
                    val clase = doc.toObject(Clase::class.java).copy(id = doc.id)
                    listaClases.add(clase)
                }
                adapter.notifyDataSetChanged() // Notifico al adaptador que los datos cambiaron
            }
    }

    // Muestra un diálogo con campos para crear una nueva clase
    private fun mostrarDialogoCrearClase() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_crear_clase, null)
        val inputNombre = view.findViewById<EditText>(R.id.inputNombreClase)
        val inputFecha = view.findViewById<EditText>(R.id.inputFechaClase)

        AlertDialog.Builder(this)
            .setTitle("Crear nueva clase")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = inputNombre.text.toString().trim()
                val fecha = inputFecha.text.toString().trim()

                // Valido que ambos campos tengan contenido antes de guardar
                if (nombre.isNotEmpty() && fecha.isNotEmpty()) {
                    val nuevaClase = Clase(nombre = nombre, fecha = fecha, creador = uid)

                    // Guardo la nueva clase en Firestore
                    firestore.collection("clases").add(nuevaClase)
                        .addOnSuccessListener { cargarClases() } // Recargo la lista si se creó bien
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al crear: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
