package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.domain.repository.IStorageRepository

import android.app.Application
import android.net.Uri
import cl.duocuc.aulaviva.data.remote.SpringBootClient
import cl.duocuc.aulaviva.data.remote.SpringBootStorageRepository
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Encapsula operaciones de Storage (subir/descargar) usando Spring Boot.
 * Migrado de Supabase directo a Spring Boot backend.
 */
class StorageRepository(private val application: Application) : IStorageRepository {

    private val appContext = application.applicationContext
    private val springBootStorage = SpringBootStorageRepository(SpringBootClient.apiService)
    private val okHttpClient = OkHttpClient()

    /**
     * Sube un PDF desde un `Uri` y retorna la URL pública en caso de éxito.
     */
    override suspend fun subirPdf(uri: Uri, nombreArchivo: String): Result<String> {
        return try {
            Log.d("StorageRepository", "Subiendo PDF a Spring Boot: $nombreArchivo")
            springBootStorage.subirPdf(uri, nombreArchivo, appContext)
        } catch (e: Exception) {
            Log.e("StorageRepository", "❌ Error subiendo PDF", e)
            Result.failure(e)
        }
    }

    /**
     * Descarga un PDF desde una URL HTTP y lo sube al backend de Spring Boot (Storage).
     * Retorna la URL pública del PDF subido.
     */
    override suspend fun subirPdfDesdeUrl(url: String, nombreArchivo: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("StorageRepository", "📥 Descargando PDF desde: $url")

            // Descargar el PDF desde la URL
            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Error descargando PDF: ${response.code} ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Respuesta vacía al descargar PDF"))
            val bytes = body.bytes()

            Log.d("StorageRepository", "✅ PDF descargado: ${bytes.size} bytes")

            // Subir directamente desde bytes (más eficiente)
            val uploadResult = springBootStorage.subirPdfDesdeBytes(bytes, nombreArchivo, appContext)

            uploadResult.fold(
                onSuccess = { uploadedUrl ->
                    Log.d("StorageRepository", "✅ PDF subido exitosamente: $uploadedUrl")
                    Result.success(uploadedUrl)
                },
                onFailure = { error ->
                    Log.e("StorageRepository", "❌ Error subiendo PDF descargado", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e("StorageRepository", "❌ Error en subirPdfDesdeUrl", e)
            Result.failure(e)
        }
    }
}
