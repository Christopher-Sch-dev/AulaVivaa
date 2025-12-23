package cl.duocuc.aulaviva.presentation.ui.ia.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTheme

class ResultadoIAActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val titulo = intent.getStringExtra("TITULO") ?: "Resultado IA"
        val contenido = intent.getStringExtra("CONTENIDO") ?: ""
        val tipoConsulta = intent.getStringExtra("TIPO_CONSULTA") ?: ""
        val nombreClase = intent.getStringExtra("NOMBRE_CLASE") ?: ""
        val descripcionClase = intent.getStringExtra("DESCRIPCION_CLASE") ?: ""
        val pdfUrl = intent.getStringExtra("PDF_URL") ?: ""

        setContent {
            AulaVivaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ResultadoIAScreen(
                        titulo = titulo,
                        contenidoInicial = contenido,
                        tipoConsulta = tipoConsulta,
                        nombreClase = nombreClase,
                        descripcionClase = descripcionClase,
                        pdfUrl = pdfUrl
                    )
                }
            }
        }
    }
}

