package cu.uci.cisol.monitoreoambiental.domain.usecase

import cu.uci.cisol.monitoreoambiental.domain.model.Usuario
import cu.uci.cisol.monitoreoambiental.domain.repository.UsuarioRepository

class GuardarUsuarioUseCase(private val repository: UsuarioRepository) {
    suspend operator fun invoke(usuario: Usuario): Result<Unit> =
        repository.guardar(usuario)
}

class ObtenerUsuarioUseCase(private val repository: UsuarioRepository) {
    suspend operator fun invoke(): Usuario? = repository.obtener()
}

class EliminarUsuarioUseCase(private val repository: UsuarioRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.eliminar()
}
