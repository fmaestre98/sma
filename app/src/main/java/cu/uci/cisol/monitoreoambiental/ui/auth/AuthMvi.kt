package cu.uci.cisol.monitoreoambiental.ui.auth

import cu.uci.cisol.monitoreoambiental.domain.model.Session
import cu.uci.cisol.monitoreoambiental.ui.util.UiText

data class AuthState(
    val isLoading: Boolean = false,
    val isLoginMode: Boolean = true,

    // Form fields
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",

    // Conflict handling
    val conflictSessions: List<Session> = emptyList(),
    val isResolvingConflict: Boolean = false,
    val selectedSessionIds: Set<String> = emptySet(),

    // Password reset dialog
    val showPasswordResetDialog: Boolean = false,
    val passwordResetEmail: String = "",
    val passwordResetEmailError: UiText? = null,
    val isPasswordResetLoading: Boolean = false,

    // URL config dialog
    val showUrlConfigDialog: Boolean = false,
    val tempUrl: String = "",

    // Error
    val error: UiText? = null
)

sealed interface AuthAction {
    data class OnEmailChange(val email: String) : AuthAction
    data class OnPasswordChange(val password: String) : AuthAction
    data class OnFirstNameChange(val name: String) : AuthAction
    data class OnLastNameChange(val name: String) : AuthAction
    data class OnUsernameChange(val username: String) : AuthAction

    object SubmitLogin : AuthAction
    object SubmitRegister : AuthAction

    data class OnSessionSelected(val sessionId: String, val selected: Boolean) : AuthAction
    object SubmitConflictResolution : AuthAction
    object CancelConflictResolution : AuthAction

    // Password reset actions
    object ShowPasswordResetDialog : AuthAction
    object DismissPasswordResetDialog : AuthAction
    data class OnPasswordResetEmailChange(val email: String) : AuthAction
    object SubmitPasswordReset : AuthAction

    // URL config actions
    object ShowUrlConfigDialog : AuthAction
    object DismissUrlConfigDialog : AuthAction
    data class OnUrlChange(val url: String) : AuthAction
    data class SaveUrl(val context: android.content.Context) : AuthAction
}

sealed interface AuthEvent {
    object LoginSuccess : AuthEvent
    data class RegisterSuccess(val message: UiText) : AuthEvent
    data class ConflictResolved(val message: UiText) : AuthEvent
    data class ShowSnackbar(val message: UiText) : AuthEvent
}
