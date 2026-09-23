package cu.uci.cisol.monitoreoambiental.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaFileDto(
    val id: String,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("human_size") val humanSize: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_public") val isPublic: Boolean = true,
)
