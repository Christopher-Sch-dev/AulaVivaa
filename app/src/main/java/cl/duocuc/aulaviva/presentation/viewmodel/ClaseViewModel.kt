package cl.duocuc.aulaviva.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.repository.ClaseRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * ViewModel para manejar la lógica de clases
 */
class ClaseViewModel : ViewModel() {

    private val repository = ClaseRepository()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // LiveData para la lista de clases
    private val _clases = MutableLiveData<List<Clase>>()
    val clases: LiveData<List<Clase>> = _clases

    // LiveData para el estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para errores
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData para operaciones exitosas
    private val _operationSuccess = MutableLiveData<String?>()
    val operationSuccess: LiveData<String?> = _operationSuccess

    // Cargar clases
    fun cargarClases() {
        _isLoading.value = true
        _error.value = null

        repository.obtenerClases(
            onSuccess = { listaClases ->
                _isLoading.value = false
                _clases.value = listaClases
            },
            onError = { errorMsg ->
                _isLoading.value = false
                _error.value = errorMsg
            }
        )
    }

    // Crear nueva clase
    fun crearClase(nombre: String, fecha: String) {
        if (nombre.isEmpty() || fecha.isEmpty()) {
            _error.value = "Nombre y fecha son obligatorios"
            return
        }

        _isLoading.value = true
        val nuevaClase = Clase(nombre = nombre, fecha = fecha, creador = uid)

        repository.crearClase(
            clase = nuevaClase,
            onSuccess = {
                _isLoading.value = false
                _operationSuccess.value = "Clase creada exitosamente"
                cargarClases() // Recargar lista
            },
            onError = { errorMsg ->
                _isLoading.value = false
                _error.value = errorMsg
            }
        )
    }

    // Actualizar clase
    fun actualizarClase(claseId: String, nombre: String, fecha: String) {
        if (nombre.isEmpty() || fecha.isEmpty()) {
            _error.value = "Nombre y fecha son obligatorios"
            return
        }

        _isLoading.value = true

        repository.actualizarClase(
            claseId = claseId,
            nombre = nombre,
            fecha = fecha,
            onSuccess = {
                _isLoading.value = false
                _operationSuccess.value = "Clase actualizada"
                cargarClases()
            },
            onError = { errorMsg ->
                _isLoading.value = false
                _error.value = errorMsg
            }
        )
    }

    // Eliminar clase
    fun eliminarClase(claseId: String) {
        _isLoading.value = true

        repository.eliminarClase(
            claseId = claseId,
            onSuccess = {
                _isLoading.value = false
                _operationSuccess.value = "Clase eliminada"
                cargarClases()
            },
            onError = { errorMsg ->
                _isLoading.value = false
                _error.value = errorMsg
            }
        )
    }

    // Limpiar mensajes
    fun clearMessages() {
        _error.value = null
        _operationSuccess.value = null
    }
}
