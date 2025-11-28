package cl.duocuc.aulaviva.backend.application.service

import cl.duocuc.aulaviva.backend.application.dto.request.LoginRequest
import cl.duocuc.aulaviva.backend.application.dto.request.RegisterRequest
import cl.duocuc.aulaviva.backend.application.dto.response.AuthResponse
import cl.duocuc.aulaviva.backend.application.dto.response.UsuarioResponse
import cl.duocuc.aulaviva.backend.domain.entity.RolUsuario
import cl.duocuc.aulaviva.backend.domain.entity.Usuario
import cl.duocuc.aulaviva.backend.infrastructure.repository.UsuarioRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val jwtService: JwtService,
    @Value("\${supabase.url}")
    private val supabaseUrl: String,
    @Value("\${supabase.anon-key}")
    private val supabaseAnonKey: String
) {
    private val supabaseClient: SupabaseClient = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseAnonKey
    ) {
        install(Auth)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse = runBlocking {
        // Autenticar con Supabase Auth
        val session = supabaseClient.auth.signInWith(Email) {
            this.email = request.email
            this.password = request.password
        }

        val user = session.user
        val userId = UUID.fromString(user.id)

        // Obtener datos adicionales del usuario
        val usuario = usuarioRepository.findById(userId)
            .orElseThrow { RuntimeException("Usuario no encontrado en la base de datos") }

        // Generar JWT
        val token = jwtService.generateToken(userId, user.email ?: "", usuario.rol.name)

        AuthResponse(
            token = token,
            userId = userId,
            email = user.email ?: "",
            rol = usuario.rol.name
        )
    }

    @Transactional
    fun register(request: RegisterRequest): AuthResponse = runBlocking {
        // Registrar en Supabase Auth
        supabaseClient.auth.signUpWith(Email) {
            this.email = request.email
            this.password = request.password
        }

        // Hacer login para obtener el usuario
        val session = supabaseClient.auth.signInWith(Email) {
            this.email = request.email
            this.password = request.password
        }

        val user = session.user
        val userId = UUID.fromString(user.id)

        // Guardar datos adicionales en tabla usuarios
        val rol = when (request.rol.lowercase()) {
            "docente" -> RolUsuario.docente
            "alumno" -> RolUsuario.alumno
            else -> throw IllegalArgumentException("Rol inválido: ${request.rol}")
        }

        val usuario = Usuario(
            id = userId,
            email = request.email,
            rol = rol,
            nombre = request.email.substringBefore("@")
        )

        usuarioRepository.save(usuario)

        // Generar JWT
        val token = jwtService.generateToken(userId, request.email, rol.name)

        AuthResponse(
            token = token,
            userId = userId,
            email = request.email,
            rol = rol.name
        )
    }

    fun getUsuario(userId: UUID): UsuarioResponse {
        val usuario = usuarioRepository.findById(userId)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        return UsuarioResponse(
            id = usuario.id,
            email = usuario.email,
            rol = usuario.rol.name,
            nombre = usuario.nombre
        )
    }
}

