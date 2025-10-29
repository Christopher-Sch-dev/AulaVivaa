package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.repository.ClaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * ViewModel para manejar la lógica de clases.
 * Ahora usa AndroidViewModel para tener acceso al Context (necesario para Room).
 * 
 * viewModelScope: Todas las corrutinas se cancelan automáticamente cuando se destruye el ViewModel.
 * Esto evita memory leaks y operaciones fantasma.
 */
class ClaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ClaseRepository(application.applicationContext)
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // LiveData que lee directamente de Room usando Flow.
    // Cada vez que Room cambia, la UI se actualiza automáticamente.
    val clases: LiveData<List<Clase>> = repository.obtenerClasesLocal().asLiveData()

    // LiveData para el estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para errores
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData para operaciones exitosas
    private val _operationSuccess = MutableLiveData<String?>()
    val operationSuccess: LiveData<String?> = _operationSuccess

    /**
     * Sincroniza clases desde Firestore a Room.
     * Esto se ejecuta al abrir la pantalla de clases.
     */
    fun sincronizarConFirestore() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.sincronizarDesdeFirestore()
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                // No muestro error porque Room sigue funcionando offline
            }
        }
    }

    /**
     * Crea una nueva clase.
     * Se guarda primero en Room (instantáneo) y luego intenta subir a Firestore.
     */
    fun crearClase(nombre: String, fecha: String) {
        // Validación simple
        if (nombre.isEmpty() || fecha.isEmpty()) {
            _error.value = "Nombre y fecha son obligatorios"
            return
        }

        _isLoading.value = true
        val nuevaClase = Clase(nombre = nombre, fecha = fecha, creador = uid)

        viewModelScope.launch {
            repository.crearClase(
                clase = nuevaClase,
                onSuccess = {
                    _isLoading.value = false
                    _operationSuccess.value = "Clase creada exitosamente"
                },
                onError = { errorMsg ->
                    _isLoading.value = false
                    _error.value = errorMsg
                }
            )
        }
    }

    /**
     * Actualiza una clase existente
     */
    fun actualizarClase(claseId: String, nombre: String, fecha: String) {
        if (nombre.isEmpty() || fecha.isEmpty()) {
            _error.value = "Nombre y fecha son obligatorios"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            repository.actualizarClase(
                claseId = claseId,
                nombre = nombre,
                fecha = fecha,
                onSuccess = {
                    _isLoading.value = false
                    _operationSuccess.value = "Clase actualizada"
                },
                onError = { errorMsg ->
                    _isLoading.value = false
                    _error.value = errorMsg
                }
            )
        }
    }

    /**
     * Elimina una clase
     */
    fun eliminarClase(claseId: String) {
        _isLoading.value = true

        viewModelScope.launch {
            repository.eliminarClase(
                claseId = claseId,
                onSuccess = {
                    _isLoading.value = false
                    _operationSuccess.value = "Clase eliminada"
                },
                onError = { errorMsg ->
                    _isLoading.value = false
                    _error.value = errorMsg
                }
            )
        }
    }

    /**
     * Limpia mensajes de error y éxito
     */
    fun clearMessages() {
        _error.value = null
        _operationSuccess.value = null
    }
}
