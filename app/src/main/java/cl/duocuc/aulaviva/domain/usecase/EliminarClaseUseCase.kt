package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IClaseRepository

class EliminarClaseUseCase(private val repository: IClaseRepository) {
    suspend operator fun invoke(claseId: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        repository.eliminarClase(claseId, onSuccess, onError)

    suspend fun invokeSimple(claseId: String) = repository.eliminarClase(claseId)
}
