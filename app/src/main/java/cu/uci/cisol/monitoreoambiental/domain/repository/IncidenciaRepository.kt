package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import kotlinx.coroutines.flow.Flow

interface IncidenciaRepository {
    suspend fun guardar(incidencia: Incidencia): Result<Unit>
    fun observarTodas(): Flow<List<Incidencia>>
    suspend fun obtenerPendientes(): List<Incidencia>
    suspend fun marcarComoEnviada(id: String, estadoId: String): Result<Unit>
    suspend fun enviarAlServidor(incidencia: Incidencia): Result<Unit>
    suspend fun syncDesdeServidor(
        page: Int? = null,
        pageSize: Int? = null,
        search: String? = null,
        ordering: String? = "-updated_at"
    ): Result<Int>
    suspend fun obtenerPorId(id: String): Incidencia?
    suspend fun sincronizarDetalle(id: String): Result<Unit>
    suspend fun actualizarEstado(
        id: String,
        estadoId: String,
        esBorrador: Boolean,
        imagenResolucionPath: String? = null,
    ): Result<Unit>
    fun observarPaginado(filtro: String, search: String, limit: Int, offset: Int): Flow<List<Incidencia>>
    fun obtenerCantidad(filtro: String, search: String): Flow<Int>
    suspend fun actualizarRemoto(
        id: String,
        theme: String,
        locality: String,
        description: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        occurredAt: Long,
        evidences: List<String>,
        assignedTo: String?,
        isActive: Boolean,
        language: String
    ): Result<Unit>
}
