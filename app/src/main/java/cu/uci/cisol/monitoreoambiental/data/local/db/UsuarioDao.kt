package cu.uci.cisol.monitoreoambiental.data.local.db

import androidx.room.*
import cu.uci.cisol.monitoreoambiental.data.local.entity.UsuarioEntity

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuarios LIMIT 1") fun obtener(): UsuarioEntity?
    @Query("SELECT * FROM usuarios LIMIT 1") fun obtenerFlow(): kotlinx.coroutines.flow.Flow<UsuarioEntity?>

    @Upsert fun guardar(usuario: UsuarioEntity)

    @Query("DELETE FROM usuarios") fun eliminar()
}
