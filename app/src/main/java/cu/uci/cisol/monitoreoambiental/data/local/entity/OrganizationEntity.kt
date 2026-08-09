package cu.uci.cisol.monitoreoambiental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cu.uci.cisol.monitoreoambiental.domain.model.Organization

@Entity(tableName = "organizations")
data class OrganizationEntity(
    @PrimaryKey val id: String,
    val slug: String?,
    val isActive: Boolean,
    val name: String,
    val description: String?,
    val email: String?,
    val phone: String?,
    val imageUrl: String?,
    val incidencesCount: Int,
)

fun OrganizationEntity.toOrganization(): Organization = Organization(
    id = id,
    slug = slug,
    isActive = isActive,
    name = name,
    description = description,
    email = email,
    phone = phone,
    imageUrl = imageUrl,
    incidencesCount = incidencesCount,
)
