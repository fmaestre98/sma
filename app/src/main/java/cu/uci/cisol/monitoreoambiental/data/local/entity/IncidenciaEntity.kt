package cu.uci.cisol.monitoreoambiental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidencias")
data class IncidenciaEntity(
    @PrimaryKey val id: String,
    val tematica: String,
    val tipo: String,
    val provincia: String,
    val municipio: String,
    val localidad: String,
    val descripcion: String,
    val imagenPath: String?,
    val latitud: Double?,
    val longitud: Double?,
    val fechaHora: Long,
    val estado: String,
    val incidenciaResueltaId: String?
)
