package cu.uci.cisol.monitoreoambiental.domain.model

data class Usuario(
    val id: String,
    val email: String,
    val userName: String,
    val firstName: String?,
    val lastName: String?,
    val phone: String,
    val role: String,
    val avatarUrl: String?
)
