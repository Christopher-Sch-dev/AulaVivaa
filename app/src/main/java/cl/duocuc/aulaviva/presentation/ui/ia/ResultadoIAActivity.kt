package cl.duocuc.aulaviva.presentation.ui.ia

// IA repository accessed through IAViewModel; import removed
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import cl.duocuc.aulaviva.databinding.ActivityResultadoIaChatBinding
import cl.duocuc.aulaviva.presentation.base.BaseActivity
import cl.duocuc.aulaviva.presentation.viewmodel.IAViewModel
import io.noties.markwon.Markwon

/**
 * ✨ NUEVA FUNCIÓN: Interfaz tipo chat para resultados de IA
 *
 * Permite al usuario:
 * 1. Ver el resultado inicial de la IA
 * 2. Enviar hasta 3 mensajes adicionales para refinar/modificar el resultado
 * 3. Cada mensaje mantiene el contexto anterior
 * 4. Los mensajes se muestran en formato chat (usuario vs IA)
 */
class ResultadoIAActivity : BaseActivity() {

    private lateinit var binding: ActivityResultadoIaChatBinding
    private val iaViewModel: IAViewModel by viewModels()
    private lateinit var markwon: Markwon

    // Estado del chat
    private var mensajesRestantes = 3
    private var conversacionCompleta = StringBuilder()
    private var tituloOriginal = ""
    private var contenidoOriginal = ""
    private var tipoConsulta = "" // Para saber qué función de IA usar
    private var nombreClase = ""
    private var descripcionClase = ""
    private var pdfUrl = ""
    private var currentSessionId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultadoIaChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge: aplicado automáticamente por BaseActivity

        // Nota: inicializamos la sesión de chat más abajo, después de obtener los datos del intent

