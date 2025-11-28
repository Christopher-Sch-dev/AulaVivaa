package cl.duocuc.aulaviva.backend.domain.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "usuarios", schema = "public")
data class Usuario(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID,

    @Column(name = "email", nullable = false)
    val email: String,

    @Column(name = "rol", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val rol: RolUsuario,

    @Column(name = "nombre")
    val nombre: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)

enum class RolUsuario {
    docente,
    alumno
}

