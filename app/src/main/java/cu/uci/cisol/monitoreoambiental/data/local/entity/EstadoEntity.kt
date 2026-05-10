package cu.uci.cisol.monitoreoambiental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia

@Entity(tableName = "estados_incidencia")
data class EstadoEntity(
    @PrimaryKey val id: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val language: String,
    val name: String,
    val description: String?,
    val colorHex: String,
    val weight: Int,
    val isFinal: Boolean,
    val isBase: Boolean
)

fun EstadoEntity.toEstadoIncidencia(): EstadoIncidencia = EstadoIncidencia(
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