        // Configurar Markwon para renderizar Markdown
        markwon = Markwon.builder(this)
            .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
                override fun configureTheme(builder: io.noties.markwon.core.MarkwonTheme.Builder) {
                    builder
                        .headingTextSizeMultipliers(floatArrayOf(1.8F, 1.6F, 1.4F, 1.2F, 1.1F, 1F))
                        .linkColor(Color.parseColor("#1976D2"))
                        .codeTextColor(Color.parseColor("#212121"))
                        .codeBackgroundColor(Color.parseColor("#F5F5F5"))
                }
            })
            .build()

        // Obtener datos del intent
        tituloOriginal = intent.getStringExtra("TITULO") ?: "Resultado IA"
        contenidoOriginal = intent.getStringExtra("CONTENIDO") ?: ""
        tipoConsulta = intent.getStringExtra("TIPO_CONSULTA") ?: ""
        nombreClase = intent.getStringExtra("NOMBRE_CLASE") ?: ""
        descripcionClase = intent.getStringExtra("DESCRIPCION_CLASE") ?: ""
        pdfUrl = intent.getStringExtra("PDF_URL") ?: ""

        setupToolbar()
        setupChat()
        setupInputMensaje()

        // Agregar el resultado inicial al chat
        agregarMensajeIA(contenidoOriginal)

        // Inicializar contexto de conversación
        conversacionCompleta.append("CONTEXTO INICIAL:\n")
        conversacionCompleta.append("Clase: $nombreClase\n")
        conversacionCompleta.append("Descripción: $descripcionClase\n")
        if (!pdfUrl.isNullOrEmpty()) {
            conversacionCompleta.append("📎 Material PDF disponible: $pdfUrl\n")
        }
        conversacionCompleta.append("Respuesta inicial de la IA:\n$contenidoOriginal\n\n")

        // 🚀 INICIALIZAR SESIÓN DE CHAT STATEFUL usando ViewModel
        // Esto carga el PDF y el historial inicial en la memoria de la IA
        iaViewModel.iniciarChatConContexto(
            nombreClase = nombreClase,
            descripcion = descripcionClase,
            pdfUrl = if (pdfUrl.isNullOrEmpty()) null else pdfUrl,
            respuestaInicial = contenidoOriginal
        ).observe(this) { result ->
            // Podemos loguear errores si aparecen, pero no bloqueamos la UI
            if (result.isFailure) {
                android.util.Log.w(
                    "ResultadoIA",
                    "Error iniciando chat: ${result.exceptionOrNull()?.message}"
                )
            }
        }

        // Obtener la sesión creada o restaurada y guardar su id para poder cerrarla al salir
        iaViewModel.obtenerUltimaSesion(nombreClase).observe(this) { res ->
            if (res.isSuccess) {
                val s = res.getOrNull()
                currentSessionId = s?.id
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Al salir del apartado resultado cerramos la sesión temporal de chat
        currentSessionId?.let { id ->
            iaViewModel.cerrarSesion(id).observe(this) { r ->
                if (r.isSuccess) android.util.Log.d("ResultadoIA", "Sesión $id cerrada")
                else android.util.Log.w(
                    "ResultadoIA",
                    "Error cerrando sesión $id: ${r.exceptionOrNull()?.message}"
                )
            }
        }
    }

    // Cerrar sesión automáticamente cuando se agoten los mensajes permitidos
    private fun intentarCerrarSesionSiAgotada() {
        if (mensajesRestantes <= 0) {
            currentSessionId?.let { id ->
                Log.d("ResultadoIA", "Mensajes agotados, cerrando sesión $id")
                iaViewModel.cerrarSesion(id).observe(this) { r ->
                    if (r.isSuccess) Log.d("ResultadoIA", "Sesión $id cerrada automáticamente")
                    else Log.w(
                        "ResultadoIA",
                        "Error cerrando sesión $id: ${r.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = tituloOriginal
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupChat() {
        actualizarContadorMensajes()
    }

    private fun setupInputMensaje() {
        // Botón enviar inicialmente deshabilitado
        binding.btnEnviarMensaje.isEnabled = false

        // Habilitar/deshabilitar botón según texto
        binding.inputMensaje.addTextChangedListener { text ->
            binding.btnEnviarMensaje.isEnabled = !text.isNullOrBlank()
        }

        // Enviar mensaje al hacer clic
        binding.btnEnviarMensaje.setOnClickListener {
            val mensaje = binding.inputMensaje.text.toString().trim()
            if (mensaje.isNotEmpty() && mensajesRestantes > 0) {
                enviarMensajeUsuario(mensaje)
            }
        }
    }

    private fun enviarMensajeUsuario(mensaje: String) {
        // Deshabilitar input mientras se procesa
        binding.inputMensaje.isEnabled = false
        binding.btnEnviarMensaje.isEnabled = false

        // Agregar mensaje del usuario al chat
        agregarMensajeUsuario(mensaje)

        // Limpiar input
        binding.inputMensaje.text?.clear()

        // Decrementar contador
        mensajesRestantes--
        actualizarContadorMensajes()

        // Agregar al contexto
        conversacionCompleta.append("USUARIO: $mensaje\n\n")

        // Mostrar indicador de carga
        val loadingView = agregarIndicadorCarga()

        // Llamar a la IA con el contexto completo
        // Usar ViewModel para enviar mensaje y observar el resultado
        val live = iaViewModel.enviarMensajeChat(mensaje)
        live.observe(this) { result ->
            // Remover indicador de carga
            binding.chatContainer.removeView(loadingView)
            if (result.isSuccess) {
                val respuesta = result.getOrNull() ?: ""
                conversacionCompleta.append("IA: $respuesta\n\n")
                agregarMensajeIA(respuesta)
                if (mensajesRestantes > 0) {
                    binding.inputMensaje.isEnabled = true
                } else {
                    binding.layoutInputMensaje.alpha = 0.5f
                    Toast.makeText(
                        this@ResultadoIAActivity,
                        "⚠️ Has alcanzado el límite de mensajes para esta consulta",
                        Toast.LENGTH_LONG
                    ).show()
                    // Intentar cerrar la sesión porque el chat es temporal
                    intentarCerrarSesionSiAgotada()
                }
            } else {
                val ex = result.exceptionOrNull()
                Toast.makeText(
                    this@ResultadoIAActivity,
                    "❌ Error: ${ex?.message}",
                    Toast.LENGTH_LONG
                ).show()
                // Restaurar contador y habilitar input
                mensajesRestantes++
                actualizarContadorMensajes()
                binding.inputMensaje.isEnabled = true
            }
        }
    }

    private fun agregarMensajeUsuario(mensaje: String) {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(48, 8, 0, 8) // Margen izquierdo para distinguir usuario
            }
            radius = 12f
            setCardBackgroundColor(Color.parseColor("#4F3BB8"))
            cardElevation = 4f
        }

        val textView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = "👤 Tú:\n$mensaje"
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(16, 12, 16, 12)
        }

        cardView.addView(textView)
        binding.chatContainer.addView(cardView)

        // Scroll al final
        binding.scrollViewChat.post {
            binding.scrollViewChat.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun agregarMensajeIA(mensaje: String) {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 48, 8) // Margen derecho para distinguir IA
            }
            radius = 12f
            setCardBackgroundColor(Color.WHITE)
            cardElevation = 4f
        }

        val textView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setTextColor(Color.parseColor("#212121"))
            textSize = 14f
            setPadding(16, 12, 16, 12)
            setLineSpacing(4f, 1f)
        }

        // Renderizar markdown
        try {
            markwon.setMarkdown(textView, "🤖 **AulaViva IA:**\n\n$mensaje")
        } catch (e: Exception) {
            textView.text = "🤖 AulaViva IA:\n\n$mensaje"
        }

        cardView.addView(textView)
        binding.chatContainer.addView(cardView)

        // Scroll al final
        binding.scrollViewChat.post {
            binding.scrollViewChat.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun agregarIndicadorCarga(): CardView {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 48, 8)
            }
            radius = 12f
            setCardBackgroundColor(Color.parseColor("#FFF9C4"))
            cardElevation = 4f
        }

        val textView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = "🤖 La IA está pensando..."
            setTextColor(Color.parseColor("#666666"))
            textSize = 14f
            setPadding(16, 12, 16, 12)
            setTypeface(null, Typeface.ITALIC)
        }

        cardView.addView(textView)
        binding.chatContainer.addView(cardView)

        // Scroll al final
        binding.scrollViewChat.post {
            binding.scrollViewChat.fullScroll(android.view.View.FOCUS_DOWN)
        }

        return cardView
    }

    private fun actualizarContadorMensajes() {
        binding.textContadorMensajes.text = when (mensajesRestantes) {
            3 -> "💬 Puedes enviar 3 mensajes más"
            2 -> "💬 Puedes enviar 2 mensajes más"
            1 -> "💬 Puedes enviar 1 mensaje más"
            else -> "⚠️ No puedes enviar más mensajes en esta consulta"
        }
    }
}
