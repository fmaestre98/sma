package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import cu.uci.cisol.monitoreoambiental.data.local.entity.EstadoEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.IncidenciaEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.LocalidadEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.UsuarioEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.OrganizationEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.TematicaEntity
import cu.uci.cisol.monitoreoambiental.data.local.db.SystemAppearanceEntity
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@TypeConverters(Converters::class)
@Database(
    entities = [
        UsuarioEntity::class,
        IncidenciaEntity::class,
        TematicaEntity::class,
        EstadoEntity::class,
        LocalidadEntity::class,
        OrganizationEntity::class,
        SystemAppearanceEntity::class,
    ],
    version = 8,
    exportSchema = true
)
abstract class MonitoreoDatabase : RoomDatabase() {
    abstract val usuarioDao: UsuarioDao
    abstract val incidenciaDao: IncidenciaDao
    abstract val tematicaDao: TematicaDao
    abstract val estadoDao: EstadoDao
    abstract val localidadDao: LocalidadDao
    abstract val organizationDao: OrganizationDao
    abstract val systemAppearanceDao: SystemAppearanceDao

    companion object {
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN country TEXT")
                db.execSQL("ALTER TABLE usuarios ADD COLUMN ci TEXT")
                db.execSQL("ALTER TABLE usuarios ADD COLUMN passport TEXT")
            }
        }
    }
}
