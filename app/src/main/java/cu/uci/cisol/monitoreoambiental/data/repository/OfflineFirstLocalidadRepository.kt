package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.data.local.db.LocalidadDao
import cu.uci.cisol.monitoreoambiental.data.local.entity.toLocalidad
import cu.uci.cisol.monitoreoambiental.data.remote.LocationsApi
import cu.uci.cisol.monitoreoambiental.data.remote.dto.toLocalidadEntity
import cu.uci.cisol.monitoreoambiental.domain.model.Localidad
import cu.uci.cisol.monitoreoambiental.domain.repository.LocalidadRepository
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstLocalidadRepository(
    private val localidadDao: LocalidadDao,
    private val locationsApi: LocationsApi,
) : LocalidadRepository {

    override fun observeLocalidades(): Flow<List<Localidad>> {
        return localidadDao.observeAllActive().map { entities ->
            entities.map { it.toLocalidad() }
        }
    }

    override suspend fun syncLocalities(): EmptyResult<DataError> {
        return when (val result = locationsApi.getLocalities()) {
            is Result.Success -> {
                val entities = result.data.results.map { it.toLocalidadEntity() }
                localidadDao.clearAll()
                localidadDao.insertAll(entities)
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
        }
    }
}
