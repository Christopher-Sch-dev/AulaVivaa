package cl.duocuc.aulaviva.domain.repository

import android.net.Uri

interface IStorageRepository {
    suspend fun subirPdf(uri: Uri, nombreArchivo: String): Result<String>

    /**
     * Descarga un PDF desde una URL HTTP y lo sube al backend de Spring Boot (Storage).
     * Retorna la URL pública del PDF subido.
     */
    suspend fun subirPdfDesdeUrl(url: String, nombreArchivo: String): Result<String>
}
