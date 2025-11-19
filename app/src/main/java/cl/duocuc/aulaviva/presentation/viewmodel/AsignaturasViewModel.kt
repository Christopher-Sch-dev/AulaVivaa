package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.data.repository.AsignaturasRepository
import cl.duocuc.aulaviva.data.supabase.SupabaseAsignaturaRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para gestión de asignaturas (DOCENTES).
 * Maneja el estado de la UI y orquesta operaciones con el Repository.
 */
class AsignaturasViewModel(application: Application) : AndroidViewModel(application) {

    // Repository
    private val database = AppDatabase.getDatabase(application)
    private val repository = AsignaturasRepository(
        asignaturaDao = database.asignaturaDao(),
        alumnoAsignaturaDao = database.alumnoAsignaturaDao(),
        supabaseRepository = SupabaseAsignaturaRepository(
            asignaturaDao = database.asignaturaDao(),
            alumnoAsignaturaDao = database.alumnoAsignaturaDao()
        )
    )

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
        // ✅ Sincronizar UNA SOLA VEZ al inicio
        sincronizarAsignaturas()
    }

    /**
     * Sincroniza asignaturas desde Supabase.
     */
    fun sincronizarAsignaturas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.sincronizarAsignaturas()
                .onSuccess {
                    android.util.Log.d("AsignaturasVM", "✅ Sincronización exitosa")
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    android.util.Log.e("AsignaturasVM", "❌ Error sincronizando", exception)
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
}
