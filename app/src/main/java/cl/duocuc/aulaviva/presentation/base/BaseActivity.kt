package cl.duocuc.aulaviva.presentation.base

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import cl.duocuc.aulaviva.presentation.util.applyEdgeToEdge

/**
 * BaseActivity centraliza el comportamiento edge-to-edge.
 * Intercepta `setContentView` para aplicar automáticamente insets
 * al content root (`android.R.id.content`).
 */
open class BaseActivity : AppCompatActivity() {

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyEdgeToContent()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        applyEdgeToContent()
    }

    private fun applyEdgeToContent() {
        val root = findViewById<View>(android.R.id.content)
        root?.let { applyEdgeToEdge(this, it) }
    }
}
