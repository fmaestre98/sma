package cu.uci.cisol.monitoreoambiental.domain.usecase

import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository

class ObtenerIncidenciasPendientesUseCase(private val repository: IncidenciaRepository) {
    suspend operator fun invoke(): List<Incidencia> = repository.obtenerPendientes()
}
