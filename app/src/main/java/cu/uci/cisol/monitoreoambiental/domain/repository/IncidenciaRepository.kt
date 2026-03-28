package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import kotlinx.coroutines.flow.Flow

interface IncidenciaRepository {
    suspend fun guardar(incidencia: Incidencia): Result<Unit>
    fun observarTodas(): Flow<List<Incidencia>>
    suspend fun obtenerPendientes(): List<Incidencia>
    suspend fun marcarComoEnviada(id: String): Result<Unit>
    suspend fun marcarComoError(id: String): Result<Unit>
    suspend fun enviarAlServidor(incidencia: Incidencia): Result<Unit>
    suspend fun obtenerPorId(id: String): Incidencia?
}
