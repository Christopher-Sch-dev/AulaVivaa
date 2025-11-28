package cl.duocuc.aulaviva.backend.domain.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "clases", schema = "public")
data class Clase(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "nombre", nullable = false, length = 200)
    val nombre: String,

    @Column(name = "descripcion", columnDefinition = "TEXT")
    val descripcion: String = "",

    @Column(name = "fecha", nullable = false)
    val fecha: String,

    @Column(name = "archivo_pdf_url", columnDefinition = "TEXT")
    val archivoPdfUrl: String = "",

    @Column(name = "archivo_pdf_nombre", columnDefinition = "TEXT")
    val archivoPdfNombre: String = "",

    @Column(name = "creador", nullable = false, columnDefinition = "UUID")
    val creador: UUID,

    @Column(name = "asignatura_id", columnDefinition = "UUID")
    val asignaturaId: UUID? = null,

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

