package cl.duocuc.aulaviva

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class PanelPrincipalActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel_principal)

        auth = FirebaseAuth.getInstance()

        val usuarioActual = auth.currentUser
        val mensajeBienvenida = findViewById<TextView>(R.id.bienvenidaTextView)

        if (usuarioActual != null) {
            mensajeBienvenida.text = "¡Bienvenid@, ${usuarioActual.email}!"
        } else {
            mensajeBienvenida.text = "¡Bienvenid@!"
        }
    }
}
