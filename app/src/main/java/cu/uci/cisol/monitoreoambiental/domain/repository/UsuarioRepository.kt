package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.Usuario

import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    suspend fun guardar(usuario: Usuario): kotlin.Result<Unit>
    suspend fun obtener(): Usuario?
    fun obtenerFlow(): Flow<Usuario?>
    suspend fun eliminar(): kotlin.Result<Unit>
    
    suspend fun refreshProfile(): Result<Unit, DataError.Network>
    suspend fun updateProfile(
        userName: String,
        firstName: String?,
        lastName: String?,
        phone: String,
        ci: String?,
        passport: String?,
        avatarUri: String?,
    ): Result<Unit, ProfileError>
    suspend fun changePassword(currentPassword: String, newPassword: String, confirmation: String): Result<Unit, ProfileError>
}
