package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.RegistrationRequest
import cu.uci.cisol.monitoreoambiental.domain.model.TokenResponse
import cu.uci.cisol.monitoreoambiental.domain.model.Usuario
import cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class MockAuthRepositoryImpl : AuthRepository {

    override suspend fun login(credentials: AuthCredentials): Result<TokenResponse> {
        // Simulate network delay
        delay(1000)

        return if (credentials.username == "admin" && credentials.contrasena == "admin123") {
            Result.success(
                    TokenResponse(
                            token = "mock-jwt-token-abcd-1234",
                            usuario =
                                    Usuario(
                                            id = "1",
                                            nombre = "Admin",
                                            apellidos = "Soporte",
                                            ci = "00010170001",
                                            telefono = "55555555"
                                    )
                    )
            )
        } else if (credentials.username == "user@test.com" && credentials.contrasena == "password"
        ) {
            Result.success(
                    TokenResponse(
                            token = "mock-jwt-token-user-5678",
                            usuario =
                                    Usuario(
                                            id = "2",
                                            nombre = "Juan",
                                            apellidos = "Pérez",
                                            ci = "90010170001",
                                            telefono = "54444444"
                                    )
                    )
            )
        } else {
            Result.failure(Exception("Credenciales inválidas (Mock)"))
        }
    }

    override suspend fun register(request: RegistrationRequest): Result<Unit> {
        delay(1000)
        return if (request.nombreUsuario.isNotBlank() && request.contrasena.length >= 6) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Datos de registro inválidos (Mock)"))
        }
    }
}
