package cl.duocuc.aulaviva.backend.infrastructure.repository

import cl.duocuc.aulaviva.backend.domain.entity.Clase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ClaseRepository : JpaRepository<Clase, String> {
    fun findByCreador(creador: UUID): List<Clase>
    fun findByAsignaturaId(asignaturaId: UUID): List<Clase>

    @Query("SELECT c FROM Clase c WHERE c.asignaturaId IN :asignaturasIds")
    fun findByAsignaturasIds(@Param("asignaturasIds") asignaturasIds: List<UUID>): List<Clase>

    @Query("""
        SELECT c FROM Clase c
        WHERE c.creador = :docenteId
        OR c.asignaturaId IN (
            SELECT a.id FROM Asignatura a WHERE a.docenteId = :docenteId
        )
    """)
    fun findClasesByDocente(@Param("docenteId") docenteId: UUID): List<Clase>
}

