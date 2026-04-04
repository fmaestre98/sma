package cu.uci.cisol.monitoreoambiental.domain.model

data class Incidencia(
    val id: String,
    val tematica: Tematica,
    val descripcion: String,
    /** Ruta de la foto principal de la incidencia. */
    val imagenPath: String?,
    /** Ruta de la foto de resolución (se agrega al marcar como RESUELTA). */
    val imagenResolucionPath: String?,
    val latitud: Double?,
    val longitud: Double?,
    val fechaHora: Long,              // epoch millis
    val estado: EstadoIncidencia,
    /** true si ya fue sincronizada con el servidor. */
    val sincronizado: Boolean = false,
)
