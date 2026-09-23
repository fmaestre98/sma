package cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IncidenciasListResponseDto(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<IncidenciaListItemDto> = emptyList(),
)

@Serializable
data class IncidenciaListItemDto(
    val id: String,
    val slug: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val language: String? = null,
    @SerialName("ticket_number") val ticketNumber: Int? = null,
    val theme: String,
    @SerialName("theme_name") val themeName: String? = null,
    @SerialName("theme_color") val themeColor: String? = null,
    @SerialName("reporter_name") val reporterName: String? = null,
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("assigned_to_name") val assignedToName: String? = null,
    val status: String,
    @SerialName("status_name") val statusName: String? = null,
    @SerialName("status_color") val statusColor: String? = null,
    val criticality: String? = null,
    @SerialName("criticality_name") val criticalityName: String? = null,
    @SerialName("criticality_color") val criticalityColor: String? = null,
    val locality: String? = null,
    @SerialName("locality_name") val localityName: String? = null,
    @SerialName("ocurred_at") val ocurredAt: String,
    val description: String? = null,
    val address: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
)

@Serializable
data class EvidenceDto(
    val id: String,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("size_mb") val sizeMb: Double? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
)

@Serializable
data class CreateIncidenciaResponseDto(
    val id: String,
    val slug: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val language: String? = null,
    val theme: String,
    @SerialName("theme_name") val themeName: String? = null,
    val locality: String? = null,
    @SerialName("locality_name") val localityName: String? = null,
    val description: String,
    val address: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("ocurred_at") val ocurredAt: String,
    @SerialName("assigned_to") val assignedTo: String? = null,
    val status: String? = null,
    @SerialName("status_name") val statusName: String? = null,
    @SerialName("status_color") val statusColor: String? = null,
    @SerialName("ticket_number") val ticketNumber: Int? = null,
    val evidences: List<EvidenceDto> = emptyList(),
)

@Serializable
data class CreateIncidenciaRequestDto(
    val theme: String,
    val locality: String,
    val description: String,
    val address: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("ocurred_at") val ocurredAt: String,
    val evidences: List<String> = emptyList(),
    @SerialName("evidences_public") val evidencesPublic: Boolean = false,
    @SerialName("assigned_to") val assignedTo: String? = null,
)

@Serializable
data class UpdateIncidenciaRequestDto(
    @SerialName("is_active") val isActive: Boolean,
    val language: String,
    val theme: String,
    val locality: String,
    val description: String,
    val address: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("ocurred_at") val ocurredAt: String,
    val evidences: List<String> = emptyList(),
    @SerialName("evidences_public") val evidencesPublic: Boolean = false,
    @SerialName("assigned_to") val assignedTo: String? = null,
)
