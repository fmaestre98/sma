package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.RegistrationRequest
import cu.uci.cisol.monitoreoambiental.domain.model.TokenResponse

interface AuthRepository {
    suspend fun login(credentials: AuthCredentials): Result<TokenResponse>
    suspend fun register(request: RegistrationRequest): Result<Unit>
}
