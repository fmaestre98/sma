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
                    val errorDto = runCatching { response.body<ErrorDetailDto>() }.getOrNull()
                    if (errorDto?.code == "account_not_verified") {
                        Result.Error(
                            AuthError.AccountNotVerified(
                                email = credentials.username,
                                message = errorDto.detail
                                    ?: "Your account is not verified. Please verify your email.",
                            ),
                        )
                    } else {
                        Result.Error(
                            AuthError.Blocked(
                                errorDto?.detail ?: "Credenciales inválidas o cuenta bloqueada",
                            ),
                        )
                    }
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
            userName = request.nombreUsuario,
            firstName = request.nombreCompleto?.split(" ")?.firstOrNull() ?: "",
            lastName = request.nombreCompleto?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
            password = request.contrasena,
            password2 = request.contrasena,
        )

        return when (val result = api.register(dto)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> mapNetworkAuthError(result.error)
        }
    }

    override suspend fun verifyRegistration(email: String, otp: String): EmptyResult<AuthError> {
        return when (val result = api.verifyRegistration(VerifyRegistrationRequestDto(email = email, otp = otp))) {
            is Result.Success -> {
                sessionRepository.saveTokens(result.data.access, result.data.refresh)
                Result.Success(Unit)
            }
            is Result.Error -> mapNetworkAuthError(result.error)
        }
    }

    override suspend fun resendOtp(email: String): EmptyResult<AuthError> {
        return when (val result = api.resendOtp(ResendOtpRequestDto(email = email))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> mapNetworkAuthError(result.error)
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
                sessionRepository.clearSession()
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

    override suspend fun requestPasswordReset(email: String): EmptyResult<AuthError> {
        return when (val result = api.requestPasswordReset(PasswordResetRequestDto(email = email))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(AuthError.NetworkError(result.error))
        }
    }

    private fun mapNetworkAuthError(error: DataError.Network): EmptyResult<AuthError> {
        return when (error) {
            DataError.Network.TOO_MANY_REQUESTS ->
                Result.Error(AuthError.OtpError("Demasiados intentos. Intente de nuevo más tarde."))
            else -> Result.Error(AuthError.NetworkError(error))
        }
    }
}
