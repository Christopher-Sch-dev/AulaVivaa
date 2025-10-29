package cl.duocuc.aulaviva.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.data.model.Clase

/**
 * Adaptador para mostrar una lista de clases en un RecyclerView
 * Recibe la lista de objetos Clase y los "conecta" con las vistas del layout
 */
class ClaseAdapter(
    private var clases: List<Clase> = emptyList(),
    private val onClaseClick: ((Clase) -> Unit)? = null
): RecyclerView.Adapter<ClaseAdapter.ClaseViewHolder>() {

    /**
     * ViewHolder: representa cada item individual de la lista
     * Aquí "guardamos" las referencias a los TextView para no buscarlos cada vez
     */
    class ClaseViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        // Referencias a los textos que voy a mostrar en cada item
        val nombre: TextView = itemView.findViewById(R.id.textNombreClase)
        val fecha: TextView = itemView.findViewById(R.id.textFechaClase)
    }

    /**
     * Método que crea una nueva vista cuando el RecyclerView la necesita
     * Inflo el layout item_clase.xml y lo convierto en un ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clase, parent, false)
        return ClaseViewHolder(view)
    }

    /**
     * Le digo al RecyclerView cuántos items tiene que mostrar
     * Simplemente retorno el tamaño de mi lista de clases
     */
    override fun getItemCount(): Int = clases.size

    /**
     * Método que "rellena" cada item con los datos correspondientes
     * position indica qué elemento de la lista tengo que mostrar
     */
    override fun onBindViewHolder(holder: ClaseViewHolder, position: Int) {
        // Tomo la clase en la posición actual y seteo sus datos en la vista
        val clase = clases[position]
        holder.nombre.text = clase.nombre
        holder.fecha.text = clase.fecha
        
        // Si hay listener de click, lo configuro
        onClaseClick?.let { listener ->
            holder.itemView.setOnClickListener {
                listener(clase)
            }
        }
    }

    /**
     * Actualizar la lista de clases
     */
    fun updateList(newClases: List<Clase>) {
        clases = newClases
        notifyDataSetChanged()
    }
}