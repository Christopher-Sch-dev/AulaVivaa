package cl.duocuc.aulaviva.presentation.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.data.model.Clase

/**
 * Adaptador para mostrar una lista de clases en un RecyclerView.
 * Ahora incluye descripción, badge de PDF y botón ver detalles.
 * ✅ TAREA 3: Agregados callbacks para editar y eliminar
 * ✅ ALUMNO: Parámetro esAlumno para ocultar botones de edición
 */
class ClaseAdapter(
    private var clases: List<Clase> = emptyList(),
    private val onClaseClick: ((Clase) -> Unit)? = null,
    private val onEditarClick: ((Clase) -> Unit)? = null,
    private val onEliminarClick: ((Clase) -> Unit)? = null,
    private val esAlumno: Boolean = false // NUEVO: flag para alumno
) : RecyclerView.Adapter<ClaseAdapter.ClaseViewHolder>() {

    /**
     * ViewHolder: representa cada item individual de la lista.
     * ✅ TAREA 3: Agregados botones editar y eliminar
     */
    class ClaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardClase: CardView = itemView.findViewById(R.id.cardClase)
        val nombre: TextView = itemView.findViewById(R.id.textNombreClase)
        val descripcion: TextView = itemView.findViewById(R.id.textDescripcionClase)
        val fecha: TextView = itemView.findViewById(R.id.textFechaClase)
        val badgePdf: TextView = itemView.findViewById(R.id.badgePdf)
        val btnVerDetalles: Button = itemView.findViewById(R.id.btnVerDetalles)
        val btnCompartir: Button = itemView.findViewById(R.id.btnCompartirClase)
        val btnEditar: Button = itemView.findViewById(R.id.btnEditarClase)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminarClase)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clase, parent, false)
        return ClaseViewHolder(view)
    }

    override fun getItemCount(): Int = clases.size

    override fun onBindViewHolder(holder: ClaseViewHolder, position: Int) {
        val clase = clases[position]

        // Seteo los datos de la clase
        holder.nombre.text = clase.nombre
        holder.descripcion.text = clase.descripcion.ifEmpty { "Sin descripción" }
        holder.fecha.text = clase.fecha

        // Mostrar badge de PDF si existe
        if (clase.archivoPdfNombre.isNotEmpty()) {
            holder.badgePdf.visibility = View.VISIBLE
        } else {
            holder.badgePdf.visibility = View.GONE
        }

        // Animación de entrada
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay((position * 50).toLong())
            .start()

        // Click en el botón ver detalles
        holder.btnVerDetalles.setOnClickListener {
            onClaseClick?.invoke(clase)
        }

        // También click en toda la card
        holder.cardClase.setOnClickListener {
            onClaseClick?.invoke(clase)
        }

        // ✅ OCULTAR BOTONES SI ES ALUMNO
        if (esAlumno) {
            holder.btnEditar.visibility = View.GONE
            holder.btnEliminar.visibility = View.GONE
            holder.btnCompartir.visibility = View.GONE
        } else {
            // ✅ TAREA 3: Botón Editar (solo docente)
            holder.btnEditar.setOnClickListener {
                onEditarClick?.invoke(clase)
            }

            // ✅ TAREA 3: Botón Eliminar (solo docente)
            holder.btnEliminar.setOnClickListener {
                onEliminarClick?.invoke(clase)
            }

            // Botón compartir (solo docente)
            holder.btnCompartir.setOnClickListener {
                val compartirIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Clase: ${clase.nombre}")
                    putExtra(
                    Intent.EXTRA_TEXT, """
                    📚 Información de Clase - Aula Viva

                    Nombre: ${clase.nombre}
                    Descripción: ${clase.descripcion}
                    📅 Fecha: ${clase.fecha}
                    ${if (clase.archivoPdfNombre.isNotEmpty()) "📄 Incluye material PDF" else ""}

                    Compartido desde Aula Viva 🎓
                """.trimIndent()
                )
            }

            val chooser = Intent.createChooser(compartirIntent, "Compartir clase con...")
            holder.itemView.context.startActivity(chooser)
            }
        }
    }

    /**
     * Actualiza la lista de clases y notifica al RecyclerView
     */
    fun updateList(nuevasClases: List<Clase>) {
        clases = nuevasClases
        notifyDataSetChanged()
    }
}
