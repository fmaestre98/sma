package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import cu.uci.cisol.monitoreoambiental.data.local.entity.IncidenciaEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.UsuarioEntity
import androidx.room.TypeConverters

@TypeConverters(Converters::class)
@Database(
    entities = [
        UsuarioEntity::class,
        IncidenciaEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MonitoreoDatabase : RoomDatabase() {
    abstract val usuarioDao: UsuarioDao
    abstract val incidenciaDao: IncidenciaDao
}
