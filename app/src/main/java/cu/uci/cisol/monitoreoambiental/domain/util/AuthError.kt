package cu.uci.cisol.monitoreoambiental.domain.util

import cu.uci.cisol.monitoreoambiental.domain.model.Session

sealed interface AuthError : Error {
    data class MultipleSessions(val activeSessions: List<Session>) : AuthError
    data class Blocked(val message: String) : AuthError
    data class AccountNotVerified(val email: String, val message: String) : AuthError
    data class OtpRequired(val email: String) : AuthError
    data class OtpError(val message: String, val retryAfter: Int? = null) : AuthError
    data class ServerMessage(val message: String) : AuthError
    data class NetworkError(val error: DataError.Network) : AuthError
    object InvalidCredentials : AuthError
    object Unknown : AuthError
}
