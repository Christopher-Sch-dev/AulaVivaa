package cl.duocuc.aulaviva.presentation.ui.clases.compose

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTheme
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class CrearEditarClaseActivityCompose : ComponentActivity() {

    private var onFechaSeleccionada: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val asignaturaId = intent.getStringExtra("ASIGNATURA_ID") ?: ""
        val asignaturaNombre = intent.getStringExtra("ASIGNATURA_NOMBRE") ?: "Asignatura"
        val claseId = intent.getStringExtra("CLASE_ID")

        if (asignaturaId.isEmpty()) {
            finish()
            return
        }

        setContent {
            AulaVivaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrearEditarClaseScreenWithDatePicker(
                        asignaturaId = asignaturaId,
                        asignaturaNombre = asignaturaNombre,
                        claseId = claseId,
                        onMostrarDatePicker = { callback ->
                            onFechaSeleccionada = callback
                            mostrarSelectorFechaHora()
                        }
                    )
                }
            }
        }
    }

    private fun mostrarSelectorFechaHora() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona la fecha")
            .build()

        datePicker.show(supportFragmentManager, "DATE_PICKER")

        datePicker.addOnPositiveButtonClickListener { selection ->
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Santiago"))
                cal.timeInMillis = selection

                val horaActual = Calendar.getInstance(TimeZone.getTimeZone("America/Santiago"))
                val hora = horaActual.get(Calendar.HOUR_OF_DAY)
                val minuto = horaActual.get(Calendar.MINUTE)

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val fechaFormateada = sdf.format(cal.time)
                        val horaFormateada = String.format("%02d:%02d", hourOfDay, minute)
                        onFechaSeleccionada?.invoke("$fechaFormateada $horaFormateada")
                    },
                    hora,
                    minuto,
                    true
                ).show()
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
}

@Composable
fun CrearEditarClaseScreenWithDatePicker(
    asignaturaId: String,
    asignaturaNombre: String,
    claseId: String?,
    onMostrarDatePicker: ((String) -> Unit) -> Unit
) {
    var fecha by remember { mutableStateOf("") }

    CrearEditarClaseScreen(
        asignaturaId = asignaturaId,
        asignaturaNombre = asignaturaNombre,
        claseId = claseId,
        fecha = fecha,
        onFechaChange = { fecha = it },
        onMostrarDatePicker = { onMostrarDatePicker { fechaSeleccionada ->
            fecha = fechaSeleccionada
        } }
    )
}

