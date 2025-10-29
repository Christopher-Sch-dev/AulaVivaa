package cl.duocuc.aulaviva.presentation.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.data.model.Clase

/**
 * Adaptador para mostrar una lista de clases en un RecyclerView.
 * Ahora incluye funcionalidad de compartir (Recurso Nativo Android).
 * 
 * Pensamiento: El adapter es el puente entre mis datos (List<Clase>)
 * y las vistas que se muestran en pantalla. RecyclerView recicla las vistas
 * para ahorrar memoria, por eso es más eficiente que un ScrollView con muchos items.
 */
class ClaseAdapter(
    private var clases: List<Clase> = emptyList(),
    private val onClaseClick: ((Clase) -> Unit)? = null
): RecyclerView.Adapter<ClaseAdapter.ClaseViewHolder>() {

    /**
     * ViewHolder: representa cada item individual de la lista.
     * Aquí guardo referencias a todas las vistas del item.
     */
    class ClaseViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.textNombreClase)
        val fecha: TextView = itemView.findViewById(R.id.textFechaClase)
        val btnCompartir: Button = itemView.findViewById(R.id.btnCompartirClase)
    }

    /**
     * Crea una nueva vista cuando el RecyclerView la necesita.
     * Inflo el layout item_clase.xml (ahora es un MaterialCardView)
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clase, parent, false)
        return ClaseViewHolder(view)
    }

    /**
     * Retorna cuántos items tiene que mostrar el RecyclerView
     */
    override fun getItemCount(): Int = clases.size

    /**
     * Rellena cada item con los datos correspondientes.
     * Aquí configuro el botón compartir (RECURSO NATIVO #2).
     */
    override fun onBindViewHolder(holder: ClaseViewHolder, position: Int) {
        val clase = clases[position]
        
        // Seteo los datos de la clase
        holder.nombre.text = clase.nombre
        holder.fecha.text = "📅 ${clase.fecha}"
        
        // ANIMACIÓN FUNCIONAL: Items aparecen con fade-in y slide up
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay((position * 50).toLong())  // Delay escalonado para efecto cascada
            .start()
        
        // Click en toda la card
        onClaseClick?.let { listener ->
            holder.itemView.setOnClickListener {
                listener(clase)
            }
        }
        
        // RECURSO NATIVO #2: Compartir contenido usando Intent
        holder.btnCompartir.setOnClickListener {
            // Intent.ACTION_SEND permite compartir texto con otras apps
            val compartirIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Clase: ${clase.nombre}")
                putExtra(Intent.EXTRA_TEXT, """
                    📚 Información de Clase - Aula Viva
                    
                    Nombre: ${clase.nombre}
                    📅 Fecha: ${clase.fecha}
                    
                    Compartido desde Aula Viva 🎓
                """.trimIndent())
            }
            
            // createChooser muestra un diálogo para elegir con qué app compartir
            // (WhatsApp, Gmail, Telegram, etc.)
            val chooser = Intent.createChooser(compartirIntent, "Compartir clase con...")
            holder.itemView.context.startActivity(chooser)
        }
    }

    /**
     * Actualiza la lista de clases y notifica al RecyclerView.
     * Este método se llama desde el ViewModel cuando los datos cambian.
     */
    fun updateList(newClases: List<Clase>) {
        clases = newClases
        notifyDataSetChanged()  // Redibuja toda la lista
    }
}