package cl.duocuc.aulaviva.data.repository

import android.content.Context
import android.net.Uri
import cl.duocuc.aulaviva.data.supabase.SupabaseClientProvider
import cl.duocuc.aulaviva.utils.IdUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Encapsula operaciones de Storage (subir/descargar) usando Supabase.
 * Usa `applicationContext` para evitar fugas de Activity.
 */
class StorageRepository(private val context: Context) {

    private val appContext = context.applicationContext

    /**
     * Sube un PDF desde un `Uri` y retorna la URL pública en caso de éxito.
     */
    suspend fun subirPdf(uri: Uri, nombreArchivo: String): Result<String> {
        return try {
            val supabase = SupabaseClientProvider.getClient()

            val inputStream = withContext(Dispatchers.IO) { appContext.contentResolver.openInputStream(uri) }
            val bytes = inputStream?.readBytes() ?: return Result.failure(Exception("No fue posible leer el archivo"))

            val uniqueName = IdUtils.generateUniqueName(nombreArchivo)
            val bucket = supabase.storage["clases"]

            // subir bytes
            bucket.upload(uniqueName, bytes)

            val publicUrl = bucket.publicUrl(uniqueName)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
