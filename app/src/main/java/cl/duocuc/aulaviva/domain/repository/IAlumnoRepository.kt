package cl.duocuc.aulaviva.domain.repository

import kotlinx.coroutines.flow.Flow
import cl.duocuc.aulaviva.data.model.Asignatura

interface IAlumnoRepository {
    fun obtenerAsignaturasInscritas(): Flow<List<Asignatura>>
    suspend fun sincronizarAsignaturasInscritas(): Result<Unit>
    suspend fun inscribirConCodigo(codigo: String): Result<Asignatura>
    suspend fun darDeBaja(asignaturaId: String): Result<Unit>
    suspend fun estaInscrito(asignaturaId: String): Boolean
    suspend fun obtenerAsignaturaPorId(asignaturaId: String): Asignatura?
}
