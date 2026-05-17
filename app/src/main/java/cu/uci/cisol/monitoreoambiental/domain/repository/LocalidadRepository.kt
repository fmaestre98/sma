package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.Localidad
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow

interface LocalidadRepository {
    fun observeLocalidades(): Flow<List<Localidad>>
    suspend fun syncLocalities(): EmptyResult<DataError>
}
