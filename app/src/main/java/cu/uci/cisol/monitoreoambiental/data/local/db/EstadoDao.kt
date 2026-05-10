package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cu.uci.cisol.monitoreoambiental.data.local.entity.EstadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EstadoDao {
    @Query("SELECT * FROM estados_incidencia WHERE isActive = 1 ORDER BY weight ASC")
    fun observeAllActive(): Flow<List<EstadoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(estados: List<EstadoEntity>)

    @Query("DELETE FROM estados_incidencia")
    suspend fun clearAll()
}
