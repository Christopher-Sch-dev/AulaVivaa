package cl.duocuc.aulaviva

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adaptador para mostrar una lista de clases en un RecyclerView
 * Recibe la lista de objetos Clase y los "conecta" con las vistas del layout
 */
class ClaseAdapter(private val clases: List<Clase>): RecyclerView.Adapter<ClaseAdapter.ClaseViewHolder>() {

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
        holder.nombre.text = clases[position].nombre
        holder.fecha.text = clases[position].fecha
    }
}
