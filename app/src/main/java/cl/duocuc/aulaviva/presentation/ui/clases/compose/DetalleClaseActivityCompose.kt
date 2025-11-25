package cl.duocuc.aulaviva.presentation.ui.clases.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTheme

class DetalleClaseActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val claseId = intent.getStringExtra("CLASE_ID") ?: ""
        val esAlumno = intent.getBooleanExtra("ES_ALUMNO", false)

        if (claseId.isEmpty()) {
            finish()
            return
        }

        setContent {
            AulaVivaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DetalleClaseScreen(
                        claseId = claseId,
                        esAlumno = esAlumno
                    )
                }
            }
        }
    }
}

