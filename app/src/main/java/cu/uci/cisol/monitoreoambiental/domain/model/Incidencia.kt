package cu.uci.cisol.monitoreoambiental.domain.model

data class Incidencia(
    val id: String,
    val tematicaId: String,
    val descripcion: String,
    /** Rutas de las fotos principales de la incidencia (máx. 5). */
    val imagenesPaths: List<String> = emptyList(),
    /** Ruta de la foto de resolución (se agrega al marcar como RESUELTA). */
    val imagenResolucionPath: String?,
    val latitud: Double?,
    val longitud: Double?,
    val fechaHora: Long,              // epoch millis
    val estadoId: String,
    val esBorrador: Boolean,
    /** true si ya fue sincronizada con el servidor. */
    val sincronizado: Boolean = false,
)
