package cu.uci.cisol.monitoreoambiental.domain.model

data class RegistrationRequest(
        val nombreUsuario: String,
        val contrasena: String,
        val confirmacionContrasena: String,
        val nombre: String = "",
        val apellidos: String = "",
        val correo: String,
        val pais: String,
        val ci: String? = null,
        val pasaporte: String? = null,
        val telefono: String? = null,
)
