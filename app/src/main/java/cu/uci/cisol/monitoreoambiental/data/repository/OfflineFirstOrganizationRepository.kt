package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.data.local.db.OrganizationDao
import cu.uci.cisol.monitoreoambiental.data.local.entity.toOrganization
import cu.uci.cisol.monitoreoambiental.data.remote.ManagementApi
import cu.uci.cisol.monitoreoambiental.data.remote.dto.toOrganizationEntity
import cu.uci.cisol.monitoreoambiental.domain.model.Organization
import cu.uci.cisol.monitoreoambiental.domain.repository.OrganizationRepository
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstOrganizationRepository(
    private val organizationDao: OrganizationDao,
    private val managementApi: ManagementApi,
) : OrganizationRepository {

    override fun observeOrganizations(): Flow<List<Organization>> {
        return organizationDao.observeAllActive().map { entities ->
            entities.map { it.toOrganization() }
        }
    }

    override suspend fun syncOrganizations(): EmptyResult<DataError> {
        return when (val result = managementApi.getOrganizations()) {
            is Result.Success -> {
                val entities = result.data.results.map { it.toOrganizationEntity() }
                organizationDao.clearAll()
                organizationDao.insertAll(entities)
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
        }
    }
}
