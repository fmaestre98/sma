package cu.uci.cisol.monitoreoambiental.domain.model

data class EstadoIncidencia(
    val id: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val language: String,
    val name: String,
    val description: String?,
    val colorHex: String,
    val weight: Int,
    val isFinal: Boolean,
    val isBase: Boolean
)
