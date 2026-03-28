package cu.uci.cisol.monitoreoambiental.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val tokenFlow: Flow<String?>
    suspend fun saveToken(token: String)
    suspend fun clearSession()
    suspend fun getToken(): String?
}
