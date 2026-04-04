package cu.uci.cisol.monitoreoambiental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidencias")
data class IncidenciaEntity(
    @PrimaryKey val id: String,
    val tematica: String,
    val descripcion: String,
    val imagenPath: String?,
    /** Foto de resolución, se rellena al marcar como RESUELTA. */
    val imagenResolucionPath: String?,
    val latitud: Double?,
    val longitud: Double?,
    val fechaHora: Long,
    val estado: String,
    /** true si ya fue sincronizada con el servidor. */
    val sincronizado: Boolean = false,
)
