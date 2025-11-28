package cl.duocuc.aulaviva.backend.domain.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "asignaturas", schema = "public")
data class Asignatura(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "nombre", nullable = false, length = 200)
    val nombre: String,

    @Column(name = "codigo_acceso", nullable = false, unique = true, length = 15)
    var codigoAcceso: String,

    @Column(name = "docente_id", nullable = false, columnDefinition = "UUID")
    val docenteId: UUID,

    @Column(name = "descripcion", columnDefinition = "TEXT")
    val descripcion: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = OffsetDateTime.now()
    }
}

