package cl.duocuc.aulaviva.presentation.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cl.duocuc.aulaviva.R

/**
 * Panel básico para alumnos.
 * Muestra un mensaje simple (extensible más adelante).
 */
class PanelAlumnoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel_alumno)
    }
}
