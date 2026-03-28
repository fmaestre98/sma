package cu.uci.cisol.monitoreoambiental.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.RegistrationRequest
import cu.uci.cisol.monitoreoambiental.domain.usecase.CheckSessionUseCase
import cu.uci.cisol.monitoreoambiental.domain.usecase.LoginUseCase
import cu.uci.cisol.monitoreoambiental.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object SuccessLogin : AuthState()
    object SuccessRegister : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
        private val loginUseCase: LoginUseCase,
        private val registerUseCase: RegisterUseCase,
        private val checkSessionUseCase: CheckSessionUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isSessionActive = checkSessionUseCase.isSessionActive

    fun login(username: String, contrasena: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = loginUseCase(AuthCredentials(username, contrasena))
            if (result.isSuccess) {
                _authState.value = AuthState.SuccessLogin
            } else {
                _authState.value =
                        AuthState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun register(username: String, contrasena: String, nombre: String, correo: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val request = RegistrationRequest(username, contrasena, nombre, correo)
            val result = registerUseCase(request)
            if (result.isSuccess) {
                _authState.value = AuthState.SuccessRegister
            } else {
                _authState.value =
                        AuthState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
