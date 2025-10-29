package cl.duocuc.aulaviva.presentation.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cl.duocuc.aulaviva.databinding.ActivityPanelPrincipalBinding
import cl.duocuc.aulaviva.presentation.ui.clases.ListaClasesActivity
import cl.duocuc.aulaviva.presentation.ui.auth.LoginActivity

class PanelPrincipalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanelPrincipalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanelPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // Botón para ver clases
        binding.irAClasesButton.setOnClickListener {
            val intent = Intent(this, ListaClasesActivity::class.java)
            startActivity(intent)
        }

        // Botón para cerrar sesión
        binding.logoutButton.setOnClickListener {
            // Aquí podrías llamar al ViewModel para logout
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
