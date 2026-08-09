package cu.uci.cisol.monitoreoambiental.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    @SerialName("user_name") val userName: String,
    @SerialName("first_name") val firstName: String?,
    @SerialName("last_name") val lastName: String?,
    val phone: String,
    val role: String,
    @SerialName("avatar_url") val avatarUrl: String?
)

@Serializable
data class UserResponseDto(
    val user: UserDto
)

@Serializable
data class UpdateUserRequestDto(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val phone: String? = null,
    val email: String? = null
)
