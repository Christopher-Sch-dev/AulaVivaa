package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IClaseRepository
import cl.duocuc.aulaviva.domain.repository.IStorageRepository
import cl.duocuc.aulaviva.domain.repository.IAuthRepository
import cl.duocuc.aulaviva.domain.usecase.CrearClaseUseCase
import cl.duocuc.aulaviva.domain.usecase.ObtenerClasesUseCase
import cl.duocuc.aulaviva.domain.usecase.SincronizarClasesUseCase
import cl.duocuc.aulaviva.domain.usecase.EliminarClaseUseCase
import android.net.Uri
import kotlinx.coroutines.launch

/**
 * ViewModel para manejar la lógica de clases.
 * Ahora usa AndroidViewModel para tener acceso al Context (necesario para Room).
 *
 * viewModelScope: Todas las corrutinas se cancelan automáticamente cuando se destruye el ViewModel.
 * Esto evita memory leaks y operaciones fantasma.
 */
class ClaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IClaseRepository = RepositoryProvider.provideClaseRepository(application)
    private val storageRepository: IStorageRepository = RepositoryProvider.provideStorageRepository(application)
    private val authRepository: IAuthRepository = RepositoryProvider.provideAuthRepository()
    private val uid: String = authRepository.getCurrentUserId() ?: ""

    // UseCases (incremental migration)
    private val obtenerClasesUseCase = ObtenerClasesUseCase(repository)
    private val sincronizarClasesUseCase = SincronizarClasesUseCase(repository)
    private val crearClaseUseCase = CrearClaseUseCase(repository)
    private val eliminarClaseUseCase = EliminarClaseUseCase(repository)

    // LiveData que lee directamente de Room usando Flow.
    // Cada vez que Room cambia, la UI se actualiza automáticamente.
    val clases: LiveData<List<Clase>> = obtenerClasesUseCase().asLiveData()

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
    fun sincronizarConSupabase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = sincronizarClasesUseCase()  // Usa Supabase vía UseCase
                result.fold(
                    onSuccess = {
                        _isLoading.value = false
                        // Opcional: _operationSuccess.value = "Sincronización completa"
                    },
                    onFailure = { e ->
                        _isLoading.value = false
                        _error.value = "Error de sincronización: ${e.message}"
                        android.util.Log.e("ClaseVM", "❌ Sincronización fallida", e)
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Error crítico de sincronización: ${e.message}"
                android.util.Log.e("ClaseVM", "❌ Error crítico sync", e)
            }
        }
    }

    /**
     * Sincroniza sólo las clases de una asignatura (útil para alumnos).
     */
    fun sincronizarClasesPorAsignatura(asignaturaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sincronizarClasesUseCase.invokePorAsignatura(asignaturaId)
            } catch (e: Exception) {
                _error.value = e.message
                android.util.Log.e("ClaseVM", "❌ Error sincronizando por asignatura", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Crea una nueva clase.
     * Se guarda primero en Room (instantáneo) y luego intenta subir a Supabase.
     */
    fun crearClase(
        nombre: String,
        descripcion: String,
        fecha: String,
        archivoPdfUrl: String = "",
        archivoPdfNombre: String = "",
        asignaturaId: String = ""  // No nullable, vacío por defecto para compatibilidad
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

        if (asignaturaId.isEmpty()) {
             // For compatibility or Demo handling: If empty, try to fetch classes or warn user.
             // But for now, enforce it strictly if we are following the rules.
             // If this is for "Demo", the demo method in PanelPrincipalVM passes valid ID so it's fine.
             // If user tries to create manual class without selecting subject...
            _error.value = "Debes seleccionar una asignatura asociada"
            return
        }

        _isLoading.value = true
        val nuevaClase = Clase(
            id = cl.duocuc.aulaviva.utils.IdUtils.generateId(), // UUID único para evitar duplicados
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            fecha = fecha.trim(),
            archivoPdfUrl = archivoPdfUrl,
            archivoPdfNombre = archivoPdfNombre,
            creador = uid,
            asignaturaId = asignaturaId
        )

        viewModelScope.launch {
            crearClaseUseCase(
                nuevaClase,
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
            eliminarClaseUseCase(claseId, onSuccess = {
                _isLoading.value = false
                _operationSuccess.value = "Clase eliminada"
            }, onError = { errorMsg ->
                _isLoading.value = false
                _error.value = errorMsg
            })
        }
    }

    /**
     * Sube un PDF y crea la clase (upload + crear).
     * La UI delega aquí en lugar de hacer la subida directamente.
     */
    fun subirPdfYCrearClase(
        uri: Uri,
        nombreArchivo: String,
        nombre: String,
        descripcion: String,
        fecha: String,
        asignaturaId: String
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val uploadResult = storageRepository.subirPdf(uri, nombreArchivo)
                uploadResult.fold(onSuccess = { url ->
                    crearClase(
                        nombre = nombre,
                        descripcion = descripcion,
                        fecha = fecha,
                        archivoPdfUrl = url,
                        archivoPdfNombre = nombreArchivo,
                        asignaturaId = asignaturaId
                    )
                }, onFailure = { ex ->
                    _isLoading.value = false
                    _error.value = "Error subiendo PDF: ${ex.message}"
                })
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Error subiendo PDF: ${e.message}"
            }
        }
    }

    /**
     * Sube un PDF y actualiza la clase existente.
     */
    fun subirPdfYActualizarClase(
        uri: Uri,
        nombreArchivo: String,
        claseId: String,
        nombre: String,
        descripcion: String,
        fecha: String
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val uploadResult = storageRepository.subirPdf(uri, nombreArchivo)
                uploadResult.fold(onSuccess = { url ->
                    actualizarClase(
                        claseId = claseId,
                        nombre = nombre,
                        descripcion = descripcion,
                        fecha = fecha,
                        archivoPdfUrl = url,
                        archivoPdfNombre = nombreArchivo
                    )
                }, onFailure = { ex ->
                    _isLoading.value = false
                    _error.value = "Error subiendo PDF: ${ex.message}"
                })
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Error subiendo PDF: ${e.message}"
            }
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
     * Obtiene todas las clases de una asignatura específica.
     * @param asignaturaId ID de la asignatura a filtrar
     * @return LiveData con la lista de clases de la asignatura
     */
    fun obtenerClasesPorAsignatura(asignaturaId: String): LiveData<List<Clase>> {
        return obtenerClasesUseCase(asignaturaId).asLiveData()
    }

    /**
     * Obtiene todas las clases de múltiples asignaturas.
     * Útil para alumnos que ven clases de todas sus asignaturas inscritas.
     * @param asignaturasIds Lista de IDs de asignaturas
     * @return LiveData con la lista de clases de todas las asignaturas
     */
    fun obtenerClasesPorAsignaturas(asignaturasIds: List<String>): LiveData<List<Clase>> {
        return obtenerClasesUseCase(asignaturasIds).asLiveData()
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
