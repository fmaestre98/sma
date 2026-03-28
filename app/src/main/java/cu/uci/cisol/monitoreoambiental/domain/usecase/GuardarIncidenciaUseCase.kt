package cu.uci.cisol.monitoreoambiental.domain.usecase

import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository

class GuardarIncidenciaUseCase(private val repository: IncidenciaRepository) {
    suspend operator fun invoke(incidencia: Incidencia): Result<Unit> =
        repository.guardar(incidencia)
}
