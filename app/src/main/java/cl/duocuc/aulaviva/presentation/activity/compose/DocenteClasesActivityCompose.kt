package cl.duocuc.aulaviva.presentation.activity.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTheme

class DocenteClasesActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val asignaturaId = intent.getStringExtra("ASIGNATURA_ID") ?: ""
        val asignaturaNombre = intent.getStringExtra("ASIGNATURA_NOMBRE") ?: "Asignatura"
        
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
                    DocenteClasesScreen(
                        asignaturaId = asignaturaId,
                        asignaturaNombre = asignaturaNombre
                    )
                }
            }
        }
    }
}

