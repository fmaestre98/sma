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
        firstName: String?,
        lastName: String?,
        phone: String,
        email: String,
        avatarUri: String? // Assuming we handle URI in the repository or viewmodel
    ): Result<Unit, DataError.Network>
}
