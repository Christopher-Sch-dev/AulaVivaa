package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.repository.ClaseRepository
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
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
    private val uid = SupabaseAuthManager.getCurrentUserId() ?: ""

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
     * Sincroniza clases desde Supabase a Room.
     * Esto se ejecuta al abrir la pantalla de clases.
     */
    fun sincronizarConFirestore() {
        // Mantener nombre para compatibilidad, pero ahora usa Supabase
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.sincronizarDesdeFirestore()  // Internamente usa Supabase
                _isLoading.value = false
            } catch (_: Exception) {
                _isLoading.value = false
                // No muestro error porque Room sigue funcionando offline
            }
        }
    }

    /**
     * Crea una nueva clase.
     * Se guarda primero en Room (instantáneo) y luego intenta subir a Firestore.
     */
    fun crearClase(
        nombre: String,
        descripcion: String,
        fecha: String,
        archivoPdfUrl: String = "",
        archivoPdfNombre: String = ""
    ) {
        // Validación completa
        if (nombre.trim().isEmpty()) {
            _error.value = "El título de la clase es obligatorio"
            return
        }

        if (descripcion.trim().isEmpty()) {
            _error.value = "La descripción es obligatoria"
            return
        }

        if (fecha.trim().isEmpty()) {
            _error.value = "La fecha es obligatoria"
            return
        }

        _isLoading.value = true
        val nuevaClase = Clase(
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            fecha = fecha.trim(),
            archivoPdfUrl = archivoPdfUrl,
            archivoPdfNombre = archivoPdfNombre,
            creador = uid
        )

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
    fun actualizarClase(
        claseId: String,
        nombre: String,
        descripcion: String,
        fecha: String,
        archivoPdfUrl: String = "",
        archivoPdfNombre: String = ""
    ) {
        if (nombre.trim().isEmpty() || fecha.trim().isEmpty()) {
            _error.value = "Nombre y fecha son obligatorios"
            return
        }

        if (descripcion.trim().isEmpty()) {
            _error.value = "La descripción es obligatoria"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            repository.actualizarClase(
                claseId = claseId,
                nombre = nombre.trim(),
                descripcion = descripcion.trim(),
                fecha = fecha.trim(),
                archivoPdfUrl = archivoPdfUrl,
                archivoPdfNombre = archivoPdfNombre,
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

    /**
     * Obtiene una clase específica por ID
     */
    @Suppress("unused")
    suspend fun obtenerClasePorId(claseId: String): Clase? {
        return try {
            if (claseId.isEmpty()) {
                _error.value = "ID de clase no válido"
                return null
            }
            repository.obtenerClasePorId(claseId)
        } catch (e: Exception) {
            _error.value = "Error al obtener la clase: ${e.message}"
            null
        }
    }

    /**
     * Valida que el UID del usuario esté disponible
     */
    @Suppress("unused")
    fun validarUsuario(): Boolean {
        if (uid.isEmpty()) {
            _error.value = "Usuario no autenticado. Por favor, inicia sesión nuevamente"
            return false
        }
        return true
    }

    /**
     * Limpia los datos locales (útil al cerrar sesión)
     */
    @Suppress("unused")
    fun limpiarDatosLocales() {
        viewModelScope.launch {
            try {
                repository.limpiarLocal()
                _operationSuccess.value = "Datos locales limpiados"
            } catch (e: Exception) {
                _error.value = "Error al limpiar datos: ${e.message}"
            }
        }
    }
}
