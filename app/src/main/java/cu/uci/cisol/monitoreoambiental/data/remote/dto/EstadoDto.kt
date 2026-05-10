package cu.uci.cisol.monitoreoambiental.data.remote.dto

import cu.uci.cisol.monitoreoambiental.data.local.entity.EstadoEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EstadosResponseDto(
    val count: Int,
    val next: String?,
    val previous: String?,
    @SerialName("estados_de_incidencia") val estados: List<EstadoDto>
)

@Serializable
data class EstadoDto(
    val id: String,
    val slug: String?,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val language: String,
    val name: String,
    val description: String?,
    @SerialName("color_hex") val colorHex: String,
    val weight: Int,
    @SerialName("is_final") val isFinal: Boolean,
    @SerialName("is_base") val isBase: Boolean
)

fun EstadoDto.toEstadoEntity(): EstadoEntity = EstadoEntity(
    id = id,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    language = language,
    name = name,
    description = description,
    colorHex = colorHex,
    weight = weight,
    isFinal = isFinal,
    isBase = isBase
)
