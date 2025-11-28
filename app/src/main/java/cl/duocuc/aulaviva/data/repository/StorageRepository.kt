package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.domain.repository.IStorageRepository

import android.app.Application
import android.net.Uri
import cl.duocuc.aulaviva.data.remote.SpringBootClient
import cl.duocuc.aulaviva.data.remote.SpringBootStorageRepository
import android.util.Log

/**
 * Encapsula operaciones de Storage (subir/descargar) usando Spring Boot.
 * Migrado de Supabase directo a Spring Boot backend.
 */
class StorageRepository(private val application: Application) : IStorageRepository {

    private val appContext = application.applicationContext
    private val springBootStorage = SpringBootStorageRepository(SpringBootClient.apiService)

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
}
