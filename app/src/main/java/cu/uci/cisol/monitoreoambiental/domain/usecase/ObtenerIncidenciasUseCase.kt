package cu.uci.cisol.monitoreoambiental.domain.usecase

import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import kotlinx.coroutines.flow.Flow

class ObtenerIncidenciasUseCase(private val repository: IncidenciaRepository) {
    operator fun invoke(): Flow<List<Incidencia>> = repository.observarTodas()
}
