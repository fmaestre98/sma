package cu.uci.cisol.monitoreoambiental.domain.model

data class Incidencia(
    val id: String,
    val tematica: Tematica,
    val descripcion: String,
    val imagenPath: String?,
    val latitud: Double?,
    val longitud: Double?,
    val fechaHora: Long,              // epoch millis
    val estado: EstadoIncidencia,
)
