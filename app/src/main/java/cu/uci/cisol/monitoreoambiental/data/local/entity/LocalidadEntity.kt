package cu.uci.cisol.monitoreoambiental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cu.uci.cisol.monitoreoambiental.domain.model.Localidad

@Entity(tableName = "localidades")
data class LocalidadEntity(
    @PrimaryKey val id: String,
    val slug: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val language: String,
    val name: String,
    val municipality: String,
)

fun LocalidadEntity.toLocalidad(): Localidad =
    Localidad(
        id = id,
        slug = slug,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        language = language,
        name = name,
        municipality = municipality,
    )
