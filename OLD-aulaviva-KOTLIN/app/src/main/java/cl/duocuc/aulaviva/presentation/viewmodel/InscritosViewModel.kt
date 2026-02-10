package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IAsignaturasRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.asLiveData
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaEntity

/**
 * ViewModel para la pantalla de inscritos.
 * Maneja la sincronización de alumnos inscritos.
 */
class InscritosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IAsignaturasRepository = RepositoryProvider.provideAsignaturasRepository(application)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * Sincroniza los inscritos desde Supabase.
     */
    fun sincronizarInscritos(asignaturaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.sincronizarInscritos(asignaturaId)
                .onSuccess {
                    android.util.Log.d("InscritosVM", "✅ Sincronización exitosa")
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    android.util.Log.e("InscritosVM", "❌ Error sincronizando", exception)
                }

            _isLoading.value = false
        }
    }

    /**
     * Expone los inscritos como LiveData para que la UI los observe.
     * Esto evita que la Activity acceda directamente a `AppDatabase`.
     */
    fun obtenerInscritosLive(asignaturaId: String) =
        repository.obtenerInscritosFlow(asignaturaId).asLiveData()

    fun clearError() {
        _error.value = null
    }
}
