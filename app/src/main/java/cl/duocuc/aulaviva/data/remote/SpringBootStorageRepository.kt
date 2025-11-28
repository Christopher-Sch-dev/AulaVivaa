package cl.duocuc.aulaviva.data.remote

import android.net.Uri
import android.util.Log
import cl.duocuc.aulaviva.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Repository remoto para Storage usando Spring Boot.
 * Reemplaza el uso directo de Supabase Storage.
 */
class SpringBootStorageRepository(
    private val apiService: SpringBootApiService
) {

    suspend fun subirPdf(uri: Uri, nombreArchivo: String, appContext: android.content.Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = appContext.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return@withContext Result.failure(Exception("No fue posible leer el archivo"))
            subirPdfDesdeBytes(bytes, nombreArchivo, appContext)
        } catch (e: Exception) {
            Log.e("SpringBootStorage", "❌ Error subiendo PDF", e)
            Result.failure(e)
        }
    }

    /**
     * Sube un PDF desde bytes directamente.
     * Útil para subir PDFs descargados desde URLs.
     */
    suspend fun subirPdfDesdeBytes(bytes: ByteArray, nombreArchivo: String, appContext: android.content.Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Crear archivo temporal
            val tempFile = File.createTempFile("upload_", ".pdf", appContext.cacheDir)
            tempFile.writeBytes(bytes)

            val requestFile = tempFile.asRequestBody("application/pdf".toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", nombreArchivo, requestFile)
            val nombrePart = nombreArchivo.toRequestBody("text/plain".toMediaType())

            val response = apiService.subirPdf("Bearer ${TokenManager.getToken()}", filePart, nombrePart)

            // Limpiar archivo temporal
            tempFile.delete()

            if (response.isSuccessful && response.body()?.success == true) {
                val uploadResponse = response.body()!!.data!!
                Log.d("SpringBootStorage", "✅ PDF subido: ${uploadResponse.url}")
                Result.success(uploadResponse.url)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Log.e("SpringBootStorage", "❌ Error subiendo PDF desde bytes", e)
            Result.failure(e)
        }
    }
}

