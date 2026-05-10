package cu.uci.cisol.monitoreoambiental.domain.model

data class Tematica(
    val id: String,
    val slug: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val language: String,
    val name: String,
    val description: String,
    val colorHex: String
)
