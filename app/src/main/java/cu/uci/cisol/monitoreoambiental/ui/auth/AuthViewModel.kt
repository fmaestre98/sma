package cu.uci.cisol.monitoreoambiental.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.R
import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.RegistrationRequest
import cu.uci.cisol.monitoreoambiental.data.remote.ApiConfig
import cu.uci.cisol.monitoreoambiental.data.remote.AuthApi
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
    private val authApi: AuthApi,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>()
    val events = _events.receiveAsFlow()

    val isSessionActive = sessionRepository.accessTokenFlow.map { it != null }

    init {
        refreshPublicConfig()
    }

    private fun refreshPublicConfig() {
        viewModelScope.launch {
            val config = authApi.getPublicSystemConfig()
            if (config is Result.Success) {
                _state.update { it.copy(allowRegistration = config.data.allowRegistration) }
            } else {
                _state.update { it.copy(allowRegistration = false) }
            }
        }
    }

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
                if (!_state.value.allowRegistration) return
                val currentState = _state.value
                if (currentState.email.isBlank() || currentState.password.isBlank() || currentState.firstName.isBlank() || currentState.lastName.isBlank() || currentState.username.isBlank()) {
                    _state.update { it.copy(error = UiText.DynamicString("Por favor llene todos los campos")) }
                } else {
                    register(currentState)
                }
            }
            is AuthAction.OnOtpChange -> _state.update { it.copy(otpCode = action.otp.filter { ch -> ch.isDigit() }.take(6), error = null) }
            AuthAction.SubmitOtpVerification -> verifyOtp()
            AuthAction.ResendOtp -> resendOtp()
            AuthAction.DismissOtpVerification -> {
                _state.update {
                    it.copy(
                        showOtpVerification = false,
                        otpCode = "",
                        otpEmail = "",
                        otpRetryAfterSeconds = null,
                        error = null,
                    )
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
            AuthAction.ShowUrlConfigDialog -> {
                _state.update { it.copy(showUrlConfigDialog = true, tempUrl = ApiConfig.BASE_URL) }
            }
            AuthAction.DismissUrlConfigDialog -> {
                _state.update { it.copy(showUrlConfigDialog = false) }
            }
            is AuthAction.OnUrlChange -> {
                _state.update { it.copy(tempUrl = action.url) }
            }
            is AuthAction.SaveUrl -> {
                val url = _state.value.tempUrl.trim()
                if (url.isNotEmpty()) {
                    ApiConfig.BASE_URL = url
                    val prefs = action.context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("base_url", url).apply()
                    _state.update { it.copy(allowRegistration = false) }
                    refreshPublicConfig()
                }
                _state.update { it.copy(showUrlConfigDialog = false) }
            }
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
                    syncCatalogs()
                    _events.send(AuthEvent.LoginSuccess)
                }
                is Result.Error -> {
                    when (val error = result.error) {
                        is AuthError.MultipleSessions -> {
                            _state.update {
                                it.copy(
                                    isResolvingConflict = true,
                                    conflictSessions = error.activeSessions,
                                )
                            }
                        }
                        is AuthError.AccountNotVerified -> {
                            _state.update {
                                it.copy(
                                    showOtpVerification = true,
                                    otpEmail = error.email,
                                    otpCode = "",
                                    error = error.toUiText(),
                                )
                            }
                            _events.send(AuthEvent.ShowSnackbar(error.toUiText()))
                        }
                        else -> {
                            _state.update { it.copy(error = error.toUiText()) }
                            _events.send(AuthEvent.ShowSnackbar(error.toUiText()))
                        }
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
                    _state.update {
                        it.copy(
                            showOtpVerification = true,
                            otpEmail = currentState.email,
                            otpCode = "",
                            error = null,
                        )
                    }
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

    private fun verifyOtp() {
        viewModelScope.launch {
            val current = _state.value
            val email = current.otpEmail.ifBlank { current.email }
            if (email.isBlank() || current.otpCode.length < 6) {
                _state.update { it.copy(error = UiText.DynamicString("Ingrese el código OTP de 6 dígitos")) }
                return@launch
            }
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.verifyRegistration(email, current.otpCode)) {
                is Result.Success -> {
                    syncCatalogs()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showOtpVerification = false,
                            otpCode = "",
                            otpEmail = "",
                        )
                    }
                    _events.send(AuthEvent.ShowSnackbar(UiText.StringResource(R.string.success_otp_verified)))
                    _events.send(AuthEvent.LoginSuccess)
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                    _events.send(AuthEvent.ShowSnackbar(result.error.toUiText()))
                }
            }
        }
    }

    private fun resendOtp() {
        viewModelScope.launch {
            val email = _state.value.otpEmail.ifBlank { _state.value.email }
            if (email.isBlank()) return@launch
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.resendOtp(email)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(AuthEvent.ShowSnackbar(UiText.StringResource(R.string.success_otp_resent)))
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                    _events.send(AuthEvent.ShowSnackbar(result.error.toUiText()))
                }
            }
        }
    }

    private suspend fun syncCatalogs() {
        tematicaRepository.syncTematicas()
        estadoRepository.syncEstados()
        localidadRepository.syncLocalities()
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
