package cu.uci.cisol.monitoreoambiental.data.remote.dto

import cu.uci.cisol.monitoreoambiental.data.local.entity.TematicaEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TematicasResponseDto(
    val count: Int,
    val next: String?,
    val previous: String?,
    val temas: List<TematicaDto>
)

@Serializable
data class TematicaDto(
    val id: String,
    val slug: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val language: String,
    val name: String,
    val description: String? = null,
    @SerialName("color_hex") val colorHex: String
)

fun TematicaDto.toTematicaEntity(): TematicaEntity = TematicaEntity(
    id = id,
    slug = slug,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    language = language,
    name = name,
    description = description,
    colorHex = colorHex
)
