package cl.duocuc.aulaviva.presentation.ui.ia

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.databinding.ActivityResultadoIaChatBinding
import cl.duocuc.aulaviva.data.repository.IARepository
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ✨ NUEVA FUNCIÓN: Interfaz tipo chat para resultados de IA
 *
 * Permite al usuario:
 * 1. Ver el resultado inicial de la IA
 * 2. Enviar hasta 3 mensajes adicionales para refinar/modificar el resultado
 * 3. Cada mensaje mantiene el contexto anterior
 * 4. Los mensajes se muestran en formato chat (usuario vs IA)
 */
class ResultadoIAActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultadoIaChatBinding
    private lateinit var iaRepository: IARepository
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultadoIaChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar repositorio IA
        iaRepository = IARepository()

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
        conversacionCompleta.append("Respuesta inicial de la IA:\n$contenidoOriginal\n\n")
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val promptCompleto = buildString {
                    append(conversacionCompleta.toString())
                    append("NUEVA INSTRUCCIÓN DEL USUARIO:\n")
                    append(mensaje)
                    append("\n\nPor favor, responde considerando TODO el contexto anterior.")
                }

                // Siempre usar procesarPromptConContexto para mantener la conversación coherente
                val respuesta = iaRepository.procesarPromptConContexto(promptCompleto)

                // Agregar respuesta al contexto
                conversacionCompleta.append("IA: $respuesta\n\n")

                withContext(Dispatchers.Main) {
                    // Remover indicador de carga
                    binding.chatContainer.removeView(loadingView)

                    // Agregar respuesta de la IA
                    agregarMensajeIA(respuesta)

                    // Habilitar input si quedan mensajes
                    if (mensajesRestantes > 0) {
                        binding.inputMensaje.isEnabled = true
                    } else {
                        // Deshabilitar completamente el input
                        binding.layoutInputMensaje.alpha = 0.5f
                        Toast.makeText(
                            this@ResultadoIAActivity,
                            "⚠️ Has alcanzado el límite de mensajes para esta consulta",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.chatContainer.removeView(loadingView)
                    Toast.makeText(
                        this@ResultadoIAActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Restaurar contador y habilitar input
                    mensajesRestantes++
                    actualizarContadorMensajes()
                    binding.inputMensaje.isEnabled = true
                }
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
