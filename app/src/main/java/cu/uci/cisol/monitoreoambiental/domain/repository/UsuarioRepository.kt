package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.Usuario

interface UsuarioRepository {
    suspend fun guardar(usuario: Usuario): Result<Unit>
    suspend fun obtener(): Usuario?
    suspend fun eliminar(): Result<Unit>
}
