package cl.duocuc.aulaviva.presentation.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.data.model.Clase

/**
 * ListAdapter para mostrar una lista de clases de forma eficiente.
 * Usa DiffUtil para aplicar cambios parciales y evitar redraws completos.
 * Eliminadas animaciones en cada bind para evitar jank durante el scroll.
 */
class ClaseAdapter(
    private val onClaseClick: ((Clase) -> Unit)? = null,
    private val onEditarClick: ((Clase) -> Unit)? = null,
    private val onEliminarClick: ((Clase) -> Unit)? = null,
    private val esAlumno: Boolean = false
) : ListAdapter<Clase, ClaseAdapter.ClaseViewHolder>(ClaseDiffCallback()) {

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

    override fun onBindViewHolder(holder: ClaseViewHolder, position: Int) {
        val clase = getItem(position)

        holder.nombre.text = clase.nombre
        holder.descripcion.text = clase.descripcion.ifEmpty { "Sin descripción" }
        holder.fecha.text = clase.fecha

        holder.badgePdf.visibility = if (clase.archivoPdfNombre.isNotEmpty()) View.VISIBLE else View.GONE

        // Clicks
        holder.btnVerDetalles.setOnClickListener { onClaseClick?.invoke(clase) }
        holder.cardClase.setOnClickListener { onClaseClick?.invoke(clase) }

        if (esAlumno) {
            holder.btnEditar.visibility = View.GONE
            holder.btnEliminar.visibility = View.GONE
            holder.btnCompartir.visibility = View.GONE
        } else {
            holder.btnEditar.setOnClickListener { onEditarClick?.invoke(clase) }
            holder.btnEliminar.setOnClickListener { onEliminarClick?.invoke(clase) }
            holder.btnCompartir.setOnClickListener {
                val compartirIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Clase: ${clase.nombre}")
                    putExtra(Intent.EXTRA_TEXT, "📚 ${clase.nombre}\n${clase.descripcion}\n📅 ${clase.fecha}")
                }
                val chooser = Intent.createChooser(compartirIntent, "Compartir clase con...")
                holder.itemView.context.startActivity(chooser)
            }
        }
    }

    /**
     * Conservamos updateList por compatibilidad con el resto del código.
     */
    fun updateList(nuevasClases: List<Clase>) {
        submitList(nuevasClases)
    }

    class ClaseDiffCallback : DiffUtil.ItemCallback<Clase>() {
        override fun areItemsTheSame(oldItem: Clase, newItem: Clase): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Clase, newItem: Clase): Boolean = oldItem == newItem
    }
}
