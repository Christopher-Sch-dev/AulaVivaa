package cl.duocuc.aulaviva.backend.application.service

import cl.duocuc.aulaviva.backend.infrastructure.config.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class StorageService(
    private val supabaseConfig: SupabaseConfig
) {
    // Configurar Supabase Client para Storage
    private val supabaseClient: SupabaseClient = createSupabaseClient(
        supabaseUrl = supabaseConfig.url,
        supabaseKey = supabaseConfig.serviceRoleKey
    ) {
        install(io.github.jan.supabase.storage.Storage)
    }

    fun subirPdf(file: MultipartFile, nombreArchivo: String, userId: UUID): String = runBlocking {
        val timestamp = System.currentTimeMillis()
        val remotePath = "${supabaseConfig.storageBucket}/$userId/${timestamp}_$nombreArchivo"

        val bytes = file.bytes
        supabaseClient.storage.from(supabaseConfig.storageBucket).upload(remotePath, bytes, upsert = false)

        supabaseClient.storage.from(supabaseConfig.storageBucket).publicUrl(remotePath)
    }
}

