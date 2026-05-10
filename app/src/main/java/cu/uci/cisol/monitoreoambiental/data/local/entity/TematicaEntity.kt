package cu.uci.cisol.monitoreoambiental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica

@Entity(tableName = "tematicas")
data class TematicaEntity(
    @PrimaryKey val id: String,
    val slug: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val language: String,
    val name: String,
    val description: String,
    val colorHex: String
)

fun TematicaEntity.toTematica(): Tematica = Tematica(
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
