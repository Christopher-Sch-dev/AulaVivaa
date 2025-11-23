package cl.duocuc.aulaviva.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IAuthRepository

/**
 * ViewModel para manejar la lógica de autenticación
 * Separa la lógica de la UI y mantiene el estado
 */
class AuthViewModel : ViewModel() {

    private val repository: IAuthRepository = RepositoryProvider.provideAuthRepository()

    // LiveData para el estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para errores
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData para login exitoso
    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    // LiveData para registro exitoso
    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    // Validar email
    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Validar password
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    // Login
    fun login(email: String, password: String) {
        _isLoading.value = true
        _error.value = null

        // Use viewModelScope to call suspend repository method
        viewModelScope.launch {
            val result = repository.login(email, password)
            result.fold(onSuccess = {
                _isLoading.value = false
                _loginSuccess.value = true
            }, onFailure = { e ->
                _isLoading.value = false
                _error.value = "Error de login: ${e.message}"
            })
        }
    }

    // Registro
    fun register(email: String, password: String, rol: String = "alumno") {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = repository.register(email, password, rol)
            result.fold(onSuccess = {
                _isLoading.value = false
                _registerSuccess.value = true
            }, onFailure = { e ->
                _isLoading.value = false
                if (e.message == "pending_confirmation") {
                    _error.value = "Cuenta creada. Revisa tu email para confirmar la cuenta."
                } else {
                    _error.value = e.message
                }
            })
        }
    }

    // Logout
    fun logout() {
        viewModelScope.launch {
            try {
                repository.logout()
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    // Obtener usuario actual
    fun getCurrentUserEmail(): String? {
        return repository.getCurrentUserEmail()
    }

    // Resetear mensajes de error
    fun clearError() {
        _error.value = null
    }
}
