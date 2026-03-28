package cu.uci.cisol.monitoreoambiental.domain.model

data class RegistrationRequest(
        val nombreUsuario: String,
        val contrasena: String,
        val nombreCompleto: String? = null,
        val correo: String? = null
)
