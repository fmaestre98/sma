package cu.uci.cisol.monitoreoambiental.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SystemAppearanceDto(
    val logo: String?,
    @SerialName("logo_url") val logoUrl: String?,
    @SerialName("background_image") val backgroundImage: String?,
    @SerialName("primary_color") val primaryColor: String,
    @SerialName("secondary_color") val secondaryColor: String,
    @SerialName("updated_at") val updatedAt: String
)
