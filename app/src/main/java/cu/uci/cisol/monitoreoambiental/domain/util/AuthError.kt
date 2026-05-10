package cu.uci.cisol.monitoreoambiental.domain.util

import cu.uci.cisol.monitoreoambiental.domain.model.Session

sealed interface AuthError : Error {
    data class MultipleSessions(val activeSessions: List<Session>) : AuthError
    data class Blocked(val message: String) : AuthError
    data class NetworkError(val error: DataError.Network) : AuthError
    object InvalidCredentials : AuthError
    object Unknown : AuthError
}
