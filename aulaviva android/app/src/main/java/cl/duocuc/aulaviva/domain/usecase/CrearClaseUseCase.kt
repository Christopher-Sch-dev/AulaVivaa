package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.domain.repository.IClaseRepository
import kotlinx.coroutines.CoroutineScope

class CrearClaseUseCase(private val repository: IClaseRepository) {
    suspend operator fun invoke(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = repository.crearClase(clase, onSuccess, onError)

    fun invokeAsync(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        scope: CoroutineScope
    ) = repository.crearClaseAsync(clase, onSuccess, onError, scope)
}
