package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.*
import cu.uci.cisol.monitoreoambiental.data.local.entity.IncidenciaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidenciaDao {

    @Query("SELECT * FROM incidencias ORDER BY fechaHora DESC")
    fun observarTodas(): Flow<List<IncidenciaEntity>>

    @Query("SELECT * FROM incidencias WHERE estado = 'PENDIENTE' ORDER BY fechaHora ASC")
    suspend fun obtenerPendientes(): List<IncidenciaEntity>

    @Query("SELECT * FROM incidencias WHERE id = :id")
    suspend fun obtenerPorId(id: String): IncidenciaEntity?

    @Upsert
    suspend fun insertar(incidencia: IncidenciaEntity)

    @Query(
        """UPDATE incidencias
           SET estado = :estado,
               imagenResolucionPath = COALESCE(:imagenResolucionPath, imagenResolucionPath),
               sincronizado = :sincronizado
           WHERE id = :id"""
    )
    suspend fun actualizarEstado(
        id: String,
        estado: String,
        imagenResolucionPath: String?,
        sincronizado: Boolean,
    )

    @Query("DELETE FROM incidencias WHERE id = :id")
    suspend fun eliminar(id: String)
}
