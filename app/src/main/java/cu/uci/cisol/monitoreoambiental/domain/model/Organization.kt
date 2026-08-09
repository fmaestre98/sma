package cu.uci.cisol.monitoreoambiental.domain.model

data class Organization(
    val id: String,
    val slug: String?,
    val isActive: Boolean,
    val name: String,
    val description: String?,
    val email: String?,
    val phone: String?,
    val imageUrl: String?,
    val incidencesCount: Int,
)
