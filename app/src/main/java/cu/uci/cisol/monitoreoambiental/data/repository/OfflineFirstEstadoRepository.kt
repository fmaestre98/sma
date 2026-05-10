package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.data.local.db.EstadoDao
import cu.uci.cisol.monitoreoambiental.data.local.entity.toEstadoIncidencia
import cu.uci.cisol.monitoreoambiental.data.remote.ManagementApi
import cu.uci.cisol.monitoreoambiental.data.remote.dto.toEstadoEntity
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.repository.EstadoRepository
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstEstadoRepository(
    private val estadoDao: EstadoDao,
    private val managementApi: ManagementApi
) : EstadoRepository {

    override fun observeEstados(): Flow<List<EstadoIncidencia>> {
        return estadoDao.observeAllActive().map { entities -> 
            entities.map { it.toEstadoIncidencia() } 
        }
    }

    override suspend fun syncEstados(): EmptyResult<DataError> {
        val result = managementApi.getEstados()
        return when (result) {
            is Result.Success -> {
                val entities = result.data.estados.map { it.toEstadoEntity() }
                estadoDao.clearAll()
                estadoDao.insertAll(entities)
                Result.Success(Unit)
            }
            is Result.Error -> {
                Result.Error(result.error)
            }
        }
    }
}
