package cu.uci.cisol.monitoreoambiental.data.remote.dto

import cu.uci.cisol.monitoreoambiental.data.local.entity.LocalidadEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocalidadesResponseDto(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<LocalidadDto> = emptyList(),
)

@Serializable
data class LocalidadDto(
    val id: String,
    val slug: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val language: String,
    val name: String,
    val municipality: String,
)

fun LocalidadDto.toLocalidadEntity(): LocalidadEntity =
    LocalidadEntity(
        id = id,
        slug = slug,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        language = language,
        name = name,
        municipality = municipality,
    )
