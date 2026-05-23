package cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IncidenciasListResponseDto(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    @SerialName("incidencias") val results: List<IncidenciaListItemDto>,
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
    @SerialName("theme_color") val themeColor: String? = null,
    @SerialName("reporter_name") val reporterName: String? = null,
    @SerialName("assigned_to_name") val assignedToName: String? = null,
    val status: String,
    @SerialName("status_color") val statusColor: String? = null,
    val criticality: String? = null,
    @SerialName("criticality_color") val criticalityColor: String? = null,
    val locality: String,
    @SerialName("ocurred_at") val ocurredAt: String,
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
    val locality: String,
    val description: String,
    val address: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("ocurred_at") val ocurredAt: String,
    @SerialName("assigned_to") val assignedTo: String? = null,
)
