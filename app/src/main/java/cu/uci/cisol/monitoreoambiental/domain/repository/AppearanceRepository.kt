package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.model.SystemAppearance
import kotlinx.coroutines.flow.Flow

interface AppearanceRepository {
    fun observeAppearance(): Flow<SystemAppearance?>
    suspend fun syncAppearance()
}
