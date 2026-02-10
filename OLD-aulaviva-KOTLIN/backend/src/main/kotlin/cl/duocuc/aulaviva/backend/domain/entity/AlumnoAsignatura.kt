package cl.duocuc.aulaviva.backend.domain.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(
    name = "alumno_asignaturas",
    schema = "public",
    uniqueConstraints = [UniqueConstraint(columnNames = ["alumno_id", "asignatura_id"])]
)
data class AlumnoAsignatura(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "alumno_id", nullable = false, columnDefinition = "UUID")
    val alumnoId: UUID,

    @Column(name = "asignatura_id", nullable = false, columnDefinition = "UUID")
    val asignaturaId: UUID,

    @Column(name = "fecha_inscripcion", nullable = false, updatable = false)
    val fechaInscripcion: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "estado", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val estado: EstadoInscripcion = EstadoInscripcion.activo
)

enum class EstadoInscripcion {
    activo,
    inactivo,
    completado
}

