package cu.uci.cisol.monitoreoambiental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidencias")
data class IncidenciaEntity(
    @PrimaryKey val id: String,
    val serverId: String? = null,
    val tematica: String,
    val localidadId: String? = null,
    val descripcion: String,
    val imagenesPaths: List<String> = emptyList(),
    val imagenResolucionPath: String?,
    val latitud: Double?,
    val longitud: Double?,
    val fechaHora: Long,
    val estadoId: String,
    val esBorrador: Boolean,
    val sincronizado: Boolean = false,
    val ticketNumber: Int? = null,
)
