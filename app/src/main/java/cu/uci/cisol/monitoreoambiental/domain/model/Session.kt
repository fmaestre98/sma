package cu.uci.cisol.monitoreoambiental.domain.model

data class Session(
    val sessionId: String,
    val ipAddress: String,
    val activeTime: String,
    val userAgent: String
)
