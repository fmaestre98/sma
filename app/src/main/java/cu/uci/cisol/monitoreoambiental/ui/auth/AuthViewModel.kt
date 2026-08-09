package cu.uci.cisol.monitoreoambiental.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.R
import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.RegistrationRequest
import cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.EstadoRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.LocalidadRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.TematicaRepository
import cu.uci.cisol.monitoreoambiental.domain.util.AuthError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import cu.uci.cisol.monitoreoambiental.ui.util.UiText
import cu.uci.cisol.monitoreoambiental.ui.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val tematicaRepository: TematicaRepository,
    private val estadoRepository: EstadoRepository,
    private val localidadRepository: LocalidadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>()
    val events = _events.receiveAsFlow()

    val isSessionActive = sessionRepository.accessTokenFlow.map { it != null }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.OnEmailChange -> _state.update { it.copy(email = action.email, error = null) }
            is AuthAction.OnPasswordChange -> _state.update { it.copy(password = action.password, error = null) }
            is AuthAction.OnFirstNameChange -> _state.update { it.copy(firstName = action.name, error = null) }
            is AuthAction.OnLastNameChange -> _state.update { it.copy(lastName = action.name, error = null) }
            is AuthAction.OnUsernameChange -> _state.update { it.copy(username = action.username, error = null) }
            AuthAction.SubmitLogin -> {
                val currentState = _state.value
                if (currentState.email.isBlank() || currentState.password.isBlank()) {
                    _state.update { it.copy(error = UiText.DynamicString("Por favor llene todos los campos")) }
                } else {
                    login(currentState)
                }
            }
            AuthAction.SubmitRegister -> {
                val currentState = _state.value
                if (currentState.email.isBlank() || currentState.password.isBlank() || currentState.firstName.isBlank() || currentState.lastName.isBlank() || currentState.username.isBlank()) {
                    _state.update { it.copy(error = UiText.DynamicString("Por favor llene todos los campos")) }
                } else {
                    register(currentState)
                }
            }
            is AuthAction.OnSessionSelected -> {
                val newSelection = if (action.selected) {
                    _state.value.selectedSessionIds + action.sessionId
                } else {
                    _state.value.selectedSessionIds - action.sessionId
                }
                _state.update { it.copy(selectedSessionIds = newSelection) }
            }
            AuthAction.CancelConflictResolution -> {
                _state.update { it.copy(isResolvingConflict = false, conflictSessions = emptyList(), selectedSessionIds = emptySet()) }
            }
            AuthAction.SubmitConflictResolution -> resolveConflict()
            // Password reset
            AuthAction.ShowPasswordResetDialog -> {
                _state.update { it.copy(showPasswordResetDialog = true, passwordResetEmail = "", passwordResetEmailError = null) }
            }
            AuthAction.DismissPasswordResetDialog -> {
                _state.update { it.copy(showPasswordResetDialog = false, passwordResetEmail = "", passwordResetEmailError = null) }
            }
            is AuthAction.OnPasswordResetEmailChange -> {
                _state.update { it.copy(passwordResetEmail = action.email, passwordResetEmailError = null) }
            }
            AuthAction.SubmitPasswordReset -> submitPasswordReset()
        }
    }

    private fun login(currentState: AuthState) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val credentials = AuthCredentials(currentState.email, currentState.password)
            val result = authRepository.login(credentials)
            
            _state.update { it.copy(isLoading = false) }
            
            when (result) {
                is Result.Success -> {
                    tematicaRepository.syncTematicas()
                    estadoRepository.syncEstados()
                    localidadRepository.syncLocalities()
                    _events.send(AuthEvent.LoginSuccess)
                }
                is Result.Error -> {
                    if (result.error is AuthError.MultipleSessions) {
                        _state.update { 
                            it.copy(
                                isResolvingConflict = true,
                                conflictSessions = result.error.activeSessions
                            )
                        }
                    } else {
                        _state.update { it.copy(error = result.error.toUiText()) }
                        _events.send(AuthEvent.ShowSnackbar(result.error.toUiText()))
                    }
                }
            }
        }
    }

    private fun register(currentState: AuthState) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val request = RegistrationRequest(
                nombreUsuario = currentState.username,
                contrasena = currentState.password,
                nombreCompleto = "${currentState.firstName} ${currentState.lastName}".trim(),
                correo = currentState.email
            )
            
            val result = authRepository.register(request)
            
            _state.update { it.copy(isLoading = false) }
            
            when (result) {
                is Result.Success -> {
                    val msg = UiText.StringResource(R.string.success_register)
                    _state.update { it.copy(isLoginMode = true) }
                    _events.send(AuthEvent.RegisterSuccess(msg))
                    _events.send(AuthEvent.ShowSnackbar(msg))
                }
                is Result.Error -> {
                    _state.update { it.copy(error = result.error.toUiText()) }
                    _events.send(AuthEvent.ShowSnackbar(result.error.toUiText()))
                }
            }
        }
    }

    private fun resolveConflict() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.selectedSessionIds.isEmpty()) return@launch

            _state.update { it.copy(isLoading = true) }

            val credentials = AuthCredentials(currentState.email, currentState.password)
            val result = authRepository.terminateSessions(credentials, currentState.selectedSessionIds.toList())

            _state.update { it.copy(isLoading = false) }

            when (result) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isResolvingConflict = false,
                            conflictSessions = emptyList(),
                            selectedSessionIds = emptySet()
                        )
                    }
                    val msg = UiText.StringResource(R.string.success_logout)
                    _events.send(AuthEvent.ConflictResolved(msg))
                    _events.send(AuthEvent.ShowSnackbar(msg))
                }
                is Result.Error -> {
                    _state.update { it.copy(error = result.error.toUiText()) }
                    _events.send(AuthEvent.ShowSnackbar(result.error.toUiText()))
                }
            }
        }
    }

    private fun submitPasswordReset() {
        val email = _state.value.passwordResetEmail.trim()
        if (email.isBlank()) {
            _state.update { it.copy(passwordResetEmailError = UiText.DynamicString("Por favor ingrese su correo")) }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(passwordResetEmailError = UiText.DynamicString("Ingrese un correo electrónico válido")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isPasswordResetLoading = true, passwordResetEmailError = null) }
            when (val result = authRepository.requestPasswordReset(email)) {
                is Result.Success -> {
                    _state.update { it.copy(isPasswordResetLoading = false, showPasswordResetDialog = false, passwordResetEmail = "") }
                    _events.send(AuthEvent.ShowSnackbar(UiText.DynamicString("Si el email existe, se ha enviado un enlace de recuperación.")))
                }
                is Result.Error -> {
                    _state.update { it.copy(isPasswordResetLoading = false) }
                    _events.send(AuthEvent.ShowSnackbar(result.error.toUiText()))
                }
            }
        }
    }
}
