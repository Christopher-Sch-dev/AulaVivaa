package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.domain.repository.IAsignaturasRepository
import cl.duocuc.aulaviva.domain.repository.IAuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import cl.duocuc.aulaviva.domain.repository.IClaseRepository
import cl.duocuc.aulaviva.domain.repository.IStorageRepository

/**
 * ViewModel para gestión de asignaturas (DOCENTES).
 * Maneja el estado de la UI y orquesta operaciones con el Repository.
 */
class AsignaturasViewModel(application: Application) : AndroidViewModel(application) {

    // Repository
    private val repository: IAsignaturasRepository = RepositoryProvider.provideAsignaturasRepository(application)
    private val authRepository: IAuthRepository = RepositoryProvider.provideAuthRepository()

    // LiveData para la lista de asignaturas (automática desde Room)
    val asignaturas: LiveData<List<Asignatura>> = repository.obtenerAsignaturasDocente().asLiveData()

    // Estados de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _operationSuccess = MutableLiveData<String?>()
    val operationSuccess: LiveData<String?> = _operationSuccess

    private val _codigoGenerado = MutableLiveData<String?>()
    val codigoGenerado: LiveData<String?> = _codigoGenerado

    init {
        // Verificar autenticación antes de sincronizar
        viewModelScope.launch {
            // Pequeño delay para asegurar que la sesión esté completamente establecida
            delay(200)

            // Solo sincronizar si el usuario está autenticado
            if (authRepository.isLoggedIn()) {
                sincronizarAsignaturas()
            } else {
                android.util.Log.w("AsignaturasVM", "⚠️ Usuario no autenticado, omitiendo sincronización inicial")
            }
        }
    }

    // ClaseRepository para operaciones relacionadas con clases (ej.: verificar existencia)
    private val claseRepository: IClaseRepository = RepositoryProvider.provideClaseRepository(application)
    // Storage repo for any future file operations related to asignaturas/classes
    private val storageRepository: IStorageRepository = RepositoryProvider.provideStorageRepository(application)

    /**
     * Verifica si una asignatura tiene clases (delegado al ClaseRepository).
     * Función suspend para usar desde coroutines.
     */
    suspend fun tieneClases(asignaturaId: String): Boolean {
        return claseRepository.tieneClases(asignaturaId)
    }

