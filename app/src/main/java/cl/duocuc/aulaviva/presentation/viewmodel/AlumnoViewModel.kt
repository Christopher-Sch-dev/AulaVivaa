package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IAlumnoRepository
import cl.duocuc.aulaviva.domain.repository.IAuthRepository
import cl.duocuc.aulaviva.domain.repository.IStorageRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel para gestión de inscripciones (ALUMNOS).
 * Maneja inscripción con código y visualización de asignaturas inscritas.
 */
class AlumnoViewModel(application: Application) : AndroidViewModel(application) {

    // Repository
    private val repository: IAlumnoRepository = RepositoryProvider.provideAlumnoRepository(application)
    private val authRepository: IAuthRepository = RepositoryProvider.provideAuthRepository()
    // Centralized storage repo (for future uploads/downloads)
    private val storageRepository: IStorageRepository = RepositoryProvider.provideStorageRepository(application)

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    private val _userEmail = MutableLiveData<String?>()
    val userEmail: LiveData<String?> = _userEmail

    // LiveData para asignaturas inscritas (automática desde Room)
    val asignaturasInscritas: LiveData<List<Asignatura>> = repository.obtenerAsignaturasInscritas().asLiveData()

    // Estados de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _inscripcionExitosa = MutableLiveData<Asignatura?>()
    val inscripcionExitosa: LiveData<Asignatura?> = _inscripcionExitosa

    init {
        // Inicializar user info y verificar autenticación antes de sincronizar
        viewModelScope.launch {
            // Pequeño delay para asegurar que la sesión esté completamente establecida
            delay(200)

            if (authRepository.isLoggedIn()) {
                // Obtener email del usuario
                _userEmail.value = authRepository.getCurrentUserEmail()
                // Sincronizar asignaturas
                sincronizarAsignaturasInscritas()
            } else {
                android.util.Log.w("AlumnoVM", "⚠️ Usuario no autenticado, omitiendo sincronización inicial")
            }
        }
    }

    /**
     * Sincroniza asignaturas inscritas desde Supabase.
     */
    fun sincronizarAsignaturasInscritas() {
        viewModelScope.launch {
            // Verificar autenticación antes de sincronizar
            if (!authRepository.isLoggedIn()) {
                android.util.Log.w("AlumnoVM", "⚠️ Usuario no autenticado, no se puede sincronizar")
                _error.value = "Sesión no válida. Por favor, inicia sesión nuevamente."
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            repository.sincronizarAsignaturasInscritas()
                .onSuccess {
                    android.util.Log.d("AlumnoVM", "✅ Sincronización exitosa")
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
                    android.util.Log.e("AlumnoVM", "❌ Error sincronizando: ${exception.message}", exception)
                }

            _isLoading.value = false
        }
    }

    /**
     * Inscribe al alumno usando código de asignatura.
     */
    fun inscribirConCodigo(codigo: String) {
        if (codigo.isBlank()) {
            _error.value = "El código no puede estar vacío"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _inscripcionExitosa.value = null

            repository.inscribirConCodigo(codigo)
                .onSuccess { asignatura ->
                    android.util.Log.d("AlumnoVM", "✅ Inscrito en: ${asignatura.nombre}")
                    // Marcar la inscripción como exitosa inmediatamente para actualizar la UI
                    _inscripcionExitosa.value = asignatura
                    // Sincronizar las asignaturas en background sin bloquear la UI
                    // Esto actualiza la lista local con los datos más recientes de Supabase
                    viewModelScope.launch {
                        try {
                            repository.sincronizarAsignaturasInscritas()
                                .onSuccess {
                                    android.util.Log.d("AlumnoVM", "✅ Sincronización después de inscripción exitosa")
                                }
                                .onFailure { e ->
                                    android.util.Log.w("AlumnoVM", "⚠️ Error sincronizando después de inscripción (no crítico)", e)
                                    // No mostrar error al usuario porque la inscripción ya fue exitosa
                                    // La sincronización es una operación secundaria
                                }
                        } catch (e: Exception) {
                            android.util.Log.w("AlumnoVM", "⚠️ Excepción sincronizando después de inscripción (no crítico)", e)
                            // No mostrar error al usuario porque la inscripción ya fue exitosa
                            // La sincronización es una operación secundaria
                        }
                    }
                }
                .onFailure { exception ->
                    android.util.Log.e("AlumnoVM", "❌ Error inscribiendo: ${exception.message}", exception)
                    _error.value = when {
                        exception.message?.contains("Código inválido", ignoreCase = true) == true ||
                        exception.message?.contains("no encontrado", ignoreCase = true) == true ->
                            "❌ El código ingresado no existe"
                        exception.message?.contains("Ya estás inscrito", ignoreCase = true) == true ->
                            "⚠️ Ya estás inscrito en esta asignatura"
                        exception.message?.contains("Usuario no autenticado", ignoreCase = true) == true ->
                            "❌ Sesión expirada. Por favor, inicia sesión nuevamente"
                        else -> "❌ Error: ${exception.message ?: "Error desconocido"}"
                    }
                }

            _isLoading.value = false
        }
    }

    /**
     * Darse de baja de una asignatura.
     */
    fun darDeBaja(asignaturaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.darDeBaja(asignaturaId)
                .onSuccess {
                    android.util.Log.d("AlumnoVM", "✅ Baja exitosa")
                    sincronizarAsignaturasInscritas()
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    android.util.Log.e("AlumnoVM", "❌ Error dando de baja", exception)
                }

            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (_: Exception) {
                // ignore
            }
            _logoutEvent.postValue(true)
        }
    }

    /**
     * Verifica si el alumno está inscrito en una asignatura.
     */
    suspend fun estaInscrito(asignaturaId: String): Boolean {
        return repository.estaInscrito(asignaturaId)
    }

    /**
     * Limpia el estado de inscripción exitosa.
     */
    fun limpiarInscripcionExitosa() {
        _inscripcionExitosa.value = null
    }

    /**
     * Limpia el error.
     */
    fun limpiarError() {
        _error.value = null
    }
}
