package cl.duocuc.aulaviva.backend.infrastructure.repository

import cl.duocuc.aulaviva.backend.domain.entity.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UsuarioRepository : JpaRepository<Usuario, UUID> {
    fun findByEmail(email: String): Usuario?
    fun existsByEmail(email: String): Boolean
}

