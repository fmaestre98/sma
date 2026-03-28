package cu.uci.cisol.monitoreoambiental.domain.model

data class Incidencia(
    val id: String,
    val tematica: Tematica,
    val tipo: TipoIncidencia,
    val provincia: String,
    val municipio: String,
    val localidad: String,
    val descripcion: String,
    val imagenPath: String?,
    val latitud: Double?,
    val longitud: Double?,
    val fechaHora: Long,              // epoch millis
    val estado: EstadoIncidencia,
    val incidenciaResueltaId: String? // para tipo SOLUCION, referencia a la incidencia que resuelve
)
