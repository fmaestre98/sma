package cu.uci.cisol.monitoreoambiental.domain.model

data class Usuario(
    val id: String,
    val nombre: String,
    val apellidos: String,
    val ci: String,        // Carnet de Identidad
    val provincia: String,
    val municipio: String,
    val telefono: String
)
