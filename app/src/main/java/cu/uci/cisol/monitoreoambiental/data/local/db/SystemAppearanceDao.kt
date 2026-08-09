package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemAppearanceDao {
    @Query("SELECT * FROM system_appearance WHERE id = 1")
    fun observeAppearance(): Flow<SystemAppearanceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appearance: SystemAppearanceEntity)
}
