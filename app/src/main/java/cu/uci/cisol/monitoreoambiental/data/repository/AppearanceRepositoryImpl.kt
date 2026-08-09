package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.data.local.db.SystemAppearanceDao
import cu.uci.cisol.monitoreoambiental.data.local.db.SystemAppearanceEntity
import cu.uci.cisol.monitoreoambiental.data.remote.ManagementApi
import cu.uci.cisol.monitoreoambiental.domain.model.SystemAppearance
import cu.uci.cisol.monitoreoambiental.domain.repository.AppearanceRepository
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppearanceRepositoryImpl(
    private val api: ManagementApi,
    private val dao: SystemAppearanceDao
) : AppearanceRepository {

    override fun observeAppearance(): Flow<SystemAppearance?> {
        return dao.observeAppearance().map { entity ->
            entity?.let {
                SystemAppearance(
                    logoUrl = it.logoUrl,
                    backgroundImage = it.backgroundImage,
                    primaryColor = it.primaryColor,
                    secondaryColor = it.secondaryColor,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    override suspend fun syncAppearance() {
        when (val result = api.getAppearance()) {
            is Result.Success -> {
                val dto = result.data
                dao.insert(
                    SystemAppearanceEntity(
                        id = 1,
                        logoUrl = dto.logoUrl ?: dto.logo,
                        backgroundImage = dto.backgroundImage,
                        primaryColor = dto.primaryColor,
                        secondaryColor = dto.secondaryColor,
                        updatedAt = dto.updatedAt
                    )
                )
            }
            is Result.Error -> {
                // Ignore error, use cached data
            }
        }
    }
}
