package cu.uci.cisol.monitoreoambiental.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class TokenResponseDto(
    val access: String,
    val refresh: String,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("access_exp") val accessExp: String? = null,
    @SerialName("refresh_exp") val refreshExp: String? = null,
    @SerialName("inactivity_timeout") val inactivityTimeout: Int? = null,
    val detail: String? = null,
)

@Serializable
data class RefreshRequestDto(
    val refresh: String
)

@Serializable
data class LogoutRequestDto(
    val refresh: String
)

@Serializable
data class MessageResponseDto(
    val message: String? = null,
    val detail: String? = null,
)

@Serializable
data class ErrorDetailDto(
    val detail: String? = null,
    val code: String? = null,
    @SerialName("retry_after") val retryAfter: Int? = null,
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    @SerialName("user_name") val userName: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val phone: String? = null,
    val password: String,
    val password2: String,
)

@Serializable
data class OtpSentResponseDto(
    val detail: String? = null,
    val email: String? = null,
)

@Serializable
data class VerifyRegistrationRequestDto(
    val email: String,
    val otp: String,
)

@Serializable
data class ResendOtpRequestDto(
    val email: String,
)

@Serializable
data class SessionDto(
    @SerialName("session_id") val sessionId: String,
    @SerialName("ip_address") val ipAddress: String,
    @SerialName("active_time") val activeTime: String,
    @SerialName("user_agent") val userAgent: String
)

@Serializable
data class CurrentSessionDto(
    @SerialName("ip_address") val ipAddress: String,
    val text: String
)

@Serializable
data class MultipleSessionsConflictDto(
    val detail: String,
    val code: String,
    @SerialName("active_sessions") val activeSessions: List<SessionDto>,
    @SerialName("current_session") val currentSession: CurrentSessionDto
)

@Serializable
data class TerminateSessionsRequestDto(
    val email: String,
    val password: String,
    @SerialName("session_ids") val sessionIds: List<String>
)

@Serializable
data class PasswordResetRequestDto(
    val email: String
)