    /**
     * Verifica si una asignatura tiene clases (versión no-suspend para usar desde UI).
     * Usa viewModelScope para ejecutar la verificación.
     */
    fun verificarTieneClases(asignaturaId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val tiene = claseRepository.tieneClases(asignaturaId)
            onResult(tiene)
        }
    }

    /**
     * Sincroniza asignaturas desde Supabase.
     */
    fun sincronizarAsignaturas() {
        viewModelScope.launch {
            // Verificar autenticación antes de sincronizar
            if (!authRepository.isLoggedIn()) {
                android.util.Log.w("AsignaturasVM", "⚠️ Usuario no autenticado, no se puede sincronizar")
                _error.value = "Sesión no válida. Por favor, inicia sesión nuevamente."
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            repository.sincronizarAsignaturas()
                .onSuccess {
                    android.util.Log.d("AsignaturasVM", "✅ Sincronización exitosa")
                }
                .onFailure { exception ->
                    // No cerrar la sesión automáticamente, solo mostrar el error
                    val errorMessage = when {
                        exception.message?.contains("Usuario no autenticado", ignoreCase = true) == true ||
                        exception.message?.contains("not authenticated", ignoreCase = true) == true ||
                        exception.message?.contains("session", ignoreCase = true) == true ->
                            "Sesión expirada. Por favor, inicia sesión nuevamente."
                        else -> exception.message ?: "Error al sincronizar asignaturas"
                    }
                    _error.value = errorMessage
                    android.util.Log.e("AsignaturasVM", "❌ Error sincronizando: ${exception.message}", exception)
                }

            _isLoading.value = false
        }
    }

    /**
     * Crea una nueva asignatura.
     */
    fun crearAsignatura(nombre: String, descripcion: String) {
        if (nombre.isBlank()) {
            _error.value = "El nombre no puede estar vacío"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.crearAsignatura(nombre, descripcion)
                .onSuccess { asignatura ->
                    android.util.Log.d("AsignaturasVM", "✅ Asignatura creada: ${asignatura.id}")

                    // Generar código automáticamente SIN llamar a sincronizarAsignaturas después
                    repository.generarCodigo(asignatura.id)
                        .onSuccess { codigo ->
                            android.util.Log.d("AsignaturasVM", "✅ Código generado: $codigo")
                            _codigoGenerado.value = codigo
                            _operationSuccess.value = "Asignatura creada exitosamente"
                            _isLoading.value = false
                        }
                        .onFailure { exception ->
                            android.util.Log.e("AsignaturasVM", "⚠️ Error generando código", exception)
                            _operationSuccess.value = "Asignatura creada (sin código)"
                            _isLoading.value = false
                        }
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    android.util.Log.e("AsignaturasVM", "❌ Error creando asignatura", exception)
                    _isLoading.value = false
                }
        }
    }

    /**
     * Genera código único para una asignatura (solo para botón manual).
     */
    fun generarCodigo(asignaturaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _codigoGenerado.value = null

            repository.generarCodigo(asignaturaId)
                .onSuccess { codigo ->
                    _codigoGenerado.value = codigo
                    android.util.Log.d("AsignaturasVM", "✅ Código generado: $codigo")
                    // NO llamar a sincronizarAsignaturas() aquí para evitar loops
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    android.util.Log.e("AsignaturasVM", "❌ Error generando código", exception)
                }

            _isLoading.value = false
        }
    }

    /**
     * Actualiza una asignatura existente.
     */
    fun actualizarAsignatura(asignatura: Asignatura) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.actualizarAsignatura(asignatura)
                .onSuccess {
                    android.util.Log.d("AsignaturasVM", "✅ Asignatura actualizada")
                    _operationSuccess.value = "Asignatura actualizada"
                    // ✅ NO llamar a sincronizarAsignaturas() - Room Flow se actualiza automáticamente
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    android.util.Log.e("AsignaturasVM", "❌ Error actualizando", exception)
                }

            _isLoading.value = false
        }
    }

    /**
     * Elimina una asignatura.
     */
    fun eliminarAsignatura(asignaturaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.eliminarAsignatura(asignaturaId)
                .onSuccess {
                    android.util.Log.d("AsignaturasVM", "✅ Asignatura eliminada")
                    _operationSuccess.value = "Asignatura eliminada"
                    // ✅ NO llamar a sincronizarAsignaturas() - Room Flow se actualiza automáticamente
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    android.util.Log.e("AsignaturasVM", "❌ Error eliminando", exception)
                }

            _isLoading.value = false
        }
    }

    /**
     * Limpia el código generado (después de mostrarlo).
     */
    fun limpiarCodigoGenerado() {
        _codigoGenerado.value = null
    }

    /**
     * Limpia el error.
     */
    fun limpiarError() {
        _error.value = null
    }

    /**
     * Obtiene el código de acceso de una asignatura para copiar.
     * Retorna el código si existe, null en caso contrario.
     */
    fun obtenerCodigoParaCopiar(asignaturaId: String): String? {
        val asignatura = asignaturas.value?.find { it.id == asignaturaId }
        return asignatura?.codigoAcceso
    }

    /**
     * Crea datos de demostración: Una asignatura y una clase asociada.
     * Útil para probar el flujo completo cuando la cuenta está vacía.
     */
    /**
     * Crea datos de demostración: Una asignatura y una clase asociada.
     * Útil para probar el flujo completo cuando la cuenta está vacía.
     */
    fun crearAsignaturaYClaseDemo() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // 1. Crear Asignatura Demo
                val nombreAsignatura = "Arquitectura de Software (Demo)"
                val descAsignatura = "Asignatura de demostración creada automáticamente."
                
                android.util.Log.d("AsignaturasVM", "🛠️ Creando asignatura demo...")
                
                // Usamos el repositorio directamente para tener control del flujo
                repository.crearAsignatura(nombreAsignatura, descAsignatura)
                    .onSuccess { asignatura ->
                        android.util.Log.d("AsignaturasVM", "✅ Asignatura demo creada: ${asignatura.id}")
                        
                        // 2. Generar Código
                        repository.generarCodigo(asignatura.id).onSuccess { codigo ->
                             _codigoGenerado.value = codigo
                        }

                        // 3. Crear Clase Demo asociada
                        val nombreClase = "Clase 1: Introducción a Patrones"
                        val descClase = "Esta es una clase de prueba para verificar la sincronización."
                        val fechaClase = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        
                        // FIX: 'sincronizado' no es parte del modelo Clase, solo de la entidad local.
                        val nuevaClase = cl.duocuc.aulaviva.data.model.Clase(
                            id = cl.duocuc.aulaviva.utils.IdUtils.generateId(),
                            nombre = nombreClase,
                            descripcion = descClase,
                            fecha = fechaClase,
                            creador = authRepository.getCurrentUserId() ?: "",
                            asignaturaId = asignatura.id
                            // sincronizado = false // REMOVED: No existe en el constructor de Clase
                        )
                        
                        // FIX: pasar callbacks onSuccess/onError, no es un Result
                        claseRepository.crearClase(
                            clase = nuevaClase,
                            onSuccess = {
                                android.util.Log.d("AsignaturasVM", "✅ Clase demo creada en local")
                                _operationSuccess.value = "Entorno Demo creado exitosamente"
                                
                                // Forzar sync de clases (se lanza en otra coroutine porque onSuccess no es suspend)
                                viewModelScope.launch {
                                    try {
                                        claseRepository.sincronizarDesdeSupabase()
                                    } catch (e: Exception) {
                                        android.util.Log.e("AsignaturasVM", "⚠️ Sync falló en demo", e)
                                    }
                                }
                            },
                            onError = { errorMsg ->
                                android.util.Log.e("AsignaturasVM", "❌ Error creando clase demo: $errorMsg")
                                _operationSuccess.value = "Asignatura creada, pero falló clase demo: $errorMsg"
                            }
                        )
                    }
                    .onFailure { e ->
                        _error.value = e.message
                        android.util.Log.e("AsignaturasVM", "❌ Error creando asignatura demo", e)
                    }

            } catch (e: Exception) {
                _error.value = "Error en demo: ${e.message}"
                android.util.Log.e("AsignaturasVM", "❌ Crash generando demo", e)
            } finally {
                // _isLoading se maneja dentro de los callbacks u observando el repositorio
                // Dejamos un delay pequeño para que la UI no parpadee si fue muy rápido
                delay(500)
                _isLoading.value = false
            }
        }
    }
}
