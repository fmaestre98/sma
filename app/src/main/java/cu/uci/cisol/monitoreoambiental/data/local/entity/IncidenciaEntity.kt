package cu.uci.cisol.monitoreoambiental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidencias")
data class IncidenciaEntity(
    @PrimaryKey val id: String,
    val tematica: String,
    val descripcion: String,
    val imagenesPaths: List<String> = emptyList(),
    /** Foto de resolución, se rellena al marcar como RESUELTA. */
    val imagenResolucionPath: String?,
    val latitud: Double?,
    val longitud: Double?,
    val fechaHora: Long,
    val estadoId: String,
    val esBorrador: Boolean,
    /** true si ya fue sincronizada con el servidor. */
    val sincronizado: Boolean = false,
)
