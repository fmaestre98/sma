package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.*
import cu.uci.cisol.monitoreoambiental.data.local.entity.IncidenciaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidenciaDao {

    @Query("SELECT * FROM incidencias ORDER BY fechaHora DESC")
    fun observarTodas(): Flow<List<IncidenciaEntity>>

    @Query("SELECT * FROM incidencias WHERE estado = 'PENDIENTE' ORDER BY fechaHora ASC")
    fun obtenerPendientes(): List<IncidenciaEntity>

    @Query("SELECT * FROM incidencias WHERE id = :id")
    fun obtenerPorId(id: String): IncidenciaEntity?

    @Upsert fun insertar(incidencia: IncidenciaEntity)

    @Query("UPDATE incidencias SET estado = :estado WHERE id = :id")
    fun actualizarEstado(id: String, estado: String)

    @Query("DELETE FROM incidencias WHERE id = :id") fun eliminar(id: String)
}
