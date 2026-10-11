package cu.uci.cisol.monitoreoambiental.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicSystemConfigDto(
    @SerialName("registration_enabled") val allowRegistration: Boolean,
)
