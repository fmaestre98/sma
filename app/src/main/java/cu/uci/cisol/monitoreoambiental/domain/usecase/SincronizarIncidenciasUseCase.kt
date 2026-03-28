package cu.uci.cisol.monitoreoambiental.domain.usecase

import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository

class SincronizarIncidenciasUseCase(private val repository: IncidenciaRepository) {
    suspend operator fun invoke(): Result<Int> = runCatching {
        val pendientes = repository.obtenerPendientes()
        var enviadas = 0
        for (incidencia in pendientes) {
            repository.enviarAlServidor(incidencia)
                .onSuccess {
                    repository.marcarComoEnviada(incidencia.id)
                    enviadas++
                }
                .onFailure {
                    repository.marcarComoError(incidencia.id)
                }
        }
        enviadas
    }
}
