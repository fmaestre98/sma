package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.data.remote.dto.*
import cu.uci.cisol.monitoreoambiental.data.util.postSafe
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.http.ContentType

class AuthApi(private val client: HttpClient) {

    companion object {
        val BASE_URL: String get() = ApiConfig.AUTH_BASE
        val USERS_BASE_URL: String get() = "${ApiConfig.API_V1_BASE}/users"
    }

    suspend fun login(request: LoginRequestDto): io.ktor.client.statement.HttpResponse {
        return client.post("$BASE_URL/token/") {
            setBody(request)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun register(request: RegisterRequestDto): Result<OtpSentResponseDto, DataError.Network> {
        return client.postSafe("$BASE_URL/register/", request)
    }

    suspend fun verifyRegistration(request: VerifyRegistrationRequestDto): Result<TokenResponseDto, DataError.Network> {
        return client.postSafe("$BASE_URL/verify-registration/", request)
    }

    suspend fun resendOtp(request: ResendOtpRequestDto): Result<OtpSentResponseDto, DataError.Network> {
        return client.postSafe("$BASE_URL/resend-otp/", request)
    }

    suspend fun logout(request: LogoutRequestDto): Result<MessageResponseDto, DataError.Network> {
        return client.postSafe("$BASE_URL/logout/", request)
    }

    suspend fun terminateSessions(request: TerminateSessionsRequestDto): Result<MessageResponseDto, DataError.Network> {
        return client.postSafe("$BASE_URL/token/terminate-sessions/", request)
    }

    suspend fun requestPasswordReset(request: PasswordResetRequestDto): Result<MessageResponseDto, DataError.Network> {
        return client.postSafe("$USERS_BASE_URL/request-password-reset/", request)
    }
}
