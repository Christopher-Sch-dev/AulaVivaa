package cl.duocuc.aulaviva.domain.usecase

import kotlinx.coroutines.flow.Flow
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.domain.repository.IClaseRepository

class ObtenerClasesUseCase(private val repository: IClaseRepository) {
    operator fun invoke(): Flow<List<Clase>> = repository.obtenerClasesLocal()
    operator fun invoke(asignaturaId: String): Flow<List<Clase>> = repository.obtenerClasesPorAsignatura(asignaturaId)
    operator fun invoke(asignaturasIds: List<String>): Flow<List<Clase>> = repository.obtenerClasesPorAsignaturas(asignaturasIds)
}
