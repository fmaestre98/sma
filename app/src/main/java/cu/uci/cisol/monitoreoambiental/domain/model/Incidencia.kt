package cu.uci.cisol.monitoreoambiental.domain.model

data class Incidencia(
    /** ID local generado en la app. */
    val id: String,
    /** ID asignado por el backend tras POST o sync. */
    val serverId: String? = null,
    val tematicaId: String,
    val localidadId: String? = null,
    val descripcion: String,
    /** Rutas de las fotos principales de la incidencia (máx. 5). */
    val imagenesPaths: List<String> = emptyList(),
    /** Ruta de la foto de resolución (se agrega al marcar como RESUELTA). */
    val imagenResolucionPath: String?,
    val latitud: Double?,
    val longitud: Double?,
    val fechaHora: Long,
    val estadoId: String,
    val esBorrador: Boolean,
    /** true si ya fue sincronizada con el servidor. */
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
