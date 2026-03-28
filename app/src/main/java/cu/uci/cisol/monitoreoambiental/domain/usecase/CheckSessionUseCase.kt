package cu.uci.cisol.monitoreoambiental.domain.usecase

import cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CheckSessionUseCase(private val sessionRepository: SessionRepository) {
    val isSessionActive: Flow<Boolean> = sessionRepository.tokenFlow.map { it != null }

    suspend fun hasSession(): Boolean {
        return sessionRepository.getToken() != null
    }
}
