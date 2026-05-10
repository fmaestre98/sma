package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow

interface TematicaRepository {
    fun observeTematicas(): Flow<List<Tematica>>
    suspend fun syncTematicas(): EmptyResult<DataError>
}
