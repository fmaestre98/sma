package cu.uci.cisol.monitoreoambiental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val email: String,
    val userName: String,
    val firstName: String?,
    val lastName: String?,
    val phone: String,
    val role: String,
    val avatarUrl: String?
)
