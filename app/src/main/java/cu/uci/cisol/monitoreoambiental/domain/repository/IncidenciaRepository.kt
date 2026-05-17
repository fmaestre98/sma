package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import kotlinx.coroutines.flow.Flow

interface IncidenciaRepository {
    suspend fun guardar(incidencia: Incidencia): Result<Unit>
    fun observarTodas(): Flow<List<Incidencia>>
    suspend fun obtenerPendientes(): List<Incidencia>
    suspend fun marcarComoEnviada(id: String, estadoId: String): Result<Unit>
    suspend fun enviarAlServidor(incidencia: Incidencia): Result<Unit>
    suspend fun syncDesdeServidor(): Result<Unit>
    suspend fun obtenerPorId(id: String): Incidencia?
    suspend fun actualizarEstado(
        id: String,
        estadoId: String,
        esBorrador: Boolean,
        imagenResolucionPath: String? = null,
    ): Result<Unit>
}
