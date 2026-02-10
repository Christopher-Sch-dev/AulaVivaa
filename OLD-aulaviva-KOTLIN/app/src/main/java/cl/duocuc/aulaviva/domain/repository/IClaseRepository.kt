package cl.duocuc.aulaviva.domain.repository

import kotlinx.coroutines.flow.Flow
import cl.duocuc.aulaviva.data.model.Clase

interface IClaseRepository {
    fun obtenerClasesLocal(): Flow<List<Clase>>
    fun obtenerClasesPorAsignatura(asignaturaId: String): Flow<List<Clase>>
    fun obtenerClasesPorAsignaturas(asignaturasIds: List<String>): Flow<List<Clase>>
    suspend fun obtenerClasePorId(claseId: String): Clase?
    suspend fun sincronizarDesdeSupabase(): Result<Unit>
    suspend fun sincronizarClasesPorAsignatura(asignaturaId: String)
    suspend fun crearClase(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
    fun crearClaseAsync(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        scope: kotlinx.coroutines.CoroutineScope
    )
    suspend fun actualizarClase(
        claseId: String,
        nombre: String,
        descripcion: String,
        fecha: String,
        archivoPdfUrl: String,
        archivoPdfNombre: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
    suspend fun actualizarClase(clase: Clase)
    suspend fun eliminarClase(
        claseId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
    suspend fun eliminarClase(claseId: String)
    suspend fun limpiarLocal()
    suspend fun tieneClases(asignaturaId: String): Boolean
}
