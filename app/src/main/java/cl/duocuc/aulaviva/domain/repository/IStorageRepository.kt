package cl.duocuc.aulaviva.domain.repository

import android.net.Uri

interface IStorageRepository {
    suspend fun subirPdf(uri: Uri, nombreArchivo: String): Result<String>
}
