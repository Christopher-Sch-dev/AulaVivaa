package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.data.repository.AlumnoRepository
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para gestión de inscripciones (ALUMNOS).
 * Maneja inscripción con código y visualización de asignaturas inscritas.
 */
class AlumnoViewModel(application: Application) : AndroidViewModel(application) {

    // Repository
    private val repository: AlumnoRepository = RepositoryProvider.provideAlumnoRepository(application)
    private val authRepository: AuthRepository = RepositoryProvider.provideAuthRepository()

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

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
        sincronizarAsignaturasInscritas()
    }

    /**
     * Sincroniza asignaturas inscritas desde Supabase.
     */
    fun sincronizarAsignaturasInscritas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.sincronizarAsignaturasInscritas()
                .onSuccess {
                    android.util.Log.d("AlumnoVM", "✅ Sincronización exitosa")
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    android.util.Log.e("AlumnoVM", "❌ Error sincronizando", exception)
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
                    _inscripcionExitosa.value = asignatura
                    android.util.Log.d("AlumnoVM", "✅ Inscrito en: ${asignatura.nombre}")
                    sincronizarAsignaturasInscritas()
                }
                .onFailure { exception ->
                    _error.value = when (exception.message) {
                        "Código inválido" -> "❌ El código ingresado no existe"
                        "Ya estás inscrito en esta asignatura" -> "⚠️ Ya estás inscrito en esta asignatura"
                        else -> "❌ Error: ${exception.message}"
                    }
                    android.util.Log.e("AlumnoVM", "❌ Error inscribiendo", exception)
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
        authRepository.logout()
        _logoutEvent.postValue(true)
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
