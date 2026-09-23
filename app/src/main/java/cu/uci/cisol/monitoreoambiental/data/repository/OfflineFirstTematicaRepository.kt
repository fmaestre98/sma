package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.data.local.db.TematicaDao
import cu.uci.cisol.monitoreoambiental.data.local.entity.toTematica
import cu.uci.cisol.monitoreoambiental.data.remote.ManagementApi
import cu.uci.cisol.monitoreoambiental.data.remote.dto.toTematicaEntity
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
import cu.uci.cisol.monitoreoambiental.domain.repository.TematicaRepository
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstTematicaRepository(
    private val tematicaDao: TematicaDao,
    private val managementApi: ManagementApi
) : TematicaRepository {

    override fun observeTematicas(): Flow<List<Tematica>> {
        return tematicaDao.observeAllActive().map { entities -> 
            entities.map { it.toTematica() } 
        }
    }

    override suspend fun syncTematicas(): EmptyResult<DataError> {
        val result = managementApi.getTematicas()
        return when (result) {
            is Result.Success -> {
                val entities = result.data.results.map { it.toTematicaEntity() }
                tematicaDao.clearAll()
                tematicaDao.insertAll(entities)
                Result.Success(Unit)
            }
            is Result.Error -> {
                Result.Error(result.error)
            }
        }
    }
}
