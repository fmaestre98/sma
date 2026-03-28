package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.data.remote.AuthApi
import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.RegistrationRequest
import cu.uci.cisol.monitoreoambiental.domain.model.TokenResponse
import cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository

class AuthRepositoryImpl(private val api: AuthApi) : AuthRepository {
    override suspend fun login(credentials: AuthCredentials): Result<TokenResponse> {
        return try {
            val response = api.login(credentials)
            Result.success(response)
        } catch (e: Exception) {
            // Log error
            Result.failure(e)
        }
    }

    override suspend fun register(request: RegistrationRequest): Result<Unit> {
        return try {
            api.register(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
