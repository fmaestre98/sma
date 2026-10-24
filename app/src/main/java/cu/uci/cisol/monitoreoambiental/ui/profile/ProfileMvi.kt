package cu.uci.cisol.monitoreoambiental.ui.profile

import cu.uci.cisol.monitoreoambiental.domain.model.Usuario
import cu.uci.cisol.monitoreoambiental.ui.util.UiText

data class ProfileState(
    val isLoading: Boolean = false,
    val user: Usuario? = null,
    val userName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val country: String = "",
    val ci: String = "",
    val passport: String = "",
    val hasUnsavedChanges: Boolean = false,
    val localAvatarUri: android.net.Uri? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val userNameError: String? = null,
    val ciError: String? = null,
    val passportError: String? = null,
    val showChangePassword: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val passwordChangeLoading: Boolean = false,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val updateSuccess: Boolean = false
)

sealed interface ProfileEvent {
    data class Error(val uiText: UiText) : ProfileEvent
    data class MessageError(val message: String) : ProfileEvent
    object UpdateSuccess : ProfileEvent
    object PasswordUpdateSuccess : ProfileEvent
}
