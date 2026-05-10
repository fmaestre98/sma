package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow

interface EstadoRepository {
    fun observeEstados(): Flow<List<EstadoIncidencia>>
    suspend fun syncEstados(): EmptyResult<DataError>
}
