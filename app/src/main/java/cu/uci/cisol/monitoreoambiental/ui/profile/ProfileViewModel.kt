package cu.uci.cisol.monitoreoambiental.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.repository.UsuarioRepository
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
                            firstName = it.firstName.ifEmpty { user.firstName ?: "" },
                            lastName = it.lastName.ifEmpty { user.lastName ?: "" },
                            phone = it.phone.ifEmpty { user.phone },
                            email = it.email.ifEmpty { user.email }
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
        _uiState.update { it.copy(firstName = name, firstNameError = null) }
    }

    fun onLastNameChange(name: String) {
        _uiState.update { it.copy(lastName = name, lastNameError = null) }
    }

    fun onPhoneChange(phone: String) {
        val newPhone = phone.filter { it.isDigit() }.take(8)
        _uiState.update { it.copy(phone = newPhone, phoneError = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return email.matches(emailRegex.toRegex())
    }

    fun onAvatarSelected(uri: android.net.Uri?) {
        _uiState.update { it.copy(localAvatarUri = uri) }
    }

    fun onSave() {
        val currentState = _uiState.value
        var hasError = false
        var firstNameError: String? = null
        var lastNameError: String? = null
        var phoneError: String? = null
        var emailError: String? = null

        if (currentState.firstName.isBlank()) {
            firstNameError = "El nombre no puede estar vacío"
            hasError = true
        }

        if (currentState.lastName.isBlank()) {
            lastNameError = "Los apellidos no pueden estar vacíos"
            hasError = true
        }

        if (currentState.phone.isBlank()) {
            phoneError = "El teléfono no puede estar vacío"
            hasError = true
        } else if (currentState.phone.length < 8) {
            phoneError = "El teléfono debe tener 8 dígitos"
            hasError = true
        }

        if (currentState.email.isBlank()) {
            emailError = "El correo no puede estar vacío"
            hasError = true
        } else if (!isValidEmail(currentState.email)) {
            emailError = "El correo no es válido"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                    phoneError = phoneError,
                    emailError = emailError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, updateSuccess = false) }
            val result = usuarioRepository.updateProfile(
                firstName = currentState.firstName.ifBlank { null },
                lastName = currentState.lastName.ifBlank { null },
                phone = currentState.phone,
                email = currentState.email,
                avatarUri = currentState.localAvatarUri?.toString()
            )
            
            _uiState.update { it.copy(isLoading = false) }
            
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(localAvatarUri = null, updateSuccess = true) }
                    _events.emit(ProfileEvent.UpdateSuccess)
                }
                is Result.Error -> {
                    _events.emit(ProfileEvent.Error(result.error.toUiText()))
                }
            }
        }
    }
}
