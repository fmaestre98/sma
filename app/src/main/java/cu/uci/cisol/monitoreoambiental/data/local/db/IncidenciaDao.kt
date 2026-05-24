package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cu.uci.cisol.monitoreoambiental.data.local.entity.IncidenciaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidenciaDao {

    @Query("SELECT * FROM incidencias ORDER BY fechaHora DESC")
    fun observarTodas(): Flow<List<IncidenciaEntity>>

    @Query("SELECT * FROM incidencias WHERE sincronizado = 0 AND esBorrador = 0 ORDER BY fechaHora ASC")
    suspend fun obtenerPendientes(): List<IncidenciaEntity>

    @Query("SELECT * FROM incidencias WHERE id = :id")
    suspend fun obtenerPorId(id: String): IncidenciaEntity?

    @Query("SELECT * FROM incidencias WHERE serverId = :serverId LIMIT 1")
    suspend fun obtenerPorServerId(serverId: String): IncidenciaEntity?

    @Upsert
    suspend fun insertar(incidencia: IncidenciaEntity)

    @Query(
        """UPDATE incidencias
           SET estadoId = :estadoId,
               esBorrador = :esBorrador,
               imagenResolucionPath = COALESCE(:imagenResolucionPath, imagenResolucionPath),
               sincronizado = :sincronizado
           WHERE id = :id""",
    )
    suspend fun actualizarEstado(
        id: String,
        estadoId: String,
        esBorrador: Boolean,
        imagenResolucionPath: String?,
        sincronizado: Boolean,
    )

    @Query(
        """UPDATE incidencias
           SET serverId = :serverId,
               sincronizado = :sincronizado,
               esBorrador = :esBorrador,
               estadoId = :estadoId,
               ticketNumber = :ticketNumber
           WHERE id = :id""",
    )
    suspend fun actualizarTrasEnvio(
        id: String,
        serverId: String,
        sincronizado: Boolean,
        esBorrador: Boolean,
        estadoId: String,
        ticketNumber: Int?,
    )

    @Query("DELETE FROM incidencias WHERE id = :id")
    suspend fun eliminar(id: String)

    @Query("""
        SELECT * FROM incidencias 
        WHERE (:filtro = 'TODAS' 
            OR (:filtro = 'BORRADORES' AND esBorrador = 1) 
            OR (:filtro = 'ENVIADAS' AND esBorrador = 0 AND sincronizado = 1))
          AND (descripcion LIKE '%' || :search || '%' 
            OR tematica LIKE '%' || :search || '%' 
            OR themeName LIKE '%' || :search || '%'
            OR :search = '')
        ORDER BY fechaHora DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun obtenerPaginado(filtro: String, search: String, limit: Int, offset: Int): Flow<List<IncidenciaEntity>>

    @Query("""
        SELECT COUNT(*) FROM incidencias 
        WHERE (:filtro = 'TODAS' 
            OR (:filtro = 'BORRADORES' AND esBorrador = 1) 
            OR (:filtro = 'ENVIADAS' AND esBorrador = 0 AND sincronizado = 1))
          AND (descripcion LIKE '%' || :search || '%' 
            OR tematica LIKE '%' || :search || '%' 
            OR themeName LIKE '%' || :search || '%'
            OR :search = '')
    """)
    fun obtenerCantidad(filtro: String, search: String): Flow<Int>
}
