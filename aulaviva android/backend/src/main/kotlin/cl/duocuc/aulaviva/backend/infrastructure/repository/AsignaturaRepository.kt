package cl.duocuc.aulaviva.backend.infrastructure.repository

import cl.duocuc.aulaviva.backend.domain.entity.Asignatura
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AsignaturaRepository : JpaRepository<Asignatura, UUID> {
    fun findByDocenteId(docenteId: UUID): List<Asignatura>
    fun findByCodigoAcceso(codigoAcceso: String): Asignatura?
    fun existsByCodigoAcceso(codigoAcceso: String): Boolean

    @Query("SELECT a FROM Asignatura a WHERE a.id IN :ids")
    fun findByIds(@Param("ids") ids: List<UUID>): List<Asignatura>
}

