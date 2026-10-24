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
    @SerialName("avatar_url") val avatarUrl: String?,
    val country: String? = null,
    val ci: String? = null,
    val passport: String? = null,
)

@Serializable
data class UserResponseDto(
    val user: UserDto
)

@Serializable
data class UpdateUserRequestDto(
    @SerialName("user_name") val userName: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val phone: String? = null,
    val ci: String? = null,
    val passport: String? = null,
)

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
    @SerialName("confirm_password") val confirmPassword: String,
)
