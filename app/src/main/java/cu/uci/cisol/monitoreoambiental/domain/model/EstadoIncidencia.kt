package cu.uci.cisol.monitoreoambiental.domain.model

enum class EstadoIncidencia(val label: String) {
    PENDIENTE("Pendiente"),
    ENVIADA("Enviada"),
    ERROR("Error al enviar")
}
