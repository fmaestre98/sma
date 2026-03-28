package cu.uci.cisol.monitoreoambiental.domain.usecase

import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.TokenResponse
import cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository

class LoginUseCase(
        private val authRepository: AuthRepository,
        private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(credentials: AuthCredentials): Result<TokenResponse> {
        val result = authRepository.login(credentials)
        if (result.isSuccess) {
            val token = result.getOrNull()?.token
            if (token != null) {
                sessionRepository.saveToken(token)
            }
        }
        return result
    }
}
