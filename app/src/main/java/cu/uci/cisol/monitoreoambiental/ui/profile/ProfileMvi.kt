package cu.uci.cisol.monitoreoambiental.ui.profile

import cu.uci.cisol.monitoreoambiental.domain.model.Usuario
import cu.uci.cisol.monitoreoambiental.ui.util.UiText

data class ProfileState(
    val isLoading: Boolean = false,
    val user: Usuario? = null,
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val localAvatarUri: android.net.Uri? = null,
    val emailError: String? = null,
    val errorMessage: String? = null,
    val updateSuccess: Boolean = false
)

sealed interface ProfileEvent {
    data class Error(val uiText: UiText) : ProfileEvent
    object UpdateSuccess : ProfileEvent
}
