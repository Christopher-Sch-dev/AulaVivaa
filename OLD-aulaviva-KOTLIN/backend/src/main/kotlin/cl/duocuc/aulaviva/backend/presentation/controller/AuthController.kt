package cl.duocuc.aulaviva.backend.presentation.controller

import cl.duocuc.aulaviva.backend.application.dto.request.LoginRequest
import cl.duocuc.aulaviva.backend.application.dto.request.RegisterRequest
import cl.duocuc.aulaviva.backend.application.dto.response.ApiResponse
import cl.duocuc.aulaviva.backend.application.dto.response.AuthResponse
import cl.duocuc.aulaviva.backend.application.dto.response.UsuarioResponse
import cl.duocuc.aulaviva.backend.application.service.AuthService
import cl.duocuc.aulaviva.backend.infrastructure.security.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(ApiResponse.success(response, "Login exitoso"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Credenciales inválidas", e.message))
        }
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        return try {
            val response = authService.register(request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Usuario registrado exitosamente"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al registrar usuario", e.message))
        }
    }

    @GetMapping("/me")
    fun getCurrentUser(): ResponseEntity<ApiResponse<UsuarioResponse>> {
        return try {
            val userId = CurrentUser.getUserId()
            val usuario = authService.getUsuario(userId)
            ResponseEntity.ok(ApiResponse.success(usuario))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Usuario no autenticado", e.message))
        }
    }
}

