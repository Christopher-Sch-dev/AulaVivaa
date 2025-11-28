package cl.duocuc.aulaviva.backend.infrastructure.repository

import cl.duocuc.aulaviva.backend.domain.entity.AlumnoAsignatura
import cl.duocuc.aulaviva.backend.domain.entity.EstadoInscripcion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AlumnoAsignaturaRepository : JpaRepository<AlumnoAsignatura, UUID> {
    fun findByAlumnoId(alumnoId: UUID): List<AlumnoAsignatura>
    fun findByAlumnoIdAndEstado(alumnoId: UUID, estado: EstadoInscripcion): List<AlumnoAsignatura>
    fun findByAsignaturaId(asignaturaId: UUID): List<AlumnoAsignatura>
    fun findByAlumnoIdAndAsignaturaId(alumnoId: UUID, asignaturaId: UUID): AlumnoAsignatura?
    fun existsByAlumnoIdAndAsignaturaId(alumnoId: UUID, asignaturaId: UUID): Boolean

    @Query("""
        SELECT aa FROM AlumnoAsignatura aa
        WHERE aa.asignaturaId IN (
            SELECT a.id FROM Asignatura a WHERE a.docenteId = :docenteId
        )
    """)
    fun findInscripcionesByDocente(@Param("docenteId") docenteId: UUID): List<AlumnoAsignatura>
}

