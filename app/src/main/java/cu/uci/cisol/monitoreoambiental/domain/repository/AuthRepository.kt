package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.RegistrationRequest
import cu.uci.cisol.monitoreoambiental.domain.util.AuthError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult

interface AuthRepository {
    suspend fun login(credentials: AuthCredentials): EmptyResult<AuthError>
    suspend fun register(request: RegistrationRequest): EmptyResult<AuthError>
    suspend fun verifyRegistration(email: String, otp: String): EmptyResult<AuthError>
    suspend fun resendOtp(email: String): EmptyResult<AuthError>
    suspend fun logout(): EmptyResult<AuthError>
    suspend fun terminateSessions(credentials: AuthCredentials, sessionIds: List<String>): EmptyResult<AuthError>
    suspend fun requestPasswordReset(email: String): EmptyResult<AuthError>
}
