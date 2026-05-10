package cu.uci.cisol.monitoreoambiental.ui.util

import android.content.Context
import androidx.annotation.StringRes
import cu.uci.cisol.monitoreoambiental.R
import cu.uci.cisol.monitoreoambiental.domain.util.AuthError
import cu.uci.cisol.monitoreoambiental.domain.util.DataError

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    class StringResource(@StringRes val id: Int, val args: Array<Any> = emptyArray()) : UiText

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(id, *args)
        }
    }
}

fun DataError.Network.toUiText(): UiText {
    return when (this) {
        DataError.Network.NO_INTERNET -> UiText.StringResource(R.string.error_no_internet)
        DataError.Network.SERVER_ERROR -> UiText.StringResource(R.string.error_server)
        DataError.Network.UNAUTHORIZED -> UiText.StringResource(R.string.error_unauthorized)
        else -> UiText.StringResource(R.string.error_unknown)
    }
}

fun AuthError.toUiText(): UiText {
    return when (this) {
        is AuthError.Blocked -> UiText.DynamicString(message)
        AuthError.InvalidCredentials -> UiText.StringResource(R.string.error_invalid_credentials)
        is AuthError.MultipleSessions -> UiText.StringResource(R.string.error_multiple_sessions)
        is AuthError.NetworkError -> error.toUiText()
        AuthError.Unknown -> UiText.StringResource(R.string.error_unknown)
    }
}
