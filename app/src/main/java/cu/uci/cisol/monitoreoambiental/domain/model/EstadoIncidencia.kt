package cu.uci.cisol.monitoreoambiental.domain.model

enum class EstadoIncidencia(val label: String) {
    /** Guardada localmente, nunca enviada al servidor. */
    BORRADOR("Borrador"),
    /** Enviada al servidor, pendiente de revisión. */
    NUEVA("Nueva"),
    PENDIENTE("Pendiente"),
    ACEPTADA("Aceptada"),
    RESUELTA("Resuelta"),
    RECHAZADA("Rechazada")
}
