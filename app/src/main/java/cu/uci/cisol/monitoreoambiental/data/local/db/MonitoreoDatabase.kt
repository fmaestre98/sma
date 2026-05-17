package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import cu.uci.cisol.monitoreoambiental.data.local.entity.EstadoEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.IncidenciaEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.LocalidadEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.UsuarioEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.TematicaEntity
import androidx.room.TypeConverters

@TypeConverters(Converters::class)
@Database(
    entities = [
        UsuarioEntity::class,
        IncidenciaEntity::class,
        TematicaEntity::class,
        EstadoEntity::class,
        LocalidadEntity::class,
    ],
    version = 4,
    exportSchema = true
)
abstract class MonitoreoDatabase : RoomDatabase() {
    abstract val usuarioDao: UsuarioDao
    abstract val incidenciaDao: IncidenciaDao
    abstract val tematicaDao: TematicaDao
    abstract val estadoDao: EstadoDao
    abstract val localidadDao: LocalidadDao
}
