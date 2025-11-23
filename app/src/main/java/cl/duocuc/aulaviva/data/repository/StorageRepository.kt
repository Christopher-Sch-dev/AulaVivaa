package cl.duocuc.aulaviva.data.repository

import android.app.Application
import android.net.Uri
import cl.duocuc.aulaviva.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import cl.duocuc.aulaviva.utils.IdUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Encapsula operaciones de Storage (subir/descargar) usando Supabase.
 * Recibe `Application` y usa su `applicationContext` para evitar fugas de Activity.
 */
class StorageRepository(private val application: Application) {

    private val appContext = application.applicationContext

    /**
     * Sube un PDF desde un `Uri` y retorna la URL pública en caso de éxito.
     */
    suspend fun subirPdf(uri: Uri, nombreArchivo: String): Result<String> {
        return try {
            val supabase = SupabaseClientProvider.getClient()

            val inputStream = withContext(Dispatchers.IO) { appContext.contentResolver.openInputStream(uri) }
            val bytes = inputStream?.readBytes() ?: return Result.failure(Exception("No fue posible leer el archivo"))

            val timestamp = System.currentTimeMillis()
            val uid = cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager.getCurrentUserId() ?: "unknown"
            val remotePath = "clases/$uid/${timestamp}_${IdUtils.generateUniqueName(nombreArchivo)}"

            Log.d("StorageRepository", "Subiendo a Supabase: $remotePath (${bytes.size} bytes)")

            // Subir usando la API común (from(...).upload)
            supabase.storage.from("clases").upload(remotePath, bytes, upsert = false)

            val publicUrl = supabase.storage.from("clases").publicUrl(remotePath)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
