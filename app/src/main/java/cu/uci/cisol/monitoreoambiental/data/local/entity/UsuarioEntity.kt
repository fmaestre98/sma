package cu.uci.cisol.monitoreoambiental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val apellidos: String,
    val ci: String,
    val telefono: String
)
