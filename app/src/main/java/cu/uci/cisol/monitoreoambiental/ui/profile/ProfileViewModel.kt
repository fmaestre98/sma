package cu.uci.cisol.monitoreoambiental.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.repository.UsuarioRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.ProfileError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import cu.uci.cisol.monitoreoambiental.ui.util.toUiText
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            usuarioRepository.obtenerFlow().collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            user = user,
                            userName = if (it.hasUnsavedChanges) it.userName else user.userName,
                            firstName = if (it.hasUnsavedChanges) it.firstName else user.firstName ?: "",
                            lastName = if (it.hasUnsavedChanges) it.lastName else user.lastName ?: "",
                            phone = if (it.hasUnsavedChanges) it.phone else user.phone,
                            email = user.email,
                            country = user.country.orEmpty(),
                            ci = if (it.hasUnsavedChanges) it.ci else user.ci.orEmpty(),
                            passport = if (it.hasUnsavedChanges) it.passport else user.passport.orEmpty(),
                        )
                    }
                }
            }
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = usuarioRepository.refreshProfile()
            _uiState.update { it.copy(isLoading = false) }
            if (result is Result.Error) {
                _events.emit(ProfileEvent.Error(result.error.toUiText()))
            }
        }
    }

    fun onFirstNameChange(name: String) {
        _uiState.update { it.copy(firstName = name, firstNameError = null, hasUnsavedChanges = true) }
    }

    fun onLastNameChange(name: String) {
        _uiState.update { it.copy(lastName = name, lastNameError = null, hasUnsavedChanges = true) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, phoneError = null, hasUnsavedChanges = true) }
    }

    fun onUserNameChange(userName: String) {
        _uiState.update { it.copy(userName = userName, userNameError = null, hasUnsavedChanges = true) }
    }

    fun onCiChange(ci: String) {
        _uiState.update { it.copy(ci = ci.filter(Char::isDigit), ciError = null, hasUnsavedChanges = true) }
    }

    fun onPassportChange(passport: String) {
        _uiState.update { it.copy(passport = passport, passportError = null, hasUnsavedChanges = true) }
    }

    fun onAvatarSelected(uri: android.net.Uri?) {
        _uiState.update { it.copy(localAvatarUri = uri, hasUnsavedChanges = true) }
    }

    fun onSave() {
        val currentState = _uiState.value
        var hasError = false
        var firstNameError: String? = null
        var lastNameError: String? = null
        var phoneError: String? = null
        var userNameError: String? = null
        var ciError: String? = null
        var passportError: String? = null

        if (currentState.userName.isBlank()) {
            userNameError = "El campo Nombre de usuario es obligatorio"
            hasError = true
        } else if (currentState.userName.trim().length !in 8..15) {
            userNameError = "El nombre de usuario debe tener entre 8 y 15 caracteres"
            hasError = true
        }

        val namePattern = Regex("[\\p{L} ]*")
        if (!currentState.firstName.matches(namePattern)) {
            firstNameError = "El nombre solo admite letras y espacios"
            hasError = true
        }
        if (!currentState.lastName.matches(namePattern)) {
            lastNameError = "Los apellidos solo admiten letras y espacios"
            hasError = true
        }

        if (currentState.phone.length > 15 || !currentState.phone.all { it.isDigit() }) {
            phoneError = "El teléfono solo admite dígitos y un máximo de 15"
            hasError = true
        }

        if (currentState.country == "CU" && currentState.ci.isBlank()) {
            ciError = "El CI es obligatorio para Cuba"
            hasError = true
        } else if (currentState.country == "CU" && currentState.ci.length != 11) {
            ciError = "El CI debe tener 11 dígitos"
            hasError = true
        }
        if (currentState.country.isNotBlank() && currentState.country != "CU" && currentState.passport.isBlank()) {
            passportError = "El pasaporte es obligatorio"
            hasError = true
        } else if (currentState.country != "CU" && currentState.passport.length > 9) {
            passportError = "El pasaporte admite un máximo de 9 caracteres"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                    userNameError = userNameError,
                    phoneError = phoneError,
                    ciError = ciError,
                    passportError = passportError,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, updateSuccess = false) }
            val result = usuarioRepository.updateProfile(
                userName = currentState.userName.trim(),
                firstName = currentState.firstName.trim(),
                lastName = currentState.lastName.trim(),
                phone = currentState.phone,
                ci = currentState.ci.takeIf {
                    currentState.country == "CU" && currentState.user?.ci.isNullOrBlank() && it.isNotBlank()
                },
                passport = currentState.passport.takeIf {
                    currentState.country.isNotBlank() && currentState.country != "CU" &&
                        currentState.user?.passport.isNullOrBlank() && it.isNotBlank()
                },
                avatarUri = currentState.localAvatarUri?.toString()
            )
            
            _uiState.update { it.copy(isLoading = false) }
            
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(localAvatarUri = null, updateSuccess = true, hasUnsavedChanges = false) }
                    _events.emit(ProfileEvent.UpdateSuccess)
                }
                is Result.Error -> {
                    when (val error = result.error) {
                        is ProfileError.Validation -> _events.emit(ProfileEvent.MessageError(error.message))
                        is ProfileError.Network -> _events.emit(ProfileEvent.Error(error.error.toUiText()))
                    }
                }
            }
        }
    }

    fun showChangePassword() {
        _uiState.update {
            it.copy(showChangePassword = true, currentPassword = "", newPassword = "", confirmNewPassword = "", passwordError = null)
        }
    }

    fun onCurrentPasswordChange(value: String) = _uiState.update { it.copy(currentPassword = value, currentPasswordError = null, passwordError = null) }
    fun onNewPasswordChange(value: String) = _uiState.update { it.copy(newPassword = value, newPasswordError = null, passwordError = null) }
    fun onConfirmNewPasswordChange(value: String) = _uiState.update { it.copy(confirmNewPassword = value, confirmPasswordError = null, passwordError = null) }

    fun cancelChangePassword() {
        _uiState.update {
            it.copy(showChangePassword = false, currentPassword = "", newPassword = "", confirmNewPassword = "",
                currentPasswordError = null, newPasswordError = null, confirmPasswordError = null, passwordError = null)
        }
    }

    fun submitChangePassword() {
        val current = _uiState.value
        var currentError: String? = null
        var newError: String? = null
        var confirmError: String? = null
        if (current.currentPassword.isBlank()) currentError = "La contraseña actual es obligatoria"
        if (current.newPassword.isBlank()) newError = "La nueva contraseña es obligatoria"
        else if (!hasPasswordPolicy(current.newPassword)) {
            newError = "La contraseña debe tener mínimo 12 caracteres e incluir mayúsculas, minúsculas, números y caracteres especiales"
        } else if (current.newPassword == current.currentPassword) {
            newError = "La nueva contraseña debe ser distinta a la actual"
        }
        if (current.confirmNewPassword.isBlank()) confirmError = "La confirmación es obligatoria"
        else if (current.confirmNewPassword != current.newPassword) confirmError = "Las contraseñas no coinciden"
        if (currentError != null || newError != null || confirmError != null) {
            _uiState.update {
                it.copy(currentPasswordError = currentError, newPasswordError = newError, confirmPasswordError = confirmError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(passwordChangeLoading = true, passwordError = null) }
            when (val result = usuarioRepository.changePassword(current.currentPassword, current.newPassword, current.confirmNewPassword)) {
                is Result.Success -> {
                    _uiState.update { it.copy(passwordChangeLoading = false) }
                    cancelChangePassword()
                    _events.emit(ProfileEvent.PasswordUpdateSuccess)
                }
                is Result.Error -> {
                    val message = when (val error = result.error) {
                        is ProfileError.Validation -> error.message
                        is ProfileError.Network -> when (error.error) {
                            cu.uci.cisol.monitoreoambiental.domain.util.DataError.Network.NO_INTERNET -> "Se requiere conexión a internet para cambiar la contraseña."
                            cu.uci.cisol.monitoreoambiental.domain.util.DataError.Network.UNAUTHORIZED -> "La sesión no es válida. Inicie sesión nuevamente."
                            cu.uci.cisol.monitoreoambiental.domain.util.DataError.Network.REQUEST_TIMEOUT -> "La solicitud tardó demasiado. Inténtelo de nuevo."
                            else -> "No se pudo cambiar la contraseña. Inténtelo de nuevo."
                        }
                    }
                    _uiState.update { it.copy(passwordChangeLoading = false, passwordError = message) }
                }
            }
        }
    }

    private fun hasPasswordPolicy(value: String): Boolean =
        value.length >= 12 && value.any { it.isUpperCase() } && value.any { it.isLowerCase() } &&
            value.any { it.isDigit() } && value.any { !it.isLetterOrDigit() }
}
