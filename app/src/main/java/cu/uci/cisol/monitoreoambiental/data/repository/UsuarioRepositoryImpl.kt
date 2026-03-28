package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.data.local.db.UsuarioDao
import cu.uci.cisol.monitoreoambiental.data.local.entity.UsuarioEntity
import cu.uci.cisol.monitoreoambiental.domain.model.Usuario
import cu.uci.cisol.monitoreoambiental.domain.repository.UsuarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsuarioRepositoryImpl(private val usuarioDao: UsuarioDao) : UsuarioRepository {

    override suspend fun guardar(usuario: Usuario): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val entity =
                    UsuarioEntity(
                            id = usuario.id,
                            nombre = usuario.nombre,
                            apellidos = usuario.apellidos,
                            ci = usuario.ci,
                            provincia = usuario.provincia,
                            municipio = usuario.municipio,
                            telefono = usuario.telefono
                    )
            usuarioDao.guardar(entity)
        }
    }

    override suspend fun obtener(): Usuario? {
        return withContext(Dispatchers.IO) {
            usuarioDao.obtener()?.let { entity ->
                Usuario(
                        id = entity.id,
                        nombre = entity.nombre,
                        apellidos = entity.apellidos,
                        ci = entity.ci,
                        provincia = entity.provincia,
                        municipio = entity.municipio,
                        telefono = entity.telefono
                )
            }
        }
    }

    override suspend fun eliminar(): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { usuarioDao.eliminar() }
    }
}
