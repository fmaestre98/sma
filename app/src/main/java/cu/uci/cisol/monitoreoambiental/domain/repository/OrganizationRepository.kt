package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.Organization
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow

interface OrganizationRepository {
    fun observeOrganizations(): Flow<List<Organization>>
    suspend fun syncOrganizations(): EmptyResult<DataError>
}
