package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_appearance")
data class SystemAppearanceEntity(
    @PrimaryKey val id: Int = 1, // Only one record
    val logoUrl: String?,
    val backgroundImage: String?,
    val primaryColor: String,
    val secondaryColor: String,
    val updatedAt: String
)
