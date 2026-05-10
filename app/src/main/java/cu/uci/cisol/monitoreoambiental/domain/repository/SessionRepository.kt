package cu.uci.cisol.monitoreoambiental.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val accessTokenFlow: Flow<String?>
    val refreshTokenFlow: Flow<String?>
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clearSession()
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
}
