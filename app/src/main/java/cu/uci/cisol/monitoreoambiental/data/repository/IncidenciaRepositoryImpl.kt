package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.data.local.db.IncidenciaDao
import cu.uci.cisol.monitoreoambiental.data.local.entity.IncidenciaEntity
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class IncidenciaRepositoryImpl(private val incidenciaDao: IncidenciaDao) : IncidenciaRepository {

    override suspend fun guardar(incidencia: Incidencia): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { incidenciaDao.insertar(incidencia.toEntity()) }
    }

    override fun observarTodas(): Flow<List<Incidencia>> {
        return incidenciaDao.observarTodas().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun obtenerPendientes(): List<Incidencia> {
        return withContext(Dispatchers.IO) {
            incidenciaDao.obtenerPendientes().map { it.toDomain() }
        }
    }

    override suspend fun marcarComoEnviada(id: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            incidenciaDao.actualizarEstado(
                id = id,
                estado = EstadoIncidencia.NUEVA.name,
                imagenResolucionPath = null,
                sincronizado = true
            )
        }
    }

    override suspend fun actualizarEstado(
        id: String,
        estado: EstadoIncidencia,
        imagenResolucionPath: String?
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val syncStatus = (estado != EstadoIncidencia.BORRADOR && estado != EstadoIncidencia.PENDIENTE)
            incidenciaDao.actualizarEstado(
                id = id,
                estado = estado.name,
                imagenResolucionPath = imagenResolucionPath,
                sincronizado = syncStatus
            )
        }
    }

    override suspend fun enviarAlServidor(incidencia: Incidencia): Result<Unit> =
            runCatching {
                // TODO: Implement Ktor network call here in the future
                // For now, simulate success
                actualizarEstado(incidencia.id, EstadoIncidencia.NUEVA, null)
            }

    override suspend fun obtenerPorId(id: String): Incidencia? {
        return withContext(Dispatchers.IO) { incidenciaDao.obtenerPorId(id)?.toDomain() }
    }

    private fun Incidencia.toEntity() =
            IncidenciaEntity(
                    id = id,
                    tematica = tematicaId,
                    descripcion = descripcion,
                    imagenesPaths = imagenesPaths,
                    imagenResolucionPath = imagenResolucionPath,
                    latitud = latitud,
                    longitud = longitud,
                    fechaHora = fechaHora,
                    estado = estado.name,
                    sincronizado = sincronizado
            )

    private fun IncidenciaEntity.toDomain() =
            Incidencia(
                    id = id,
                    tematicaId = tematica,
                    descripcion = descripcion,
                    imagenesPaths = imagenesPaths,
                    imagenResolucionPath = imagenResolucionPath,
                    latitud = latitud,
                    longitud = longitud,
                    fechaHora = fechaHora,
                    estado = enumValueOf<EstadoIncidencia>(estado),
                    sincronizado = sincronizado
            )
}
