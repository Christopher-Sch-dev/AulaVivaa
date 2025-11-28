package cl.duocuc.aulaviva.backend.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class SupabaseConfig(
    @Value("\${supabase.url}")
    val url: String,

    @Value("\${supabase.anon-key}")
    val anonKey: String,

    @Value("\${supabase.service-role-key}")
    val serviceRoleKey: String,

    @Value("\${supabase.storage.bucket}")
    val storageBucket: String
)

