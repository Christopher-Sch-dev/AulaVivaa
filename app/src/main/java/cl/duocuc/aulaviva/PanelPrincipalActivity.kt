package cl.duocuc.aulaviva

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button

class PanelPrincipalActivity : AppCompatActivity() {

    // Variable para manejar la autenticación de Firebase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cargar diseño visual del panel principal
        setContentView(R.layout.activity_panel_principal)

        // verificar quién está logueado
        auth = FirebaseAuth.getInstance()

        val btnClases = findViewById<Button>(R.id.irAClasesButton)
        btnClases.setOnClickListener {
            val intent = Intent(this, ListaClasesActivity::class.java)
            startActivity(intent)
        }


        // Obtengo al usuario que está actualmente autenticado (puede ser null si no hay nadie)
        val usuarioActual = auth.currentUser

        // Traigo la referencia al TextView donde voy a mostrar el mensaje de bienvenida
        val mensajeBienvenida = findViewById<TextView>(R.id.bienvenidaTextView)

        // personalizar saludo
        if (usuarioActual != null) {
            mensajeBienvenida.text = "¡Bienvenid@, ${usuarioActual.email}!"
        } else {
            mensajeBienvenida.text = "¡Bienvenid@!"
        }
    }
}
