package cu.uci.cisol.monitoreoambiental.data.remote.dto

import cu.uci.cisol.monitoreoambiental.data.local.entity.OrganizationEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrganizationsResponseDto(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<OrganizationDto> = emptyList(),
)

@Serializable
data class OrganizationDto(
    val id: String,
    val slug: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val language: String? = null,
    val name: String,
    val description: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("incidences_count") val incidencesCount: Int = 0,
)

fun OrganizationDto.toOrganizationEntity(): OrganizationEntity = OrganizationEntity(
    id = id,
    slug = slug,
    isActive = isActive,
    name = name,
    description = description,
    email = email,
    phone = phone,
    imageUrl = logoUrl,
    incidencesCount = incidencesCount,
)
