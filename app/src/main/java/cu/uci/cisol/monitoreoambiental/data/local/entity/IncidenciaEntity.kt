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
    val themeColor: String? = null,
    val statusColor: String? = null,
    val criticality: String? = null,
    val criticalityColor: String? = null,
    val themeName: String? = null,
    val statusName: String? = null,
    val localityName: String? = null,
    val reporterName: String? = null,
    val assignedToName: String? = null,
    val address: String? = null,
    val assignedToId: String? = null,
    val updatedAt: String? = null,
    val language: String? = null,
    val isActive: Boolean = true,
)
