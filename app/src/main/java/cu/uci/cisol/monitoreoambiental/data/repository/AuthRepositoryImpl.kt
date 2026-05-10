package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.data.remote.AuthApi
import cu.uci.cisol.monitoreoambiental.data.remote.dto.*
import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.RegistrationRequest
import cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository
import cu.uci.cisol.monitoreoambiental.domain.util.AuthError
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.CancellationException
import java.nio.channels.UnresolvedAddressException
import kotlinx.serialization.SerializationException

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val sessionRepository: SessionRepository
) : AuthRepository {

    override suspend fun login(credentials: AuthCredentials): EmptyResult<AuthError> {
        return try {
            val request = LoginRequestDto(
                email = credentials.username,
                password = credentials.contrasena
            )
            val response = api.login(request)
            
            when (response.status.value) {
                in 200..299 -> {
                    val tokenResponse = response.body<TokenResponseDto>()
                    sessionRepository.saveTokens(tokenResponse.access, tokenResponse.refresh)
                    Result.Success(Unit)
                }
                401 -> {
                    val errorDto = response.body<ErrorDetailDto>()
                    Result.Error(AuthError.Blocked(errorDto.detail))
                }
                409 -> {
                    val conflictDto = response.body<MultipleSessionsConflictDto>()
                    Result.Error(AuthError.MultipleSessions(conflictDto.activeSessions.map { it.toSession() }))
                }
                else -> Result.Error(AuthError.InvalidCredentials)
            }
        } catch (e: UnresolvedAddressException) {
            Result.Error(AuthError.NetworkError(DataError.Network.NO_INTERNET))
        } catch (e: SerializationException) {
            Result.Error(AuthError.NetworkError(DataError.Network.SERIALIZATION))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(AuthError.Unknown)
        }
    }

    override suspend fun register(request: RegistrationRequest): EmptyResult<AuthError> {
        val dto = RegisterRequestDto(
            email = request.correo ?: "",
            firstName = request.nombreCompleto?.split(" ")?.firstOrNull() ?: "",
            lastName = request.nombreCompleto?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
            password = request.contrasena,
            password1 = request.contrasena,
            password2 = request.contrasena,
            userNameUnderscore = request.nombreUsuario,
            username = request.nombreUsuario
        )
        
        return when (val result = api.register(dto)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(AuthError.NetworkError(result.error))
        }
    }

    override suspend fun logout(): EmptyResult<AuthError> {
        val refreshToken = sessionRepository.getRefreshToken() ?: return Result.Success(Unit)
        val result = api.logout(LogoutRequestDto(refresh = refreshToken))
        return when (result) {
            is Result.Success -> {
                sessionRepository.clearSession()
                Result.Success(Unit)
            }
            is Result.Error -> {
                sessionRepository.clearSession() // Clear locally even if API fails
                Result.Error(AuthError.NetworkError(result.error))
            }
        }
    }

    override suspend fun terminateSessions(credentials: AuthCredentials, sessionIds: List<String>): EmptyResult<AuthError> {
        val request = TerminateSessionsRequestDto(
            email = credentials.username,
            password = credentials.contrasena,
            sessionIds = sessionIds
        )
        return when (val result = api.terminateSessions(request)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(AuthError.NetworkError(result.error))
        }
    }
}
