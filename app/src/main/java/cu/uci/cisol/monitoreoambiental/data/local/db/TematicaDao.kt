package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cu.uci.cisol.monitoreoambiental.data.local.entity.TematicaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TematicaDao {
    @Query("SELECT * FROM tematicas WHERE isActive = 1")
    fun observeAllActive(): Flow<List<TematicaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tematicas: List<TematicaEntity>)

    @Query("DELETE FROM tematicas")
    suspend fun clearAll()
}
