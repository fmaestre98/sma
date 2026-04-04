package cu.uci.cisol.monitoreoambiental.domain.model

enum class EstadoIncidencia(val label: String) {
    PENDIENTE("Pendiente"),
    NUEVA("Nueva"),
    ACEPTADA("Aceptada"),
    RESUELTA("Resuelta"),
    RECHAZADA("Rechazada")

}
