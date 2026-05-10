package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.RegistrationRequest
import cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository
import cu.uci.cisol.monitoreoambiental.domain.util.AuthError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import kotlinx.coroutines.delay

class MockAuthRepositoryImpl : AuthRepository {

    override suspend fun login(credentials: AuthCredentials): EmptyResult<AuthError> {
        delay(1000)
        return if (credentials.username == "admin" && credentials.contrasena == "admin123") {
            Result.Success(Unit)
        } else if (credentials.username == "user@test.com" && credentials.contrasena == "password") {
            Result.Success(Unit)
        } else {
            Result.Error(AuthError.InvalidCredentials)
        }
    }

    override suspend fun register(request: RegistrationRequest): EmptyResult<AuthError> {
        delay(1000)
        return if (request.nombreUsuario.isNotBlank() && request.contrasena.length >= 6) {
            Result.Success(Unit)
        } else {
            Result.Error(AuthError.Unknown)
        }
    }

    override suspend fun logout(): EmptyResult<AuthError> {
        delay(500)
        return Result.Success(Unit)
    }

    override suspend fun terminateSessions(credentials: AuthCredentials, sessionIds: List<String>): EmptyResult<AuthError> {
        delay(1000)
        return Result.Success(Unit)
    }
}
