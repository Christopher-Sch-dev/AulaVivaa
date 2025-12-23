package cl.duocuc.aulaviva.domain.repository

import kotlinx.coroutines.flow.Flow
import cl.duocuc.aulaviva.data.model.Asignatura

interface IAsignaturasRepository {
    fun obtenerAsignaturasDocente(): Flow<List<Asignatura>>
    suspend fun sincronizarAsignaturas(): Result<Unit>
    suspend fun sincronizarInscritos(asignaturaId: String): Result<Unit>
    suspend fun crearAsignatura(nombre: String, descripcion: String): Result<Asignatura>
    suspend fun generarCodigo(asignaturaId: String): Result<String>
    suspend fun actualizarAsignatura(asignatura: Asignatura): Result<Asignatura>
    suspend fun eliminarAsignatura(asignaturaId: String): Result<Unit>
    suspend fun obtenerAsignaturaPorId(asignaturaId: String): Asignatura?
    fun obtenerInscritosFlow(asignaturaId: String): Flow<List<cl.duocuc.aulaviva.data.local.AlumnoAsignaturaEntity>>
}
