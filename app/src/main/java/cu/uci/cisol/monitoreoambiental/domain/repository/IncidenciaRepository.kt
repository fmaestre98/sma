package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import kotlinx.coroutines.flow.Flow

interface IncidenciaRepository {
    suspend fun guardar(incidencia: Incidencia): Result<Unit>
    fun observarTodas(): Flow<List<Incidencia>>
    suspend fun obtenerPendientes(): List<Incidencia>
    suspend fun marcarComoEnviada(id: String, estadoId: String): Result<Unit>
    suspend fun enviarAlServidor(incidencia: Incidencia, defaultEstadoId: String): Result<Unit>
    suspend fun obtenerPorId(id: String): Incidencia?
    /**
     * Actualiza el estado de una incidencia.
     * @param imagenResolucionPath ruta de la foto de resolución (solo al marcar como RESUELTA).
     */
    suspend fun actualizarEstado(
        id: String,
        estadoId: String,
        esBorrador: Boolean,
        imagenResolucionPath: String? = null,
    ): Result<Unit>
}
