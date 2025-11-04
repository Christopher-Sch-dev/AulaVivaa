package cl.duocuc.aulaviva.presentation.ui.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PdfViewerActivity : AppCompatActivity() {
    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var pfd: ParcelFileDescriptor? = null
    private var imageView: ImageView? = null
    private var pageIndex = 0
    private var pageCount = 0
    private var progressBar: ProgressBar? = null
    private var statusText: android.widget.TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("PDF_URL") ?: ""

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val toolbar = MaterialToolbar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (56 * resources.displayMetrics.density).toInt()
            )
            title = "Ver PDF"
            setNavigationIcon(android.R.drawable.ic_menu_revert)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar)

        if (url.startsWith("http")) {
            // Vista de progreso mientras descargo
            val loading = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            progressBar = ProgressBar(this)
            statusText = android.widget.TextView(this).apply { text = "Descargando PDF..." }
            loading.addView(progressBar)
            loading.addView(statusText)
            root.addView(loading)
            setContentView(root)

            Thread {
                try {
                    val file = downloadPdfToCache(url)
                    runOnUiThread {
                        try {
                            openRendererFromFile(file)
                            showPage(0)
                        } catch (e: Exception) {
                            // Fallback a Google Viewer embebido
                            mostrarEnWebView(url, root)
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        mostrarEnWebView(url, root)
                    }
                }
            }.start()
            return
        }

        // Local content:// con PdfRenderer (Android 5+)
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val btnPrev = Button(context).apply {
                text = "Anterior"
                setOnClickListener { showPage(pageIndex - 1) }
            }
            val btnNext = Button(context).apply {
                text = "Siguiente"
                setOnClickListener { showPage(pageIndex + 1) }
            }
            addView(btnPrev, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnNext, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        imageView = ImageView(this).apply {
            adjustViewBounds = true
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val container = FrameLayout(this)
        container.addView(imageView)
        scroll.addView(container)

        root.addView(controls)
        root.addView(scroll)
        setContentView(root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            openRenderer(url)
            showPage(0)
        } else {
            // Fallback mínimo: abrir externo si no hay soporte
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "application/pdf")
                flags = android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(intent)
            finish()
        }
    }

    private fun mostrarEnWebView(url: String, root: LinearLayout) {
        root.removeViews(1, root.childCount - 1)
        val web = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            settings.javaScriptEnabled = true
            val gview = "https://docs.google.com/gview?embedded=1&url=" + Uri.encode(url)
            loadUrl(gview)
        }
        root.addView(web)
    }

    private fun downloadPdfToCache(urlStr: String): File {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            requestMethod = "GET"
        }
        conn.connect()
        if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
        val file = File(cacheDir, "pdf_${System.currentTimeMillis()}.pdf")
        conn.inputStream.use { input ->
            FileOutputStream(file).use { out ->
                val buf = ByteArray(8 * 1024)
                var n: Int
                while (true) {
                    n = input.read(buf)
                    if (n == -1) break
                    out.write(buf, 0, n)
                }
                out.flush()
            }
        }
        return file
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openRenderer(url: String) {
        val uri = Uri.parse(url)
        try {
            pfd = contentResolver.openFileDescriptor(uri, "r")
            renderer = PdfRenderer(pfd!!)
            pageCount = renderer?.pageCount ?: 0
        } catch (_: Exception) {
            // Si falla, intento con Google Viewer
            val web = WebView(this)
            val gview = "https://docs.google.com/gview?embedded=1&url=" + Uri.encode(url)
            web.loadUrl(gview)
            setContentView(web)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openRendererFromFile(file: File) {
        pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(pfd!!)
        pageCount = renderer?.pageCount ?: 0
        // Construyo el resto de la UI de render si aún no estaba
        if (imageView == null) {
            // Crear controles e imagen si veníamos del flujo de descarga
            val controls = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val btnPrev = Button(context).apply {
                    text = "Anterior"
                    setOnClickListener { showPage(pageIndex - 1) }
                }
                val btnNext = Button(context).apply {
                    text = "Siguiente"
                    setOnClickListener { showPage(pageIndex + 1) }
                }
                addView(
                    btnPrev,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                )
                addView(
                    btnNext,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                )
            }
            val scroll = ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            imageView = ImageView(this).apply {
                adjustViewBounds = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            val container = FrameLayout(this)
            container.addView(imageView)
            scroll.addView(container)
            val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as LinearLayout
            root.removeViews(1, root.childCount - 1)
            root.addView(controls)
            root.addView(scroll)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun showPage(index: Int) {
        val r = renderer ?: return
        if (index < 0 || index >= (r.pageCount)) return
        currentPage?.close()
        pageIndex = index
        currentPage = r.openPage(index)
        val page = currentPage ?: return
        val screenW = resources.displayMetrics.widthPixels
        val scale = screenW.toFloat() / page.width
        val bmpH = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(screenW, bmpH, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        imageView?.setImageBitmap(bitmap)
    }

    override fun onDestroy() {
        try {
            currentPage?.close()
        } catch (_: Exception) {
        }
        try {
            renderer?.close()
        } catch (_: Exception) {
        }
        try {
            pfd?.close()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
