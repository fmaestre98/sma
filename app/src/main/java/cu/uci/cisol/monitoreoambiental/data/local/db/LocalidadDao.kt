package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cu.uci.cisol.monitoreoambiental.data.local.entity.LocalidadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalidadDao {
    @Query("SELECT * FROM localidades WHERE isActive = 1")
    fun observeAllActive(): Flow<List<LocalidadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(localidades: List<LocalidadEntity>)

    @Query("DELETE FROM localidades")
    suspend fun clearAll()

    @Query("SELECT * FROM localidades WHERE isActive = 1")
    suspend fun getAllActive(): List<LocalidadEntity>
}
