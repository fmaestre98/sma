package cu.uci.cisol.monitoreoambiental.domain.usecase

import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository

class ActualizarEstadoIncidenciaUseCase(private val repository: IncidenciaRepository) {
    suspend operator fun invoke(
        id: String,
        estado: EstadoIncidencia,
        imagenResolucionPath: String? = null
    ): Result<Unit> {
        return repository.actualizarEstado(id, estado, imagenResolucionPath)
    }
}
